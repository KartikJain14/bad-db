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
            // --- PART 1: CREATE AND INITIALIZE ---
            Schema studentSchema = new Schema();
            studentSchema.addColumn("id", DataType.INT);
            studentSchema.addColumn("name", DataType.STRING);
            studentSchema.addColumn("grade", DataType.FLOAT);
            studentSchema.addColumn("is_active", DataType.BOOLEAN);

            System.out.println("--- STEP 1: INITIALIZING NEW DATABASE ---");
            Table table1 = new Table("Students", studentSchema, dbPath);
            table1.initialize();
            
            insertStudent(table1, 101, "Alice Smith", 88.5f, true);
            insertStudent(table1, 105, "Bob Johnson", 92.0f, true);
            
            System.out.println("Closing database...\n");
            table1.close();

            // --- PART 2: LOAD AND RE-OPEN ---
            System.out.println("--- STEP 2: RE-OPENING EXISTING DATABASE ---");
            Table table2 = new Table(dbPath);
            table2.open(); // Reads schema and rebuilds index automatically
            
            System.out.println("Inserting new record into existing database...");
            insertStudent(table2, 110, "Diana Prince", 98.4f, true);

            System.out.println("\n--- FINAL RECORD LIST (LOADED FROM DISK) ---");
            table2.getAllRecords().forEach(r -> System.out.println("Loaded: " + r));

            table2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void insertStudent(Table table, int id, String name, float grade, boolean active) throws IOException {
        Record record = new Record(table.getSchema().getColumnCount());
        record.setValue(0, id);
        record.setValue(1, name);
        record.setValue(2, grade);
        record.setValue(3, active);
        table.insertRecord(record);
        System.out.println("Inserted: " + record);
    }
}
