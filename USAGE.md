# Bad-DB: How to Use the Engine

This guide walks you through the basic steps of interacting with the Bad-DB engine.

## ⚙️ Initializing the Database

The entry point for all operations is the `Database` class. Upon initialization, it automatically sets up the storage layer, opens the WAL, and performs any necessary crash recovery.

```java
import org.baddb.engine.Database;

// Initialization manages "mydb.db" and "mydb.wal" files automatically.
try (Database db = new Database("mydb")) {
    // Database operations go here...
}
```

## 📋 Table & Schema Creation

Before you can store records, you must define a table schema and register it with the system catalog.

```java
import org.baddb.catalog.Column;
import org.baddb.catalog.DataType;
import org.baddb.catalog.Schema;
import java.util.List;

// Define columns: ID (int), Name (string), Age (int)
List<Column> columns = List.of(
    new Column("id", DataType.INT, true, true), // isNotNull, isPrimaryKey
    new Column("name", DataType.STRING, true, false),
    new Column("age", DataType.INT, false, false)
);

Schema mySchema = new Schema(columns);

// Registration: This persists metadata to Page 0 of mydb.db.
db.createTable("users", mySchema);
```

## 🔄 Transactional Record Insertion

Every mutation in Bad-DB must take place within a transaction to ensure ACID compliance.

```java
import org.baddb.engine.Table;
import org.baddb.engine.Record;
import org.baddb.transaction.Transaction;

Table users = db.getTable("users");
Transaction tx = db.beginTransaction();

try {
    Record r1 = new Record();
    r1.set("id", 1);
    r1.set("name", "Alice");
    r1.set("age", 30);
    
    // Insertion: This updates the buffer pool and logs to the WAL.
    users.insert(tx, r1);

    // Commit: This ensures the WAL is synced and records are permanent.
    db.commit(tx);
} catch (Exception e) {
    // Rollback: Reverts memory changes based on undo logs.
    db.rollback(tx);
}
```

## 🔍 Data Retrieval & Scanning

Records can be retrieved one-by-one or via a full table scan.

```java
import java.util.Iterator;

Transaction scanTx = db.beginTransaction();
Iterator<Record> it = users.scan(scanTx);

while (it.hasNext()) {
    Record r = it.next();
    System.out.println("User: " + r.get("id") + ", " + r.get("name"));
}

db.commit(scanTx);
```

## 🗑️ Updates & Deletions

Records can be updated or deleted using their unique `RID` (Record Identifier).

```java
import org.baddb.common.RID;

Transaction modifyTx = db.beginTransaction();

// Insert a record and get its ID
RID rid = users.insert(modifyTx, myRecord);

// Modify the same record
Record updateRec = new Record();
updateRec.set("id", 1);
updateRec.set("name", "Alice-Updated");
users.update(modifyTx, rid, updateRec);

// Delete the record
users.delete(modifyTx, rid);

db.commit(modifyTx);
```

## 🛠️ Closing the Database

Always use a `try-with-resources` or manually call `db.close()`. This ensures all committed modifications in memory are flushed to disk and file handles are released.

---

For internal architecture, refer to [INTERNALS.md](./INTERNALS.md).
