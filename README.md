# NetRoute — Network Traffic & Routing Simulator

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Build Tool](https://img.shields.io/badge/Build-Maven%203.9-blue.svg)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0%2B%20%2F%209.0-blue.svg)](https://www.mysql.com/)
[![Testing](https://img.shields.io/badge/Tests-134%20Passing-brightgreen.svg)](https://junit.org/junit5/)
[![Academic Course](https://img.shields.io/badge/Course-CSL%20202%3A%20OOP-purple.svg)](#academic-context)

**NetRoute** is an interactive, standalone Java desktop network traffic and routing simulator. Developed as a showcase academic project for **CSL 202: Introduction to Object-Oriented Programming**, NetRoute integrates core Object-Oriented Design patterns, classic graph algorithms (Dijkstra's shortest path with a priority queue min-heap), discrete-event packet simulation, dynamic link fault injection, and persistent MySQL database storage.

---

## Table of Contents

- [1. Overview & Objectives](#1-overview--objectives)
- [2. Feature Highlights](#2-feature-highlights)
- [3. Technology Stack](#3-technology-stack)
- [4. Architecture & Design](#4-architecture--design)
- [5. Project Directory Structure](#5-project-directory-structure)
- [6. Prerequisites](#6-prerequisites)
- [7. Database Setup & Configuration](#7-database-setup--configuration)
- [8. Build & Test Instructions](#8-build--test-instructions)
- [9. Launching the Application](#9-launching-the-application)
- [10. User Guide](#10-user-guide)
- [11. Automated Test Suite](#11-automated-test-suite)
- [12. Confirmed Limitations](#12-confirmed-limitations)
- [13. Planned Enhancements](#13-planned-enhancements)
- [14. Application Screenshots](#14-application-screenshots)
- [15. Academic Context](#15-academic-context)

---

## 1. Overview & Objectives

In modern computer networking, routing protocols must dynamically compute shortest paths, accommodate link latency and bandwidth limitations, and adapt in real time to physical link failures. 

NetRoute was engineered with two overarching objectives:
1. **Network Engineering Simulation:** Provide an educational, visual sandbox where students and engineers can model custom network topologies (Computers, Routers, Servers), calculate optimal routes using Dijkstra's algorithm, transmit simulated packets through buffered FIFO queues, and inject link failures to observe immediate dynamic rerouting or packet loss.
2. **Object-Oriented Software Engineering:** Demonstrate rigorous application of Object-Oriented Programming (OOP) principles in Java 21, including the four pillars of OOP (Abstraction, Encapsulation, Inheritance, Polymorphism), clean separation of concerns across a layered architecture (Model, View, Controller/Service, Repository), event-driven listener patterns, and ACID-compliant transactional JDBC persistence.

---

## 2. Feature Highlights

### 🖥️ Interactive Topology Visualization
- Custom vector graphics canvas built with Java2D rendering (anti-aliasing, node halos, directed/undirected edge lines, cost badges, and failure indicators).
- Interactive mouse dragging: reposition any network node freely on the 2D canvas with automatic coordinate tracking and edge recalculation.
- Real-time route highlighting with glowing cyan paths and animated in-flight packet indicators.

### 🌐 Diverse Network Nodes & Links
- **Node Specialization:** Distinct models for end-user client **Computers**, routing **Routers**, and destination **Servers**, each extending an abstract base node with dedicated visual styling.
- **Configurable Links:** Weighted connections with user-defined routing costs (weights), transmission delays (simulated clock ticks), packet capacities (bandwidth limits), and directional flags (bidirectional or directed).

### 🧭 Dijkstra Shortest-Path Routing
- Pure Java implementation of Dijkstra's algorithm over an adjacency-list graph representation.
- Employs a Java `java.util.PriorityQueue` (Min-Heap) for efficient $O((V + E) \log V)$ vertex relaxation.
- Displays hop-by-hop node sequences, total accumulated routing costs, and reachability validation.

### 📦 Discrete-Event Packet Simulation
- Discrete tick-based simulation clock (`simulationTicks`) tracking packet lifecycle progression without wall-clock thread sleep drift.
- Each link features an independent FIFO `PacketQueue` with link bandwidth capacity constraints.
- Real-time tracking of packet states: `QUEUED`, `IN_TRANSIT`, `DELIVERED`, `FAILED`, and `DROPPED`.

### ⚡ Fault Injection & Dynamic Rerouting
- Deactivate any operational link with one click to simulate physical cable cuts or hardware port failures.
- When an active packet encounters a downstream link failure, the simulation engine automatically triggers an on-the-fly Dijkstra route recalculation from the packet's current hop.
- If an alternate path exists, the packet is dynamically rerouted and the reroute counter increments; if isolated, the packet is gracefully dropped.
- Restoring a link re-enables it for subsequent shortest-path calculations.

### 📊 Real-Time Metrics & Statistics
- Live tracking of packet delivery ratio (%), average transmission delay (simulated ticks), total transmitted packets, delivered packets, submission failures, and transit drops.
- Precise monitoring of network operational status: total links, active links, currently failed links, and dynamic reroute events.

### 💾 Unified Save & New Project Workflow
- **One-Click New Project:** Instantly clear nodes, links, packet queues, and simulation clocks to begin a fresh topology.
- **Unified Save Workflow:** A single `Save` action handles both new and existing networks. If a topology was previously loaded from MySQL, it updates the existing record in-place; if new, it prompts for a name and description.
- **Transactional Atomicity:** If unsaved simulation activity exists, the network topology and the simulation execution metrics are persisted together in a single atomic MySQL transaction with automatic rollback on error.

### 📂 Topology History & Simulation History
- **Saved Topologies:** Browse, inspect, load, and delete network topology snapshots stored in MySQL. Loading reconstructs the entire network graph onto the canvas.
- **Simulation Run History:** Dedicated analytics view listing all recorded simulation runs sorted newest-first (`ORDER BY run_timestamp DESC, id DESC`). Includes instant search filtering (by topology name, run ID, or topology ID) and a read-only detail inspection modal dialog.
- **History Retention Guarantee:** Deleting a saved topology permanently never erases historical simulation runs; foreign keys safely disassociate via `ON DELETE SET NULL` while preserving denormalized topology names and metrics.

---

## 3. Technology Stack

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Language** | **Java 21 (LTS)** | Strongly-typed OOP domain modeling, record types, modern collections |
| **GUI Framework** | **Java Swing & Java2D** | Desktop graphical interface, custom canvas double-buffering, vector graphics |
| **Layout Management** | `CardLayout`, `BorderLayout`, `BoxLayout` | Multi-page desktop navigation (`Landing`, `Simulator`, `Topologies`, `Run History`, `Tech Stack`, `About`) |
| **Data Structures** | `PriorityQueue`, `HashMap`, `ArrayList`, `ArrayDeque` | Graph adjacency lists, Min-Heap Dijkstra relaxation, FIFO link transmission queues |
| **Database** | **MySQL 8.0+ / 9.0** | Relational storage for topologies, nodes, links, and simulation metrics |
| **Database Driver** | **MySQL Connector/J 9.1.0** | Standard JDBC connectivity, transaction control, prepared statements |
| **Build Management** | **Apache Maven 3.9+** | Dependency resolution, test orchestration, and build lifecycle |
| **Testing Framework** | **JUnit Jupiter 5.11.4** | Automated unit and integration test suite |

---

## 4. Architecture & Design

NetRoute follows a clean multi-tier architecture that decouples user interface components from backend simulation and database persistence.

```mermaid
flowchart TD
    subgraph UI ["Presentation Layer (com.netroute.ui)"]
        Main["Main.java"] --> NetRouteFrame["NetRouteFrame"]
        NetRouteFrame --> LandingPage["LandingPage"]
        NetRouteFrame --> TopologyPanel["TopologyPanel (Java2D Canvas)"]
        NetRouteFrame --> ControlPanel["ControlPanel (User Controls)"]
        NetRouteFrame --> StatisticsPanel["StatisticsPanel (Live Metrics)"]
        NetRouteFrame --> HistoryPage["HistoryPage (Saved Topologies)"]
        NetRouteFrame --> SimHistoryPage["SimulationHistoryPage (Run History)"]
        NetRouteFrame --> TechStackPage["TechStackPage"]
        NetRouteFrame --> AboutPage["AboutPage"]
        NetRouteFrame --> Theme["Theme (Styling & Dark Palette)"]
    end

    subgraph Service ["Service Layer (com.netroute.service)"]
        StorageService["TopologyStorageService (Façade)"]
    end

    subgraph Core ["Simulation & Routing Core"]
        GraphManager["GraphManager (com.netroute.graph)"]
        Network["Network (Adjacency Graph)"]
        Router["DijkstraRouter (com.netroute.routing)"]
        Engine["SimulationEngine (com.netroute.simulation)"]
        Stats["NetworkStatistics (com.netroute.statistics)"]
        PQueue["PacketQueue (FIFO Queue)"]
    end

    subgraph Models ["Domain Models (com.netroute.model)"]
        NetworkNode["NetworkNode (Abstract)"]
        Computer["Computer"]
        RouterNode["Router"]
        Server["Server"]
        NetworkLink["NetworkLink"]
        Packet["Packet"]
        Route["Route"]
    end

    subgraph Persistence ["Persistence Layer (com.netroute.repository)"]
        DBConfig["DatabaseConfig"]
        DBConn["DatabaseConnection"]
        TopRepo["TopologyRepository"]
        SimRepo["SimulationHistoryRepository"]
    end

    subgraph Storage ["External Storage"]
        MySQL[("MySQL Database netroute_db")]
    end

    %% Relationships
    NetRouteFrame --> StorageService
    NetRouteFrame --> Engine
    NetRouteFrame --> GraphManager
    GraphManager --> Network
    Engine --> Network
    Engine --> Router
    Engine --> PQueue
    Stats --> Engine
    Stats --> Network
    Router --> Network
    Router --> Route

    NetworkNode <|-- Computer
    NetworkNode <|-- RouterNode
    NetworkNode <|-- Server
    Network --> NetworkNode
    Network --> NetworkLink

    StorageService --> TopRepo
    StorageService --> SimRepo
    TopRepo --> DBConn
    SimRepo --> DBConn
    DBConn --> DBConfig
    DBConn --> MySQL
```

### Applied OOP Pillars in NetRoute

1. **Abstraction:**
   - [`NetworkNode`](file:///D:/NetRoute/src/main/java/com/netroute/model/NetworkNode.java) defines a generic vertex contract (`id`, `name`, `x`, `y`, `getType()`), hiding hardware-level nuances.
   - Interface listeners (`ControlPanel.Listener`, `LandingPage.Listener`, `HistoryPage.Listener`) decouple UI user events from simulation processing.
2. **Encapsulation:**
   - Domain objects ([`NetworkLink`](file:///D:/NetRoute/src/main/java/com/netroute/model/NetworkLink.java), [`Packet`](file:///D:/NetRoute/src/main/java/com/netroute/model/Packet.java), [`Route`](file:///D:/NetRoute/src/main/java/com/netroute/model/Route.java)) guard internal variables with private fields, defensive validation (e.g., non-negative delays, positive link costs), and unmodifiable view returns (`Collections.unmodifiableMap`, `List.copyOf`).
3. **Inheritance:**
   - Concrete node classes ([`Computer`](file:///D:/NetRoute/src/main/java/com/netroute/model/Computer.java), [`Router`](file:///D:/NetRoute/src/main/java/com/netroute/model/Router.java), [`Server`](file:///D:/NetRoute/src/main/java/com/netroute/model/Server.java)) inherit coordinate handling and identity from `NetworkNode`.
   - UI views inherit standard Swing capabilities from `JPanel` and `JFrame`.
4. **Polymorphism:**
   - Custom Java2D paint pipelines override `paintComponent(Graphics g)` across panels.
   - The graph management layer processes `NetworkNode` references polymorphically regardless of whether the node is a workstation, core router, or datacenter server.

---

## 5. Project Directory Structure

```text
D:\NetRoute
│   .gitignore                              # Git exclusion rules (excludes target/, db.properties, etc.)
│   database-schema.sql                     # Full MySQL schema (topologies, nodes, links, simulation_runs)
│   db.properties.example                   # Template for local MySQL credentials
│   mvnw                                    # Linux/macOS Maven wrapper script
│   mvnw.cmd                                # Windows Maven wrapper batch script
│   pom.xml                                 # Maven project descriptor & dependencies
│   README.md                               # Project documentation
│
├───.mvn\wrapper                            # Maven wrapper JAR and configuration
│
└───src
    ├───main\java\com\netroute
    │   │   Main.java                       # Application entrypoint (SwingUtilities.invokeLater)
    │   │
    │   ├───graph                           # Graph representation and factory
    │   │       GraphManager.java           # Network construction, sample network, node positioning
    │   │       Network.java                # Adjacency-list graph holding nodes and links
    │   │
    │   ├───model                           # Domain entities
    │   │       Computer.java               # End-user workstation node (subclass of NetworkNode)
    │   │       NetworkLink.java            # Weighted edge (cost, delay, capacity, active status)
    │   │       NetworkNode.java            # Abstract base node (ID, label, canvas X/Y coordinates)
    │   │       NodeType.java               # Enum discriminator (COMPUTER, ROUTER, SERVER)
    │   │       Packet.java                 # Simulated packet (source, destination, route, status)
    │   │       PacketStatus.java           # Enum (QUEUED, IN_TRANSIT, DELIVERED, FAILED, DROPPED)
    │   │       Route.java                  # Ordered node sequence with accumulated cost
    │   │       Router.java                 # Intermediate routing node (subclass of NetworkNode)
    │   │       Server.java                 # Destination/host server node (subclass of NetworkNode)
    │   │
    │   ├───repository                      # JDBC database persistence layer
    │   │       DatabaseConfig.java         # Connection settings resolution (Env -> file -> defaults)
    │   │       DatabaseConnection.java     # Connection factory, ping check, diagnostic test
    │   │       DuplicateTopologyNameException.java # Unique constraint failure exception
    │   │       SavedTopology.java          # In-memory representation of loaded topology
    │   │       SimulationHistoryRepository.java # DAO for simulation_runs table
    │   │       SimulationRunRecord.java    # Immutable historical simulation snapshot
    │   │       TopologyRepository.java     # DAO for topologies, topology_nodes, topology_links
    │   │       TopologySummary.java        # Lightweight topology header for list views
    │   │       UnifiedSaveResult.java      # Result holder for atomic unified save operations
    │   │
    │   ├───routing                         # Routing algorithms
    │   │       DijkstraRouter.java         # PriorityQueue-based Dijkstra shortest-path implementation
    │   │
    │   ├───service                         # High-level application services
    │   │       TopologyStorageService.java # Persistence façade decoupling UI from JDBC
    │   │
    │   ├───simulation                      # Discrete-event simulation engine
    │   │       PacketQueue.java            # FIFO queue for link transmission buffering
    │   │       SimulationEngine.java       # Discrete tick clock, packet stepper, failure handler
    │   │
    │   ├───statistics                      # Real-time metrics calculations
    │   │       NetworkStatistics.java      # Packet delivery ratio, average delay, link status counts
    │   │
    │   └───ui                              # Swing presentation layer
    │           AboutPage.java              # Academic details and project overview
    │           ControlPanel.java           # Routing, node/link, failure, and step controls
    │           HistoryPage.java            # Saved topologies management view
    │           LandingPage.java            # Welcome dashboard and feature navigation
    │           NetRouteFrame.java          # Main window frame and CardLayout controller
    │           SimulationHistoryPage.java  # Historical simulation analytics table & details modal
    │           StatisticsPanel.java        # Live simulation telemetry panel
    │           TechStackPage.java          # OOP pillars and architecture documentation view
    │           Theme.java                  # Design system, dark palette, typography, button factory
    │           TopologyPanel.java          # Interactive Java2D network graph canvas
    │
    └───test\java\com\netroute              # Automated JUnit 5 test suites (134 tests)
        ├───graph
        │       GraphManagerTest.java
        ├───repository
        │       DatabaseConfigTest.java
        │       DatabaseConnectionIntegrationTest.java
        │       DuplicateTopologyNameExceptionTest.java
        │       FailureMetricSemanticsRegressionTest.java
        │       SimulationHistoryRepositoryTest.java
        │       TopologyReconstructionTest.java
        ├───routing
        │       DijkstraRouterTest.java
        ├───service
        │       TopologyStorageServiceTest.java
        ├───simulation
        │       SimulationEngineTest.java
        ├───statistics
        │       NetworkStatisticsTest.java
        └───ui
                HistoryPageTest.java
                NetRouteFrameTest.java
                NewProjectFeatureTest.java
                SimulationHistoryPageTest.java
                UnifiedSaveIntegrationTest.java
```

---

## 6. Prerequisites

Ensure your system meets the following verified requirements:

1. **Operating System:** Windows 10/11, macOS, or Linux with an active graphical desktop display environment.
2. **Java Development Kit (JDK):** **JDK 21 or higher** (LTS). Verify with:
   ```cmd
   java -version
   ```
3. **Build Tool:** Apache Maven 3.8+ (or use the included `mvnw.cmd` / `mvnw` wrapper). Verify with:
   ```cmd
   mvn -version
   ```
4. **Relational Database (Optional for in-memory simulation, required for persistence):**
   - **MySQL Server 8.0+ or 9.0+** running on `localhost:3306`.
   - Working MySQL Command Line Client or MySQL Workbench.

---

## 7. Database Setup & Configuration

NetRoute includes full database persistence for network topologies and simulation execution logs. If MySQL is unavailable, NetRoute continues to operate in offline mode with in-memory simulation.

### Step 1: Create Database and Tables
Open the MySQL Command Line Client or terminal and run the schema file:

```sql
source D:/NetRoute/database-schema.sql;
```

Alternatively, from the Windows command prompt:
```cmd
mysql -u root -p < D:\NetRoute\database-schema.sql
```

This creates the database `netroute_db` and initializes four normalized tables:
- `topologies`: Stores saved network headers, names, descriptions, and timestamps.
- `topology_nodes`: Stores vertices (`COMPUTER`, `ROUTER`, `SERVER`) and canvas coordinates.
- `topology_links`: Stores weighted edges with costs, delays, capacities, and active states.
- `simulation_runs`: Stores historical execution snapshots, delivered packets, delivery ratios, delays, failed links, and scenario notes.

### Step 2: Configure Local Credentials
Copy the example configuration file in the project root to create your local `db.properties`:

```cmd
copy db.properties.example db.properties
```

Open `db.properties` in a text editor and enter your local MySQL credentials:

```properties
# JDBC Connection URL
db.url=jdbc:mysql://localhost:3306/netroute_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8

# Local MySQL Authentication
db.user=root
db.password=YOUR_LOCAL_MYSQL_PASSWORD

# Timeout in seconds
db.timeout=5
```

> [!SECURITY]
> **Credential Protection:** `db.properties` is explicitly listed in `.gitignore` and will never be committed to Git. Never commit production passwords or credentials to source control.

### Alternative: Environment Variables
You can also configure the connection without editing files by setting environment variables:
- `NETROUTE_DB_URL`
- `NETROUTE_DB_USER`
- `NETROUTE_DB_PASSWORD`

---

## 8. Build & Test Instructions

NetRoute includes the Apache Maven Wrapper, allowing seamless builds on Windows without requiring a pre-installed global Maven executable.

### Compile Source Code
```cmd
.\mvnw.cmd compile
```

### Run Automated Tests
Execute the full test suite (134 tests across 14 test classes):
```cmd
.\mvnw.cmd test -B
```

### Clean & Package Standalone Application JAR
```cmd
.\mvnw.cmd clean package -DskipTests
```
This produces `target\netroute-1.0.0-SNAPSHOT.jar`.

---

## 9. Launching the Application

### Option A: Launch via Maven Exec Plugin (Recommended for Development)
The project's `pom.xml` configures the `exec-maven-plugin` with `com.netroute.Main`. Run:

```cmd
.\mvnw.cmd compile exec:java
```

This automatically resolves all runtime dependencies—including MySQL Connector/J—and launches the graphical user interface.

### Option B: Launch using the `java` Command on Windows
If launching the compiled classes directly from the command line, specify the runtime classpath containing both the application classes and the MySQL Connector/J driver:

```cmd
java -cp "target/classes;%USERPROFILE%/.m2/repository/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar" com.netroute.Main
```

*(Note: Ensure you have run `.\mvnw.cmd compile` at least once prior to running this command).*

---

## 10. User Guide

### 10.1 Getting Started & Navigation
Upon startup, NetRoute opens to the **Landing Page**. Use the top header bar to navigate between:
- **Launch Simulator:** Opens the interactive routing canvas.
- **Topologies:** Opens the Saved Network Topologies history page.
- **Run History:** Opens the Simulation Execution & Analytics history page.
- **Tech Stack:** Displays OOP principles, data structures, and architecture details.
- **About:** Displays academic course information and project objectives.

### 10.2 Designing a Network Topology
1. In the simulator, the workspace initializes with a default 6-node campus network:
   - 3 Computers (`PC1`, `PC2`, `PC3`)
   - 2 Routers (`R1`, `R2`)
   - 1 Server (`S1`)
2. **Adding a Node:**
   - Scroll down the right-hand **Control Panel** to the **Add Node** card.
   - Choose a node type: `Computer`, `Router`, or `Server`.
   - Enter a unique Node ID (e.g., `PC4`) and display name (e.g., `Lab PC 4`).
   - Click **Add Node**. The node appears on the canvas.
   - Click and drag the node anywhere on the canvas to customize your layout.
3. **Connecting Nodes with a Link:**
   - In the **Add Link** card, select the **Source** and **Destination** nodes.
   - Configure the **Cost** (routing weight $\ge 0$), **Delay** (simulated ticks $\ge 0$), and **Capacity** (packets per tick $\ge 1$).
   - Choose whether the link is bidirectional or directed.
   - Click **Add Link**. The connection renders immediately with its cost badge.

### 10.3 Calculating Shortest Paths
1. At the top of the **Control Panel**, select a **Source Node** and **Destination Node**.
2. Click **Find Shortest Route**.
3. Dijkstra's algorithm computes the path with minimum total cost:
   - The optimal path is highlighted in bright cyan on the canvas.
   - The path sequence (e.g., `PC1 → R1 → R2 → S1`) and total routing cost are displayed in the results box.

### 10.4 Simulating Packet Transmission
1. Select a Source and Destination node.
2. Click **Send Packet**. A packet with unique ID (e.g., `PKT-1`) is generated and queued on the first link of the shortest path.
3. Click **Step Simulation (1 Tick)** to advance the simulation clock by 1 tick:
   - Packets travel across edges according to each link's configured delay.
   - In-flight packet dots and countdown timers animate along the link.
   - The Activity Log logs hops, arrivals, and queue changes.
4. When a packet reaches its destination, it is marked `DELIVERED`, and latency statistics update automatically.

### 10.5 Testing Fault Injection & Dynamic Rerouting
1. In the **Link Failure & Recovery** card, select any active link from the dropdown.
2. Click **Fail Link**:
   - The link turns red and dashed, indicating an outage.
   - Any packet currently en route that required that link will detect the failure and invoke Dijkstra's algorithm to recalculate a route from its current position.
   - If an alternative path exists, the packet reroutes dynamically, and the **Reroutes** counter increments.
   - If no alternative path exists, the packet status transitions to `DROPPED`.
3. Select the failed link and click **Restore Link** to reactivate it.

### 10.6 Saving Topologies & Simulation Runs (Unified Save)
1. Click the **💾 Save** button on the top simulator navigation bar (or press `Ctrl+S`).
2. **New Topology:** Enter a unique name (e.g., `Datacenter Pod Alpha`) and optional description.
3. **Existing Loaded Topology:** NetRoute automatically updates the existing record in MySQL without prompting for duplicate names.
4. **Atomic Simulation Run Recording:** If you executed a simulation prior to clicking Save, NetRoute records the run's metrics (total packets, delivered, failed, dropped, delivery ratio, average delay, currently failed links, reroutes, and optional notes) into the `simulation_runs` table within the same atomic database transaction.

### 10.7 Starting a Fresh Network
Click the **＋ New** button at the top-left of the simulator (or press `Ctrl+N`). If you have unsaved changes, NetRoute prompts you to save or discard before wiping the workspace to a clean, empty canvas.

### 10.8 Inspecting Topology & Simulation History
- **Topologies History:** Click **📂 History** to view saved topologies. Click **Load** to reconstruct any saved network into the active canvas, or **Delete** to remove it.
- **Simulation Run History:** Click **📊 Run History** to open the analytics table.
  - Type in the **Filter** box to filter runs in real time by topology name or run ID.
  - Double-click any row (or select and click **👁 View Details**) to open a read-only modal dialog displaying complete execution telemetry, delivery statistics, and scenario notes.

---

## 11. Automated Test Suite

NetRoute features an automated test suite comprising **134 tests** with 100% pass rate.

```cmd
.\mvnw.cmd test -B
```

### Test Coverage Breakdown

| Package | Test Class | Tests | Focus Area |
| :--- | :--- | :---: | :--- |
| `com.netroute.graph` | `GraphManagerTest` | 27 | Node/link addition, removal, bidirectional adjacency, duplicate prevention, cost randomization |
| `com.netroute.routing` | `DijkstraRouterTest` | 11 | Shortest-path calculation, edge relaxation, unreachable nodes, directed vs undirected paths |
| `com.netroute.simulation` | `SimulationEngineTest` | 9 | Packet queuing, step progression, delivery verification, transit drops, capacity limits |
| `com.netroute.statistics` | `NetworkStatisticsTest` | 12 | Delivery ratio formulas, average delay math, active/inactive link counts, reroute counters |
| `com.netroute.repository` | `DatabaseConfigTest` | 4 | Priority resolution (Environment Variables $\to$ `db.properties` $\to$ Defaults) |
| `com.netroute.repository` | `DatabaseConnectionIntegrationTest` | 2 | Live MySQL connection ping and database metadata retrieval |
| `com.netroute.repository` | `DuplicateTopologyNameExceptionTest` | 1 | Unique constraint validation on duplicate topology names |
| `com.netroute.repository` | `TopologyReconstructionTest` | 1 | Full serialization and deserialization of network graphs from database |
| `com.netroute.repository` | `SimulationHistoryRepositoryTest` | 3 | Query ordering (`run_timestamp DESC, id DESC`), metric mapping, retention on topology deletion |
| `com.netroute.repository` | `FailureMetricSemanticsRegressionTest` | 3 | Regression test verifying that saved Failures metric equals currently failed links across all lifecycle transitions |
| `com.netroute.service` | `TopologyStorageServiceTest` | 6 | Storage façade delegating to repositories, offline fallback handling |
| `com.netroute.ui` | `HistoryPageTest` | 5 | Saved topology card rendering, empty state banners, offline error handling |
| `com.netroute.ui` | `SimulationHistoryPageTest` | 8 | Run table rendering, live search filtering, detail modal inspection, zero-mutation guarantee |
| `com.netroute.ui` | `NewProjectFeatureTest` | 8 | Workspace wipe, engine clock reset, unsaved changes confirmation dialogs |
| `com.netroute.ui` | `UnifiedSaveIntegrationTest` | 9 | Single-transaction topology + run saves, rollback verification, overwrite logic |
| `com.netroute.ui` | `NetRouteFrameTest` | 25 | End-to-end UI callbacks, menu items, card switching, fail/restore event workflows |
| **Total** | **16 Test Suites** | **134** | **100% Passing (0 Failures, 0 Errors, 0 Skipped)** |

---

## 12. Confirmed Limitations

Based on direct inspection of the codebase:
- **Discrete Tick Resolution:** Simulation time advances in discrete integer units (`simulationTicks`). There is no continuous fractional millisecond timing model.
- **Single-Packet UI Submission:** The GUI control panel submits one packet per click. Continuous high-volume packet streams are stepped tick-by-tick.
- **Desktop Environment Requirement:** The UI relies on Java Swing and Java2D; it requires a graphical desktop session (tests automatically detect headless environments and execute headless-safe assertions).
- **Single Database Host:** The repository layer is configured for a single MySQL database instance as specified in `db.properties`.

---

## 13. Planned Enhancements

The following features represent architectural enhancements planned for subsequent phases:
- **Phase 5.3 Multi-Run Analytics:** Comparative side-by-side run diffing, graphical delivery ratio trend lines over time, and export of historical runs to CSV/JSON format.
- **Automated Traffic Generator:** Configurable Poisson or burst packet generators to simulate high-concurrency peak loads.
- **Additional Routing Protocols:** Implementation of Distance-Vector (Bellman-Ford) routing to compare convergence times against Dijkstra link-state routing.

---

## 14. Application Screenshots

*Screenshots can be placed in a `docs/screenshots/` folder.*

| Screen | Description | Placeholder |
| :--- | :--- | :--- |
| **Landing Page** | NetRoute welcome screen and portal navigation | `docs/screenshots/01_landing_page.png` |
| **Network Simulator** | Interactive canvas with highlighted Dijkstra path and control sidebar | `docs/screenshots/02_simulator_canvas.png` |
| **Fault Injection** | Dynamic rerouting during a simulated link failure | `docs/screenshots/03_link_failure_reroute.png` |
| **Topology History** | Saved topologies page displaying cards and load controls | `docs/screenshots/04_topology_history.png` |
| **Simulation History** | Historical simulation runs table, search filter, and detail modal | `docs/screenshots/05_simulation_history.png` |

---

## 15. Academic Context

- **Course:** CSL 202: Introduction to Object-Oriented Programming
- **Project Title:** NetRoute — Network Traffic & Routing Simulator
- **Platform:** Java Desktop Application (Java 21 LTS, Swing, Java2D, Maven, MySQL)
- **Primary Pillars Evaluated:** Abstraction, Encapsulation, Inheritance, Polymorphism, Data Structures, Relational Persistence.
