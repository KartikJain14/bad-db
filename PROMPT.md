You are a senior database systems engineer tasked with building a **real, educational database engine core in Java 21**, designed as an SDK that will later be consumed by a custom query language parser.

This is NOT a serialization project.
This is NOT a CSV clone.
This must resemble a **real database engine architecture** at a simplified scale.

---

## 🔷 Project Context

This is a university OOP project.
Constraints:

* Java 21
* No third-party dependencies
* Standard library only
* Clean, production-style architecture
* Extensible design (future parser + GUI)
* Strong OOP usage
* Clear separation of concerns

The result must look like a lightweight embedded database engine similar in architectural spirit to SQLite (but vastly simplified).

---

# 🔷 Core Requirements — ENGINE LEVEL

## 1️⃣ Page-Based Storage (MANDATORY)

DO NOT store rows sequentially using DataOutputStream append.

Instead:

* Use fixed-size pages (e.g., 4KB per page)
* Store pages inside a single `.db` file per database
* Implement:

  * Page abstraction
  * Page ID
  * Page header (page type, number of slots, free space offset)
  * Slotted page layout for variable-length records

Records must:

* Be stored inside pages
* Have record identifiers (RID = pageId + slotId)
* Support variable-length fields

---

## 2️⃣ Catalog & Metadata System

Implement a system catalog that stores:

* Table metadata
* Schema definitions
* Root page of table
* Root page of index (if exists)

This should be stored inside reserved pages in the database file.

---

## 3️⃣ Schema Model

Support:

* INT
* FLOAT
* BOOLEAN
* STRING (variable-length)
* NOT NULL
* PRIMARY KEY

Schema must be stored persistently in catalog pages.

---

## 4️⃣ Record Operations (REAL ONES)

Engine API must support:

* createTable(name, schema)
* insert(table, record)
* update(table, rid, newValues)
* delete(table, rid)
* scan(table) → iterator
* search(table, predicate)

Delete should use tombstones initially.

---

## 5️⃣ Basic Transaction & ACID (Lite but Real)

Implement a minimal transaction manager:

* BEGIN
* COMMIT
* ROLLBACK

ACID (simplified):

Atomicity:

* Use a basic Write-Ahead Log (WAL file)
* Log record before modifying page
* On crash recovery, replay committed transactions only

Durability:

* Flush WAL before commit completes

Isolation:

* Simple single-threaded engine is acceptable
* Document isolation assumption

Consistency:

* Enforce schema constraints

Do NOT overcomplicate concurrency. Single-threaded is fine.

---

## 6️⃣ Index Structure (Lite but Real)

Implement a simple index abstraction.

Minimum acceptable:

* B+ Tree skeleton implementation
* Node class
* Internal vs leaf nodes
* Insert
* Search

It can be minimal but must be architecturally correct.

Primary key should optionally use this index.

---

## 7️⃣ Buffer Layer

Implement:

* PageCache / BufferManager
* Load page into memory
* Mark dirty pages
* Flush to disk

No fancy LRU needed — simple Map-based caching is fine.

---

## 8️⃣ Layered Architecture (Very Important)

Structure must follow:

* storage/

  * Page
  * SlottedPage
  * BufferManager
  * DiskManager
* catalog/

  * Schema
  * Column
  * TableMetadata
* transaction/

  * Transaction
  * LogManager
  * RecoveryManager
* index/

  * BPlusTree
  * Node
* engine/

  * Database
  * Table
* common/

  * DataType
  * RID

Parser will interact ONLY with `Database` and `Table`.

No file I/O outside storage layer.

---

## 🔷 OOP Requirements

Must demonstrate:

* Encapsulation
* Composition over inheritance
* Interfaces where meaningful
* No god class
* No static procedural dumping
* Clear responsibilities per class

Design for extensibility.

---

## 🔷 Deliverables

Generate:

1. Full project structure
2. All core classes
3. Documentation comments
4. Minimal main method demonstrating:

* Create database
* Create table
* Insert rows
* Commit transaction
* Query using scan
* Demonstrate rollback

No GUI.
No parser.
Engine-level only.

---

## 🔷 Design Philosophy

This must resemble a real database engine skeleton, not a serialized file wrapper.

Even if simplified, architecture must reflect:

* Page management
* Logging
* Catalog
* Transaction boundary
* Index layer

Think “mini SQLite internal engine in Java”.