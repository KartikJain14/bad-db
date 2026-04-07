# BadDB SDK Developer Guide

This guide provides instructions on how to use the BadDB storage engine in your Java applications.

## 1. Getting Started

BadDB is a lightweight binary storage engine with B-Tree indexing. It is designed for educational purposes and provides a simple API for table management.

### Maven/Build Setup
Currently, BadDB is a standalone library. To use it, include the `org.baddb` package in your source directory.

## 2. Core API Usage

### Defining a Schema
The `Schema` class defines the structure of your table.
```java
Schema schema = new Schema();
schema.addColumn("id", DataType.INT);         // Column 0 (Primary Key)
schema.addColumn("username", DataType.STRING);
schema.addColumn("balance", DataType.FLOAT);
```

### Initializing or Re-opening a Table
The `Table` class manages both the binary file and the in-memory index.

#### Option A: Create a NEW Database (Overwrites existing)
```java
Table myTable = new Table("Users", schema, "users.db");
myTable.initialize(); // Creates the file and writes metadata
```

#### Option B: Load an EXISTING Database (Persists data)
```java
Table myTable = new Table("users.db");
myTable.open(); // Reads schema from file and rebuilds the B-Tree index
```

### Inserting Data
Create a `Record` object and populate it based on the schema order.
```java
Record row = new Record(schema.getColumnCount());
row.setValue(0, 500);
row.setValue(1, "john_doe");
row.setValue(2, 1250.75f);

myTable.insertRecord(row);
```

### Querying Data (SQL-like)
BadDB supports both sequential scans and indexed lookups.

#### SELECT * (All Records)
```java
List<Record> all = myTable.getAllRecords();
```

#### SELECT WHERE (Equality)
Use the `select` method for basic filtering.
```java
// Uses B-Tree index if searching by column 0 (ID)
List<Record> results = myTable.select("id", 500);

// Uses sequential scan for other columns
List<Record> users = myTable.select("username", "john_doe");
```

## 3. Data Types
The following types are supported via the `DataType` enum:
- `INT`: Java `int`
- `FLOAT`: Java `float`
- `STRING`: Java `String` (stored as UTF-8)
- `BOOLEAN`: Java `boolean`

## 4. Closing the Database
Always close the table to ensure the `RandomAccessFile` handle is released properly.
```java
myTable.close();
```
