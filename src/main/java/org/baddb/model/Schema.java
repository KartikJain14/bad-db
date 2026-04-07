package org.baddb.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the layout of a database table.
 */
public class Schema {
    private final List<Column> columns;

    public Schema() {
        this.columns = new ArrayList<>();
    }

    public void addColumn(String name, DataType type) {
        columns.add(new Column(name, type));
    }

    public List<Column> getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String toString() {
        return "Schema: " + columns;
    }
}
