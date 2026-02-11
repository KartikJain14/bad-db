package org.bad.db.engine;

import org.bad.db.catalog.TableMetadata;
import org.bad.db.common.RID;
import org.bad.db.storage.BufferManager;
import org.bad.db.storage.SlottedPage;
import org.bad.db.storage.Page;
import org.bad.db.transaction.Transaction;

import java.io.*;
import java.util.*;

/**
 * Handles high-level table operations.
 */
public class Table {
    private final TableMetadata metadata;
    private final BufferManager bufferManager;

    public Table(TableMetadata metadata, BufferManager bufferManager) {
        this.metadata = metadata;
        this.bufferManager = bufferManager;
    }

    public RID insert(Record record, Transaction txn) throws IOException {
        byte[] data = serializeRecord(record);
        
        // Find a page with space
        int currentPageId = metadata.getFirstPageId();
        while (true) {
            Page page = bufferManager.getPage(currentPageId);
            SlottedPage slottedPage = new SlottedPage(page);
            
            byte[] before = page.getData().clone();
            int slotId = slottedPage.insertRecord(data);
            if (slotId != -1) {
                if (txn != null) txn.addWrite(page, before);
                return new RID(currentPageId, slotId);
            }
            
            // Try next page or allocate
            currentPageId++;
            // Note: In this simple engine, we assume tables can expand into new pages.
            // If the next page is beyond what we've allocated, we allocate it properly.
            try {
                bufferManager.getPage(currentPageId);
            } catch (Exception e) {
                currentPageId = bufferManager.allocatePage();
            }
        }
    }

    public Record get(RID rid) throws IOException {
        Page page = bufferManager.getPage(rid.pageId());
        SlottedPage slottedPage = new SlottedPage(page);
        byte[] data = slottedPage.getRecord(rid.slotId());
        if (data == null) return null;
        return deserializeRecord(rid, data);
    }

    public List<Record> scan() throws IOException {
        List<Record> results = new ArrayList<>();
        int currentPageId = metadata.getFirstPageId();
        
        // Very simple scan: iterate pages until we find an empty one 
        // (In real system we'd know the page range)
        for (int p = 0; p < 10; p++) { // Scan first 10 pages for demo
            int pid = currentPageId + p;
            Page page = bufferManager.getPage(pid);
            SlottedPage slottedPage = new SlottedPage(page);
            int count = slottedPage.getSlotCount();
            if (count == 0 && p > 0) break; 
            
            for (int s = 0; s < count; s++) {
                byte[] data = slottedPage.getRecord(s);
                if (data != null) {
                    results.add(deserializeRecord(new RID(pid, s), data));
                }
            }
        }
        return results;
    }

    private byte[] serializeRecord(Record record) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(record.getValues());
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private Record deserializeRecord(RID rid, byte[] data) throws IOException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Map<String, Object> values = (Map<String, Object>) ois.readObject();
            return new Record(rid, values);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
