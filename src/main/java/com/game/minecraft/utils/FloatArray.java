package com.game.minecraft.utils;

import java.util.Arrays;

public class FloatArray {
  private float[] data;
  private int size;

  public FloatArray(int initialCapacity) {
    data = new float[initialCapacity];
    size = 0;
  }

  public void add(float value) {
    if (size == data.length) {
      grow();
    }
    data[size++] = value;
  }

  private void grow() {
    float[] newData = new float[data.length * 2];
    System.arraycopy(data, 0, newData, 0, data.length);
    data = newData;
  }

  public float get(int index) {
    return data[index];
  }

  public float[] elements() {
    return Arrays.copyOf(data, size);
  }

  public int size() {
    return size;
  }

  public void clear() {
    size = 0;
  }
}
