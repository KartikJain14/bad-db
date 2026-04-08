package org.baddb;

import org.baddb.model.DataType;
import org.baddb.model.Record;
import org.baddb.model.Schema;
import org.baddb.storage.Table;

import java.io.IOException;

/**
 * Entry point for the BadDB demonstration.
 */
public class Main {
    public static void main(String[] args) {
        String dbPath = "student_records.db";
        try {
            Schema studentSchema = new Schema();
            studentSchema.addColumn("id", DataType.INT);
            studentSchema.addColumn("name", DataType.STRING);
            studentSchema.addColumn("grade", DataType.FLOAT);
            studentSchema.addColumn("is_active", DataType.BOOLEAN);

            System.out.println("--- STEP 1: INITIALIZING NEW DATABASE ---");
            Table students = new Table("Students", studentSchema, dbPath);
            students.initialize();

            students.insertRecord(studentRecord(studentSchema, 101, "Alice Smith", 88.5f, true));
            students.insertRecord(studentRecord(studentSchema, 105, "Bob Johnson", 92.0f, true));
            students.insertRecord(studentRecord(studentSchema, 110, "Diana Prince", 98.4f, true));
            printTable(students, "After inserts");

            System.out.println("\n--- STEP 2: BASIC LOOKUPS ---");
            System.out.println("Primary key 105 -> " + students.searchByPrimaryKey(105));
            System.out.println("Select by name 'Alice Smith' -> " + students.select("name", "Alice Smith"));
            System.out.println("Exists(999) -> " + students.exists(999));
            System.out.println("Count -> " + students.countRecords());

            System.out.println("\n--- STEP 3: UPDATE / DELETE / UPSERT ---");
            students.updateRecord(105, studentRecord(studentSchema, 105, "Bob Johnson", 95.25f, true));
            students.deleteRecord(101);
            boolean updated = students.upsertRecord(studentRecord(studentSchema, 110, "Diana Prince", 99.1f, true));
            boolean inserted = students.upsertRecord(studentRecord(studentSchema, 115, "Eve Adams", 91.2f, true));
            System.out.println("Upsert existing record updated? " + updated);
            System.out.println("Upsert new record updated? " + inserted);
            printTable(students, "After update/delete/upsert");

            System.out.println("\n--- STEP 4: COMPACT AND CLOSE ---");
            students.compact();
            printTable(students, "After compaction");
            students.close();

            System.out.println("\n--- STEP 5: REOPEN FROM DISK ---");
            Table reopened = new Table(dbPath);
            reopened.open();
            printTable(reopened, "Reloaded from disk");
            reopened.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Record studentRecord(Schema schema, int id, String name, float grade, boolean active) {
        Record record = new Record(schema.getColumnCount());
        record.setValue(0, id);
        record.setValue(1, name);
        record.setValue(2, grade);
        record.setValue(3, active);
        return record;
    }

    private static void printTable(Table table, String title) throws IOException {
        System.out.println(title + ":");
        for (Record record : table.getAllRecords()) {
            System.out.println("  " + record);
        }
    }
}
