package org.baddb.transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction represents a unit of work that must be processed atomically.
 * It tracks its own state, unique ID, modified pages, and undo logs for recovery.
 */
public class Transaction {
    /** The unique identifier for this transaction. */
    private final int txId;
    /** Current state of the transaction (ACTIVE, COMMITTED, or ABORTED). */
    private TransactionState state;
    /** Keeps track of which pages were touched during this transaction. Useful for commit/abort workflows. */
    private final List<Integer> modifiedPages;
    /** Keeps track of page states BEFORE they were updated. This allow UNDO if the transaction aborts. */
    private final List<UndoEntry> undoLogs;

    /**
     * Helper record to store a snapshot of a page before it was modified.
     *
     * @param pageId ID of the page
     * @param beforeImage the raw byte array of the page's prior state
     */
    public record UndoEntry(int pageId, byte[] beforeImage) {}

    /**
     * Initializes a new transaction in the ACTIVE state.
     *
     * @param txId the unique ID given by the TransactionManager
     */
    public Transaction(int txId) {
        this.txId = txId;
        this.state = TransactionState.ACTIVE;
        this.modifiedPages = new ArrayList<>();
        this.undoLogs = new ArrayList<>();
    }

    /**
     * Adds an entry to the transaction's private undo history.
     *
     * @param pageId ID of the updated page
     * @param beforeImage the old contents of the page
     */
    public void addUndo(int pageId, byte[] beforeImage) {
        undoLogs.add(new UndoEntry(pageId, beforeImage));
    }

    /** @return the list of undo entries for rollback */
    public List<UndoEntry> getUndoLogs() {
        return undoLogs;
    }

    /** @return the unique transaction ID */
    public int getTxId() {
        return txId;
    }

    /** @return current transaction state */
    public TransactionState getState() {
        return state;
    }

    /** @param state updates the transaction state */
    public void setState(TransactionState state) {
        this.state = state;
    }

    /**
     * Registers a page ID as being modified by this transaction.
     *
     * @param pageId the ID of the page
     */
    public void addModifiedPage(int pageId) {
        if (!modifiedPages.contains(pageId)) {
            modifiedPages.add(pageId);
        }
    }

    /** @return the list of all unique page IDs modified by this transaction */
    public List<Integer> getModifiedPages() {
        return modifiedPages;
    }
}
