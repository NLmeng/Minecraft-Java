package com.game.minecraft.world;

import java.util.List;

public final class Vertex {
  public static final float ATLAS = 256.0f;
  public static final float NORMAL_TILE = 16.0f;
  public static final float NORMAL_FACE_OFFSET =
      0.5f; // offset to position vertices around the origin

  // 3d coords  / 2d texture coords
  // X,Y,Z      / U,V
  // each "normal" faces +- 0.5f to extend about x,y,z to a size of 1x1x1
  private Vertex() {}

  public static void addNormalTopFaceWithTexture(
      List<Float> vertices, float x, float y, float z, float u, float v) {
    float uTopStart = u / ATLAS;
    float uTopEnd = (u + NORMAL_TILE) / ATLAS;
    float vTopStart = v / ATLAS;
    float vTopEnd = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uTopStart,
        vTopStart,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uTopEnd,
        vTopStart,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uTopEnd,
        vTopEnd,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uTopStart,
        vTopEnd);
  }

  public static void addNormalBottomFaceWithTexture(
      List<Float> vertices, float x, float y, float z, float u, float v) {
    float uBottomStart = u / ATLAS;
    float uBottomEnd = (u + NORMAL_TILE) / ATLAS;
    float vBottomStart = v / ATLAS;
    float vBottomEnd = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uBottomStart,
        vBottomStart,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uBottomEnd,
        vBottomStart,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uBottomEnd,
        vBottomEnd,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uBottomStart,
        vBottomEnd);
  }

  public static void addNormalFrontFaceWithTexture(
      List<Float> vertices, float x, float y, float z, float u, float v) {
    float uSideStart = u / ATLAS;
    float uSideEnd = (u + NORMAL_TILE) / ATLAS;
    float vSideStart = v / ATLAS;
    float vSideEnd = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideStart,
        vSideEnd,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideEnd,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideStart,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideStart,
        vSideStart);
  }

  public static void addNormalBackFaceWithTexture(
      List<Float> vertices, float x, float y, float z, float u, float v) {
    float uSideStart = u / ATLAS;
    float uSideEnd = (u + NORMAL_TILE) / ATLAS;
    float vSideStart = v / ATLAS;
    float vSideEnd = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideEnd,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideStart,
        vSideEnd,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideStart,
        vSideStart,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideStart);
  }

  public static void addNormalLeftFaceWithTexture(
      List<Float> vertices, float x, float y, float z, float u, float v) {
    float uSideStart = u / ATLAS;
    float uSideEnd = (u + NORMAL_TILE) / ATLAS;
    float vSideStart = v / ATLAS;
    float vSideEnd = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideStart,
        vSideEnd,
        x - NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideEnd,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideStart,
        x - NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideStart,
        vSideStart);
  }

  public static void addNormalRightFaceWithTexture(
      List<Float> vertices, float x, float y, float z, float u, float v) {
    float uSideStart = u / ATLAS;
    float uSideEnd = (u + NORMAL_TILE) / ATLAS;
    float vSideStart = v / ATLAS;
    float vSideEnd = (v + NORMAL_TILE) / ATLAS;

    addFace(
        vertices,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideEnd,
        x + NORMAL_FACE_OFFSET,
        y - NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideStart,
        vSideEnd,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z - NORMAL_FACE_OFFSET,
        uSideStart,
        vSideStart,
        x + NORMAL_FACE_OFFSET,
        y + NORMAL_FACE_OFFSET,
        z + NORMAL_FACE_OFFSET,
        uSideEnd,
        vSideStart);
  }

  /**
   * Adds a single face (two triangles) in a counterclockwise order to the vertex list. Each
   * triangle is represented by 3 vertices, and each vertex contains 5 floats (3 for position and 2
   * for UV texture coordinates).
   */
  private static void addFace(
      List<Float> vertices,
      float x0,
      float y0,
      float z0,
      float u0,
      float v0,
      float x1,
      float y1,
      float z1,
      float u1,
      float v1,
      float x2,
      float y2,
      float z2,
      float u2,
      float v2,
      float x3,
      float y3,
      float z3,
      float u3,
      float v3) {

    // Triangle 1
    vertices.add(x0);
    vertices.add(y0);
    vertices.add(z0);
    vertices.add(u0);
    vertices.add(v0);

    vertices.add(x1);
    vertices.add(y1);
    vertices.add(z1);
    vertices.add(u1);
    vertices.add(v1);

    vertices.add(x2);
    vertices.add(y2);
    vertices.add(z2);
    vertices.add(u2);
    vertices.add(v2);

    // Triangle 2
    vertices.add(x2);
    vertices.add(y2);
    vertices.add(z2);
    vertices.add(u2);
    vertices.add(v2);

    vertices.add(x3);
    vertices.add(y3);
    vertices.add(z3);
    vertices.add(u3);
    vertices.add(v3);

    vertices.add(x0);
    vertices.add(y0);
    vertices.add(z0);
    vertices.add(u0);
    vertices.add(v0);
  }
}
