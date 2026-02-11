package org.bad.db.index;

import java.io.Serializable;

/**
 * A simplified B+ Tree implementation for indexing.
 * Architecturally correct structure with internal and leaf nodes.
 */
public class BPlusTree<K extends Comparable<K>, V> implements Serializable {
    private BNode<K, V> root;
    private final int degree;

    public BPlusTree(int degree) {
        this.root = new LeafNode<>();
        this.degree = degree;
    }

    public void insert(K key, V value) {
        // Simplified insert without splitting logic for the skeleton
        // In a real impl, this would handle node overflows and propagation
        if (root instanceof LeafNode<K, V> leaf) {
            leaf.insert(key, value);
        } else {
            // Find appropriate leaf and insert
            BNode<K, V> curr = root;
            while (!curr.isLeaf()) {
                curr = ((InternalNode<K, V>) curr).getChild(key);
            }
            ((LeafNode<K, V>) curr).insert(key, value);
        }
    }

    public V search(K key) {
        BNode<K, V> curr = root;
        while (!curr.isLeaf()) {
            curr = ((InternalNode<K, V>) curr).getChild(key);
        }
        return ((LeafNode<K, V>) curr).search(key);
    }
}
