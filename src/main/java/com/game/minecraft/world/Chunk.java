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

    }
    
    private void generateFlatTerrain() {
        for (int x = 0; x < CHUNK_X; x++) {
            for (int z = 0; z < CHUNK_Z; z++) {
                blocks[x][0][z] = Blocks.GRASS;
            }
        }
    }
    
}
