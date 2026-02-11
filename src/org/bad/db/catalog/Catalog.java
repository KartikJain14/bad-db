package org.bad.db.catalog;

import org.bad.db.storage.BufferManager;
import org.bad.db.storage.SlottedPage;
import org.bad.db.storage.Page;
import org.bad.db.common.DataType;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalog manages metadata about tables in the database.
 * For this simplified engine, it stores everything in Page 0.
 */
public class Catalog {
    private final BufferManager bufferManager;
    private final Map<String, TableMetadata> tables;
    private static final int CATALOG_PAGE_ID = 0;

    public Catalog(BufferManager bufferManager) throws IOException {
        this.bufferManager = bufferManager;
        this.tables = new HashMap<>();
        load();
    }

    public void addTable(TableMetadata metadata) throws IOException {
        tables.put(metadata.getTableName(), metadata);
        save();
    }

    public TableMetadata getTable(String name) {
        return tables.get(name);
    }

    public Map<String, TableMetadata> getAllTables() {
        return tables;
    }

    private void save() throws IOException {
        Page page = bufferManager.getPage(CATALOG_PAGE_ID);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(tables);
        oos.flush();
        
        byte[] serialized = baos.toByteArray();
        if (serialized.length > page.getData().length) {
            throw new IOException("Catalog too big for a single page (educational limitation)");
        }
        
        System.arraycopy(serialized, 0, page.getData(), 0, serialized.length);
        page.setDirty(true);
        bufferManager.flushPage(CATALOG_PAGE_ID);
    }

    @SuppressWarnings("unchecked")
    private void load() throws IOException {
        Page page = bufferManager.getPage(CATALOG_PAGE_ID);
        byte[] data = page.getData();
        
        // Check if page is empty (just initialized)
        if (data[0] == 0 && data[1] == 0) return;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Map<String, TableMetadata> loaded = (Map<String, TableMetadata>) ois.readObject();
            tables.putAll(loaded);
        } catch (Exception e) {
            // New database or corrupt catalog, start fresh
        }
    }
}
