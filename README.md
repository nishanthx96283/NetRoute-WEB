# NetRoute — Network Traffic & Routing Simulator

**Every Node Has a Path. Every Packet Has a Purpose.**

NetRoute is a Java-based network simulation application that models computer networks, calculates efficient routes, simulates packet transmission, and demonstrates how networks respond to link failures. It provides a graphical interface for building network topologies, monitoring simulation statistics, and saving network configurations and simulation history to a MySQL database.

## Table of Contents

- [Overview]
- [Features]
- [Technology Stack]
- [System Architecture]
- [Project Structure]
- [How Routing Works]
- [Database Design]
- [Prerequisites]
- [Database Setup]
- [Build and Run]
- [Using NetRoute]
- [Testing]
- [Learning Outcomes]
- [Future Enhancements]
- [Academic Information]

## Overview

Computer networks consist of interconnected devices that exchange data through communication links. Selecting an efficient path between devices is important for reliable and effective data transmission.

NetRoute provides an interactive environment to understand network topology, shortest-path routing, packet movement, link failures, and route recovery. Instead of working only with theoretical network diagrams, users can create and modify a network and observe the effects of routing decisions and failures through a graphical interface.

The application combines graph algorithms, object-oriented programming, simulation, data persistence, and visualization in a single desktop application.

## Features

### Network Topology Management
- Create and manage network nodes such as computers, routers, and servers.
- Add and remove network links.
- Configure link costs.
- Represent networks using graph data structures.
- Support directed and undirected links.
- Enable or disable links to represent network failures.

### Shortest-Path Routing
- Calculate routes using Dijkstra's shortest-path algorithm.
- Use link costs to determine efficient paths.
- Represent the network using an adjacency-list structure.
- Recalculate routes when the network topology changes.

### Packet Simulation
- Simulate packet transmission across network links.
- Run a simulation or advance it one tick at a time.
- Track packet status and simulation activity.
- Observe how packets move through the network.

### Failure and Recovery Simulation
- Simulate network link failures.
- Restore failed links.
- Observe alternative routing paths when the topology changes.
- Track the current number of failed links in simulation statistics.

### Statistics and Visualization
- Display network topology graphically.
- View routing and simulation information.
- Monitor relevant network and packet statistics.
- Randomize link costs to experiment with different routing outcomes.

### Persistence and History
- Save network topologies to a MySQL database.
- Load previously saved topologies.
- Delete saved topologies.
- Save simulation run metrics.
- Browse simulation history and inspect saved run details.
- Maintain topology records and simulation history independently.

### Desktop Interface
- Java Swing-based graphical user interface.
- Interactive topology visualization using Java2D.
- Separate pages for the main application, topology history, simulation history, and project information.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Core application logic and object-oriented design |
| Java Swing | Desktop graphical user interface |
| Java2D | Network topology visualization |
| Graph Data Structures | Network representation using adjacency lists |
| Dijkstra's Algorithm | Shortest-path routing |
| PriorityQueue | Efficient selection of the next routing candidate |
| MySQL | Persistent storage of topologies and simulation runs |
| JDBC | Communication between Java and MySQL |
| Maven | Dependency management, building, and testing |
| JUnit | Automated unit testing |

## System Architecture

NetRoute follows a modular architecture that separates the user interface, network model, routing logic, simulation engine, and database operations.

```text
+--------------------------------------+
|         Java Swing User Interface    |
| Control Panel | Topology | Statistics|
+-------------------+------------------+
                    |
                    v
+--------------------------------------+
|          Network Model               |
| Nodes | Links | Graph Representation |
+-------------------+------------------+
                    |
                    v
+--------------------------------------+
|         Routing Module               |
|       Dijkstra's Algorithm           |
+-------------------+------------------+
                    |
                    v
+--------------------------------------+
|        Simulation Engine             |
| Packet Movement | Failures | Recovery|
+-------------------+------------------+
                    |
                    v
+--------------------------------------+
|       Statistics and Persistence     |
| Metrics | Services | Repositories    |
+-------------------+------------------+
                    |
                    v
+--------------------------------------+
|             MySQL Database           |
| Topologies | Nodes | Links | Runs    |
+--------------------------------------+
```

### Major Components

- **Model:** Defines network nodes, links, packets, routes, and related entities.
- **Graph:** Manages network connectivity and adjacency-list representation.
- **Routing:** Calculates shortest paths using Dijkstra's algorithm.
- **Simulation:** Handles packet transmission, link failures, restoration, and rerouting.
- **Statistics:** Calculates and maintains simulation metrics.
- **Service and Repository:** Coordinate application operations and database persistence.
- **User Interface:** Provides interactive controls and graphical network visualization.
- **Database:** Stores network configurations and simulation history.

## Project Structure

The main Java packages are organized by responsibility.

```text
NetRoute/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── netroute/
│   │   │           ├── model/
│   │   │           ├── graph/
│   │   │           ├── routing/
│   │   │           ├── simulation/
│   │   │           ├── statistics/
│   │   │           ├── repository/
│   │   │           ├── service/
│   │   │           ├── ui/
│   │   │           └── Main.java
│   │   └── resources/
│   └── test/
├── db.properties.example
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

The structure above illustrates the main package organization. Individual files and resources may vary with the current implementation.

## How Routing Works

NetRoute uses Dijkstra's shortest-path algorithm to calculate a minimum-cost route between network nodes.

The algorithm works as follows:

1. Assign a distance of zero to the source node.
2. Assign an initial distance of infinity to the remaining nodes.
3. Select the unprocessed node with the smallest known distance.
4. Examine its outgoing links.
5. Update neighboring distances whenever a lower-cost route is discovered.
6. Repeat until the destination is reached or no reachable nodes remain.
7. Reconstruct the route using the recorded predecessor information.

A priority queue helps efficiently select the next node to process.

For a graph with non-negative edge weights, Dijkstra's algorithm is appropriate for shortest-path routing. The precise time complexity depends on the graph representation and priority-queue implementation.

When a link fails, the network's available connectivity changes. The application can recalculate the route so that packets can use an alternative path when one exists.

## Database Design

NetRoute uses MySQL to persist topology configurations and simulation history.

| Table | Purpose |
|---|---|
| `topologies` | Stores saved network topology information |
| `topology_nodes` | Stores nodes belonging to saved topologies |
| `topology_links` | Stores links and associated link information |
| `simulation_runs` | Stores simulation run metrics and history |

Topology records are associated with their nodes and links. The database schema uses relationships to maintain consistency between saved topologies and their associated data.

Simulation history preserves recorded run information independently of the active simulation. Depending on the database relationship and deletion behavior, a historical run may remain available even after its associated topology is deleted.

## Prerequisites

Install the following software before building the application:

- Java Development Kit (JDK) compatible with the project's configured Java version.
- MySQL Server.
- Git, if cloning the repository.
- A terminal or IDE such as IntelliJ IDEA or Visual Studio Code.

The repository includes a Maven wrapper, so a separate Maven installation may not be necessary.

## Database Setup

1. Install and start MySQL Server.
2. Create the database:

   ```sql
   CREATE DATABASE netroute_db;
   ```

3. Configure the database connection using the project's supported configuration mechanism.
4. Use `db.properties.example` as a reference for the expected configuration keys.
5. If the application does not initialize the schema automatically, execute the project's schema SQL file before running it.

Keep local database credentials in your private configuration file or environment variables. Do not commit real passwords or other secrets to GitHub.

The application supports the following environment-variable overrides:

- `NETROUTE_DB_URL`
- `NETROUTE_DB_USER`
- `NETROUTE_DB_PASSWORD`

Refer to the project's configuration implementation for the exact connection URL, timeout settings, and schema initialization procedure.

## Build and Run

### 1. Clone the repository

```bash
git clone https://github.com/DJKnight-Nishanth/NetRoute.git
cd NetRoute
```

### 2. Compile the application

On Windows PowerShell:

```powershell
.\mvnw.cmd compile
```

On Linux or macOS:

```bash
./mvnw compile
```

### 3. Run the tests

On Windows:

```powershell
.\mvnw.cmd test
```

On Linux or macOS:

```bash
./mvnw test
```

### 4. Launch the application

After compiling, launch the main class with the required runtime dependencies available.

For the existing Windows setup, the following command may be used if the MySQL Connector/J JAR is present at the specified path:

```powershell
java -cp "target\classes;C:\Users\<YOUR_WINDOWS_USERNAME>\.m2\repository\com\mysql\mysql-connector-j\9.1.0\mysql-connector-j-9.1.0.jar" com.netroute.Main
```

Replace the username and JAR path with the actual values on your system.

If the dependency version or path differs, update the command accordingly. Configure the database before using database-dependent features.

## Using NetRoute

1. Launch the application.
2. Inspect the initial network topology.
3. Add, remove, or modify nodes and links as required.
4. Configure link costs or randomize them to experiment with different network conditions.
5. Calculate routes between nodes.
6. Send packets and run the simulation.
7. Advance the simulation one tick at a time to inspect its progress.
8. Simulate a link failure and observe whether an alternative route is available.
9. Restore failed links and examine the resulting network state.
10. Review the displayed statistics.
11. Save a topology and revisit it through topology history.
12. Inspect saved simulation runs through simulation history.

Available actions may depend on the current network state and whether the database connection is configured correctly.

## Testing

The project includes automated tests for supported application components.

Testing should cover:

- Network node and link operations.
- Graph connectivity and topology changes.
- Shortest-path calculations.
- Packet simulation behavior.
- Link failure and restoration.
- Statistics calculations.
- Database persistence and retrieval.
- Topology and simulation history operations.
- Invalid inputs and boundary cases.

Run the test suite using:

```powershell
.\mvnw.cmd test
```

Record the actual test results from the latest test execution when preparing a project report or academic submission.

## Learning Outcomes

This project demonstrates the practical application of:

- Object-oriented programming concepts in Java.
- Graph representations and graph traversal concepts.
- Shortest-path algorithms.
- Priority queues and related data structures.
- Discrete-step simulation.
- Dynamic network failure and recovery handling.
- Database design and JDBC integration.
- Modular software architecture.
- Automated testing and version control.

## Future Enhancements

Possible future improvements include:

- Additional routing algorithms such as Bellman–Ford.
- More detailed packet queues and congestion models.
- Advanced performance analytics.
- Comparisons between multiple simulation runs.
- Exportable reports and topology diagrams.
- More realistic network delay and packet-loss models.

These are potential extensions rather than claims about currently implemented functionality.

## Academic Information

**Project:** NetRoute — Network Traffic & Routing Simulator

**Subject:** Introduction to Object Oriented Programming

**Course Code:** CSL 202

**Institution:** Indian Institute of Information Technology

**Repository:** [DJKnight-Nishanth/NetRoute](https://github.com/DJKnight-Nishanth/NetRoute)

---

*NetRoute is an academic project developed to demonstrate object-oriented programming, graph algorithms, network simulation, and database integration.*