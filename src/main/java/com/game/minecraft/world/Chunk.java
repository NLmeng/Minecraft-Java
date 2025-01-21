package com.game.minecraft.world;

import org.lwjgl.system.MemoryStack;

import com.game.minecraft.world.Blocks;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import org.lwjgl.BufferUtils;

public class Chunk {

    public static final int CHUNK_X = 16;
    public static final int CHUNK_Y = 1;
    public static final int CHUNK_Z = 16;

    private final Blocks[][][] blocks = new Blocks[CHUNK_X][CHUNK_Y][CHUNK_Z];

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
    }

    public float getXCoord() { return xcoord; }
    public float getYCoord() { return ycoord; }
    public float getZCoord() { return zcoord; }

    public int getVaoId() {
        return chunkVaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public Matrix4f getModelMatrix4f() {
        return modelMatrix;
    }

    public void buildMesh() {
        List<Float> vertices = new ArrayList<>();
    
        // Generate vertex data for each block in the chunk
        for (int x = 0; x < CHUNK_X; x++) {
            for (int y = 0; y < CHUNK_Y; y++) {
                for (int z = 0; z < CHUNK_Z; z++) {
                    if (blocks[x][y][z] != null) {
                        addBlockToMesh(vertices, x, y, z);
                    }
                }
            }
        }
    
        // Convert List<Float> to float[]
        float[] vertexData = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexData[i] = vertices.get(i);
        }
    
        // Use BufferUtils for heap-based FloatBuffer
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertexData.length);
        vertexBuffer.put(vertexData).flip();
    
        // Upload vertex data to GPU
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
    }
    
    
    private void addBlockToMesh(List<Float> vertices, int x, int y, int z) {
        Blocks block = blocks[x][y][z];
    
        float xPos = this.xcoord + x;
        float yPos = this.ycoord + y;
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
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uTopStart); vertices.add(vTopStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uTopEnd); vertices.add(vTopStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uTopEnd); vertices.add(vTopEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uTopEnd); vertices.add(vTopEnd);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uTopStart); vertices.add(vTopEnd);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uTopStart); vertices.add(vTopStart);

        // Bottom face
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uBottomStart); vertices.add(vBottomStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uBottomEnd); vertices.add(vBottomStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uBottomEnd); vertices.add(vBottomEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uBottomEnd); vertices.add(vBottomEnd);
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uBottomStart); vertices.add(vBottomEnd);
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uBottomStart); vertices.add(vBottomStart);

        // Front face
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideStart); vertices.add(vSideEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideStart); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideStart); vertices.add(vSideEnd);

        // Back face
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideEnd); vertices.add(vSideEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideEnd); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideEnd); vertices.add(vSideEnd);

        // Left face
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideEnd);
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideEnd);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideStart);
        vertices.add(xPos - 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideEnd);

        // Right face
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideEnd);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos - 0.5f); vertices.add(uSideStart); vertices.add(vSideStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos + 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideStart);
        vertices.add(xPos + 0.5f); vertices.add(yPos - 0.5f); vertices.add(zPos + 0.5f); vertices.add(uSideEnd); vertices.add(vSideEnd);
    }
    
    private void generateFlatTerrain() {
        for (int x = 0; x < CHUNK_X; x++) {
            for (int z = 0; z < CHUNK_Z; z++) {
                blocks[x][0][z] = Blocks.GRASS;
            }
        }
    }
    
}
