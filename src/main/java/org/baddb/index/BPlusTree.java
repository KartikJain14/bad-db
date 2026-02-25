package org.baddb.index;

import org.baddb.common.RID;
import java.util.TreeMap;

public class BPlusTree implements Index {
    private Node root;

    public BPlusTree() {
        this.root = new LeafNode();
    }

    @Override
    public void insert(Object key, RID rid) {
        root.insert(key, rid);
        // Simplified: no rebalancing/splitting in this skeleton
    }

    @Override
    public RID search(Object key) {
        return root.search(key);
    }

    private abstract static class Node {
        abstract void insert(Object key, RID rid);
        abstract RID search(Object key);
    }

    private static class InternalNode extends Node {
        private final TreeMap<Object, Node> children = new TreeMap<>();

        @Override
        void insert(Object key, RID rid) {
            Object floorKey = children.floorKey(key);
            if (floorKey == null) floorKey = children.firstKey();
            children.get(floorKey).insert(key, rid);
        }

        @Override
        RID search(Object key) {
            Object floorKey = children.floorKey(key);
            if (floorKey == null) floorKey = children.firstKey();
            return children.get(floorKey).search(key);
        }
    }

    private static class LeafNode extends Node {
        private final TreeMap<Object, RID> entries = new TreeMap<>();

        @Override
        void insert(Object key, RID rid) {
            entries.put(key, rid);
        }

        @Override
        RID search(Object key) {
            return entries.get(key);
        }
    }
}
