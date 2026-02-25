package org.baddb.catalog;

import java.io.Serializable;

public record Column(String name, DataType type, boolean isNotNull, boolean isPrimaryKey) implements Serializable {
}
