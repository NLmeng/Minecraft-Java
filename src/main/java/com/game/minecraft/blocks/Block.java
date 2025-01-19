package com.game.minecraft.blocks;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Block {

    private final int vaoId;
    private final int CUBE_VERTEX_COUNT = 36; // 6 faces, 2 triangles each, 3 vertices each
    private final Matrix4f modelMatrix;
    private final float xpos;
    private final float ypos;
    private final float zpos;

    public Block(float atlasSize, float tileSize, int pixelX, int pixelY, float xpos, float ypos, float zpos) {
        this.vaoId = createBlockVAO(atlasSize, tileSize, pixelX, pixelY);
        this.xpos = xpos;
        this.ypos = ypos;
        this.zpos = zpos;
        this.modelMatrix = new Matrix4f().translate(this.xpos, this.ypos, this.zpos);
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getCubeVertexCount() {
        return CUBE_VERTEX_COUNT;
    }

    public Matrix4f getModelMatrix4f() {
        return modelMatrix;
    }

    public float getXpos() {
        return xpos;
    }

    public float getYpos() {
        return ypos;
    }

    public float getZpos() {
        return zpos;
    }

    private int createBlockVAO(float atlasSize, float tileSize, int pixelX, int pixelY) {
        float uStart = pixelX / atlasSize;
        float uEnd = (pixelX + tileSize) / atlasSize;
        float vStart = pixelY / atlasSize;
        float vEnd = (pixelY + tileSize) / atlasSize;

        float[] vertexData = {
            // 3d coords  / 2d texture coords
            // X,Y,Z      / U,V

            // Front
            -0.5f, -0.5f,  0.5f,  uStart, vEnd,
             0.5f, -0.5f,  0.5f,  uEnd,   vEnd,
             0.5f,  0.5f,  0.5f,  uEnd,   vStart,
             0.5f,  0.5f,  0.5f,  uEnd,   vStart,
            -0.5f,  0.5f,  0.5f,  uStart, vStart,
            -0.5f, -0.5f,  0.5f,  uStart, vEnd,

            // Back
            -0.5f, -0.5f, -0.5f,  uEnd,   vEnd,
             0.5f, -0.5f, -0.5f,  uStart, vEnd,
             0.5f,  0.5f, -0.5f,  uStart, vStart,
             0.5f,  0.5f, -0.5f,  uStart, vStart,
            -0.5f,  0.5f, -0.5f,  uEnd,   vStart,
            -0.5f, -0.5f, -0.5f,  uEnd,   vEnd,

            // Left
            -0.5f, -0.5f, -0.5f,  uEnd,   vEnd,
            -0.5f, -0.5f,  0.5f,  uStart, vEnd,
            -0.5f,  0.5f,  0.5f,  uStart, vStart,
            -0.5f,  0.5f,  0.5f,  uStart, vStart,
            -0.5f,  0.5f, -0.5f,  uEnd,   vStart,
            -0.5f, -0.5f, -0.5f,  uEnd,   vEnd,

            // Right
             0.5f, -0.5f,  0.5f,  uEnd,   vEnd,
             0.5f, -0.5f, -0.5f,  uStart, vEnd,
             0.5f,  0.5f, -0.5f,  uStart, vStart,
             0.5f,  0.5f, -0.5f,  uStart, vStart,
             0.5f,  0.5f,  0.5f,  uEnd,   vStart,
             0.5f, -0.5f,  0.5f,  uEnd,   vEnd,

            // Top
            -0.5f,  0.5f, -0.5f,  uStart, vStart,
             0.5f,  0.5f, -0.5f,  uEnd,   vStart,
             0.5f,  0.5f,  0.5f,  uEnd,   vEnd,
             0.5f,  0.5f,  0.5f,  uEnd,   vEnd,
            -0.5f,  0.5f,  0.5f,  uStart, vEnd,
            -0.5f,  0.5f, -0.5f,  uStart, vStart,

            // Bottom
            -0.5f, -0.5f, -0.5f,  uEnd,   vStart,
             0.5f, -0.5f, -0.5f,  uStart, vStart,
             0.5f, -0.5f,  0.5f,  uStart, vEnd,
             0.5f, -0.5f,  0.5f,  uStart, vEnd,
            -0.5f, -0.5f,  0.5f,  uEnd,   vEnd,
            -0.5f, -0.5f, -0.5f,  uEnd,   vStart,
        };

        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        int vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        try (MemoryStack stack = stackPush()) {
            FloatBuffer vertexBuffer = stack.mallocFloat(vertexData.length);
            vertexBuffer.put(vertexData).flip(); // set position to 0
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        }

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        return vaoId;
    }
}
