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

public class Table {
    private final TableMetadata metadata;
    private final BufferManager bufferManager;
    private final TransactionManager txManager;

    public Table(TableMetadata metadata, BufferManager bufferManager, TransactionManager txManager) {
        this.metadata = metadata;
        this.bufferManager = bufferManager;
        this.txManager = txManager;
    }

    public RID insert(Transaction tx, Record record) throws IOException {
        byte[] data = record.serialize(metadata.getSchema());
        int currPageId = metadata.getRootPageId();

        while (true) {
            SlottedPage page = (SlottedPage) bufferManager.getPage(currPageId);
            byte[] before = page.getData().clone();
            
            int slotId = page.insertRecord(data);
            if (slotId != -1) {
                txManager.logUpdate(tx, currPageId, before, page.getData());
                return new RID(currPageId, slotId);
            }

            // No space, move to next page or allocate
            int nextPageId = page.getNextPageId();
            if (nextPageId == 0) { // Using 0 as null-equivalent for next page since 0 is catalog
                SlottedPage newPage = (SlottedPage) bufferManager.createPage();
                page.setNextPageId(newPage.getPageId());
                txManager.logUpdate(tx, currPageId, before, page.getData());
                currPageId = newPage.getPageId();
            } else {
                currPageId = nextPageId;
            }
        }
    }

    public Record get(Transaction tx, RID rid) throws IOException {
        SlottedPage page = (SlottedPage) bufferManager.getPage(rid.pageId());
        byte[] data = page.getRecord(rid.slotId());
        if (data == null) return null;
        return Record.deserialize(data, metadata.getSchema());
    }

    public void update(Transaction tx, RID rid, Record record) throws IOException {
        SlottedPage page = (SlottedPage) bufferManager.getPage(rid.pageId());
        byte[] before = page.getData().clone();
        byte[] data = record.serialize(metadata.getSchema());
        
        if (!page.updateRecord(rid.slotId(), data)) {
            // If in-place update fails (size changed), we delete and re-insert is common,
            // but for this lite engine, we'll just throw for now or mark as deleted.
            throw new IOException("Update failed: size mismatch (fragmentation not handled in lite engine)");
        }
        txManager.logUpdate(tx, rid.pageId(), before, page.getData());
    }

    public void delete(Transaction tx, RID rid) throws IOException {
        SlottedPage page = (SlottedPage) bufferManager.getPage(rid.pageId());
        byte[] before = page.getData().clone();
        page.deleteRecord(rid.slotId());
        txManager.logUpdate(tx, rid.pageId(), before, page.getData());
    }

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
