package com.game.minecraft.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.lwjgl.BufferUtils;

public class FileReader {

  public static String loadFromResource(String resourcePath) {
    try {
      Path path = Paths.get(FileReader.class.getClassLoader().getResource(resourcePath).toURI());
      return Files.readString(path);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Failed to load resource: " + resourcePath, e);
    }
  }

  public static ByteBuffer loadTextureFromResource(String resourcePath) {
    try {
      Path path = Paths.get(FileReader.class.getClassLoader().getResource(resourcePath).toURI());
      byte[] fileBytes = Files.readAllBytes(path);
      ByteBuffer buffer = BufferUtils.createByteBuffer(fileBytes.length);
      buffer.put(fileBytes);
      buffer.flip();
      return buffer;
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Failed to load texture resource: " + resourcePath, e);
    }
  }
}
