package org.bad.db.catalog;

import java.io.Serializable;

public class TableMetadata implements Serializable {
    private final String tableName;
    private final Schema schema;
    private final int firstPageId;
    private int rootIndexPageId; // For B+ Tree index if any

    public TableMetadata(String tableName, Schema schema, int firstPageId) {
        this.tableName = tableName;
        this.schema = schema;
        this.firstPageId = firstPageId;
        this.rootIndexPageId = -1;
    }

    public String getTableName() { return tableName; }
    public Schema getSchema() { return schema; }
    public int getFirstPageId() { return firstPageId; }
    public int getRootIndexPageId() { return rootIndexPageId; }
    public void setRootIndexPageId(int rootIndexPageId) { this.rootIndexPageId = rootIndexPageId; }
}
