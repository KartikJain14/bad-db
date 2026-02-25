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

public class Demo {
    public static void main(String[] args) {
        try (Database db = new Database("testdb")) {
            // 1. Create Schema
            List<Column> columns = List.of(
                new Column("id", DataType.INT, true, true),
                new Column("name", DataType.STRING, true, false),
                new Column("age", DataType.INT, false, false)
            );
            Schema schema = new Schema(columns);

            // 2. Create Table
            System.out.println("Creating table 'users'...");
            db.createTable("users", schema);

            Table users = db.getTable("users");

            // 3. Begin Transaction
            Transaction tx = db.beginTransaction();
            System.out.println("Started transaction " + tx.getTxId());

            // 4. Insert Records
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

            // 5. Commit
            db.commit(tx);
            System.out.println("Committed transaction.");

            // 6. Scan Records
            System.out.println("Scanning users:");
            tx = db.beginTransaction();
            Iterator<Record> it = users.scan(tx);
            while (it.hasNext()) {
                Record r = it.next();
                System.out.println(" - " + r.get("id") + ": " + r.get("name") + " (" + r.get("age") + ")");
            }

            // 7. Demonstrate Rollback Scenario
            System.out.println("Demonstrating rollback...");
            Transaction failTx = db.beginTransaction();
            Record r3 = new Record();
            r3.set("id", 3);
            r3.set("name", "Charlie");
            users.insert(failTx, r3);
            db.rollback(failTx);
            System.out.println("Rolled back transaction.");

            // Verify Charlie is not there
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
