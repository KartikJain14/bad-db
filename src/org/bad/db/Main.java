package org.bad.db;

import org.bad.db.catalog.Column;
import org.bad.db.catalog.Schema;
import org.bad.db.common.DataType;
import org.bad.db.engine.Database;
import org.bad.db.engine.Record;
import org.bad.db.engine.Table;
import org.bad.db.transaction.Transaction;

import java.util.List;
import java.util.Map;

/**
 * Demo runner for the BadDB engine.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("--- Starting BadDB Engine Demo ---");
        
        try (Database db = new Database("db_demo")) {
            // 1. Create Schema
            Schema userSchema = new Schema(List.of(
                new Column("id", DataType.INT, false, true),
                new Column("name", DataType.STRING, false, false),
                new Column("active", DataType.BOOLEAN, false, false)
            ));

            // 2. Create Table
            System.out.println("Creating table 'users'...");
            Table usersTable = db.createTable("users", userSchema);

            // 3. Insert Records in a Transaction
            System.out.println("Starting transaction...");
            Transaction txn = db.beginTransaction();
            
            usersTable.insert(new Record(null, Map.of("id", 1, "name", "Alice", "active", true)), txn);
            usersTable.insert(new Record(null, Map.of("id", 2, "name", "Bob", "active", false)), txn);
            
            System.out.println("Committing transaction...");
            db.commit(txn);

            // 4. Query using Scan
            System.out.println("\nQuerying all users (Scan):");
            List<Record> allUsers = usersTable.scan();
            allUsers.forEach(System.out::println);

            // 5. Demonstrate Rollback
            System.out.println("\nDemonstrating rollback...");
            Transaction failTxn = db.beginTransaction();
            usersTable.insert(new Record(null, Map.of("id", 3, "name", "Charlie", "active", true)), failTxn);
            System.out.println("Aborting transaction...");
            db.rollback(failTxn);

            System.out.println("Users after rollback (Charlie should not appear):");
            usersTable.scan().forEach(System.out::println);

            System.out.println("\n--- Demo Completed Successfully ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
