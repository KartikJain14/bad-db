# 🔥 AI IDE PROMPT — BUILD EXTENSIBLE CORE DATABASE ENGINE (JAVA 21)

You are a senior database systems engineer.

You are designing a **real, extensible embedded database engine core** in **Java 21**, without third-party libraries.

This is NOT a serialization project.
This must architecturally resemble a real database engine.

The output will later be consumed by a custom query parser and GUI.

---

# 🔷 HARD CONSTRAINTS

* Java 21
* No third-party libraries
* Standard library only
* Clean OOP design
* No god classes
* No procedural dumping
* Clear layering
* Designed for extensibility

The final result must look like a small SDK.

---

# 🔷 DESIGN GOALS

Build a **core database engine** that includes:

* Page-based storage
* Slotted page layout
* Buffer manager
* Disk manager
* System catalog
* CRUD operations
* Basic transactions
* Write-ahead logging (WAL)
* Crash recovery (simplified)
* Index abstraction with B+ tree skeleton
* Clean API surface for future parser

Single-threaded engine is acceptable.

---

# 🔷 STORAGE LAYER (MANDATORY)

## 1️⃣ Page-Based File Structure

* Database stored in a single `.db` file
* Fixed-size pages (e.g., 4096 bytes)
* Each page has:

  * Page ID
  * Page type
  * Free space pointer
  * Slot directory (for variable-length records)

Use a **Slotted Page Layout**:

* Records stored from bottom upward
* Slot directory grows from top downward
* Supports variable-length strings

Implement:

* Page (abstract)
* SlottedPage
* PageType enum
* RID (pageId + slotId)

---

## 2️⃣ Disk Manager

Responsible for:

* Reading pages from disk
* Writing pages to disk
* Allocating new pages
* Managing page IDs

No business logic here.

---

## 3️⃣ Buffer Manager

* In-memory page cache
* Map<PageId, Page>
* Dirty page tracking
* Flush mechanism
* No need for full LRU (simple strategy acceptable)

All engine operations must go through buffer manager.

---

# 🔷 CATALOG SYSTEM

Implement system catalog stored in reserved pages.

Catalog must persist:

* Table metadata
* Schema (columns, data types, constraints)
* Root page ID of table
* Root page ID of index

Classes:

* Schema
* Column
* TableMetadata
* CatalogManager

Catalog must survive restart.

---

# 🔷 DATA TYPES

Support:

* INT
* FLOAT
* BOOLEAN
* STRING (variable-length)

Include:

* NOT NULL constraint
* PRIMARY KEY metadata

Primary key index must be supported via index layer.

---

# 🔷 TABLE & RECORD OPERATIONS

Expose clean SDK-level API:

```java
Database db = new Database("mydb");
db.createTable("users", schema);

Transaction tx = db.beginTransaction();

RID rid = db.insert(tx, "users", record);
db.update(tx, "users", rid, newValues);
db.delete(tx, "users", rid);

Iterator<Record> it = db.scan(tx, "users");
List<Record> results = db.search(tx, "users", predicate);

db.commit(tx);
```

Required operations:

* createTable
* insert
* update
* delete (tombstone allowed)
* scan
* search (linear scan acceptable initially)

All operations must require a Transaction object.

---

# 🔷 TRANSACTION & ACID (Lite but Real)

Implement:

## Transaction Manager

* beginTransaction()
* commit()
* rollback()

## Write-Ahead Logging (WAL)

* Separate `.wal` file
* Log before modifying any page
* Log must contain:

  * Transaction ID
  * Operation type
  * Before image
  * After image

## Atomicity

* On crash, replay committed transactions
* Undo uncommitted transactions

## Durability

* WAL flushed before commit returns

Isolation:

* Single-threaded engine acceptable
* Document assumption

Consistency:

* Enforce schema constraints
* Enforce NOT NULL
* Primary key uniqueness if index exists

---

# 🔷 INDEX LAYER (Lite but Architecturally Real)

Implement:

* Index interface
* BPlusTree class (minimal but structurally correct)
* Node abstraction
* LeafNode
* InternalNode

Required:

* insert(key, RID)
* search(key)

Primary key should optionally use this index.

Even if simplified, architecture must resemble real B+ tree.

---

# 🔷 LAYERED PACKAGE STRUCTURE

Design packages like:

```
engine/
storage/
buffer/
catalog/
transaction/
index/
common/
```

Parser will only interact with:

* Database
* Transaction
* Table API

No other internal classes exposed.

---

# 🔷 EXTENSIBILITY REQUIREMENTS

Design must allow:

* Adding secondary indexes later
* Adding concurrency later
* Adding query planner later
* Adding optimizer later

Avoid tightly coupling layers.

Use interfaces where appropriate.

---

# 🔷 CODING STANDARDS

* Clear documentation comments
* Explain page layout clearly
* Explain WAL logic clearly
* Explain recovery process clearly
* No magic numbers
* Constants centralized
* No static global state abuse

---

# 🔷 MINIMAL DEMONSTRATION

Include a minimal `main()` that:

* Creates database
* Creates table
* Begins transaction
* Inserts records
* Commits
* Scans records
* Demonstrates rollback scenario

No GUI.
No SQL parser.

---

# 🔷 IMPORTANT

This must resemble a real database engine skeleton.

It must NOT degrade into:

* Sequential file append
* Simple object serialization
* Map stored in file
* CSV-like structure

Architecture correctness > feature completeness.

---

Produce full Java source code structured cleanly.

Document assumptions clearly.