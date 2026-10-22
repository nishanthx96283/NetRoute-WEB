package com.netroute.repository;

import com.netroute.statistics.NetworkStatistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC Data Access Object for recording and querying simulation execution analytics.
 * <p>
 * Retention Policy:
 * Historical runs retain their denormalized {@code topologyName} even if the underlying
 * topology is permanently removed.
 */
public class SimulationHistoryRepository {

    private final DatabaseConnection databaseConnection;

    public SimulationHistoryRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = Objects.requireNonNull(databaseConnection, "databaseConnection is required");
    }

    /**
     * Records a historical snapshot of simulation performance.
     *
     * @param topologyId optional ID of the topology on which the simulation ran (nullable)
     * @param topologyName display name of the topology at the time of execution
     * @param stats current network statistics object
     * @param simulationTicks current simulation clock tick counter
     * @param notes optional run description or scenario notes
     * @return generated simulation run ID
     */
    public int recordSimulationRun(
            Integer topologyId,
            String topologyName,
            NetworkStatistics stats,
            long simulationTicks,
            String notes) throws SQLException {
        Objects.requireNonNull(topologyName, "topologyName is required");
        Objects.requireNonNull(stats, "stats is required");

        String sql = "INSERT INTO simulation_runs (topology_id, topology_name, simulation_ticks, "
                + "total_packets, delivered_packets, failed_packets, dropped_packets, "
                + "delivery_ratio, avg_delivery_delay, link_failures, reroutes, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (topologyId != null && topologyId > 0) {
                ps.setInt(1, topologyId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, topologyName.trim());
            ps.setLong(3, simulationTicks);
            ps.setInt(4, stats.getTotalPackets());
            ps.setInt(5, stats.getDeliveredCount());
            ps.setInt(6, stats.getFailedCount());
            ps.setInt(7, stats.getDroppedCount());
            ps.setDouble(8, stats.getDeliveryRatio());
            ps.setDouble(9, stats.getAverageDeliveryDelay());
            ps.setInt(10, stats.getLinkFailureCount());
            ps.setInt(11, stats.getTotalReroutes());
            ps.setString(12, notes == null ? "" : notes.trim());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Failed to obtain generated simulation run ID.");
            }
        }
    }

    /**
     * Retrieves all recorded simulation runs for a specific topology.
     */
    public List<SimulationRunRecord> listRunsForTopology(int topologyId) throws SQLException {
        String sql = "SELECT id, topology_id, topology_name, run_timestamp, simulation_ticks, "
                + "total_packets, delivered_packets, failed_packets, dropped_packets, "
                + "delivery_ratio, avg_delivery_delay, link_failures, reroutes, notes "
                + "FROM simulation_runs WHERE topology_id = ? ORDER BY run_timestamp DESC";

        List<SimulationRunRecord> list = new ArrayList<>();
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topologyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRun(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves the most recent simulation runs across all topologies.
     */
    public List<SimulationRunRecord> listRecentRuns(int limit) throws SQLException {
        int max = Math.max(1, limit);
        String sql = "SELECT id, topology_id, topology_name, run_timestamp, simulation_ticks, "
                + "total_packets, delivered_packets, failed_packets, dropped_packets, "
                + "delivery_ratio, avg_delivery_delay, link_failures, reroutes, notes "
                + "FROM simulation_runs ORDER BY run_timestamp DESC LIMIT ?";

        List<SimulationRunRecord> list = new ArrayList<>();
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, max);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRun(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves a single simulation run by its ID.
     */
    public Optional<SimulationRunRecord> getRunById(int runId) throws SQLException {
        String sql = "SELECT id, topology_id, topology_name, run_timestamp, simulation_ticks, "
                + "total_packets, delivered_packets, failed_packets, dropped_packets, "
                + "delivery_ratio, avg_delivery_delay, link_failures, reroutes, notes "
                + "FROM simulation_runs WHERE id = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRun(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Deletes a specific simulation run record.
     */
    public boolean deleteRun(int runId) throws SQLException {
        String sql = "DELETE FROM simulation_runs WHERE id = ?";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            return ps.executeUpdate() > 0;
        }
    }

    private SimulationRunRecord mapRun(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int topIdVal = rs.getInt("topology_id");
        Integer topologyId = rs.wasNull() ? null : topIdVal;
        String name = rs.getString("topology_name");
        Timestamp ts = rs.getTimestamp("run_timestamp");
        LocalDateTime runTime = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        long ticks = rs.getLong("simulation_ticks");
        int total = rs.getInt("total_packets");
        int delivered = rs.getInt("delivered_packets");
        int failed = rs.getInt("failed_packets");
        int dropped = rs.getInt("dropped_packets");
        double ratio = rs.getDouble("delivery_ratio");
        double delay = rs.getDouble("avg_delivery_delay");
        int failures = rs.getInt("link_failures");
        int reroutes = rs.getInt("reroutes");
        String notes = rs.getString("notes");

        return new SimulationRunRecord(
                id, topologyId, name, runTime, ticks, total, delivered, failed, dropped,
                ratio, delay, failures, reroutes, notes
        );
    }
}
