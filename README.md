# Bad-DB: An Educational Database Engine

Bad-DB is a minimal, educational relational database engine written in Java 21 from scratch. It avoids third-party libraries to demonstrate the core architectural principles of how modern databases (like SQLite or Postgres) handle storage, persistence, and ACID transactions.

## 🚀 Key Features

*   **Page-Based Storage**: A custom disk manager that organizes data into fixed-size 4KB blocks.
*   **Buffer Pool**: A memory-resident page cache that reduces disk I/O.
*   **Slotted Pages**: Flexible record storage layout allowing for variable-length rows and efficient deletion.
*   **System Catalog**: Persistent metadata and schema management stored in the database's "root" page (ID 0).
*   **ACID Transactions**: Support for Atomicity, Consistency, Isolation, and Durability.
*   **Write-Ahead Logging (WAL)**: All modifications are logged to a persistent journal before being applied, ensuring durability.
*   **ARIES-Style Recovery**: Automated database recovery after an unexpected crash by replaying the WAL.
*   **B+ Tree Indexing**: Skeleton implementation for accelerated record lookups by key.

## 🛠️ Components

1.  **Storage Layer** (`org.baddb.storage`): Manages direct file I/O and page-level formatting.
2.  **Buffer Layer** (`org.baddb.buffer`): Implements the page cache and flushing policy.
3.  **Catalog Layer** (`org.baddb.catalog`): Handles table definitions, schemas, and persistence of metadata.
4.  **Transaction Layer** (`org.baddb.transaction`): Manages the WAL, commit/rollback logic, and crash recovery.
5.  **Engine Layer** (`org.baddb.engine`): The high-level API for creating tables, inserting, and scanning records.

## 📁 Getting Started

### Prerequisites
*   Java 21 or higher.
*   No external dependencies required.

### Exploring the Code
*   Check out [USAGE.md](./USAGE.md) for a quick guide on how to interact with the engine.
*   Read [INTERNALS.md](./INTERNALS.md) for a deep dive into the architecture and design decisions.
*   Run the provided `Demo.java` to see the engine in action.

---

*Note: Bad-DB is intended for educational purposes only. It prioritizes clarity and simplicity over high performance or production-ready features (like query optimization or advanced concurrency).*
