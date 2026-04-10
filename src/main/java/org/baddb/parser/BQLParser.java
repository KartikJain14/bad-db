package org.baddb.parser;

import org.baddb.model.DataType;
import org.baddb.model.Record;
import org.baddb.model.Schema;
import org.baddb.storage.Table;

import java.io.IOException;
import java.util.List;

public class BQLParser {
    private Table activeTable;

    public BQLParser() {
        this.activeTable = null;
    }

    public boolean execute(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            return true;
        }

        String[] tokens = statement.trim().split("\\s+");
        String command = tokens[0].toUpperCase();

        try {
            switch (command) {
                case "INIT":
                    handleInit(tokens);
                    break;
                case "OPEN":
                    handleOpen(tokens);
                    break;
                case "INSERT":
                    handleInsert(tokens);
                    break;
                case "UPSERT":
                    handleUpsert(tokens);
                    break;
                case "UPDATE":
                    handleUpdate(tokens);
                    break;
                case "DELETE":
                    handleDelete(tokens);
                    break;
                case "SEARCH":
                    handleSearch(tokens);
                    break;
                case "EXISTS":
                    handleExists(tokens);
                    break;
                case "COUNT":
                    handleCount();
                    break;
                case "SELECT":
                    handleSelect(tokens);
                    break;
                case "SELECT_ALL":
                    handleSelectAll();
                    break;
                case "COMPACT":
                    handleCompact();
                    break;
                case "CLOSE":
                    handleClose();
                    break;
                case "EXIT":
                    handleCloseSilently();
                    return false;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Available commands: INIT, OPEN, INSERT, UPSERT, UPDATE, DELETE, SEARCH, EXISTS, COUNT, SELECT, SELECT_ALL, COMPACT, CLOSE, EXIT");
            }
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }

        return true;
    }

    private void handleInit(String[] tokens) throws IOException {
        if (tokens.length < 4) {
            System.out.println("Usage: INIT <tableName> <dbPath> <colName1>:<type1> [<colName2>:<type2> ...]");
            return;
        }
        String tableName = tokens[1];
        String dbPath = tokens[2];
        Schema schema = new Schema();

        for (int i = 3; i < tokens.length; i++) {
            String[] colParts = tokens[i].split(":");
            if (colParts.length != 2) {
                throw new IllegalArgumentException("Column definition must be in format name:type");
            }
            DataType type = DataType.valueOf(colParts[1].toUpperCase());
            schema.addColumn(colParts[0], type);
        }

        if (activeTable != null) {
            activeTable.close();
        }

        activeTable = new Table(tableName, schema, dbPath);
        activeTable.initialize();
        System.out.println("Initialized new database: " + tableName + " at " + dbPath + " with schema " + schema);
    }

    private void handleOpen(String[] tokens) throws IOException {
        if (tokens.length != 2) {
            System.out.println("Usage: OPEN <dbPath>");
            return;
        }

        if (activeTable != null) {
            activeTable.close();
        }

        activeTable = new Table(tokens[1]);
        activeTable.open();
        System.out.println("Opened database at: " + tokens[1]);
        System.out.println("Table: " + activeTable.getName());
        System.out.println(activeTable.getSchema());
    }

    private void checkActiveTable() {
        if (activeTable == null) {
            throw new IllegalStateException("No active table. Use INIT or OPEN first.");
        }
    }

    private void handleInsert(String[] tokens) throws IOException {
        checkActiveTable();
        Record record = parseRecord(tokens, 1);
        activeTable.insertRecord(record);
        System.out.println("Record inserted.");
    }

    private void handleUpsert(String[] tokens) throws IOException {
        checkActiveTable();
        Record record = parseRecord(tokens, 1);
        boolean updated = activeTable.upsertRecord(record);
        if (updated) {
            System.out.println("Record updated via upsert.");
        } else {
            System.out.println("Record inserted via upsert.");
        }
    }

    private void handleUpdate(String[] tokens) throws IOException {
        checkActiveTable();
        if (tokens.length < 2) {
            System.out.println("Usage: UPDATE <primaryKey> <val1> <val2> ...");
            return;
        }
        int pk = Integer.parseInt(tokens[1]);
        Record record = parseRecord(tokens, 2);
        boolean success = activeTable.updateRecord(pk, record);
        if (success) {
            System.out.println("Record updated.");
        } else {
            System.out.println("Record not found.");
        }
    }

    private void handleDelete(String[] tokens) throws IOException {
        checkActiveTable();
        if (tokens.length != 2) {
            System.out.println("Usage: DELETE <primaryKey>");
            return;
        }
        int pk = Integer.parseInt(tokens[1]);
        boolean success = activeTable.deleteRecord(pk);
        if (success) {
            System.out.println("Record deleted.");
        } else {
            System.out.println("Record not found.");
        }
    }

    private void handleSearch(String[] tokens) throws IOException {
        checkActiveTable();
        if (tokens.length != 2) {
            System.out.println("Usage: SEARCH <primaryKey>");
            return;
        }
        int pk = Integer.parseInt(tokens[1]);
        Record record = activeTable.searchByPrimaryKey(pk);
        if (record != null) {
            System.out.println(record);
        } else {
            System.out.println("No record found.");
        }
    }

    private void handleExists(String[] tokens) {
        checkActiveTable();
        if (tokens.length != 2) {
            System.out.println("Usage: EXISTS <primaryKey>");
            return;
        }
        int pk = Integer.parseInt(tokens[1]);
        boolean exists = activeTable.exists(pk);
        System.out.println("Exists: " + exists);
    }

    private void handleCount() {
        checkActiveTable();
        System.out.println("Total records: " + activeTable.countRecords());
    }

    private void handleSelect(String[] tokens) throws IOException {
        checkActiveTable();
        if (tokens.length == 2 && (tokens[1].equalsIgnoreCase("ALL") || tokens[1].equals("*"))) {
            handleSelectAll();
        }
        if (tokens.length < 3) {
            System.out.println("Usage: SELECT <colName> <value>");
            return;
        }
        String colName = tokens[1];
        Object value = parseValue(tokens[2], getColumnType(colName));
        List<Record> results = activeTable.select(colName, value);
        for (Record r : results) {
            System.out.println(r);
        }
        System.out.println("Found " + results.size() + " record(s).");
    }

    private void handleSelectAll() throws IOException {
        checkActiveTable();
        List<Record> results = activeTable.getAllRecords();
        for (Record r : results) {
            System.out.println(r);
        }
        System.out.println("Found " + results.size() + " record(s).");
    }

    private void handleCompact() throws IOException {
        checkActiveTable();
        activeTable.compact();
        System.out.println("Table compacted.");
    }

    private void handleClose() throws IOException {
        checkActiveTable();
        activeTable.close();
        activeTable = null;
        System.out.println("Table closed.");
    }

    private void handleCloseSilently() {
        if (activeTable != null) {
            try {
                activeTable.close();
            } catch (IOException ignored) {}
        }
    }

    private Record parseRecord(String[] tokens, int startIndex) {
        Schema schema = activeTable.getSchema();
        if (tokens.length - startIndex != schema.getColumnCount()) {
            throw new IllegalArgumentException("Expected " + schema.getColumnCount() + " values but got " + (tokens.length - startIndex));
        }

        Record record = new Record(schema.getColumnCount());
        for (int i = 0; i < schema.getColumnCount(); i++) {
            DataType type = schema.getColumns().get(i).getType();
            Object value = parseValue(tokens[startIndex + i], type);
            record.setValue(i, value);
        }
        return record;
    }

    private DataType getColumnType(String colName) {
        for (org.baddb.model.Column col : activeTable.getSchema().getColumns()) {
            if (col.getName().equalsIgnoreCase(colName)) {
                return col.getType();
            }
        }
        throw new IllegalArgumentException("Unknown column: " + colName);
    }

    private Object parseValue(String val, DataType type) {
        if (val.equalsIgnoreCase("null")) {
            return null;
        }
        switch (type) {
            case INT:
                return Integer.parseInt(val);
            case FLOAT:
                return Float.parseFloat(val);
            case BOOLEAN:
                return Boolean.parseBoolean(val);
            case STRING:
                // Handle basic strings. If they have spaces in BQL, they need quotes
                // For simplicity, assuming no spaces if not quoted, or we might need a better tokenizer.
                // This barebones parser splits by space, so values cannot have spaces unless we do advanced parsing.
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    return val.substring(1, val.length() - 1);
                }
                return val;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
