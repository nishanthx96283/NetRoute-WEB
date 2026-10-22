package com.netroute.service;

import com.netroute.graph.Network;
import com.netroute.repository.DatabaseConnection;
import com.netroute.repository.DuplicateTopologyNameException;
import com.netroute.repository.SavedTopology;
import com.netroute.repository.SimulationHistoryRepository;
import com.netroute.repository.SimulationRunRecord;
import com.netroute.repository.TopologyRepository;
import com.netroute.repository.TopologySummary;
import com.netroute.statistics.NetworkStatistics;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * High-level application façade for database persistence, topology management,
 * and simulation analytics logging.
 * <p>
 * Decouples Swing UI and future controllers from raw SQL statements and JDBC details.
 */
public class TopologyStorageService {

    private final DatabaseConnection databaseConnection;
    private final TopologyRepository topologyRepository;
    private final SimulationHistoryRepository historyRepository;

    public TopologyStorageService() {
        this(new DatabaseConnection());
    }

    public TopologyStorageService(DatabaseConnection databaseConnection) {
        this.databaseConnection = Objects.requireNonNull(databaseConnection, "databaseConnection is required");
        this.topologyRepository = new TopologyRepository(databaseConnection);
        this.historyRepository = new SimulationHistoryRepository(databaseConnection);
    }

    public TopologyStorageService(
            DatabaseConnection databaseConnection,
            TopologyRepository topologyRepository,
            SimulationHistoryRepository historyRepository) {
        this.databaseConnection = Objects.requireNonNull(databaseConnection, "databaseConnection is required");
        this.topologyRepository = Objects.requireNonNull(topologyRepository, "topologyRepository is required");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository is required");
    }

    /**
     * Checks if the database is configured and currently reachable.
     */
    public boolean isDatabaseAvailable() {
        return databaseConnection.isAvailable();
    }

    /**
     * Diagnostic connection test returning response times and server versions.
     */
    public DatabaseConnection.ConnectionResult testConnection() {
        return databaseConnection.testConnection();
    }

    /**
     * Saves the current network state to the database.
     *
     * @param name non-empty unique topology name
     * @param description optional description
     * @param network network to serialize
     * @param overwrite whether to overwrite if name already exists
     * @return generated or updated topology ID
     * @throws DuplicateTopologyNameException if name exists and overwrite is false
     * @throws SQLException on database failure
     */
    public int saveTopology(String name, String description, Network network, boolean overwrite)
            throws SQLException, DuplicateTopologyNameException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Topology name cannot be empty.");
        }
        if (network == null || network.nodeCount() == 0) {
            throw new IllegalArgumentException("Cannot save an empty network.");
        }
        return topologyRepository.saveTopology(name.trim(), description, network, overwrite);
    }

    /**
     * Loads a saved topology and reconstructs its complete {@link Network} graph.
     */
    public Optional<SavedTopology> loadTopology(int topologyId) throws SQLException {
        return topologyRepository.loadTopology(topologyId);
    }

    /**
     * Loads a saved topology by its unique name.
     */
    public Optional<SavedTopology> loadTopologyByName(String name) throws SQLException {
        return topologyRepository.loadTopologyByName(name);
    }

    /**
     * Lists active (non-archived) saved topologies.
     */
    public List<TopologySummary> listActiveTopologies() throws SQLException {
        return topologyRepository.listTopologies(false);
    }

    /**
     * Lists all saved topologies, including archived ones.
     */
    public List<TopologySummary> listAllTopologies(boolean includeArchived) throws SQLException {
        return topologyRepository.listTopologies(includeArchived);
    }

    /**
     * Checks if a topology with the given name already exists.
     */
    public boolean existsByName(String name) throws SQLException {
        return topologyRepository.existsByName(name);
    }

    /**
     * Soft-deletes a topology (sets is_archived = true).
     */
    public boolean archiveTopology(int topologyId) throws SQLException {
        return topologyRepository.archiveTopology(topologyId);
    }

    /**
     * Restores an archived topology.
     */
    public boolean restoreTopology(int topologyId) throws SQLException {
        return topologyRepository.restoreTopology(topologyId);
    }

    /**
     * Permanently deletes a topology.
     * Note: Does NOT delete simulation runs; their foreign keys become NULL while retaining historical metrics.
     */
    public boolean deleteTopologyPermanently(int topologyId) throws SQLException {
        return topologyRepository.deleteTopologyPermanently(topologyId);
    }

    /**
     * Records a simulation run snapshot for future analytics.
     */
    public int recordSimulationRun(
            Integer topologyId,
            String topologyName,
            NetworkStatistics stats,
            long simulationTicks,
            String notes) throws SQLException {
        return historyRepository.recordSimulationRun(topologyId, topologyName, stats, simulationTicks, notes);
    }

    /**
     * Lists simulation history for a specific topology.
     */
    public List<SimulationRunRecord> listRunsForTopology(int topologyId) throws SQLException {
        return historyRepository.listRunsForTopology(topologyId);
    }

    /**
     * Lists the most recent simulation runs across all topologies.
     */
    public List<SimulationRunRecord> listRecentRuns(int limit) throws SQLException {
        return historyRepository.listRecentRuns(limit);
    }

    /**
     * Retrieves a single simulation run record by its ID.
     */
    public Optional<SimulationRunRecord> getRunById(int runId) throws SQLException {
        return historyRepository.getRunById(runId);
    }

    /**
     * Deletes a specific simulation run record.
     */
    public boolean deleteRun(int runId) throws SQLException {
        return historyRepository.deleteRun(runId);
    }
}
