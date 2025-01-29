package com.game.minecraft.utils;

import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.ChunkCoordinate;
import java.util.HashMap;
import java.util.Map;

public class LRU {

  private class Node {
    private Node next, prev;
    private ChunkCoordinate key;
    private Blocks[][][] val;

    Node() {}

    Node(ChunkCoordinate key, Blocks[][][] data) {
      this.key = key;
      this.val = data;
    }
  }

  private int cap = 25 * 25;
  private Node MRUptr = new Node();
  private Node LRUptr = new Node();
  private Map<ChunkCoordinate, Node> cache = new HashMap<>();

  public LRU(int cap) {
    this.cap = cap;
    MRUptr.prev = LRUptr;
    LRUptr.next = MRUptr;
  }

  public Blocks[][][] get(ChunkCoordinate key) {
    if (cache.containsKey(key)) {
      Node node = cache.get(key);
      remove(node);
      insert(node);
      return node.val;
    } else {
      Blocks[][][] loaded = PersistStorage.loadFromFile(key);
      if (loaded != null) {
        put(key, loaded);
        return loaded;
      }
    }
    return null;
  }

  public void put(ChunkCoordinate key, Blocks[][][] val) {
    if (cache.containsKey(key)) {
      remove(cache.get(key));
    }
    cache.put(key, new Node(key, val));
    insert(cache.get(key));

    if (cache.size() > cap) {
      Node lru = LRUptr.next;
      PersistStorage.saveToFile(lru.key, lru.val);
      remove(lru);
      cache.remove(lru.key);
    }
  }

  public void remove(Node node) {
    Node tmpPrev = node.prev;
    Node tmpNext = node.next;
    tmpPrev.next = node.next;
    tmpNext.prev = node.prev;
    node.next = node.prev = null;
  }

  public void insert(Node node) {
    Node mruPrev = MRUptr.prev;
    node.next = MRUptr;
    MRUptr.prev = node;
    node.prev = mruPrev;
    mruPrev.next = node;
  }
}
