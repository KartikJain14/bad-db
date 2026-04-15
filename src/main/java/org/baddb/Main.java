package org.baddb;

import org.baddb.model.Column;
import org.baddb.model.DataType;
import org.baddb.model.Schema;
import org.baddb.parser.BQLParser;

import java.util.Scanner;

/**
 * Main entry point for BadDB.
 * Now refactored to use BQLParser for all operations.
 */
public class Main {

    private static final BQLParser parser = new BQLParser();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Welcome to BadDB Console!");

        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Create New Database (via BQL INIT)");
            System.out.println("2. Open Existing Database (via BQL OPEN)");
            System.out.println("3. Insert/Upsert Record (via BQL INSERT/UPSERT)");
            System.out.println("4. Query Records (via BQL SEARCH/SELECT)");
            System.out.println("5. BQL Interactive Mode");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            String choiceStr = scanner.nextLine().trim();
            if (choiceStr.isEmpty()) continue;

            int choice;
            try {
                choice = Integer.parseInt(choiceStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid option. Please enter a number.");
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
                        parser.execute("CLOSE");
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid option. Choose 1-6.");
                }
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void menuCreateDatabase() {
        try {
            System.out.print("Enter database path (e.g. data.bad): ");
            String path = scanner.nextLine().trim();
            if (path.isEmpty()) return;

            System.out.print("Enter table name: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) return;

            StringBuilder query = new StringBuilder("INIT ");
            query.append(name).append(" ").append(path);

            System.out.println("Define columns (First column MUST be Primary Key of type INT):");
            boolean first = true;
            while (true) {
                if (first) {
                    System.out.println("Adding Primary Key column...");
                } else {
                    System.out.print("Add another column? (y/n): ");
                    if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
                        break;
                    }
                }
                
                System.out.print("Column Name: ");
                String colName = scanner.nextLine().trim();
                System.out.print("Column Type (INT, FLOAT, STRING, BOOLEAN): ");
                String colType = scanner.nextLine().trim().toUpperCase();
                
                query.append(" ").append(colName).append(":").append(colType);
                first = false;
            }

            parser.execute(query.toString());
        } catch (Exception e) {
            System.out.println("Error creating database: " + e.getMessage());
        }
    }

    private static void menuOpenDatabase() {
        try {
            System.out.print("Enter database path: ");
            String path = scanner.nextLine().trim();
            if (path.isEmpty()) return;
            parser.execute("OPEN " + path);
        } catch (Exception e) {
            System.out.println("Error opening database: " + e.getMessage());
        }
    }

    private static void menuInsertRecord() {
        try {
            Schema schema = parser.getSchema();
            if (schema == null) {
                System.out.println("No active database. Open or create one first.");
                return;
            }

            System.out.print("Use UPSERT instead of INSERT? (y/n): ");
            boolean useUpsert = scanner.nextLine().trim().equalsIgnoreCase("y");
            
            StringBuilder query = new StringBuilder(useUpsert ? "UPSERT" : "INSERT");

            System.out.println("Enter values for the record:");
            for (Column col : schema.getColumns()) {
                System.out.print(col.getName() + " (" + col.getType() + "): ");
                String val = scanner.nextLine().trim();
                if (val.isEmpty()) val = "null";
                
                // Quote strings if they contain spaces or are just raw strings
                if (col.getType() == DataType.STRING && !val.equals("null") && !val.startsWith("\"")) {
                    val = "\"" + val + "\"";
                }
                query.append(" ").append(val);
            }

            parser.execute(query.toString());
        } catch (Exception e) {
            System.out.println("Error inserting record: " + e.getMessage());
        }
    }

    private static void menuQueryRecords() {
        try {
            if (parser.getSchema() == null) {
                System.out.println("No active database. Open or create one first.");
                return;
            }

            System.out.println("1. Find by Primary Key (SEARCH)");
            System.out.println("2. Select by Column Value (SELECT)");
            System.out.println("3. View All Records (SELECT ALL)");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter Primary Key: ");
                    String pk = scanner.nextLine().trim();
                    if (pk.isEmpty()) return;
                    parser.execute("SEARCH " + pk);
                    break;
                case "2":
                    System.out.print("Enter Column Name: ");
                    String colName = scanner.nextLine().trim();
                    if (colName.isEmpty()) return;
                    
                    System.out.print("Enter Value: ");
                    String val = scanner.nextLine().trim();
                    if (val.isEmpty()) return;

                    // Small helper to quote strings if needed
                    boolean isString = false;
                    for (Column c : parser.getSchema().getColumns()) {
                        if (c.getName().equalsIgnoreCase(colName)) {
                            isString = (c.getType() == DataType.STRING);
                            break;
                        }
                    }
                    if (isString && !val.startsWith("\"")) {
                        val = "\"" + val + "\"";
                    }

                    parser.execute("SELECT " + colName + " " + val);
                    break;
                case "3":
                    parser.execute("SELECT ALL");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } catch (Exception e) {
            System.out.println("Error querying records: " + e.getMessage());
        }
    }

    private static void runBQLMode() {
        System.out.println("--- BQL INTERACTIVE MODE ---");
        System.out.println("Type EXIT to return to Main Menu.");
        
        while (true) {
            System.out.print("bql> ");
            String bql = scanner.nextLine().trim();
            if (bql.isEmpty()) continue;
            if (bql.equalsIgnoreCase("EXIT")) {
                break;
            }
            try {
                parser.execute(bql);
            } catch (Exception e) {
                System.out.println("BQL Execution Error: " + e.getMessage());
            }
        }
    }
}
