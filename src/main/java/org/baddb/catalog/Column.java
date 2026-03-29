package org.baddb.catalog;

import java.io.Serializable;

/**
 * Meta-information for a single column in a table.
 *
 * @param name the unique name of the column within the table
 * @param type the data type stored in this column (e.g., INT, STRING)
 * @param isNotNull if true, null values are not permitted in this column
 * @param isPrimaryKey if true, this column (or part of a composite set) uniquely identifies a row
 */
public record Column(String name, DataType type, boolean isNotNull, boolean isPrimaryKey) implements Serializable {
}
