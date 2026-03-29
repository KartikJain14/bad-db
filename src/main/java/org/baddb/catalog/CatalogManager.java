package org.baddb.catalog;

import org.baddb.buffer.BufferManager;
import org.baddb.storage.SlottedPage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * CatalogManager handles the persistence of table definitions (metadata) within the database.
 * It uses a fixed page (ID 0) as the "root" catalog page where all table metadata is stored.
 * This class provides an in-memory cache of table definitions for fast lookup during query execution.
 */
public class CatalogManager {
    /** Page 0 is reserved as the entry point for the database catalog. */
    private static final int CATALOG_PAGE_ID = 0;
    /** The buffer manager used for reading/writing catalog pages. */
    private final BufferManager bufferManager;
    /** In-memory cache of table name to its corresponding metadata. */
    private final Map<String, TableMetadata> tables;

    /**
     * Initializes the CatalogManager.
     * It performs a bootstrap check to ensure Page 0 exists and loads any existing table metadata.
     *
     * @param bufferManager the buffer manager to use for I/O
     * @throws IOException if occurs an error reading or initializing the catalog
     */
    public CatalogManager(BufferManager bufferManager) throws IOException {
        this.bufferManager = bufferManager;
        this.tables = new HashMap<>();
        loadCatalog();
    }

    /**
     * Scans the catalog page (Page 0) and hydrates the in-memory 'tables' map.
     *
     * @throws IOException if occurs a disk read error
     */
    private void loadCatalog() throws IOException {
        // Bootstrap: check if Page 0 exists. If not, this is a fresh database.
        try {
            bufferManager.getPage(CATALOG_PAGE_ID);
        } catch (IOException e) {
            // If Page 0 doesn't exist, create it.
            bufferManager.createPage(); 
            bufferManager.flushPage(CATALOG_PAGE_ID);
        }

        // Read the catalog page (assumed to be a SlottedPage for flexibility).
        SlottedPage catalogPage = (SlottedPage) bufferManager.getPage(CATALOG_PAGE_ID);
        for (int i = 0; i < catalogPage.getSlotCount(); i++) {
            byte[] data = catalogPage.getRecord(i);
            if (data != null) {
                try {
                    // Deserialize the byte array back into a TableMetadata object.
                    TableMetadata meta = TableMetadata.deserialize(data);
                    tables.put(meta.getTableName(), meta);
                } catch (Exception e) {
                    // Skip corrupted or invalid metadata records found in the catalog.
                }
            }
        }
    }

    /**
     * Creates a new table entry in the database.
     * Persists the metadata to Page 0 and updates the in-memory cache.
     *
     * @param tableName unique name for the new table
     * @param schema the structure of the table
     * @param rootPageId the ID of the first data page for this table
     * @throws IOException if the table already exists or if there is no space in the catalog page
     */
    public void createTable(String tableName, Schema schema, int rootPageId) throws IOException {
        if (tables.containsKey(tableName)) {
            throw new IOException("Table already exists: " + tableName);
        }

        // 1. Create the metadata object.
        TableMetadata meta = new TableMetadata(tableName, schema, rootPageId);
        
        // 2. Persist to disk at Page 0.
        SlottedPage catalogPage = (SlottedPage) bufferManager.getPage(CATALOG_PAGE_ID);
        try {
            // TableMetadata is serialized and stored as a record in the SlottedPage.
            int slotId = catalogPage.insertRecord(meta.serialize());
            if (slotId == -1) {
                throw new IOException("No space in catalog page for table " + tableName + ". Catalog expansion not implemented.");
            }
        } catch (IOException e) {
            throw e;
        }

        // 3. If disk write succeeded, update the cache.
        tables.put(tableName, meta);
    }

    /**
     * Retrieves the metadata for a specific table.
     *
     * @param tableName name of the table to look up
     * @return the TableMetadata if found, or null otherwise
     */
    public TableMetadata getTableMetadata(String tableName) {
        return tables.get(tableName);
    }

    /**
     * Checks if a table with the given name exists in the database.
     *
     * @param tableName name of the table
     * @return true if it exists, false otherwise
     */
    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName);
    }
}
