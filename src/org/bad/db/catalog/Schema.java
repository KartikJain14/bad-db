package org.bad.db.catalog;

import java.io.Serializable;
import java.util.List;

public class Schema implements Serializable {
    private final List<Column> columns;

    public Schema(List<Column> columns) {
        this.columns = columns;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.size();
    }
}
