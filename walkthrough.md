# Walkthrough: Core Database Engine (Java 21)

I have implemented a real, page-based embedded database engine core in Java 21. The engine follows a layered architecture resembling production database systems like SQLite or PostgreSQL.

## Core Components

### 📦 Storage Layer
- **[Constants.java](file:///d:/repos/bad-db/src/main/java/org/baddb/common/Constants.java)**: Centralized configuration (4096 byte pages, header offsets).
- **[DiskManager.java](file:///d:/repos/bad-db/src/main/java/org/baddb/storage/DiskManager.java)**: Handles raw I/O using `RandomAccessFile`.
- **[SlottedPage.java](file:///d:/repos/bad-db/src/main/java/org/baddb/storage/SlottedPage.java)**: Implements the slotted page layout, allowing variable-length records to be stored efficiently.

### 🧠 Buffer & Catalog
- **[BufferManager.java](file:///d:/repos/bad-db/src/main/java/org/baddb/buffer/BufferManager.java)**: Caches pages in memory and manages dirty page flushing.
- **[CatalogManager.java](file:///d:/repos/bad-db/src/main/java/org/baddb/catalog/CatalogManager.java)**: Manages table metadata and schemas, persisting them in a reserved catalog page (Page 0).

### ⚡ Transactions & ACID
- **[TransactionManager.java](file:///d:/repos/bad-db/src/main/java/org/baddb/transaction/TransactionManager.java)**: Coordinates `BEGIN`, `COMMIT`, and `ROLLBACK`.
- **[WALManager.java](file:///d:/repos/bad-db/src/main/java/org/baddb/transaction/WALManager.java)**: Implements Write-Ahead Logging for durability and atomicity.
- **[RecoveryManager.java](file:///d:/repos/bad-db/src/main/java/org/baddb/transaction/RecoveryManager.java)**: Replays the WAL on startup to redo committed transactions and undo uncommitted ones.

### 🚀 SDK API
- **[Database.java](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Database.java)**: The high-level entry point for the engine.
- **[Table.java](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Table.java)**: Provides CRUD operations ([insert](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Table.java#26-52), [get](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Record.java#21-24), [update](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Table.java#60-72), [delete](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Table.java#73-79), [scan](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Table.java#80-96), [search](file:///d:/repos/bad-db/src/main/java/org/baddb/engine/Table.java#97-108)) and handles automatic paging.

## Demo Results

The implementation was verified using **[Demo.java](file:///d:/repos/bad-db/src/main/java/org/baddb/Demo.java)**.

### Output
```text
Creating table 'users'...
Started transaction 1
Committed transaction.
Scanning users:
 - 1: Alice (30)
 - 2: Bob (25)
Demonstrating rollback...
Rolled back transaction.
Charlie found after rollback? false
```

### Key Features Demonstrated:
- **Persistence**: Data is stored in `testdb.db`.
- **ACID properties**: Rollback successfully reverts in-memory and on-disk changes.
- **Variable-length records**: Strings and primitives are packed into slotted pages.
- **Auto-paging**: The system automatically allocates new pages when existing ones are full.
