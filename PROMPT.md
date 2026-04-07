You are a senior Java developer building a **mini database storage engine** for a university Object-Oriented Programming project.

The implementation must follow the provided academic report but may include a **simple B-Tree index for optimization**, without overcomplicating the system.

---

# 🔷 PROJECT CONTEXT

This is an **educational database system** focused on:

* Binary file storage
* Record serialization
* Schema management
* Basic indexing using B-Tree

It is NOT a production database.

Keep the design:

* Simple
* Clean
* Explainable in viva

---

# 🔷 CORE REQUIREMENTS

## 1️⃣ File Initialization (MANDATORY)

Use `RandomAccessFile`.

When creating a database file:

* Write:

  * Magic number (database signature)
  * Version number
  * Number of tables

---

## 2️⃣ FILE STRUCTURE (STRICT)

The binary file must contain:

### Header Section

* Magic number
* Version
* Table count

---

### Schema Section

For each table:

* Table name
* Number of columns
* For each column:

  * Column name
  * Data type

---

### Data Section

* Records stored **sequentially**
* Serialized in schema order
* Variable-length strings allowed

---

## 3️⃣ RECORD STORAGE

* Each record is serialized using:

  * `writeInt`, `writeFloat`, `writeBoolean`, `writeUTF`
* Maintain column order
* No object serialization

---

## 4️⃣ DATA RETRIEVAL

* Default: sequential scan
* Return records as objects

---

# 🔷 5️⃣ SIMPLE B-TREE INDEX (IMPORTANT BUT KEEP IT LIGHT)

Add a **basic B-Tree index for PRIMARY KEY only**.

### Requirements:

* In-memory B-Tree (no need to persist to file for now)
* Key → Record position (file offset)

---

### B-Tree Features:

* Insert(key, offset)
* Search(key) → offset
* No delete required (optional)
* Keep order small (e.g., 3 or 4)

---

### When inserting a record:

* Store its file offset
* Insert key + offset into B-Tree

---

### When searching:

* Use B-Tree first
* Jump directly to file offset
* Read record

---

### Keep B-Tree:

* Simple
* Clean
* Fully explainable

---

# 🔷 SUPPORTED DATA TYPES

* INT
* FLOAT
* STRING
* BOOLEAN

Use enum `DataType`.

---

# 🔷 OOP DESIGN (VERY IMPORTANT)

Create clean classes:

* `DataType` (enum)
* `Column`
* `Schema`
* `Record`
* `Table`
* `DatabaseFileManager`
* `BTree`
* `BTreeNode`

---

### Responsibilities:

* `DatabaseFileManager`
  → Handles all file operations

* `Table`
  → Manages schema, insert, read, index usage

* `Record`
  → Represents row data

* `BTree`
  → Index structure (key → file offset)

---

# 🔷 FUNCTIONAL REQUIREMENTS

Must support:

* createTable
* insertRecord
* getAllRecords (scan)
* searchByPrimaryKey (uses B-Tree)

---

# 🔷 IMPORTANT CONSTRAINTS

DO NOT implement:

* Transactions
* WAL
* ACID
* Concurrency
* Disk-based indexing
* Query parser

---

# 🔷 DEMONSTRATION

Include a `main()`:

1. Create DB
2. Create table with primary key
3. Insert multiple records
4. Print all records (scan)
5. Search by primary key using B-Tree

---

# 🔷 CODE QUALITY

* Clear comments
* Explain:

  * File structure
  * Serialization
  * B-Tree logic
* Keep code readable and modular

---

# 🔷 DESIGN PHILOSOPHY

* Follow report strictly for storage
* Add B-Tree as a small optimization layer
* Keep everything explainable in 5–10 minutes

---

# 🔷 GOAL

Produce a system that:

* Is NOT a CSV clone
* Uses real binary storage
* Has a basic indexing mechanism
* Looks like solid student work
* Is easy to explain in viva