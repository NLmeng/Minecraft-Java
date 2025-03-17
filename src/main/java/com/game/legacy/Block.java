package com.game.legacy;
// legacy: update to use FloatArray

// package com.game.minecraft.world;

// import static org.lwjgl.opengl.GL46C.*;
// import static org.lwjgl.system.MemoryStack.stackPush;

// import java.nio.FloatBuffer;
// import java.util.ArrayList;
// import java.util.List;
// import org.joml.Matrix4f;
// import org.lwjgl.system.MemoryStack;

// public class Block {

//   private final int vaoId;
//   private final int CUBE_VERTEX_COUNT = 36; // 6 faces, 2 triangles each, 3 vertices each
//   private final Matrix4f modelMatrix;
//   private final float xpos;
//   private final float ypos;
//   private final float zpos;

//   public Block(float xpos, float ypos, float zpos, Blocks block) {
//     this.vaoId = createBlockVAO(block);
//     this.xpos = xpos;
//     this.ypos = ypos;
//     this.zpos = zpos;
//     this.modelMatrix = new Matrix4f().translate(this.xpos, this.ypos, this.zpos);
//   }

//   public int getVaoId() {
//     return vaoId;
//   }

//   public int getCubeVertexCount() {
//     return CUBE_VERTEX_COUNT;
//   }

//   public Matrix4f getModelMatrix4f() {
//     return modelMatrix;
//   }

//   public float getXpos() {
//     return xpos;
//   }

//   public float getYpos() {
//     return ypos;
//   }

//   public float getZpos() {
//     return zpos;
//   }

//   private int createBlockVAO(Blocks block) {
//     List<Float> vertices = new ArrayList<>();

//     Vertex.addNormalTopFaceWithTexture(vertices, 0, 0, 0, block.getTopX(), block.getTopY());
//     Vertex.addNormalBottomFaceWithTexture(
//         vertices, 0, 0, 0, block.getBottomX(), block.getBottomY());
//     Vertex.addNormalFrontFaceWithTexture(vertices, 0, 0, 0, block.getSideX(), block.getSideY());
//     Vertex.addNormalBackFaceWithTexture(vertices, 0, 0, 0, block.getSideX(), block.getSideY());
//     Vertex.addNormalLeftFaceWithTexture(vertices, 0, 0, 0, block.getSideX(), block.getSideY());
//     Vertex.addNormalRightFaceWithTexture(vertices, 0, 0, 0, block.getSideX(), block.getSideY());

//     float[] vertexData = new float[vertices.size()];
//     for (int i = 0; i < vertices.size(); i++) {
//       vertexData[i] = vertices.get(i);
//     }

//     int vaoId = glGenVertexArrays();
//     glBindVertexArray(vaoId);

//     int vboId = glGenBuffers();
//     glBindBuffer(GL_ARRAY_BUFFER, vboId);

//     try (MemoryStack stack = stackPush()) {
//       FloatBuffer vertexBuffer = stack.mallocFloat(vertexData.length);
//       vertexBuffer.put(vertexData).flip(); // set position to 0
//       glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
//     }

//     glVertexAttribPointer(
//         0, // index, use 0 for xyz
//         3, // num of components: xyz
//         GL_FLOAT, // data type
//         false, // is normalized?
//         5 * Float.BYTES, // offset to next attributes (3 from index 0, 2 from index 1 below)
//         0L // starting position
//         );
//     glEnableVertexAttribArray(0);
//     glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);
//     glEnableVertexAttribArray(1);

//     glBindBuffer(GL_ARRAY_BUFFER, 0);
//     glBindVertexArray(0);

//     return vaoId;
//   }
// }
