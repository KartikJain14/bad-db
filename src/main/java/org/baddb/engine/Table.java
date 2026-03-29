package org.baddb.engine;

import org.baddb.catalog.TableMetadata;
import org.baddb.buffer.BufferManager;
import org.baddb.common.RID;
import org.baddb.storage.SlottedPage;
import org.baddb.transaction.Transaction;
import org.baddb.transaction.TransactionManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Table provides a high-level API to interact with records in a specific database table.
 * It manages a "Heap File" structure, which is a linked list (chain) of data pages.
 */
public class Table {
    /** Metadata describing the table's structure and entry point. */
    private final TableMetadata metadata;
    /** Buffer pool for page access. */
    private final BufferManager bufferManager;
    /** Transaction manager for logging changes. */
    private final TransactionManager txManager;

    /**
     * Initializes a Table accessor.
     *
     * @param metadata the table's metadata
     * @param bufferManager the buffer manager
     * @param txManager the transaction manager
     */
    public Table(TableMetadata metadata, BufferManager bufferManager, TransactionManager txManager) {
        this.metadata = metadata;
        this.bufferManager = bufferManager;
        this.txManager = txManager;
    }

    /**
     * Inserts a record into the table.
     * Searches for a page with sufficient space in the chain. 
     * If no such page exists, it allocates a new one and appends it to the chain.
     *
     * @param tx the transaction performing the insertion
     * @param record the record to insert
     * @return the RID (page, slot) of the newly inserted record
     * @throws IOException if occurs a disk or logging error
     */
    public RID insert(Transaction tx, Record record) throws IOException {
        byte[] data = record.serialize(metadata.getSchema());
        int currPageId = metadata.getRootPageId();

        // Iterate through the linked list of pages until we find space or the end.
        while (true) {
            SlottedPage page = (SlottedPage) bufferManager.getPage(currPageId);
            // Snapshot the page state BEFORE modification for the WAL 'beforeImage'.
            byte[] before = page.getData().clone();
            
            int slotId = page.insertRecord(data);
            if (slotId != -1) {
                // Success: log the change and return the unique identifier.
                txManager.logUpdate(tx, currPageId, before, page.getData());
                return new RID(currPageId, slotId);
            }

            // Failure: No space in the current page. Move to the next page in the chain.
            int nextPageId = page.getNextPageId();
            if (nextPageId == 0) { 
                // We reached the end of the chain. Allocate a brand new page.
                SlottedPage newPage = (SlottedPage) bufferManager.createPage();
                // Link the old last page to the new one.
                page.setNextPageId(newPage.getPageId());
                // Log the mutation of the old page's 'next' pointer.
                txManager.logUpdate(tx, currPageId, before, page.getData());
                currPageId = newPage.getPageId();
            } else {
                currPageId = nextPageId;
            }
        }
    }

    /**
     * Retrieves a record from the table by its ID.
     *
     * @param tx the accessing transaction
     * @param rid the record's unique ID
     * @return the Record if found, or null
     * @throws IOException if occurs a read error
     */
    public Record get(Transaction tx, RID rid) throws IOException {
        SlottedPage page = (SlottedPage) bufferManager.getPage(rid.pageId());
        byte[] data = page.getRecord(rid.slotId());
        if (data == null) return null;
        return Record.deserialize(data, metadata.getSchema());
    }

    /**
     * Updates an existing record.
     * Currently only supports in-place updates of the same size.
     *
     * @param tx the transaction performing the update
     * @param rid the ID of the record to update
     * @param record the new record data
     * @throws IOException if sizes mismatch or occurs an I/O error
     */
    public void update(Transaction tx, RID rid, Record record) throws IOException {
        SlottedPage page = (SlottedPage) bufferManager.getPage(rid.pageId());
        byte[] before = page.getData().clone();
        byte[] data = record.serialize(metadata.getSchema());
        
        if (!page.updateRecord(rid.slotId(), data)) {
            // Real engines handle size changes via delete+insert or fragmentation logic.
            // Simplified for this educational version.
            throw new IOException("Update failed: size mismatch (fragmentation not handled in lite engine)");
        }
        txManager.logUpdate(tx, rid.pageId(), before, page.getData());
    }

    /**
     * Deletes a record from the table.
     *
     * @param tx the transaction performing the deletion
     * @param rid the ID of the record to delete
     * @throws IOException if occurs an I/O error
     */
    public void delete(Transaction tx, RID rid) throws IOException {
        SlottedPage page = (SlottedPage) bufferManager.getPage(rid.pageId());
        byte[] before = page.getData().clone();
        page.deleteRecord(rid.slotId());
        txManager.logUpdate(tx, rid.pageId(), before, page.getData());
    }

    /**
     * Performs a full table scan by following the chain of pages and reading every record.
     *
     * @param tx the scanning transaction
     * @return an iterator over all records in the table
     * @throws IOException if occurs a read error
     */
    public Iterator<Record> scan(Transaction tx) throws IOException {
        List<Record> results = new ArrayList<>();
        int currPageId = metadata.getRootPageId();

        while (currPageId != 0) {
            SlottedPage page = (SlottedPage) bufferManager.getPage(currPageId);
            for (int i = 0; i < page.getSlotCount(); i++) {
                byte[] data = page.getRecord(i);
                if (data != null) {
                    results.add(Record.deserialize(data, metadata.getSchema()));
                }
            }
            currPageId = page.getNextPageId();
        }
        return results.iterator();
    }

    /**
     * Searches for records matching a specific condition using a table scan.
     *
     * @param tx the searching transaction
     * @param predicate the condition to test each record against
     * @return a list of matching records
     * @throws IOException if occurs a read error
     */
    public List<Record> search(Transaction tx, Predicate<Record> predicate) throws IOException {
        List<Record> results = new ArrayList<>();
        Iterator<Record> it = scan(tx);
        while (it.hasNext()) {
            Record r = it.next();
            if (predicate.test(r)) {
                results.add(r);
            }
        }
        return results;
    }
}
