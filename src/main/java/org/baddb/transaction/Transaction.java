package org.baddb.transaction;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private final int txId;
    private TransactionState state;
    private final List<Integer> modifiedPages;
    private final List<UndoEntry> undoLogs;

    public record UndoEntry(int pageId, byte[] beforeImage) {}

    public Transaction(int txId) {
        this.txId = txId;
        this.state = TransactionState.ACTIVE;
        this.modifiedPages = new ArrayList<>();
        this.undoLogs = new ArrayList<>();
    }

    public void addUndo(int pageId, byte[] beforeImage) {
        undoLogs.add(new UndoEntry(pageId, beforeImage));
    }

    public List<UndoEntry> getUndoLogs() {
        return undoLogs;
    }

    public int getTxId() {
        return txId;
    }

    public TransactionState getState() {
        return state;
    }

    public void setState(TransactionState state) {
        this.state = state;
    }

    public void addModifiedPage(int pageId) {
        if (!modifiedPages.contains(pageId)) {
            modifiedPages.add(pageId);
        }
    }

    public List<Integer> getModifiedPages() {
        return modifiedPages;
    }
}
