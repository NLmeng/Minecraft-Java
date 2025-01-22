package com.game.minecraft.world;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.FloatBuffer;
import java.util.*;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class Chunk {

  public static final int CHUNK_X = 16;
  public static final int CHUNK_Y = 256;
  public static final int CHUNK_Z = 16;

  private final Blocks[][][] blocks = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];
  private boolean isDirty;

  private final float xcoord, ycoord, zcoord;
  private int chunkVaoId;
  private int chunkVboId;
  private int vertexCount;
  private final Matrix4f modelMatrix;

  public Chunk(float xpos, float ypos, float zpos) {
    this.xcoord = xpos;
    this.ycoord = ypos;
    this.zcoord = zpos;
    this.modelMatrix = new Matrix4f().translate(this.xcoord, this.ycoord, this.zcoord);

    generateFlatTerrain();
    buildMesh();
    this.isDirty = true;
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

  public void setBlockAt(int x, int y, int z, Blocks block) {
    if (x < 0 || x >= CHUNK_X || y < 0 || y >= CHUNK_Y || z < 0 || z >= CHUNK_Z) {
      return;
    }
    blocks[x][y][z] = block;
    isDirty = true;
    buildMesh();
  }

  public Blocks[][][] getAllBlocks() {
    return blocks;
  }

  public Blocks getBlockAt(int x, int y, int z) {
    if (x < 0 || x >= Chunk.CHUNK_X || y < 0 || y >= Chunk.CHUNK_Y || z < 0 || z >= Chunk.CHUNK_Z)
      return null;

    return blocks[x][y][z];
  }

  public Matrix4f getModelMatrix4f() {
    return modelMatrix;
  }

  public void buildMesh() {
    List<Float> vertices = new ArrayList<>();

    for (int x = 0; x < CHUNK_X; x++) {
      for (int y = 0; y < CHUNK_Y; y++) {
        for (int z = 0; z < CHUNK_Z; z++) {
          if (blocks[x][y][z] != null) {
            addBlockToMesh(vertices, x, y, z);
          }
        }
      }
    }

    float[] vertexData = new float[vertices.size()];
    for (int i = 0; i < vertices.size(); i++) {
      vertexData[i] = vertices.get(i);
    }

    // Use BufferUtils for heap-based FloatBuffer
    FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertexData.length);
    vertexBuffer.put(vertexData).flip();

    chunkVaoId = glGenVertexArrays();
    glBindVertexArray(chunkVaoId);

    chunkVboId = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, chunkVboId);
    glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

    glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0L);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    vertexCount = vertexData.length / 5; // Each vertex has 5 floats (position + texture)
    isDirty = false;
  }

  private void addBlockToMesh(List<Float> vertices, int x, int y, int z) {
    Blocks block = blocks[x][y][z];
    block.setSolidStateAs(true);

    float xPos = this.xcoord + x;
    float yPos = this.ycoord - y;
    float zPos = this.zcoord + z;

    float uTopStart = block.getTopX() / Sizes.ATLAS;
    float uTopEnd = (block.getTopX() + Sizes.NORMAL_TILE) / Sizes.ATLAS;
    float vTopStart = block.getTopY() / Sizes.ATLAS;
    float vTopEnd = (block.getTopY() + Sizes.NORMAL_TILE) / Sizes.ATLAS;

    float uBottomStart = block.getBottomX() / Sizes.ATLAS;
    float uBottomEnd = (block.getBottomX() + Sizes.NORMAL_TILE) / Sizes.ATLAS;
    float vBottomStart = block.getBottomY() / Sizes.ATLAS;
    float vBottomEnd = (block.getBottomY() + Sizes.NORMAL_TILE) / Sizes.ATLAS;

    float uSideStart = block.getSideX() / Sizes.ATLAS;
    float uSideEnd = (block.getSideX() + Sizes.NORMAL_TILE) / Sizes.ATLAS;
    float vSideStart = block.getSideY() / Sizes.ATLAS;
    float vSideEnd = (block.getSideY() + Sizes.NORMAL_TILE) / Sizes.ATLAS;

    // Top face
    if (!(y - 1 >= 0 && blocks[x][y - 1][z] != null && blocks[x][y - 1][z].isSolid())) {
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uTopStart);
      vertices.add(vTopStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uTopEnd);
      vertices.add(vTopStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uTopEnd);
      vertices.add(vTopEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uTopEnd);
      vertices.add(vTopEnd);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uTopStart);
      vertices.add(vTopEnd);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uTopStart);
      vertices.add(vTopStart);
    }

    // Bottom face
    if (!(y + 1 < CHUNK_Y && blocks[x][y + 1][z] != null && blocks[x][y + 1][z].isSolid())) {
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uBottomStart);
      vertices.add(vBottomStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uBottomEnd);
      vertices.add(vBottomStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uBottomEnd);
      vertices.add(vBottomEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uBottomEnd);
      vertices.add(vBottomEnd);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uBottomStart);
      vertices.add(vBottomEnd);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uBottomStart);
      vertices.add(vBottomStart);
    }

    // Front face
    if (!(z + 1 < CHUNK_Z && blocks[x][y][z + 1] != null && blocks[x][y][z + 1].isSolid())) {
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideEnd);
    }

    // Back face
    if (!(z - 1 >= 0 && blocks[x][y][z - 1] != null && blocks[x][y][z - 1].isSolid())) {
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideEnd);
    }

    // Left face
    if (!(x - 1 >= 0 && blocks[x - 1][y][z] != null && blocks[x - 1][y][z].isSolid())) {
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideEnd);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideEnd);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideStart);
      vertices.add(xPos - 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideEnd);
    }

    // Right face
    if (!(x + 1 < CHUNK_X && blocks[x + 1][y][z] != null && blocks[x + 1][y][z].isSolid())) {
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideEnd);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos - 0.5f);
      vertices.add(uSideStart);
      vertices.add(vSideStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos + 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideStart);
      vertices.add(xPos + 0.5f);
      vertices.add(yPos - 0.5f);
      vertices.add(zPos + 0.5f);
      vertices.add(uSideEnd);
      vertices.add(vSideEnd);
    }
  }

  private void generateFlatTerrain() {
    for (int x = 0; x < CHUNK_X; x++) {
      for (int z = 0; z < CHUNK_Z; z++) {
        for (int y = 0; y < CHUNK_Y; y++) {
          if (y == 0) blocks[x][y][z] = Blocks.GRASS;
          else if (y <= 4) blocks[x][y][z] = Blocks.DIRT;
          else if (y <= 60) blocks[x][y][z] = Blocks.STONE;
          else if (y <= 65) blocks[x][y][z] = Blocks.BEDROCK;
        }
      }
    }
  }
}
