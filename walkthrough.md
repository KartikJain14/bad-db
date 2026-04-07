# Mini Database Storage Engine - Walkthrough

This document explains the design and implementation of the database storage engine.

## 1. File Structure (Binary Layout)

The database uses a custom binary format managed by `RandomAccessFile`. This allows jumping directly to any record's byte offset.

### Layout at a Glance:
| Field | Type | Description |
| :--- | :--- | :--- |
| Magic Number | 4 bytes | Identifies the file as a valid DB ('DB01') |
| Version | INT | Database version |
| Table Count | INT | Number of tables in the file |
| **Schema Area** | | |
| Table Name | UTF String | Name of the table |
| Column Count | INT | Number of columns |
| Col Name | UTF String | Column name |
| Col Type | INT | Ordinal of `DataType` enum |
| **Data Area** | | |
| Values | Sequential | Binary data based on Column Type |

---

## 2. Core Components

### `DataType` (Enum)
Defines the primitive types the engine understands: `INT`, `FLOAT`, `STRING`, `BOOLEAN`.

### `Column` & `Schema`
Classes that define metadata. `Schema` holds a `List<Column>`. This is written once to the file during initialization and used to interpret binary chunks as records.

### `Record`
A data container. It stores values in an `Object[]` array. When writing, we map these objects back to binary using `writeUTF`, `writeInt`, etc.

### `DatabaseFileManager`
The low-level I/O orchestrator.
* **`createNewDatabase`**: Formats the file with headers and schema.
* **`appendRecord`**: Writes records to the end of the file. Crucially, it returns the *byte offset* where the record was written.
* **`readRecord`**: Given an offset, it seeks directly to that position and reads the data based on the schema.

### `BTree` & `BTreeNode` (In-Memory Index)
A basic B-Tree (order 4) that stores primary key to offset mappings.
* **Optimization**: When a record is inserted, its (Key, Offset) pair is added to the B-Tree.
* **Search**: The `searchByPrimaryKey` method first queries the B-Tree to find the byte offset, then uses `RandomAccessFile.seek()` to jump directly to the record, avoiding a slow sequential scan.

---

## 3. How to Run

1. **Compile**: `javac -d out src/main/java/com/minidb/*.java`
2. **Run**: `java -cp out com.minidb.Main`

The `Main` class will:
1. Create `student_records.db`.
2. Populate it with sample students.
3. Perform a **full table scan** (demonstrating sequential reading).
4. Perform **indexed lookups** (demonstrating O(log n) search performance).

---

## 4. Academic Presentation Tips
During a viva, you can explain:
* **Serialization**: We don't use `Serializable` interface. We manually write bytes (e.g., `writeInt`) to keep the format fixed and predictable.
* **Random Access**: Why `RandomAccessFile`? Because unlike `FileInputStream`, it allows us to jump to the middle of the file instantly using the B-Tree offset.
* **B-Tree**: Explain that the B-Tree remains in memory for speed, while the data is safely written to disk.
