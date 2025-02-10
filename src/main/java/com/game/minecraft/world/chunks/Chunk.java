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

  private final Blocks[][][] blocks = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];
  private final FloatArray vertices = new FloatArray(1024);

  private Chunk front, back, left, right;
  private boolean isDirty;
  private final float xcoord, ycoord, zcoord;

  private int chunkVaoId;
  private int chunkVboId;
  private int vertexCount;
  // origin 0,0,0 and will translate through xyz in buildmesh
  private final Matrix4f modelMatrix = new Matrix4f().translate(0, 0, 0);

  public Chunk(float xpos, float ypos, float zpos) {
    xcoord = xpos;
    ycoord = ypos;
    zcoord = zpos;

    front = null;
    back = null;
    left = null;
    right = null;

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

  public int getVaoId() {
    return chunkVaoId;
  }

  public int getVertexCount() {
    return vertexCount;
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
    boolean isChanged = false;
    for (int x = 0; x < CHUNK_X; x++) {
      for (int y = 0; y < CHUNK_Y; y++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          if (!isChanged && blocks[x][y][z] != data[x][y][z]) isChanged = true;
          blocks[x][y][z] = data[x][y][z];
        }
      }
    }
    if (isChanged) this.setAsDirty();
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

  public void buildMesh() {
    if (!isDirty) return;

    cleanupGPUResources();
    vertices.clear();

    for (int y = 0; y < CHUNK_Y; y++) {
      for (int x = 0; x < CHUNK_X; x++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          addBlockToMesh(x, y, z, blocks[x][y][z]);
        }
      }
    }

    uploadMeshToGPU(vertices.elements(), vertices.size());

    this.vertexCount = vertices.size() / FLOATS_PER_VERTEX;
    this.isDirty = false;
  }

  private void uploadMeshToGPU(float[] data, int count) {
    FloatBuffer buffer = BufferUtils.createFloatBuffer(count);
    buffer.put(data, 0, count).flip();

    if (chunkVaoId == 0) {
      chunkVaoId = glGenVertexArrays();
    }
    glBindVertexArray(chunkVaoId);

    if (chunkVboId == 0) {
      chunkVboId = glGenBuffers();
    }
    glBindBuffer(GL_ARRAY_BUFFER, chunkVboId);
    glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

    glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
    glEnableVertexAttribArray(0);

    glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3L * Float.BYTES);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  public void cleanup() {
    cleanupGPUResources();
    cleanNeighbors();
  }

  private void cleanupGPUResources() {
    if (chunkVaoId != 0) glDeleteVertexArrays(chunkVaoId);
    if (chunkVboId != 0) glDeleteBuffers(chunkVboId);
    chunkVaoId = chunkVboId = 0;
  }

  private void cleanNeighbors() {
    if (front != null) front.setNeighbor(Direction.BACKWARD, null);
    if (back != null) back.setNeighbor(Direction.FORWARD, null);
    if (left != null) left.setNeighbor(Direction.RIGHT, null);
    if (right != null) right.setNeighbor(Direction.LEFT, null);
    front = back = left = right = null;
  }

  private void addBlockToMesh(int x, int y, int z, Blocks block) {
    if (block == null) return;

    float xPos = xcoord + x;
    float yPos = ycoord - y;
    float zPos = zcoord + z;

    if (!blockExistsAndNotTransparentAt(x, y - 1, z))
      Vertex.addNormalTopFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getTopX(), block.getTopY());
    if (!blockExistsAndNotTransparentAt(x, y + 1, z))
      Vertex.addNormalBottomFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getBottomX(), block.getBottomY());
    if (!blockExistsAndNotTransparentAt(x, y, z + 1))
      Vertex.addNormalFrontFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!blockExistsAndNotTransparentAt(x, y, z - 1))
      Vertex.addNormalBackFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!blockExistsAndNotTransparentAt(x - 1, y, z))
      Vertex.addNormalLeftFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
    if (!blockExistsAndNotTransparentAt(x + 1, y, z))
      Vertex.addNormalRightFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
  }

  private boolean blockExistsAndNotTransparentAt(int x, int y, int z) {
    if (!inBounds(0, y, 0)) return false; // theres no vertical neighbors

    if (x < 0) {
      if (left != null) {
        return left.blockExistsAndNotTransparentAt(x + CHUNK_X, y, z);
      } else {
        return false;
      }
    } else if (x >= CHUNK_X) {
      if (right != null) {
        return right.blockExistsAndNotTransparentAt(x - CHUNK_X, y, z);
      } else {
        return false;
      }
    }

    if (z < 0) {
      if (back != null) {
        return back.blockExistsAndNotTransparentAt(x, y, z + CHUNK_Z);
      } else {
        return false;
      }
    } else if (z >= CHUNK_Z) {
      if (front != null) {
        return front.blockExistsAndNotTransparentAt(x, y, z - CHUNK_Z);
      } else {
        return false;
      }
    }

    return blockExistsAndNotTransparentLocallyAt(x, y, z);
  }

  private boolean blockExistsAndNotTransparentLocallyAt(int x, int y, int z) {
    if (!inBounds(x, y, z)) {
      return false;
    }
    return (blocks[x][y][z] != null) && (blocks[x][y][z].isSolid());
  }

  private boolean inBounds(int x, int y, int z) {
    return (x >= 0 && x < CHUNK_X && y >= 0 && y < CHUNK_Y && z >= 0 && z < CHUNK_Z);
  }
}
