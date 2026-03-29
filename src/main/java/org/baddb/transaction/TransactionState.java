package org.baddb.transaction;

/**
 * Defines the possible states of a transaction during its lifecycle.
 */
public enum TransactionState {
    /** The transaction is currently running and performing operations. */
    ACTIVE,
    /** The transaction has successfully finished and all changes are persistent. */
    COMMITTED,
    /** The transaction has been cancelled, and any partial changes must be undone. */
    ABORTED
}
