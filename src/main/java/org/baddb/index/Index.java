package org.baddb.index;

import org.baddb.common.RID;
import java.io.IOException;

/**
 * Interface for all index implementations in the database.
 * Defines the basic contract for inserting and searching for records.
 */
public interface Index {
    void insert(Object key, RID rid) throws IOException;
    RID search(Object key) throws IOException;
}
