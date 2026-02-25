package org.baddb.catalog;

import org.baddb.buffer.BufferManager;
import org.baddb.storage.SlottedPage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CatalogManager {
    private static final int CATALOG_PAGE_ID = 0;
    private final BufferManager bufferManager;
    private final Map<String, TableMetadata> tables;

    public CatalogManager(BufferManager bufferManager) throws IOException {
        this.bufferManager = bufferManager;
        this.tables = new HashMap<>();
        loadCatalog();
    }

    private void loadCatalog() throws IOException {
        // Bootstrap: check if Page 0 exists
        try {
            bufferManager.getPage(CATALOG_PAGE_ID);
        } catch (IOException e) {
            // File might be empty, allocate Page 0
            bufferManager.createPage(); 
            bufferManager.flushPage(CATALOG_PAGE_ID);
        }

        SlottedPage catalogPage = (SlottedPage) bufferManager.getPage(CATALOG_PAGE_ID);
        for (int i = 0; i < catalogPage.getSlotCount(); i++) {
            byte[] data = catalogPage.getRecord(i);
            if (data != null) {
                try {
                    TableMetadata meta = TableMetadata.deserialize(data);
                    tables.put(meta.getTableName(), meta);
                } catch (Exception e) {
                    // Skip corrupted/invalid records
                }
            }
        }
    }

    public void createTable(String tableName, Schema schema, int rootPageId) throws IOException {
        if (tables.containsKey(tableName)) {
            throw new IOException("Table already exists: " + tableName);
        }

        TableMetadata meta = new TableMetadata(tableName, schema, rootPageId);
        tables.put(tableName, meta);

        SlottedPage catalogPage = (SlottedPage) bufferManager.getPage(CATALOG_PAGE_ID);
        try {
            int slotId = catalogPage.insertRecord(meta.serialize());
            if (slotId == -1) {
                throw new IOException("No space in catalog page for table " + tableName);
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public TableMetadata getTableMetadata(String tableName) {
        return tables.get(tableName);
    }

    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName);
    }
}
