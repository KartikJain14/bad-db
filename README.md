# Bad Database Storage Engine

BadDB is a lightweight binary database storage engine with B-Tree indexing. It is designed for university Object-Oriented Programming (OOP) projects and provides a clean, simple, and explainable interface for binary file storage.

## 🌟 Key Features

*   **Binary Binary Layout**: Direct record storage using `RandomAccessFile`.
*   **Sequential Storage**: Records are stored one after another to maintain a predictable layout.
*   **B-Tree Indexing**: An in-memory B-Tree for $O(\log n)$ primary key search performance.
*   **SQL-like API**: Supports `CREATE TABLE`, `INSERT INTO`, and `SELECT WHERE`.
*   **Explainable Code**: Built with modularity and educational clarity as top priorities.

## 📂 Project Structure

| File | Description |
| :--- | :--- |
| `src/main/java/org/baddb/model/DataType.java` | Enum defining supported types (INT, FLOAT, STRING, BOOLEAN). |
| `src/main/java/org/baddb/model/Column.java` | Metadata for a single column (name, type). |
| `src/main/java/org/baddb/model/Schema.java` | Collection of columns defining a table's layout. |
| `src/main/java/org/baddb/model/Record.java` | Holds data for a single row. |
| `src/main/java/org/baddb/index/BTreeNode.java` | Node of the B-Tree index. |
| `src/main/java/org/baddb/index/BTree.java` | Core B-Tree implementation (maps Key -> File Offset). |
| `src/main/java/org/baddb/storage/DatabaseFileManager.java` | Low-level binary I/O orchestrator. |
| `src/main/java/org/baddb/storage/Table.java` | High-level API for SQL-like operations. |
| `src/main/java/org/baddb/Main.java` | Demonstration of the system's capabilities. |

## 🛠️ How to Compile and Run

### Prerequisites
*   Java Development Kit (JDK 8 or higher).

### Step 1: Compile from the Root Directory
```powershell
# Compile all files including subpackages
javac -d out (dir src/main/java/org/baddb/*.java -Recurse)
```

### Step 2: Run the Demonstration
```powershell
java -cp out org.baddb.Main
```

### Step 3: Run the SDK Guide
See `sdk_guide.md` for instructions on how to use BadDB in your own code.

## 📖 In-Depth Documentation
*   **[SDK Guide](sdk_guide.md)**: Developer guide for building apps with BadDB.
*   **[Detailed Code Explanation](code_explanation.md)**: Line-by-line breakdown of every function for beginners.
