package org.baddb;

import org.baddb.catalog.Column;
import org.baddb.catalog.DataType;
import org.baddb.catalog.Schema;
import org.baddb.engine.Database;
import org.baddb.engine.Record;
import org.baddb.engine.Table;
import org.baddb.transaction.Transaction;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Demo provides a simple, end-to-end example of using the Bad-DB engine.
 * It demonstrates:
 * 1. Database initialization and component assembly.
 * 2. Schema and table creation.
 * 3. Transactional record insertion and COMMIT.
 * 4. Table scanning and record retrieval.
 * 5. Rollback of uncommitted changes.
 */
public class Demo {
    /**
     * The main execution entry point for the demo.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Use try-with-resources to ensure the database is closed safely (flushing memory to disk).
        try (Database db = new Database("testdb")) {
            // 1. Create Schema: Define the structure of our 'users' table.
            List<Column> columns = List.of(
                new Column("id", DataType.INT, true, true),
                new Column("name", DataType.STRING, true, false),
                new Column("age", DataType.INT, false, false)
            );
            Schema schema = new Schema(columns);
 
            // 2. Create Table: Register the table with the system catalog.
            System.out.println("Creating table 'users'...");
            db.createTable("users", schema);
 
            Table users = db.getTable("users");
 
            // 3. Begin Transaction: Every operation must be part of a transaction.
            Transaction tx = db.beginTransaction();
            System.out.println("Started transaction " + tx.getTxId());
 
            // 4. Insert Records: Populate the table with some initial data.
            Record r1 = new Record();
            r1.set("id", 1);
            r1.set("name", "Alice");
            r1.set("age", 30);
            users.insert(tx, r1);
 
            Record r2 = new Record();
            r2.set("id", 2);
            r2.set("name", "Bob");
            r2.set("age", 25);
            users.insert(tx, r2);
 
            // 5. Commit: Persist the insertions to the database.
            db.commit(tx);
            System.out.println("Committed transaction.");
 
            // 6. Scan Records: Read back all committed data using a new transaction.
            System.out.println("Scanning users:");
            tx = db.beginTransaction();
            Iterator<Record> it = users.scan(tx);
            while (it.hasNext()) {
                Record r = it.next();
                System.out.println(" - " + r.get("id") + ": " + r.get("name") + " (" + r.get("age") + ")");
            }
 
            // 7. Demonstrate Rollback Scenario: Show that partial changes can be undone.
            System.out.println("Demonstrating rollback...");
            Transaction failTx = db.beginTransaction();
            Record r3 = new Record();
            r3.set("id", 3);
            r3.set("name", "Charlie");
            // This record is currently only in the buffer pool (and WAL).
            users.insert(failTx, r3);
            // Rollback reverts the memory changes for this transaction.
            db.rollback(failTx);
            System.out.println("Rolled back transaction.");
 
            // Verify Charlie is not there.
            it = users.scan(tx);
            boolean foundCharlie = false;
            while (it.hasNext()) {
                if ("Charlie".equals(it.next().get("name"))) foundCharlie = true;
            }
            System.out.println("Charlie found after rollback? " + foundCharlie);
 
            db.commit(tx);
 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
