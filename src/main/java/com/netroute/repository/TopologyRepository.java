package com.netroute.repository;

import com.netroute.graph.Network;
import com.netroute.model.Computer;
import com.netroute.model.NetworkLink;
import com.netroute.model.NetworkNode;
import com.netroute.model.NodeType;
import com.netroute.model.Router;
import com.netroute.model.Server;

import com.netroute.statistics.NetworkStatistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC Data Access Object for persisting and reconstructing network topologies.
 * <p>
 * Key design features:
 * <ul>
 *   <li>Atomic transactions with rollback on failure to prevent partial saves.</li>
 *   <li>Unified save workflow coordinating topology update/creation and simulation metrics on a single connection.</li>
 *   <li>Duplicate name handling supporting Cancel, Rename, or explicit Overwrite.</li>
 *   <li>Full graph reconstruction (node types, names, coordinates, link costs, delays, capacities, active state).</li>
 *   <li>Both soft-archival and permanent deletion policies.</li>
 * </ul>
 */
public class TopologyRepository {

    private final DatabaseConnection databaseConnection;
    private final SimulationHistoryRepository historyRepository;

    public TopologyRepository(DatabaseConnection databaseConnection) {
        this(databaseConnection, new SimulationHistoryRepository(databaseConnection));
    }

    public TopologyRepository(DatabaseConnection databaseConnection, SimulationHistoryRepository historyRepository) {
        this.databaseConnection = Objects.requireNonNull(databaseConnection, "databaseConnection is required");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository is required");
    }

    /**
     * Saves a network topology into MySQL within a single atomic transaction.
     * Backwards-compatible convenience method delegating to {@link #saveUnified}.
     *
     * @param name unique name for the topology
     * @param description optional notes or summary
     * @param network the in-memory graph to save
     * @param overwriteIfExists if true, updates existing topology; if false, throws {@link DuplicateTopologyNameException}
     * @return generated or updated topology ID
     * @throws DuplicateTopologyNameException if name already exists and overwriteIfExists is false
     * @throws SQLException on database error
     */
    public int saveTopology(String name, String description, Network network, boolean overwriteIfExists)
            throws SQLException, DuplicateTopologyNameException {
        UnifiedSaveResult result = saveUnified(null, name, description, network, overwriteIfExists, null, 0, null);
        return result.getTopologyId();
    }

    /**
     * Executes a unified save workflow within a single atomic transaction on a single JDBC connection.
     * <p>
     * Supports:
     * <ul>
     *   <li>Overwriting a previously loaded topology by ID (Case A)</li>
     *   <li>Saving a new topology or resolving name conflicts (Case B)</li>
     *   <li>Persisting optional simulation run analytics along with the topology</li>
     * </ul>
     *
     * @param topologyId optional ID of an existing loaded topology to overwrite (nullable)
     * @param name display name for the topology (required)
     * @param description optional description
     * @param network in-memory network graph containing nodes and links (required)
     * @param overwriteIfExists whether to overwrite an existing topology if name conflicts
     * @param statsToRecord optional simulation statistics to persist alongside the topology (nullable)
     * @param simulationTicks simulation ticks counter for the run
     * @param runNotes optional notes for the simulation run
     * @return {@link UnifiedSaveResult} detailing the saved topology and simulation run ID
     * @throws DuplicateTopologyNameException if name already exists under a different ID and overwrite is false
     * @throws SQLException on database error
     */
    public UnifiedSaveResult saveUnified(
            Integer topologyId,
            String name,
            String description,
            Network network,
            boolean overwriteIfExists,
            NetworkStatistics statsToRecord,
            long simulationTicks,
            String runNotes) throws SQLException, DuplicateTopologyNameException {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(network, "network is required");

        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Topology name cannot be empty");
        }
        if (network.nodeCount() == 0) {
            throw new IllegalArgumentException("Cannot save an empty network");
        }

        try (Connection conn = databaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                int targetTopologyId;
                String targetTopologyName;
                boolean created;

                if (topologyId != null && topologyId > 0) {
                    // Case A: Overwrite existing topology by ID
                    Optional<TopologySummary> existingById = findSummaryByIdInternal(conn, topologyId);
                    if (existingById.isEmpty()) {
                        throw new SQLException("Topology with ID " + topologyId + " was not found in the database.");
                    }

                    // Check if renaming to a name already used by a DIFFERENT topology
                    Optional<TopologySummary> existingByName = findSummaryByNameInternal(conn, trimmedName);
                    if (existingByName.isPresent() && existingByName.get().getId() != topologyId) {
                        if (!overwriteIfExists) {
                            conn.rollback();
                            throw new DuplicateTopologyNameException(trimmedName, existingByName.get().getId());
                        }
                    }

                    targetTopologyId = topologyId;
                    targetTopologyName = trimmedName;
                    created = false;

                    String sqlUpdate = "UPDATE topologies SET name = ?, description = ?, node_count = ?, link_count = ?, "
                            + "is_archived = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                        psUpdate.setString(1, targetTopologyName);
                        psUpdate.setString(2, description == null ? "" : description.trim());
                        psUpdate.setInt(3, network.nodeCount());
                        psUpdate.setInt(4, network.linkCount());
                        psUpdate.setInt(5, targetTopologyId);
                        psUpdate.executeUpdate();
                    }

                    // Remove old links and nodes before re-inserting
                    try (PreparedStatement psDelLinks = conn.prepareStatement("DELETE FROM topology_links WHERE topology_id = ?")) {
                        psDelLinks.setInt(1, targetTopologyId);
                        psDelLinks.executeUpdate();
                    }
                    try (PreparedStatement psDelNodes = conn.prepareStatement("DELETE FROM topology_nodes WHERE topology_id = ?")) {
                        psDelNodes.setInt(1, targetTopologyId);
                        psDelNodes.executeUpdate();
                    }
                } else {
                    // Case B: New topology or overwrite by name
                    Optional<TopologySummary> existing = findSummaryByNameInternal(conn, trimmedName);
                    if (existing.isPresent()) {
                        if (!overwriteIfExists) {
                            conn.rollback();
                            throw new DuplicateTopologyNameException(trimmedName, existing.get().getId());
                        }
                        targetTopologyId = existing.get().getId();
                        targetTopologyName = trimmedName;
                        created = false;

                        String sqlUpdate = "UPDATE topologies SET description = ?, node_count = ?, link_count = ?, "
                                + "is_archived = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                        try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                            psUpdate.setString(1, description == null ? "" : description.trim());
                            psUpdate.setInt(2, network.nodeCount());
                            psUpdate.setInt(3, network.linkCount());
                            psUpdate.setInt(4, targetTopologyId);
                            psUpdate.executeUpdate();
                        }

                        // Remove old links and nodes before re-inserting
                        try (PreparedStatement psDelLinks = conn.prepareStatement("DELETE FROM topology_links WHERE topology_id = ?")) {
                            psDelLinks.setInt(1, targetTopologyId);
                            psDelLinks.executeUpdate();
                        }
                        try (PreparedStatement psDelNodes = conn.prepareStatement("DELETE FROM topology_nodes WHERE topology_id = ?")) {
                            psDelNodes.setInt(1, targetTopologyId);
                            psDelNodes.executeUpdate();
                        }
                    } else {
                        // Insert new topology header
                        String sqlInsert = "INSERT INTO topologies (name, description, node_count, link_count, is_archived) "
                                + "VALUES (?, ?, ?, ?, FALSE)";
                        try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                            psInsert.setString(1, trimmedName);
                            psInsert.setString(2, description == null ? "" : description.trim());
                            psInsert.setInt(3, network.nodeCount());
                            psInsert.setInt(4, network.linkCount());
                            psInsert.executeUpdate();

                            try (ResultSet rsKeys = psInsert.getGeneratedKeys()) {
                                if (!rsKeys.next()) {
                                    throw new SQLException("Failed to obtain generated topology ID.");
                                }
                                targetTopologyId = rsKeys.getInt(1);
                            }
                        }
                        targetTopologyName = trimmedName;
                        created = true;
                    }
                }

                // Batch insert all nodes
                String sqlNode = "INSERT INTO topology_nodes (topology_id, node_id, name, node_type, pos_x, pos_y) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psNode = conn.prepareStatement(sqlNode)) {
                    for (NetworkNode node : network.getNodes().values()) {
                        psNode.setInt(1, targetTopologyId);
                        psNode.setString(2, node.getId());
                        psNode.setString(3, node.getName());
                        psNode.setString(4, node.getType().name());
                        psNode.setDouble(5, node.getX());
                        psNode.setDouble(6, node.getY());
                        psNode.addBatch();
                    }
                    psNode.executeBatch();
                }

                // Batch insert all links
                String sqlLink = "INSERT INTO topology_links (topology_id, link_id, source_node_id, destination_node_id, "
                        + "cost, delay, capacity, is_active, is_directed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psLink = conn.prepareStatement(sqlLink)) {
                    for (NetworkLink link : network.getLinks().values()) {
                        psLink.setInt(1, targetTopologyId);
                        psLink.setString(2, link.getId());
                        psLink.setString(3, link.getSourceId());
                        psLink.setString(4, link.getDestinationId());
                        psLink.setDouble(5, link.getCost());
                        psLink.setDouble(6, link.getDelay());
                        psLink.setInt(7, link.getCapacity());
                        psLink.setBoolean(8, link.isActive());
                        psLink.setBoolean(9, link.isDirected());
                        psLink.addBatch();
                    }
                    psLink.executeBatch();
                }

                // Simulation run persistence within the SAME transaction
                Integer simulationRunId = null;
                if (statsToRecord != null) {
                    int currentlyFailedLinks = (network != null)
                            ? network.countInactiveLinks()
                            : statsToRecord.getCurrentlyFailedLinks();
                    simulationRunId = historyRepository.recordSimulationRunInternal(
                            conn,
                            targetTopologyId,
                            targetTopologyName,
                            statsToRecord,
                            currentlyFailedLinks,
                            simulationTicks,
                            runNotes
                    );
                }

                conn.commit();
                return new UnifiedSaveResult(targetTopologyId, targetTopologyName, created, simulationRunId);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * Reconstructs a full {@link SavedTopology} (including its {@link Network} graph) by ID.
     */
    public Optional<SavedTopology> loadTopology(int topologyId) throws SQLException {
        try (Connection conn = databaseConnection.getConnection()) {
            Optional<TopologySummary> summaryOpt = findSummaryByIdInternal(conn, topologyId);
            if (summaryOpt.isEmpty()) {
                return Optional.empty();
            }
            TopologySummary summary = summaryOpt.get();
            Network network = new Network();

            // Reconstruct nodes
            String sqlNodes = "SELECT node_id, name, node_type, pos_x, pos_y FROM topology_nodes "
                    + "WHERE topology_id = ? ORDER BY node_id ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlNodes)) {
                ps.setInt(1, topologyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id = rs.getString("node_id");
                        String name = rs.getString("name");
                        NodeType type = NodeType.valueOf(rs.getString("node_type"));
                        double x = rs.getDouble("pos_x");
                        double y = rs.getDouble("pos_y");

                        NetworkNode node = switch (type) {
                            case COMPUTER -> new Computer(id, name, x, y);
                            case ROUTER -> new Router(id, name, x, y);
                            case SERVER -> new Server(id, name, x, y);
                        };
                        network.addNode(node);
                    }
                }
            }

            // Reconstruct links
            String sqlLinks = "SELECT link_id, source_node_id, destination_node_id, cost, delay, capacity, "
                    + "is_active, is_directed FROM topology_links WHERE topology_id = ? ORDER BY link_id ASC";
            try (PreparedStatement ps = conn.prepareStatement(sqlLinks)) {
                ps.setInt(1, topologyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String linkId = rs.getString("link_id");
                        String src = rs.getString("source_node_id");
                        String dst = rs.getString("destination_node_id");
                        double cost = rs.getDouble("cost");
                        double delay = rs.getDouble("delay");
                        int capacity = rs.getInt("capacity");
                        boolean active = rs.getBoolean("is_active");
                        boolean directed = rs.getBoolean("is_directed");

                        NetworkLink link = new NetworkLink(linkId, src, dst, cost, delay, capacity, directed);
                        link.setActive(active);
                        network.addLink(link);
                    }
                }
            }

            return Optional.of(new SavedTopology(summary, network));
        }
    }

    /**
     * Loads a topology by exact name.
     */
    public Optional<SavedTopology> loadTopologyByName(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try (Connection conn = databaseConnection.getConnection()) {
            Optional<TopologySummary> summary = findSummaryByNameInternal(conn, name.trim());
            if (summary.isEmpty()) {
                return Optional.empty();
            }
            return loadTopology(summary.get().getId());
        }
    }

    /**
     * Lists all saved topologies.
     *
     * @param includeArchived whether to include soft-archived topologies
     */
    public List<TopologySummary> listTopologies(boolean includeArchived) throws SQLException {
        String sql = "SELECT id, name, description, node_count, link_count, is_archived, created_at, updated_at "
                + "FROM topologies "
                + (includeArchived ? "" : "WHERE is_archived = FALSE ")
                + "ORDER BY updated_at DESC";

        List<TopologySummary> list = new ArrayList<>();
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapSummary(rs));
            }
        }
        return list;
    }

    /**
     * Checks whether a topology with the given name exists.
     */
    public boolean existsByName(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            return false;
        }
        try (Connection conn = databaseConnection.getConnection()) {
            return findSummaryByNameInternal(conn, name.trim()).isPresent();
        }
    }

    /**
     * Soft-deletes a topology by setting is_archived = true.
     */
    public boolean archiveTopology(int topologyId) throws SQLException {
        String sql = "UPDATE topologies SET is_archived = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topologyId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Restores an archived topology.
     */
    public boolean restoreTopology(int topologyId) throws SQLException {
        String sql = "UPDATE topologies SET is_archived = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topologyId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Permanently deletes a topology and its nodes/links.
     * Note: Associated simulation runs have their topology_id set to NULL to preserve simulation history.
     */
    public boolean deleteTopologyPermanently(int topologyId) throws SQLException {
        String sql = "DELETE FROM topologies WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topologyId);
            return ps.executeUpdate() > 0;
        }
    }

    private Optional<TopologySummary> findSummaryByIdInternal(Connection conn, int topologyId) throws SQLException {
        String sql = "SELECT id, name, description, node_count, link_count, is_archived, created_at, updated_at "
                + "FROM topologies WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topologyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSummary(rs));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<TopologySummary> findSummaryByNameInternal(Connection conn, String name) throws SQLException {
        String sql = "SELECT id, name, description, node_count, link_count, is_archived, created_at, updated_at "
                + "FROM topologies WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapSummary(rs));
                }
            }
        }
        return Optional.empty();
    }

    private TopologySummary mapSummary(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        int nodeCount = rs.getInt("node_count");
        int linkCount = rs.getInt("link_count");
        boolean archived = rs.getBoolean("is_archived");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        LocalDateTime created = createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updated = updatedTs != null ? updatedTs.toLocalDateTime() : created;

        return new TopologySummary(id, name, desc, nodeCount, linkCount, archived, created, updated);
    }
}
