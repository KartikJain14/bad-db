package org.baddb.model;

import java.util.Arrays;

/**
 * Represents a single row of data in a table.
 */
public class Record {
    private final Object[] values;

    public Record(int columnCount) {
        this.values = new Object[columnCount];
    }

    public void setValue(int index, Object value) {
        if (index >= 0 && index < values.length) {
            values[index] = value;
        }
    }

    public Object getValue(int index) {
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return null;
    }

    public Object[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
