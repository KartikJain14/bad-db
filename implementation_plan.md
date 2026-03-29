# Implementation Plan: Extensible Database Engine (Java 21)

This plan outlines the architecture for a real, page-based embedded database engine core.

## Proposed Changes

### Storage Layer
- Create `org.baddb.storage.DiskManager`: Manages the `.db` file using `RandomAccessFile`.
- Create `org.baddb.storage.Page`: Abstract base with header management (PageId, Type, FreeSpace).
- Create `org.baddb.storage.SlottedPage`: Implements slotted layout for variable-length records.
- Create `org.baddb.common.RID`: Record identifier (pageId:int, slotId:int).

### Buffer Layer
- Create `org.baddb.buffer.BufferManager`: Manages a pool of pages, dirty flags, and flushing.

### Catalog Layer
- Create `org.baddb.catalog.Schema`, `Column`, and `DataType`.
- Create `org.baddb.catalog.CatalogManager`: Persists system metadata (table name to root page mapping).

### Transaction & WAL
- Create `org.baddb.transaction.Transaction`: Maintains tx context (isolation level, state).
- Create `org.baddb.transaction.WALManager`: Logs modifications to a `.wal` file.
- Create `org.baddb.transaction.LogRecord`: Types for Insert, Update, Delete, Commit, Abort.
- Implement Recovery: Replay WAL on startup.

### Engine API
- Create `org.baddb.engine.Database`: Entry point for starting the engine.
- Create `org.baddb.engine.Table`: High-level CRUD operations.

### Index Layer
- Create `org.baddb.index.Index` interface.
- Create `org.baddb.index.BPlusTree`: Minimal skeleton for primary key indexing.

## Verification Plan

### Automated Tests
- Unit tests for `SlottedPage` serialization.
- Buffer manager eviction and dirty page flushing.
- WAL recovery test: crash before commit and verify state.

### Manual Verification
- Run a demo `main()` method showing table creation, record insertion, and transaction rollback.
