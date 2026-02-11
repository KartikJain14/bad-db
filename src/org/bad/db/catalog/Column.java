package org.bad.db.catalog;

import org.bad.db.common.DataType;

import java.io.Serializable;

public class Column implements Serializable {
    private final String name;
    private final DataType type;
    private final boolean isNullable;
    private final boolean isPrimaryKey;

    public Column(String name, DataType type, boolean isNullable, boolean isPrimaryKey) {
        this.name = name;
        this.type = type;
        this.isNullable = isNullable;
        this.isPrimaryKey = isPrimaryKey;
    }

    public String getName() { return name; }
    public DataType getType() { return type; }
    public boolean isNullable() { return isNullable; }
    public boolean isPrimaryKey() { return isPrimaryKey; }
}
