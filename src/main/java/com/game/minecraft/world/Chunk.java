package com.game.minecraft.world;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class Chunk {

  public static final int CHUNK_X = 16;
  public static final int CHUNK_Y = 256;
  public static final int CHUNK_Z = 16;
  private static final int FLOATS_PER_VERTEX = 5;

  private final Blocks[][][] blocks = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];

  private Chunk front, back, left, right;
  private boolean isDirty, isBuilt;
  private final float xcoord, ycoord, zcoord;

  private int chunkVaoId;
  private int chunkVboId;
  private int vertexCount;

  private final Matrix4f modelMatrix;

  public Chunk(float xpos, float ypos, float zpos) {
    xcoord = xpos;
    ycoord = ypos;
    zcoord = zpos;

    modelMatrix = new Matrix4f().translate(0, 0, 0);

    front = null;
    back = null;
    left = null;
    right = null;

    isDirty = true;
    isBuilt = false;
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
        for (int z = 0; z < CHUNK_Z; z++) {
          copy[x][y][z] = blocks[x][y][z];
        }
      }
    }
    return copy;
  }

  public void setBlockData(Blocks[][][] data) {
    for (int x = 0; x < CHUNK_X; x++) {
      for (int y = 0; y < CHUNK_Y; y++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          blocks[x][y][z] = data[x][y][z];
        }
      }
    }
    isDirty = true;
  }

  public void setBlockAt(int x, int y, int z, Blocks block) {
    if (!inBounds(x, y, z)) {
      return;
    }
    blocks[x][y][z] = block;
    isDirty = true;
    buildMesh();
  }

  public void setNeighbor(String direction, Chunk neighbor) {
    switch (direction.toLowerCase()) {
      case "front":
        front = neighbor;
        break;
      case "back":
        back = neighbor;
        break;
      case "left":
        left = neighbor;
        break;
      case "right":
        right = neighbor;
        break;
      default:
        // TODO: exception
        break;
    }
  }

  public void buildMesh() {
    if (!isDirty) return;

    List<Float> vertices = new ArrayList<>();
    for (int y = 0; y < CHUNK_Y; y++) {
      for (int x = 0; x < CHUNK_X; x++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          addBlockToMesh(vertices, x, y, z, blocks[x][y][z]);
        }
      }
    }

    float[] vertexData = new float[vertices.size()];
    for (int i = 0; i < vertices.size(); i++) {
      vertexData[i] = vertices.get(i);
    }

    uploadMeshToGPU(vertexData);

    this.vertexCount = vertexData.length / FLOATS_PER_VERTEX;
    this.isDirty = false;
  }

  private void uploadMeshToGPU(float[] vertexData) {
    FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertexData.length);
    vertexBuffer.put(vertexData).flip();

    if (chunkVaoId == 0) {
      chunkVaoId = glGenVertexArrays();
    }
    glBindVertexArray(chunkVaoId);

    if (chunkVboId == 0) {
      chunkVboId = glGenBuffers();
    }
    glBindBuffer(GL_ARRAY_BUFFER, chunkVboId);
    glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

    glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
    glEnableVertexAttribArray(0);

    glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3L * Float.BYTES);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  private void addBlockToMesh(List<Float> vertices, int x, int y, int z, Blocks block) {
    if (block == null) return;

    float xPos = xcoord + x;
    float yPos = ycoord - y;
    float zPos = zcoord + z;

    if (!blockExistsAt(x, y - 1, z))
      Vertex.addNormalTopFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getTopX(), block.getTopY());

    if (!blockExistsAt(x, y + 1, z))
      Vertex.addNormalBottomFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getBottomX(), block.getBottomY());

    if (!blockExistsAt(x, y, z + 1))
      Vertex.addNormalFrontFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());

    if (!blockExistsAt(x, y, z - 1))
      Vertex.addNormalBackFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());

    if (!blockExistsAt(x - 1, y, z))
      Vertex.addNormalLeftFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());

    if (!blockExistsAt(x + 1, y, z))
      Vertex.addNormalRightFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
  }

  public void generateFlatTerrain() {
    Random random = new Random();
    int dirtHeight = CHUNK_Y - (60 + random.nextInt(6));

    for (int x = 0; x < CHUNK_X; x++) {
      for (int z = 0; z < CHUNK_Z; z++) {

        for (int y = 0; y < CHUNK_Y; y++) {
          if (y >= CHUNK_Y - 5) {
            blocks[x][y][z] = Blocks.BEDROCK;
          } else if (y >= dirtHeight) {
            blocks[x][y][z] = Blocks.DIRT;
          } else if (y >= CHUNK_Y - 60) {
            blocks[x][y][z] = Blocks.STONE;
          } else if (y == dirtHeight - 1) {
            blocks[x][y][z] = Blocks.GRASS;
          }
        }
      }
    }

    blocks[0][dirtHeight - 1][0] = Blocks.BEDROCK;
    blocks[0][dirtHeight - 1][15] = Blocks.BEDROCK;
    blocks[15][dirtHeight - 1][15] = Blocks.BEDROCK;
    blocks[15][dirtHeight - 1][0] = Blocks.BEDROCK;

    isBuilt = true;
    isDirty = true;
  }

  private boolean blockExistsAt(int x, int y, int z) {
    if (!inBounds(0, y, 0)) return false;

    if (x < 0) {
      if (left != null) {
        return left.blockExistsAt(x + CHUNK_X, y, z);
      } else {
        return false;
      }
    } else if (x >= CHUNK_X) {
      if (right != null) {
        return right.blockExistsAt(x - CHUNK_X, y, z);
      } else {
        return false;
      }
    }

    if (z < 0) {
      if (back != null) {
        return back.blockExistsAt(x, y, z + CHUNK_Z);
      } else {
        return false;
      }
    } else if (z >= CHUNK_Z) {
      if (front != null) {
        return front.blockExistsAt(x, y, z - CHUNK_Z);
      } else {
        return false;
      }
    }

    return blockExistsLocallyAt(x, y, z);
  }

  private boolean blockExistsLocallyAt(int x, int y, int z) {
    if (!inBounds(x, y, z)) {
      return false;
    }
    return (blocks[x][y][z] != null);
  }

  private boolean inBounds(int x, int y, int z) {
    return (x >= 0 && x < CHUNK_X && y >= 0 && y < CHUNK_Y && z >= 0 && z < CHUNK_Z);
  }
}
