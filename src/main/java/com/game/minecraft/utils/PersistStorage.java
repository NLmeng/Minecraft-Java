package com.game.minecraft.utils;

import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.ChunkCoordinate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class PersistStorage {

  public static final String INSTANCE_FILE_NAME = UUID.randomUUID().toString().replace("-", "");

  public static void saveToFile(ChunkCoordinate coord, Blocks[][][] data) {
    File dir = new File(INSTANCE_FILE_NAME);
    if (!dir.exists()) {
      dir.mkdir();
    }
    File file = new File(dir, coord.x() + "_" + coord.z() + ".dat");

    try (FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Blocks[][][] loadFromFile(ChunkCoordinate coord) {
    File file = new File(INSTANCE_FILE_NAME, coord.x() + "_" + coord.z() + ".dat");
    if (!file.exists()) {
      return null;
    }

    try (FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis)) {
      return (Blocks[][][]) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }
}
