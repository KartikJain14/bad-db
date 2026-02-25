package org.baddb.index;

import org.baddb.common.RID;
import java.io.IOException;

public interface Index {
    void insert(Object key, RID rid) throws IOException;
    RID search(Object key) throws IOException;
}
