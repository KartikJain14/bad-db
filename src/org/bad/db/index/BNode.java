package org.bad.db.index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class BNode<K extends Comparable<K>, V> implements Serializable {
    protected List<K> keys;
    protected boolean isLeaf;

    public BNode() {
        this.keys = new ArrayList<>();
    }

    public List<K> getKeys() { return keys; }
    public boolean isLeaf() { return isLeaf; }
}
