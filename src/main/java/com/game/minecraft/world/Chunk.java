package com.game.minecraft.world;

import static org.lwjgl.opengl.GL46C.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class Chunk {

  public static final int CHUNK_X = 16;
  public static final int CHUNK_Y = 256;
  public static final int CHUNK_Z = 16;
  private static final int FLOATS_PER_VERTEX = 5;

  private final Blocks[][][] blocks = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];
  private boolean isDirty;
  private int blocksRenderLimit = 16 * 16 * 4;

  private final float xcoord, ycoord, zcoord;

  private int chunkVaoId;
  private int chunkVboId;
  private int vertexCount;

  private final Matrix4f modelMatrix;

  public Chunk(float xpos, float ypos, float zpos) {
    this.xcoord = xpos;
    this.ycoord = ypos;
    this.zcoord = zpos;
    this.modelMatrix = new Matrix4f().translate(0, 0, 0);

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

  public Matrix4f getModelMatrix4f() {
    return modelMatrix;
  }

  public int getNumberBlocksToRender() {
    return blocksRenderLimit;
  }

  public void setNumberBlocksToRender(int limit) {
    blocksRenderLimit = limit;
  }

  public void setBlockAt(int x, int y, int z, Blocks block) {
    if (!inBounds(x, y, z)) {
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
    if (!inBounds(x, y, z)) {
      return null;
    }
    return blocks[x][y][z];
  }

  public void buildMesh() {
    List<Float> vertices = new ArrayList<>();
    int blockRendered = 0;
    for (int y = 0; y < CHUNK_Y && blockRendered < blocksRenderLimit; y++) {
      for (int x = 0; x < CHUNK_X && blockRendered < blocksRenderLimit; x++) {
        for (int z = 0; z < CHUNK_Z && blockRendered < blocksRenderLimit; z++) {
          Blocks block = blocks[x][y][z];
          if (block != null) {
            addBlockToMesh(vertices, x, y, z, block);
            blockRendered++;
          }
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

    block.setSolidStateAs(true);

    float xPos = xcoord + x;
    float yPos = ycoord - y;
    float zPos = zcoord + z;

    if (!isBlockSolid(x, y - 1, z))
      Vertex.addNormalTopFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getTopX(), block.getTopY());

    if (!isBlockSolid(x, y + 1, z))
      Vertex.addNormalBottomFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getBottomX(), block.getBottomY());

    if (!isBlockSolid(x, y, z + 1))
      Vertex.addNormalFrontFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());

    if (!isBlockSolid(x, y, z - 1))
      Vertex.addNormalBackFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());

    if (!isBlockSolid(x - 1, y, z))
      Vertex.addNormalLeftFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());

    if (!isBlockSolid(x + 1, y, z))
      Vertex.addNormalRightFaceWithTexture(
          vertices, xPos, yPos, zPos, block.getSideX(), block.getSideY());
  }

  private void generateFlatTerrain() {
    for (int x = 0; x < CHUNK_X; x++) {
      for (int z = 0; z < CHUNK_Z; z++) {
        for (int y = 0; y < CHUNK_Y; y++) {
          if (y >= CHUNK_Y - 5) {
            blocks[x][y][z] = Blocks.BEDROCK;
          } else if (y >= CHUNK_Y - 60) {
            blocks[x][y][z] = Blocks.STONE;
          } else if (y >= CHUNK_Y - 64) {
            blocks[x][y][z] = Blocks.DIRT;
          } else if (y == CHUNK_Y - 65) {
            blocks[x][y][z] = Blocks.GRASS;
          }
        }
      }
    }
    blocks[0][CHUNK_Y - 65][0] = Blocks.BEDROCK;
    blocks[0][CHUNK_Y - 65][15] = Blocks.BEDROCK;
    blocks[15][CHUNK_Y - 65][15] = Blocks.BEDROCK;
    blocks[15][CHUNK_Y - 65][0] = Blocks.BEDROCK;
  }

  private boolean isBlockSolid(int x, int y, int z) {
    if (!inBounds(x, y, z)) {
      return false;
    }
    Blocks block = blocks[x][y][z];
    return (block != null && block.isSolid());
  }

  private boolean inBounds(int x, int y, int z) {
    return (x >= 0 && x < CHUNK_X && y >= 0 && y < CHUNK_Y && z >= 0 && z < CHUNK_Z);
  }
}
