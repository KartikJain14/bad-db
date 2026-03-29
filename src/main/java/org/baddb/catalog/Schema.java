package org.baddb.catalog;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A Schema defines the structure of a table, composed of one or more {@link Column} definitions.
 */
public class Schema implements Serializable {
    /** The ordered list of columns that make up this schema. */
    private final List<Column> columns;

    /**
     * Initializes a schema with the provided list of columns.
     *
     * @param columns the list of column definitions
     */
    public Schema(List<Column> columns) {
        this.columns = columns;
    }

    /**
     * Returns the column definitions.
     *
     * @return an unmodifiable view of the columns list
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList(columns);
    }
}
