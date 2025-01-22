package com.game.minecraft.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.BufferUtils;

public class FileReader {

  /** Load a text-based resource from within the JAR or resources folder. */
  public static String loadFromResource(String resourcePath) {
    try (InputStream in = FileReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new FileNotFoundException("Resource not found: " + resourcePath);
      }
      byte[] bytes = in.readAllBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load resource: " + resourcePath, e);
    }
  }

  /** Load a binary resource into a ByteBuffer. */
  public static ByteBuffer loadTextureFromResource(String resourcePath) {
    try (InputStream in = FileReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new FileNotFoundException("Texture resource not found: " + resourcePath);
      }
      byte[] fileBytes = in.readAllBytes();
      ByteBuffer buffer = BufferUtils.createByteBuffer(fileBytes.length);
      buffer.put(fileBytes).flip();
      return buffer;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load texture resource: " + resourcePath, e);
    }
  }
}
