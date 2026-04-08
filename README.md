# BadDB Storage Engine

BadDB is a lightweight binary storage engine for Java. It keeps the code small and explainable, while now supporting enough basic CRUD behavior to be usable for a classroom project or an early proof of concept.

## Key Features

- Binary on-disk storage using `RandomAccessFile`
- In-memory B-Tree index for primary-key lookups
- Insert, read, update, delete, and upsert operations
- Equality-based filtering with `select(columnName, value)`
- Active-record scanning with `getAllRecords()`
- Convenience helpers like `exists`, `countRecords`, and `compact`
- Automatic index rebuild when reopening a database file
- Automatic upgrade of legacy v1 files to the newer record-marker format

## What Changed

BadDB now stores a small active/deleted marker in each record. That makes a few practical features possible without turning the project into a full database engine:

- `updateRecord(...)` appends the new version of a row and marks the old one as deleted
- `deleteRecord(...)` hides a row from future reads while keeping the file format simple
- `compact()` rewrites the file with only active rows, which is useful after many updates or deletes

This keeps the implementation understandable while making the API much more usable.

## Core API

### Create a table

```java
Schema schema = new Schema();
schema.addColumn("id", DataType.INT);
schema.addColumn("name", DataType.STRING);
schema.addColumn("grade", DataType.FLOAT);
schema.addColumn("is_active", DataType.BOOLEAN);

Table students = new Table("Students", schema, "students.db");
students.initialize();
```

### Insert

```java
Record row = new Record(schema.getColumnCount());
row.setValue(0, 101);
row.setValue(1, "Alice Smith");
row.setValue(2, 88.5f);
row.setValue(3, true);

students.insertRecord(row);
```

### Query

```java
Record byId = students.searchByPrimaryKey(101);
List<Record> byName = students.select("name", "Alice Smith");
List<Record> all = students.getAllRecords();
boolean exists = students.exists(101);
int count = students.countRecords();
```

### Update

```java
Record updated = new Record(schema.getColumnCount());
updated.setValue(0, 101);
updated.setValue(1, "Alice Smith");
updated.setValue(2, 91.0f);
updated.setValue(3, true);

students.updateRecord(101, updated);
```

### Delete

```java
students.deleteRecord(101);
```

### Upsert

```java
students.upsertRecord(updated);
```

### Compact

```java
students.compact();
```

## Project Structure

| File | Description |
| :--- | :--- |
| `src/main/java/org/baddb/model/DataType.java` | Enum defining supported types (`INT`, `FLOAT`, `STRING`, `BOOLEAN`). |
| `src/main/java/org/baddb/model/Column.java` | Metadata for one schema column. |
| `src/main/java/org/baddb/model/Schema.java` | Collection of columns defining a table layout. |
| `src/main/java/org/baddb/model/Record.java` | Holds one row of data. |
| `src/main/java/org/baddb/index/BTreeNode.java` | B-Tree node implementation. |
| `src/main/java/org/baddb/index/BTree.java` | In-memory key-to-offset index. |
| `src/main/java/org/baddb/storage/DatabaseFileManager.java` | Low-level binary file read/write layer. |
| `src/main/java/org/baddb/storage/Table.java` | High-level CRUD API. |
| `src/main/java/org/baddb/Main.java` | Runnable demo showing the current features. |

## Compile and Run

### Prerequisites

- JDK 8 or higher

### Compile

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java src/main/java | ForEach-Object { $_.FullName })
```

### Run

```powershell
java -cp out org.baddb.Main
```

## Documentation

- `sdk_guide.md`: quick developer-facing usage guide
- `code_explanation.md`: beginner-friendly implementation walkthrough
- `walkthrough.md`: project walkthrough notes
