package org.bad.db.index;

import java.util.ArrayList;
import java.util.List;

public class InternalNode<K extends Comparable<K>, V> extends BNode<K, V> {
    private final List<BNode<K, V>> children;

    public InternalNode() {
        super();
        this.isLeaf = false;
        this.children = new ArrayList<>();
    }

    public void addChild(K key, BNode<K, V> child) {
        keys.add(key);
        children.add(child);
    }

    public void addLeftmostChild(BNode<K, V> child) {
        children.add(0, child);
    }

    public List<BNode<K, V>> getChildren() { return children; }

    public BNode<K, V> getChild(K key) {
        int i = 0;
        while (i < keys.size() && key.compareTo(keys.get(i)) >= 0) {
            i++;
        }
        return children.get(i);
    }
}
