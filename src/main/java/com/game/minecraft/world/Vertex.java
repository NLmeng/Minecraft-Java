package com.game.minecraft.world;

import com.game.minecraft.utils.FloatArray;

public final class Vertex {
  public static final float ATLAS = 256.0f;
  public static final float NORMAL_TILE = 16.0f;
  public static final float NORMAL_FACE_OFFSET = 0.5f;

  private Vertex() {}

  public static void addNormalTopFaceWithTexture(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    float u0 = u / ATLAS;
    float u1 = (u + NORMAL_TILE) / ATLAS;
    float v0 = v / ATLAS;
    float v1 = (v + NORMAL_TILE) / ATLAS;

    // counterclockwise quad
    addFace(
        vertices,
        // triangle 1
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v0,
        color,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u1,
        v0,
        color,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v1,
        color,
        // triangle 2
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u0,
        v1,
        color);
  }

  public static void addNormalBottomFaceWithTexture(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    float u0 = u / ATLAS;
    float u1 = (u + NORMAL_TILE) / ATLAS;
    float v0 = v / ATLAS;
    float v1 = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v0,
        color,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u1,
        v0,
        color,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v1,
        color,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u0,
        v1,
        color);
  }

  public static void addNormalFrontFaceWithTexture(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    float u0 = u / ATLAS;
    float u1 = (u + NORMAL_TILE) / ATLAS;
    float v0 = v / ATLAS;
    float v1 = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u0,
        v1,
        color,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v1,
        color,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v0,
        color,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u0,
        v0,
        color);
  }

  public static void addNormalBackFaceWithTexture(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    float u0 = u / ATLAS;
    float u1 = (u + NORMAL_TILE) / ATLAS;
    float v0 = v / ATLAS;
    float v1 = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u1,
        v1,
        color,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v1,
        color,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v0,
        color,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u1,
        v0,
        color);
  }

  public static void addNormalLeftFaceWithTexture(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    float u0 = u / ATLAS;
    float u1 = (u + NORMAL_TILE) / ATLAS;
    float v0 = v / ATLAS;
    float v1 = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v1,
        color,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v1,
        color,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v0,
        color,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v0,
        color);
  }

  public static void addNormalRightFaceWithTexture(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    float u0 = u / ATLAS;
    float u1 = (u + NORMAL_TILE) / ATLAS;
    float v0 = v / ATLAS;
    float v1 = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v1,
        color,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v1,
        color,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        u0,
        v0,
        color,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        u1,
        v0,
        color);
  }

  /**
   * Adds a quad (two triangles) in counterclockwise order. Each vertex now has 8 floats: x, y, z,
   * u, v, r, g, b.
   *
   * <p>Pass the 4 corners of the quad in this order:
   *
   * <p>(x0, y0, z0, u0, v0, color) (x1, y1, z1, u1, v1, color) (x2, y2, z2, u2, v2, color) (x3, y3,
   * z3, u3, v3, color)
   *
   * <p>Then it’s formed into two triangles.
   */
  private static void addFace(
      FloatArray vertices,
      float x0,
      float y0,
      float z0,
      float u0,
      float v0,
      float[] c0,
      float x1,
      float y1,
      float z1,
      float u1,
      float v1,
      float[] c1,
      float x2,
      float y2,
      float z2,
      float u2,
      float v2,
      float[] c2,
      float x3,
      float y3,
      float z3,
      float u3,
      float v3,
      float[] c3) {

    // Triangle 1
    addVertex(vertices, x0, y0, z0, u0, v0, c0);
    addVertex(vertices, x1, y1, z1, u1, v1, c1);
    addVertex(vertices, x2, y2, z2, u2, v2, c2);

    // Triangle 2
    addVertex(vertices, x2, y2, z2, u2, v2, c2);
    addVertex(vertices, x3, y3, z3, u3, v3, c3);
    addVertex(vertices, x0, y0, z0, u0, v0, c0);
  }

  private static void addVertex(
      FloatArray vertices, float x, float y, float z, float u, float v, float[] color) {
    vertices.add(x);
    vertices.add(y);
    vertices.add(z);
    vertices.add(u);
    vertices.add(v);
    // color
    vertices.add(color[0]);
    vertices.add(color[1]);
    vertices.add(color[2]);
  }
}
