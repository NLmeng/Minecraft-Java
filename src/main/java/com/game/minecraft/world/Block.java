package com.game.minecraft.world;

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

    public Block(float xpos, float ypos, float zpos, Blocks block) {
        this.vaoId = createBlockVAO(block);
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

    private int createBlockVAO(Blocks block) {
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

        float[] vertexData = {
            // each faces +- 0.5f to extend about 0,0,0 to 1x1x1
            // 3d coords  / 2d texture coords
            // X,Y,Z      / U,V
            // Top face
            -0.5f,  0.5f, -0.5f,  uTopStart, vTopStart,
            0.5f,  0.5f, -0.5f,  uTopEnd,   vTopStart,
            0.5f,  0.5f,  0.5f,  uTopEnd,   vTopEnd,
            0.5f,  0.5f,  0.5f,  uTopEnd,   vTopEnd,
            -0.5f,  0.5f,  0.5f,  uTopStart, vTopEnd,
            -0.5f,  0.5f, -0.5f,  uTopStart, vTopStart,

            // Bottom face
            -0.5f, -0.5f, -0.5f,  uBottomStart, vBottomStart,
            0.5f, -0.5f, -0.5f,  uBottomEnd,   vBottomStart,
            0.5f, -0.5f,  0.5f,  uBottomEnd,   vBottomEnd,
            0.5f, -0.5f,  0.5f,  uBottomEnd,   vBottomEnd,
            -0.5f, -0.5f,  0.5f,  uBottomStart, vBottomEnd,
            -0.5f, -0.5f, -0.5f,  uBottomStart, vBottomStart,

            // Front face
            -0.5f, -0.5f,  0.5f,  uSideStart, vSideEnd,
            0.5f, -0.5f,  0.5f,  uSideEnd,   vSideEnd,
            0.5f,  0.5f,  0.5f,  uSideEnd,   vSideStart,
            0.5f,  0.5f,  0.5f,  uSideEnd,   vSideStart,
            -0.5f,  0.5f,  0.5f,  uSideStart, vSideStart,
            -0.5f, -0.5f,  0.5f,  uSideStart, vSideEnd,

            // Back face
            -0.5f, -0.5f, -0.5f,  uSideEnd,   vSideEnd,
            0.5f, -0.5f, -0.5f,  uSideStart, vSideEnd,
            0.5f,  0.5f, -0.5f,  uSideStart, vSideStart,
            0.5f,  0.5f, -0.5f,  uSideStart, vSideStart,
            -0.5f,  0.5f, -0.5f,  uSideEnd,   vSideStart,
            -0.5f, -0.5f, -0.5f,  uSideEnd,   vSideEnd,

            // Left face
            -0.5f, -0.5f, -0.5f,  uSideStart, vSideEnd,
            -0.5f, -0.5f,  0.5f,  uSideEnd,   vSideEnd,
            -0.5f,  0.5f,  0.5f,  uSideEnd,   vSideStart,
            -0.5f,  0.5f,  0.5f,  uSideEnd,   vSideStart,
            -0.5f,  0.5f, -0.5f,  uSideStart, vSideStart,
            -0.5f, -0.5f, -0.5f,  uSideStart, vSideEnd,

            // Right face
            0.5f, -0.5f,  0.5f,  uSideEnd,   vSideEnd,
            0.5f, -0.5f, -0.5f,  uSideStart, vSideEnd,
            0.5f,  0.5f, -0.5f,  uSideStart, vSideStart,
            0.5f,  0.5f, -0.5f,  uSideStart, vSideStart,
            0.5f,  0.5f,  0.5f,  uSideEnd,   vSideStart,
            0.5f, -0.5f,  0.5f,  uSideEnd,   vSideEnd,
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
