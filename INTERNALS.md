# Bad-DB: Internal Architecture & Design

This document explains the technical foundations and architectural layers of Bad-DB.

## 🏗️ Architectural Layers

Bad-DB follows a classic database engine design, with layers from low-level storage to high-level query APIs.

### 1. Storage Layer (`org.baddb.storage`)

The storage layer is responsible for translating logical "page" requests into physical disk I/O.

*   **Fixed-Size Pages**: All data is stored in the database file as a series of 4KB pages. This matches the native block size of most disk drives, optimizing device throughput.
*   **DiskManager**: Uses `java.io.RandomAccessFile` in "rw" mode. It provides direct access by page ID (calculated as `offset = pageId * 4096`).
*   **Page Header**: Every page contains a 13-byte header at its start, which stores:
    *   **PageID (4 bytes)**: Unique identifier for the page.
    *   **PageType (1 byte)**: Indicates if it's a Slotted page, Catalog page, or Index node.
    *   **FreeSpacePointer (2 bytes)**: Offset indicating where the next record should be written.
    *   **SlotCount (2 bytes)**: Total number of records currently stored in the page.
    *   **NextPageId (4 bytes)**: Linkage to the next page in a chain (Heap File).

### 2. Slotted Page Layout (`SlottedPage.java`)

To handle variable-length records, Bad-DB uses a **Slotted-Page** architecture:
*   **Slot Directory**: Grows from the header downwards (top-to-bottom). Each slot is 4 bytes (2 for offset, 2 for size).
*   **Records**: Grow from the end of the page upwards (bottom-to-top).
*   **Free Space**: The gap between the slot directory and the record data.

This design permits O(1) access to records via an `RID(pageId, slotId)` and allows records to be deleted (marked as tombstones) or updated without shifting all other data in the page.

### 3. Buffer Pool Layer (`org.baddb.buffer`)

Reading/writing to disk is expensive. The `BufferManager` keeps a subset of pages in memory (`pageCache`).

*   **Current Strategy**: Simple "flush-all on capacity" for maximum clarity. When the cache hits 100 pages, all dirty pages are flushed to disk before new ones are added.
*   **Dirty Flag**: Each `Page` object tracks whether it has been modified. Only dirty pages are written to disk during a flush.

### 4. System Catalog & Metadata (`org.baddb.catalog`)

Bad-DB stores its own metadata internally.
*   **Bootstrapping**: **Page 0** is reserved for the database catalog. 
*   **Serialization**: Table schemas (`TableMetadata`) are serialized using standard Java Serialization and stored as records in Page 0.
*   **Heap File Entry**: Each entry in the catalog contains the `rootPageId`, which is the head of the page chain for that table.

### 5. Transaction & WAL Layer (`org.baddb.transaction`)

To ensure durability, Bad-DB implements a **Write-Ahead Log (WAL)**.

*   **WAL Protocol**: Every update to a page is first logged to the `.wal` file as a `LogRecord` before the memory modification occurs.
*   **Before/After Images**: Each log record for an update operation contains the full 4KB image of the page BEFORE and AFTER the change.
    *   **Undo**: The *before-image* allows the engine to revert (rollback) uncommitted changes.
    *   **Redo**: The *after-image* allows the engine to replay (recover) committed changes that weren't yet flushed to disk.

#### 🚀 Crash Recovery (ARIES-style)
When the database starts, `RecoveryManager` performs a two-pass recovery:
1.  **Analysis Pass**: Scans the WAL to identify which transactions ended with a `COMMIT` record.
2.  **Redo/Undo Pass**: 
    *   Redoes `afterImage` for any transaction that has a `COMMIT`.
    *   Undoes (reverts to `beforeImage`) for any transaction that was still `ACTIVE` when the crash occurred.

### 6. Indexing Skeleton (`org.baddb.index`)

Bad-DB includes a basic **B+ Tree Index** implementation (`BPlusTree.java`).
*   **Self-Balancing**: Leaf nodes store keys and their associated `RID`s.
*   **Sorted Order**: Keeps data sorted for fast range queries and unique lookups.

---

### 🔥 Philosophy of Design

*   **No Third-Party Libraries**: Forces a deep understanding of standard Java features (`ByteBuffer`, `RandomAccessFile`, `DataOutputStream`).
*   **Clean Separation**: Each package has a distinct responsibility (e.g., storage doesn't know about transactions).
*   **Educational Simplicity**: Decisions like "flush-all" and "in-place updates only" are made to keep the logic readable while still covering the fundamental concepts of database engineering.
