package com.game.minecraft.world;

import static org.lwjgl.opengl.GL46C.*;

import com.game.minecraft.utils.Direction;
import com.game.minecraft.utils.FloatArray;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class Chunk {

  public static final int CHUNK_X = 16;
  public static final int CHUNK_Y = 256;
  public static final int CHUNK_Z = 16;
  private static final int FLOATS_PER_VERTEX = 5;
  private static final int STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

  private final Blocks[][][] blocks = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];
  private final FloatArray opaqueVertices = new FloatArray(1024);
  private final FloatArray waterVertices = new FloatArray(1024);

  private Chunk front, back, left, right;
  private boolean isDirty;
  private final float xcoord, ycoord, zcoord;

  private int opaqueVaoId;
  private int opaqueVboId;
  private int opaqueVertexCount;

  private int waterVaoId;
  private int waterVboId;
  private int waterVertexCount;

  // origin 0,0,0 and will translate through xyz in buildmesh
  private final Matrix4f modelMatrix = new Matrix4f().translate(0, 0, 0);

  public Chunk(float xpos, float ypos, float zpos) {
    xcoord = xpos;
    ycoord = ypos;
    zcoord = zpos;

    front = back = left = right = null;

    isDirty = true;
  }

  public float getXCoord() {
    return xcoord;
  }

  public float getYCoord() {
    return ycoord;
  }

  public float getZCoord() {
    return zcoord;
  }

  public int getOpaqueVaoId() {
    return opaqueVaoId;
  }

  public int getOpaqueVertexCount() {
    return opaqueVertexCount;
  }

  public int getWaterVaoId() {
    return waterVaoId;
  }

  public int getWaterVertexCount() {
    return waterVertexCount;
  }

  public boolean isDirty() {
    return isDirty;
  }

  public Matrix4f getModelMatrix4f() {
    return modelMatrix;
  }

  public Blocks[][][] getAllBlocks() {
    return blocks;
  }

  public Blocks getBlockAt(int x, int y, int z) {
    if (!inBounds(x, y, z)) {
      return null;
    }
    return blocks[x][y][z];
  }

  public Blocks[][][] copyBlockData() {
    Blocks[][][] copy = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];
    for (int x = 0; x < CHUNK_X; x++) {
      for (int y = 0; y < CHUNK_Y; y++) {
        System.arraycopy(blocks[x][y], 0, copy[x][y], 0, CHUNK_Z);
      }
    }
    return copy;
  }

  public void setBlockData(Blocks[][][] data) {
    if (isDataEqual(data)) return;

    for (int x = 0; x < CHUNK_X; x++) {
      for (int y = 0; y < CHUNK_Y; y++) {
        System.arraycopy(data[x][y], 0, blocks[x][y], 0, CHUNK_Z);
      }
    }
    setAsDirty();
  }

  private boolean isDataEqual(Blocks[][][] data) {
    for (int x = 0; x < CHUNK_X; x++) {
      for (int y = 0; y < CHUNK_Y; y++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          if (blocks[x][y][z] != data[x][y][z]) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public void setBlockAt(int x, int y, int z, Blocks block) {
    if (!inBounds(x, y, z)) {
      return;
    }
    blocks[x][y][z] = block;
    this.setAsDirty();
  }

  public void setAsDirty() {
    isDirty = true;
  }

  public void setNeighbor(Direction direction, Chunk neighbor) {
    switch (direction) {
      case FORWARD -> front = neighbor;
      case BACKWARD -> back = neighbor;
      case LEFT -> left = neighbor;
      case RIGHT -> right = neighbor;
    }
    if (neighbor != null) this.setAsDirty();
  }

  public void cleanup() {
    cleanupGPUResources();
    cleanNeighbors();
  }

  private void cleanupGPUResources() {
    if (opaqueVaoId != 0) glDeleteVertexArrays(opaqueVaoId);
    if (opaqueVboId != 0) glDeleteBuffers(opaqueVboId);
    if (waterVaoId != 0) glDeleteVertexArrays(waterVaoId);
    if (waterVboId != 0) glDeleteBuffers(waterVboId);
    opaqueVaoId = opaqueVboId = waterVaoId = waterVboId = 0;
  }

  private void cleanNeighbors() {
    if (front != null) front.setNeighbor(Direction.BACKWARD, null);
    if (back != null) back.setNeighbor(Direction.FORWARD, null);
    if (left != null) left.setNeighbor(Direction.RIGHT, null);
    if (right != null) right.setNeighbor(Direction.LEFT, null);
    front = back = left = right = null;
  }

  public void buildMesh() {
    if (!isDirty) return;

    cleanupGPUResources();
    opaqueVertices.clear();
    waterVertices.clear();

    for (int y = 0; y < CHUNK_Y; y++) {
      for (int x = 0; x < CHUNK_X; x++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          addBlockToMesh(x, y, z, blocks[x][y][z]);
        }
      }
    }

    opaqueVertexCount = opaqueVertices.size() / FLOATS_PER_VERTEX;
    uploadVerticesToGPU(opaqueVertices, true);

    waterVertexCount = waterVertices.size() / FLOATS_PER_VERTEX;
    uploadVerticesToGPU(waterVertices, false);

    isDirty = false;
  }

  private void uploadVerticesToGPU(FloatArray verticesArray, boolean isOpaque) {
    FloatBuffer buffer = BufferUtils.createFloatBuffer(verticesArray.size());
    buffer.put(verticesArray.elements(), 0, verticesArray.size()).flip();

    int vao = glGenVertexArrays();
    int vbo = glGenBuffers();

    if (isOpaque) {
      opaqueVaoId = vao;
      opaqueVboId = vbo;
    } else {
      waterVaoId = vao;
      waterVboId = vbo;
    }

    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

    glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0L);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  private void addBlockToMesh(int x, int y, int z, Blocks block) {
    if (block == null) return;

    float xPos = xcoord + x;
    float yPos = ycoord - y;
    float zPos = zcoord + z;

    if (block == Blocks.WATER1) {
      addWaterBlockToMesh(x, y, z, xPos, yPos, zPos, block);
    } else {
      addOpaqueBlockFaces(x, y, z, xPos, yPos, zPos, block);
    }
  }

  private void addOpaqueBlockFaces(
      int x, int y, int z, float xPos, float yPos, float zPos, Blocks block) {
    if (!blockExistsAndNotTransparentAt(x, y - 1, z))
      Vertex.addNormalTopFaceWithTexture(
          opaqueVertices, xPos, yPos, zPos, block.getTopX(), block.getTopY());
    if (!blockExistsAndNotTransparentAt(x, y + 1, z))
      Vertex.addNormalBottomFaceWithTexture(
          opaqueVertices, xPos, yPos, zPos, block.getBottomX(), block.getBottomY());
    if (!blockExistsAndNotTransparentAt(x, y, z + 1))
      Vertex.addNormalFrontFaceWithTexture(
          opaqueVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!blockExistsAndNotTransparentAt(x, y, z - 1))
      Vertex.addNormalBackFaceWithTexture(
          opaqueVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!blockExistsAndNotTransparentAt(x - 1, y, z))
      Vertex.addNormalLeftFaceWithTexture(
          opaqueVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!blockExistsAndNotTransparentAt(x + 1, y, z))
      Vertex.addNormalRightFaceWithTexture(
          opaqueVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
  }

  private void addWaterBlockToMesh(
      int x, int y, int z, float xPos, float yPos, float zPos, Blocks block) {
    if (!waterExistsAt(x, y - 1, z))
      Vertex.addNormalTopFaceWithTexture(
          waterVertices, xPos, yPos, zPos, block.getTopX(), block.getTopY());
    if (!waterExistsAt(x, y + 1, z))
      Vertex.addNormalBottomFaceWithTexture(
          waterVertices, xPos, yPos, zPos, block.getBottomX(), block.getBottomY());
    if (!waterExistsAt(x, y, z + 1))
      Vertex.addNormalFrontFaceWithTexture(
          waterVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!waterExistsAt(x, y, z - 1))
      Vertex.addNormalBackFaceWithTexture(
          waterVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!waterExistsAt(x - 1, y, z))
      Vertex.addNormalLeftFaceWithTexture(
          waterVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!waterExistsAt(x + 1, y, z))
      Vertex.addNormalRightFaceWithTexture(
          waterVertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
  }

  private boolean blockExistsAndNotTransparentAt(int x, int y, int z) {
    if (!inBounds(0, y, 0)) return false; // theres no vertical neighbors

    if (x < 0) {
      return left != null && left.blockExistsAndNotTransparentAt(x + CHUNK_X, y, z);
    } else if (x >= CHUNK_X) {
      return right != null && right.blockExistsAndNotTransparentAt(x - CHUNK_X, y, z);
    }

    if (z < 0) {
      return back != null && back.blockExistsAndNotTransparentAt(x, y, z + CHUNK_Z);
    } else if (z >= CHUNK_Z) {
      return front != null && front.blockExistsAndNotTransparentAt(x, y, z - CHUNK_Z);
    }

    return blockExistsAndNotTransparentLocallyAt(x, y, z);
  }

  private boolean blockExistsAndNotTransparentLocallyAt(int x, int y, int z) {
    if (!inBounds(x, y, z)) {
      return false;
    }
    return (blocks[x][y][z] != null && blocks[x][y][z].isSolid());
  }

  private boolean waterExistsAt(int x, int y, int z) {
    if (!inBounds(0, y, 0)) return false;
    if (x < 0) {
      return (left != null) && left.waterExistsAt(x + CHUNK_X, y, z);
    } else if (x >= CHUNK_X) {
      return (right != null) && right.waterExistsAt(x - CHUNK_X, y, z);
    }

    if (z < 0) {
      return (back != null) && back.waterExistsAt(x, y, z + CHUNK_Z);
    } else if (z >= CHUNK_Z) {
      return (front != null) && front.waterExistsAt(x, y, z - CHUNK_Z);
    }

    return blocks[x][y][z] == Blocks.WATER1;
  }

  private boolean inBounds(int x, int y, int z) {
    return (x >= 0 && x < CHUNK_X && y >= 0 && y < CHUNK_Y && z >= 0 && z < CHUNK_Z);
  }
}
