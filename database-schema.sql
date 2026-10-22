-- ============================================================================
-- NetRoute Network Traffic & Routing Simulator
-- Database Schema for MySQL 8.0+ / 9.0+
-- Database: netroute_db
-- ============================================================================

-- Step 1: Create Database
CREATE DATABASE IF NOT EXISTS netroute_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE netroute_db;

-- ----------------------------------------------------------------------------
-- Table 1: topologies
-- Header record for saved network topologies.
-- Supports unique naming, soft-archival (is_archived), and metadata tracking.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS topologies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    node_count INT NOT NULL DEFAULT 0,
    link_count INT NOT NULL DEFAULT 0,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_topology_name UNIQUE (name)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 2: topology_nodes
-- Vertices belonging to a saved topology.
-- Stores ID, display name, node type discriminator, and 2D canvas coordinates.
-- Cascades on topology deletion.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS topology_nodes (
    topology_id INT NOT NULL,
    node_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    node_type ENUM('COMPUTER', 'ROUTER', 'SERVER') NOT NULL,
    pos_x DOUBLE NOT NULL,
    pos_y DOUBLE NOT NULL,
    PRIMARY KEY (topology_id, node_id),
    CONSTRAINT fk_nodes_topology
        FOREIGN KEY (topology_id) REFERENCES topologies(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 3: topology_links
-- Weighted edges belonging to a saved topology.
-- Stores link ID, endpoints, routing cost, simulated delay, capacity,
-- directionality, and active/inactive operational status.
-- Cascades on topology or endpoint node deletion.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS topology_links (
    topology_id INT NOT NULL,
    link_id VARCHAR(50) NOT NULL,
    source_node_id VARCHAR(50) NOT NULL,
    destination_node_id VARCHAR(50) NOT NULL,
    cost DOUBLE NOT NULL,
    delay DOUBLE NOT NULL,
    capacity INT NOT NULL DEFAULT 4,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_directed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (topology_id, link_id),
    CONSTRAINT fk_links_topology
        FOREIGN KEY (topology_id) REFERENCES topologies(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_links_source_node
        FOREIGN KEY (topology_id, source_node_id) REFERENCES topology_nodes(topology_id, node_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_links_dest_node
        FOREIGN KEY (topology_id, destination_node_id) REFERENCES topology_nodes(topology_id, node_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_links_positive_cost
        CHECK (cost > 0.0),
    CONSTRAINT chk_links_non_negative_delay
        CHECK (delay >= 0.0),
    CONSTRAINT chk_links_min_capacity
        CHECK (capacity >= 1)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 4: simulation_runs
-- Historical performance and delivery analytics from simulation runs.
-- Retention Policy:
--   - topology_id is nullable with ON DELETE SET NULL.
--   - topology_name is stored denormalized at run time.
--   - Deleting a saved topology will NEVER erase historical simulation runs.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS simulation_runs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    topology_id INT NULL,
    topology_name VARCHAR(100) NOT NULL,
    run_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    simulation_ticks BIGINT NOT NULL DEFAULT 0,
    total_packets INT NOT NULL DEFAULT 0,
    delivered_packets INT NOT NULL DEFAULT 0,
    failed_packets INT NOT NULL DEFAULT 0,
    dropped_packets INT NOT NULL DEFAULT 0,
    delivery_ratio DOUBLE NOT NULL DEFAULT 0.0,
    avg_delivery_delay DOUBLE NOT NULL DEFAULT 0.0,
    link_failures INT NOT NULL DEFAULT 0,
    reroutes INT NOT NULL DEFAULT 0,
    notes VARCHAR(255) NULL,
    CONSTRAINT fk_runs_topology
        FOREIGN KEY (topology_id) REFERENCES topologies(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Indexes for query optimization
-- ----------------------------------------------------------------------------
CREATE INDEX idx_topologies_updated ON topologies(updated_at DESC);
CREATE INDEX idx_topologies_archived ON topologies(is_archived, updated_at DESC);
CREATE INDEX idx_runs_topology_time ON simulation_runs(topology_id, run_timestamp DESC);
CREATE INDEX idx_runs_timestamp ON simulation_runs(run_timestamp DESC);
