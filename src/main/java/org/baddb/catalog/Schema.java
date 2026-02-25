package org.baddb.catalog;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Schema implements Serializable {
    private final List<Column> columns;

    public Schema(List<Column> columns) {
        this.columns = columns;
    }

    public List<Column> getColumns() {
        return Collections.unmodifiableList(columns);
    }
}
