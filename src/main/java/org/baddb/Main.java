package org.baddb;

import org.baddb.model.DataType;
import org.baddb.model.Record;
import org.baddb.model.Schema;
import org.baddb.parser.BQLParser;
import org.baddb.storage.Table;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static Table activeTable = null;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Welcome to BadDB Console!");

        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Create New Database (GUI Prompt)");
            System.out.println("2. Open Existing Database (GUI Prompt)");
            System.out.println("3. Insert/Upsert Record (GUI Prompt)");
            System.out.println("4. Query Records (GUI Prompt)");
            System.out.println("5. BQL Interactive Mode");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            String choiceStr = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(choiceStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid option.");
                continue;
            }

            try {
                switch (choice) {
                    case 1:
                        menuCreateDatabase();
                        break;
                    case 2:
                        menuOpenDatabase();
                        break;
                    case 3:
                        menuInsertRecord();
                        break;
                    case 4:
                        menuQueryRecords();
                        break;
                    case 5:
                        runBQLMode();
                        break;
                    case 6:
                        if (activeTable != null) {
                            activeTable.close();
                        }
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void menuCreateDatabase() throws IOException {
        System.out.print("Enter database path (e.g. data.bad): ");
        String path = scanner.nextLine().trim();
        System.out.print("Enter table name: ");
        String name = scanner.nextLine().trim();

        Schema schema = new Schema();
        while (true) {
            System.out.print("Add column? (y/n): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
                break;
            }
            System.out.print("Column Name: ");
            String colName = scanner.nextLine().trim();
            System.out.print("Column Type (INT, FLOAT, STRING, BOOLEAN): ");
            String colType = scanner.nextLine().trim().toUpperCase();

            try {
                schema.addColumn(colName, DataType.valueOf(colType));
                System.out.println("Added column " + colName);
            } catch (Exception e) {
                System.out.println("Invalid type. Column not added.");
            }
        }

        if (schema.getColumnCount() == 0) {
            System.out.println("Schema must have at least one column. Initializing aborted.");
            return;
        }

        if (activeTable != null) {
            activeTable.close();
        }

        activeTable = new Table(name, schema, path);
        activeTable.initialize();
        System.out.println("Database successfully created and loaded as active table.");
    }

    private static void menuOpenDatabase() throws IOException {
        System.out.print("Enter database path: ");
        String path = scanner.nextLine().trim();

        if (activeTable != null) {
            activeTable.close();
        }

        activeTable = new Table(path);
        activeTable.open();
        System.out.println("Database loaded: " + activeTable.getName());
        System.out.println(activeTable.getSchema());
    }

    private static void checkActiveTable() {
        if (activeTable == null) {
            throw new IllegalStateException("No active database. Create or open one first.");
        }
    }

    private static void menuInsertRecord() throws IOException {
        checkActiveTable();
        Schema schema = activeTable.getSchema();
        Record record = new Record(schema.getColumnCount());

        System.out.println("Enter values for the new record:");
        for (int i = 0; i < schema.getColumnCount(); i++) {
            org.baddb.model.Column col = schema.getColumns().get(i);
            System.out.print(col.getName() + " (" + col.getType() + "): ");
            String val = scanner.nextLine().trim();
            Object parsedVal = parseValue(val, col.getType());
            record.setValue(i, parsedVal);
        }

        System.out.print("Use UPSERT instead of INSERT? (y/n): ");
        boolean useUpsert = scanner.nextLine().trim().equalsIgnoreCase("y");

        if (useUpsert) {
            activeTable.upsertRecord(record);
            System.out.println("Record upserted.");
        } else {
            activeTable.insertRecord(record);
            System.out.println("Record inserted.");
        }
    }

    private static void menuQueryRecords() throws IOException {
        checkActiveTable();
        System.out.println("1. Find by Primary Key");
        System.out.println("2. Select by Column Value");
        System.out.println("3. View All Records");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("Enter Primary Key: ");
            int pk = Integer.parseInt(scanner.nextLine().trim());
            Record r = activeTable.searchByPrimaryKey(pk);
            if (r != null) {
                System.out.println(r);
            } else {
                System.out.println("Not found.");
            }
        } else if (choice.equals("2")) {
            System.out.print("Enter Column Name: ");
            String colName = scanner.nextLine().trim();
            System.out.print("Enter Value: ");
            String val = scanner.nextLine().trim();

            DataType type = null;
            for (org.baddb.model.Column col : activeTable.getSchema().getColumns()) {
                if (col.getName().equalsIgnoreCase(colName)) {
                    type = col.getType();
                    break;
                }
            }

            if (type == null) {
                System.out.println("Column not found.");
                return;
            }

            Object parsedVal = parseValue(val, type);
            List<Record> results = activeTable.select(colName, parsedVal);
            for (Record r : results) {
                System.out.println(r);
            }
            System.out.println("Total: " + results.size());

        } else if (choice.equals("3")) {
            List<Record> results = activeTable.getAllRecords();
            for (Record r : results) {
                System.out.println(r);
            }
            System.out.println("Total: " + results.size());
        }
    }

    private static void runBQLMode() {
        System.out.println("--- BQL INTERACTIVE MODE ---");
        System.out.println("Type EXIT to return to Main Menu.");
        BQLParser parser = new BQLParser();

        // If we already have an active table, we could pass it to the parser
        // But for simplicity of the isolated session, BQLParser has its own state.
        
        while (true) {
            System.out.print("bql> ");
            String bql = scanner.nextLine().trim();
            if (bql.isEmpty()) continue;
            if (bql.toUpperCase().equals("EXIT")) {
                if (parser.execute("CLOSE")) {} // safely close
                break;
            }
            parser.execute(bql);
        }
    }

    private static Object parseValue(String val, DataType type) {
        if (val.equalsIgnoreCase("null")) {
            return null;
        }
        switch (type) {
            case INT: return Integer.parseInt(val);
            case FLOAT: return Float.parseFloat(val);
            case BOOLEAN: return Boolean.parseBoolean(val);
            case STRING: return val; // No quotes needed in GUI prompt mode
            default: throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
