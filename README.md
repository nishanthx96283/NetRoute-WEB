# NetRoute — Network Traffic & Routing Simulator (Web & Desktop)

**Every Node Has a Path. Every Packet Has a Purpose.**

NetRoute is a high-performance network simulation platform designed to model computer networks, compute optimal paths using Dijkstra's algorithm, simulate discrete-event packet delivery with queuing and link capacity constraints, evaluate dynamic fault recovery under link failures, and persist network topologies and analytics into a relational MySQL database.

Originally built as a Java Swing desktop application, **NetRoute Web** provides a modern, responsive browser-based web edition powered by a lightweight Java 21 backend (`com.sun.net.httpserver.HttpServer` with Virtual Threads) and an interactive HTML5 Canvas visualizer, without using heavy frameworks like Spring Boot, React, or Angular.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture & Technology Stack](#architecture--technology-stack)
- [Project Structure](#project-structure)
- [How Routing Works](#how-routing-works)
- [Discrete-Event Simulation Engine](#discrete-event-simulation-engine)
- [Database Design & Retention Policy](#database-design--retention-policy)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Build and Run](#build-and-run)
  - [Running the Web Application (Default)](#running-the-web-application-default)
  - [Running the Original Desktop Application](#running-the-original-desktop-application)
- [REST API Specification](#rest-api-specification)
- [Testing & Verification](#testing--verification)
- [Academic Information](#academic-information)
- [License](#license)

---

## Overview

Computer networks rely on efficient routing protocols and resilient topologies to maintain reliable data flow during outages and network congestion. NetRoute bridges theoretical network algorithms and practical experimentation by combining:

1. **Shortest-Path Pathfinding**: Minimum-cost route discovery via Dijkstra's algorithm on weighted adjacency lists with deterministic tie-breaking.
2. **Discrete-Event Simulation**: Deterministic tick-based packet transit modeling FIFO queuing, hop delays, and link capacities.
3. **Resilience & Dynamic Fault Recovery**: Automatic rerouting of live in-flight packets when links fail, and packet drop tracking when no alternative route exists.
4. **Relational Persistence**: Transactional MySQL persistence with unified atomic saves for topologies and simulation run analytics.
5. **Dual Presentation Modes**:
   - **Web Application**: Accessible via any modern web browser at `http://localhost:8080` with an interactive HTML5 Canvas, responsive controls, and real-time telemetry.
   - **Desktop Application**: The original Java Swing / Java2D desktop client remains 100% intact and functional.

---

## Key Features

### 1. Interactive Network Visualization
- HTML5 Canvas visualizer with support for high-DPI (Retina) displays.
- Visually distinct node types: Computers (tech blue), Routers (teal), Servers (purple).
- Links with weight, delay, and capacity badges.
- Active Dijkstra routes highlighted with an electric cyan glow.
- Failed links rendered with distinct red dashed patterns and warning badges.
- Smooth mouse drag-and-drop repositioning of nodes, pan, and zoom controls (0.4x - 2.5x).
- Real-time animated packet particles traveling along active hops during simulation ticks.

### 2. Algorithmic Routing (Dijkstra)
- Evaluates shortest paths strictly based on routing costs.
- Automatically ignores inactive/failed links.
- Uses `PriorityQueue` min-heaps with deterministic secondary tie-breaking on node IDs and link IDs.

### 3. Discrete-Event Packet Simulator
- Integer tick-based clock without wall-clock sleeps or socket overhead.
- FIFO packet queues per link with capacity constraints ($\max$ packets dispatched per tick).
- Step-by-step tick advancement or "Run to Idle" execution.
- Tracking across full packet lifecycles (`CREATED` $\to$ `QUEUED` $\to$ `IN_TRANSIT` $\to$ `DELIVERED` / `FAILED` / `DROPPED`).

### 4. Dynamic Fault Recovery & Resilience
- Instant link severance (`failLink`) and restoration (`restoreLink`).
- In-transit packets affected by link failure are immediately rerouted from their current vertex using Dijkstra.
- Packets are dropped if no alternative path exists to the destination.
- Semantics: The failures metric accurately reflects the number of currently inactive links at any point in time.

### 5. Topology & Analytics Persistence
- Save custom topologies to MySQL `netroute_db`.
- Load saved topologies and reconstruct live network graphs in memory.
- Unified atomic saves: Save topology updates and simulation run metrics in a single database transaction.
- Browse, filter, and inspect historical simulation run experiments.

---

## Architecture & Technology Stack

```text
+---------------------------------------------------------------------------------+
|                          FRONTEND (Browser Client)                              |
|   HTML5  |  CSS3 (Slate Dark Theme #0B1118)  |  Vanilla ES6 JavaScript          |
|   - Interactive HTML5 Canvas visualizer (pan, zoom, node drag, packet anim)     |
|   - Real-time Telemetry Dashboard (delivery ratio, delay ticks, reroutes)       |
|   - Navigation Views: Simulator, Saved Topologies, History, Tech Stack, About   |
+----------------------------------------+----------------------------------------+
                                         |  HTTP / REST (JSON)
                                         v
+---------------------------------------------------------------------------------+
|                        BACKEND (Java 21 Web Server)                             |
|   com.sun.net.httpserver.HttpServer  |  Java 21 Virtual Threads  |  Google Gson |
|   - StaticFileHandler: Serves frontend assets securely with MIME types          |
|   - NetworkHandler: Topology mutations, node positioning, link management       |
|   - RoutingHandler: Dijkstra route calculations                                 |
|   - SimulationHandler: Packet submission, tick stepping, link failure/restore   |
|   - StatisticsHandler: Real-time telemetry calculations                         |
|   - HistoryHandler: Topologies and simulation runs CRUD with MySQL              |
+----------------------------------------+----------------------------------------+
                                         |  Calls Existing Services
                                         v
+---------------------------------------------------------------------------------+
|                     REUSED CORE BUSINESS LOGIC (100% Pure Java)                 |
|   - com.netroute.graph.Network & GraphManager                                   |
|   - com.netroute.routing.DijkstraRouter                                         |
|   - com.netroute.simulation.SimulationEngine & PacketQueue                      |
|   - com.netroute.statistics.NetworkStatistics                                  |
|   - com.netroute.service.TopologyStorageService                                 |
|   - com.netroute.repository.* (JDBC Repositories)                              |
+----------------------------------------+----------------------------------------+
                                         |  JDBC (MySQL Connector/J 9.1.0)
                                         v
+---------------------------------------------------------------------------------+
|                           MySQL DATABASE (netroute_db)                          |
|   topologies  |  topology_nodes  |  topology_links  |  simulation_runs          |
+---------------------------------------------------------------------------------+
```

### Technology Breakdown

| Component | Technology | Purpose |
|---|---|---|
| **Language** | Java 21 | Core algorithms, data structures, and web server |
| **Concurrency** | Virtual Threads | High-throughput concurrent HTTP request handling |
| **Web Server** | JDK `HttpServer` | Built-in zero-dependency HTTP server (port 8080) |
| **Serialization**| Google Gson 2.11.0 | Fast, standard JSON serialization |
| **Frontend UI** | HTML5, CSS3, Vanilla JS | Clean Single-Page Application (no React/Vue) |
| **Visualization**| HTML5 Canvas 2D | Responsive technical network graph visualizer |
| **Database** | MySQL 8.0+ / 9.0+ | Persistent storage for topologies and history |
| **Connectivity**| JDBC (MySQL Connector/J)| Type-safe relational communication and transactions |
| **Build & Test**| Maven Wrapper & JUnit 5 | Build automation and unit/integration testing |

---

## Project Structure

```text
D:\NetRoute-Web/
├── .mvn/wrapper/                     # Maven wrapper
├── database-schema.sql               # MySQL relational schema DDL
├── db.properties.example             # Database configuration template
├── pom.xml                           # Project Object Model
├── README.md                         # Documentation
├── src/
│   ├── main/
│   │   ├── java/com/netroute/
│   │   │   ├── Main.java             # Desktop Swing launcher
│   │   │   ├── model/                # Nodes, Links, Packets, Routes
│   │   │   ├── graph/                # Network adjacency list & GraphManager
│   │   │   ├── routing/              # DijkstraRouter implementation
│   │   │   ├── simulation/           # SimulationEngine & PacketQueue
│   │   │   ├── statistics/           # NetworkStatistics calculations
│   │   │   ├── repository/           # JDBC DAOs & DatabaseConnection
│   │   │   ├── service/              # TopologyStorageService application façade
│   │   │   ├── ui/                   # Original Swing components (untouched)
│   │   │   └── web/                  # Web Server & REST API Handlers
│   │   │       ├── WebMain.java      # Web application entry point
│   │   │       ├── WebServer.java    # Embedded HTTP server setup
│   │   │       ├── WebApplicationState.java # Centralized thread-safe state
│   │   │       ├── StaticFileHandler.java   # Secure asset server
│   │   │       ├── HttpUtils.java    # HTTP & JSON utility methods
│   │   │       ├── dto/              # JSON data transfer records
│   │   │       └── handler/          # REST API route handlers
│   │   └── resources/
│   │       └── static/               # Frontend assets
│   │           ├── index.html        # Main HTML dashboard
│   │           ├── css/style.css     # Slate dark styling
│   │           └── js/
│   │               ├── api.js        # REST API client
│   │               ├── canvas.js     # HTML5 Canvas visualizer
│   │               └── app.js        # View controller & event wiring
│   └── test/
│       └── java/com/netroute/        # 152 unit & integration tests
```

---

## How Routing Works

1. Nodes maintain outgoing edges in an adjacency list.
2. Inactive links (`isActive() == false`) are ignored during candidate traversal.
3. Candidate distances are maintained in a min-heap priority queue ordered by accumulated routing cost.
4. Equal-cost paths use deterministic tie-breaking:
   - Prefer lower alphabetical node ID.
   - If still tied, prefer lower alphabetical link ID.
5. Reconstructs an immutable `Route` object with nodes, links, and total cost.

---

## Discrete-Event Simulation Engine

- Time advances in discrete integer ticks ($t = 0, 1, 2, \dots$).
- When a packet is submitted, an initial Dijkstra route is assigned.
- At each tick:
  1. **Complete Arrivals**: Checks in-flight transmissions where `arrivalTick <= time`. If destination is reached, packet is marked `DELIVERED`. Otherwise, enqueues to next link.
  2. **Start Transmissions**: For each link, polls queued packets up to the link's `capacity` and computes hop duration:
     $$\text{duration} = \max(1, \lceil \text{link.getDelay()} \rceil) \text{ ticks}$$
- **Link Failure**: When `failLink` is invoked:
  - Affected live packets are halted and rerouted via Dijkstra from their current vertex.
  - If no path exists, status transitions to `DROPPED`.

---

## Database Design & Retention Policy

NetRoute connects to MySQL database `netroute_db`:

1. `topologies`: Header record for saved topologies with unique name constraint.
2. `topology_nodes`: Vertices with canvas coordinates `(pos_x, pos_y)`. Cascades on topology deletion.
3. `topology_links`: Weighted edges with cost, delay, capacity, active state. Cascades on topology deletion.
4. `simulation_runs`: Historical execution analytics.
   - **Retention Policy**: `topology_id` is foreign-keyed with `ON DELETE SET NULL`, while `topology_name` is stored denormalized. Deleting a saved topology will **never erase** historical simulation analytics.

---

## Prerequisites

- **Java Development Kit (JDK) 21** or higher.
- **MySQL Server 8.0+ / 9.0+** (optional for in-memory simulation; required for database persistence).
- A modern web browser (Chrome, Edge, Firefox, Safari).

---

## Database Setup

1. Start MySQL Server.
2. Create the database:
   ```sql
   CREATE DATABASE netroute_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Initialize the schema using the provided DDL:
   ```bash
   mysql -u root -p netroute_db < database-schema.sql
   ```
4. Copy `db.properties.example` to `db.properties` in the project root and add your MySQL credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/netroute_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.user=root
   db.password=your_password_here
   db.timeout=5
   ```
   *(Alternatively, set environment variables `NETROUTE_DB_URL`, `NETROUTE_DB_USER`, `NETROUTE_DB_PASSWORD`).*

---

## Build and Run

### Running the Web Application (Default)

Compile and launch NetRoute Web using the Maven Wrapper:

```bash
.\mvnw.cmd compile exec:java
```

Or run via Java directly:
```bash
.\mvnw.cmd compile
java -cp "target/classes;target/dependency/*" com.netroute.web.WebMain
```

Once started, open your web browser and navigate to:

👉 **`http://localhost:8080`**

### Running the Original Desktop Application

The original Java Swing desktop interface remains completely intact and can be launched at any time:

```bash
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.netroute.Main"
```

---

## REST API Specification

| Endpoint | Method | Description |
|---|---|---|
| `/api/network` | `GET` | Snapshot of all nodes, links, and project metadata |
| `/api/network/reset` | `POST` | Reset in-memory graph to default 6-node campus network |
| `/api/network/new-project` | `POST` | Clear canvas for a new blank topology project |
| `/api/network/randomize-costs` | `POST` | Randomize all link costs between 1 and 10 |
| `/api/network/node` | `POST` | Add node (`COMPUTER`, `ROUTER`, `SERVER`) |
| `/api/network/node` | `DELETE`| Remove node and its incident links by `?id=` |
| `/api/network/node/position` | `POST` | Update node coordinates `(x, y)` |
| `/api/network/link` | `POST` | Connect two nodes with cost, delay, and capacity |
| `/api/network/link` | `DELETE`| Delete link by `?id=` |
| `/api/route` | `POST` | Calculate Dijkstra route `{sourceId, destinationId}` |
| `/api/simulation/state` | `GET` | Get simulation clock, active packets, and queue sizes |
| `/api/simulation/packet` | `POST` | Submit packet `{sourceId, destinationId, payloadSize}` |
| `/api/simulation/step` | `POST` | Advance simulation by 1 tick |
| `/api/simulation/run` | `POST` | Run simulation loop until all packets are idle |
| `/api/simulation/reset` | `POST` | Reset simulation engine for current network |
| `/api/simulation/fail-link` | `POST` | Mark link inactive, triggering live packet rerouting |
| `/api/simulation/restore-link`| `POST`| Restore failed link to active operational status |
| `/api/statistics` | `GET` | Current network and packet delivery statistics |
| `/api/history/topologies` | `GET` | List saved topologies from MySQL |
| `/api/history/topologies/save`| `POST`| Atomically persist topology and optional simulation run |
| `/api/history/topologies/load`| `POST`| Load topology by ID into the active simulation |
| `/api/history/topologies` | `DELETE`| Delete or archive saved topology by `?id=` |
| `/api/history/runs` | `GET` | List historical simulation run analytics |
| `/api/db/status` | `GET` | Health check of MySQL connection and response times |

---

## Testing & Verification

Run the full automated test suite:

```bash
.\mvnw.cmd test -B
```

### Test Suite Summary
- **152 Total Tests** across 24 test classes.
- **150 Tests Passing**, **2 Skipped** (live database integration tests only execute when a local MySQL instance is reachable).
- 100% of core algorithmic, simulation, routing, repository, and web REST API tests pass cleanly with 0 failures or errors.

---

## Academic Information

- **Subject:** Introduction to Object Oriented Programming
- **Course Code:** CSL 202
- **Institution:** Indian Institute of Information Technology
- **Project Name:** NetRoute — Network Traffic & Routing Simulator
- **Tagline:** *Every Node Has a Path. Every Packet Has a Purpose.*

---

## License

This project is licensed under the MIT License.
