# BadDB SDK Developer Guide

This guide shows how to use BadDB as a small embeddable storage layer in a Java application.

## 1. What BadDB Supports

BadDB is intentionally simple. It is not trying to compete with a full relational database, but it now covers the basics most PoCs need:

- Create and reopen a single-table database file
- Insert rows
- Fetch rows by primary key
- Filter rows by equality on any column
- Update rows by primary key
- Delete rows by primary key
- Upsert rows
- Count rows and check existence
- Compact the file after many updates or deletes

## 2. Defining a Schema

The first column is treated as the primary key and must be an `INT`.

```java
Schema schema = new Schema();
schema.addColumn("id", DataType.INT);         // Primary key
schema.addColumn("username", DataType.STRING);
schema.addColumn("balance", DataType.FLOAT);
schema.addColumn("is_active", DataType.BOOLEAN);
```

## 3. Creating or Opening a Table

### Create a new database file

```java
Table users = new Table("Users", schema, "users.db");
users.initialize();
```

### Open an existing database file

```java
Table users = new Table("users.db");
users.open();
```

When an existing file is opened, BadDB rebuilds the in-memory B-Tree index automatically.

## 4. Inserting a Record

```java
Record row = new Record(schema.getColumnCount());
row.setValue(0, 500);
row.setValue(1, "john_doe");
row.setValue(2, 1250.75f);
row.setValue(3, true);

users.insertRecord(row);
```

Notes:

- Duplicate primary keys are rejected.
- Values must match the schema types exactly.
- Null values are currently rejected to keep the engine simple.

## 5. Querying Data

### Fetch by primary key

```java
Record user = users.searchByPrimaryKey(500);
```

### Filter with equality

```java
List<Record> exactName = users.select("username", "john_doe");
List<Record> activeUsers = users.select("is_active", true);
```

### Fetch all active rows

```java
List<Record> allUsers = users.getAllRecords();
```

### Helper methods

```java
boolean exists = users.exists(500);
int total = users.countRecords();
```

## 6. Updating a Record

Updates are primary-key based.

```java
Record updated = new Record(schema.getColumnCount());
updated.setValue(0, 500);
updated.setValue(1, "john_doe");
updated.setValue(2, 1500.00f);
updated.setValue(3, true);

boolean changed = users.updateRecord(500, updated);
```

Behavior:

- Returns `false` if the row does not exist
- Requires the updated row to keep the same primary key
- Internally appends a new row version and marks the old one as deleted

## 7. Deleting a Record

```java
boolean deleted = users.deleteRecord(500);
```

Behavior:

- Returns `false` if the row does not exist
- Uses a logical delete marker, so the file stays easy to append to

## 8. Upsert

Use `upsertRecord(...)` when you want insert-or-update behavior.

```java
boolean updatedExisting = users.upsertRecord(updated);
```

Return value:

- `true` if an existing row was updated
- `false` if a new row was inserted

## 9. Compacting the File

Because updates and deletes leave older row versions behind, you can occasionally compact the file:

```java
users.compact();
```

This rewrites the database file so only active rows remain.

## 10. Data Types

Supported `DataType` values:

- `INT` -> Java `Integer`
- `FLOAT` -> Java `Float`
- `STRING` -> Java `String`
- `BOOLEAN` -> Java `Boolean`

## 11. Closing the Table

Always close the table when you are done:

```java
users.close();
```

## 12. Design Notes

BadDB keeps the B-Tree index in memory and reconstructs it from disk on open. This keeps the implementation approachable while still giving fast primary-key reads during runtime.
