package com.game.minecraft.camera;

import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import com.game.minecraft.utils.FileReader;
import com.game.minecraft.world.Block;
import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.Chunk;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class Renderer {

  private final String VERTEX_SHADER_SRC = FileReader.loadFromResource("shaders/vertex.glsl");
  private final String FRAGMENT_SHADER_SRC = FileReader.loadFromResource("shaders/fragment.glsl");

  private int shaderProgram;
  private int atlasTextureId;
  private int uMVP;

  private final float FOV = 70f;
  private final float ZNEAR = 0.1f;
  private final float ZFAR = 100f;

  // Blocks
  private Block blockA;
  private Block blockB;

  // Chunks
  private Chunk chunkA;

  public void init() {
    shaderProgram = createShaderProgram(VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
    uMVP = glGetUniformLocation(shaderProgram, "uMVP");
    atlasTextureId = loadFullAtlas("assets/atlas.png");

    blockA = new Block(0.0f, -10f, -50f, Blocks.DIRT);
    blockB = new Block(1.0f, -10f, -50f, Blocks.STONE);

    chunkA = new Chunk(0.0f, -2, 0.0f);

    glEnable(GL_DEPTH_TEST); // add 3d layers to models
  }

  // private long lastRemovalTime;
  // private static final long REMOVAL_INTERVAL = 10000;

  // private void testRemoveRandomTopLayerBlocks(Chunk chunk) {
  //   Random random = new Random();

  //   for (int x = 0; x < Chunk.CHUNK_X; x++) {
  //     for (int z = 0; z < Chunk.CHUNK_Z; z++) {
  //       if (random.nextBoolean()) {
  //         chunk.setBlockAt(x, 0, z, null);
  //       }
  //     }
  //   }
  // }

  public void render(Camera camera, int width, int height) {
    glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glUseProgram(shaderProgram);

    Matrix4f projection =
        new Matrix4f()
            .perspective(
                (float) Math.toRadians(FOV), // fov
                (float) width / height, // aspect ratio
                ZNEAR, // clipping
                ZFAR); // render distance

    Matrix4f view = camera.getViewMatrix(); // cameras position & orient

    // long currentTime = System.currentTimeMillis();
    // if (currentTime - lastRemovalTime >= REMOVAL_INTERVAL) {
    //   testRemoveRandomTopLayerBlocks(chunkA);
    //   lastRemovalTime = currentTime;
    // }

    renderBlock(blockA, projection, view);
    renderBlock(blockB, projection, view);

    renderChunk(chunkA, projection, view);
    glBindVertexArray(0);
    glUseProgram(0);
  }

  private void renderBlock(Block block, Matrix4f projection, Matrix4f view) {
    renderObject(
        projection, view, block.getModelMatrix4f(), block.getVaoId(), block.getCubeVertexCount());
  }

  private void renderChunk(Chunk chunk, Matrix4f projection, Matrix4f view) {
    renderObject(
        projection, view, chunk.getModelMatrix4f(), chunk.getVaoId(), chunk.getVertexCount());
  }

  private void renderObject(
      Matrix4f projection, Matrix4f view, Matrix4f modelMatrix, int vaoId, int vertexCount) {
    Matrix4f mvp = projection.mul(view, new Matrix4f()).mul(modelMatrix);
    setMVPUniform(mvp);
    glBindTexture(GL_TEXTURE_2D, atlasTextureId);
    glBindVertexArray(vaoId);
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
  }

  private void setMVPUniform(Matrix4f mvp) {
    try (MemoryStack stack = stackPush()) {
      FloatBuffer matrixData = stack.mallocFloat(16);
      mvp.get(matrixData); // actually copies to matrixdata and not other way around
      glUniformMatrix4fv(
          uMVP, // uniformed for gpu
          false, // not transposed (row-major)
          matrixData // matrix data
          );
    }
  }

  private int loadFullAtlas(String atlasPath) {
    int textureId = glGenTextures();
    glBindTexture(GL_TEXTURE_2D, textureId);

    ByteBuffer imageData = FileReader.loadTextureFromResource(atlasPath);
    STBImage.stbi_set_flip_vertically_on_load(false);

    try (MemoryStack stack = stackPush()) {
      IntBuffer width = stack.mallocInt(1);
      IntBuffer height = stack.mallocInt(1);
      IntBuffer channel = stack.mallocInt(1);

      ByteBuffer img = STBImage.stbi_load_from_memory(imageData, width, height, channel, 4);
      if (img == null) {
        throw new RuntimeException("Failed to load atlas: " + atlasPath);
      }

      glTexImage2D(
          GL_TEXTURE_2D, // 2d
          0, // set mipmap 0 = base texture
          GL_RGBA, // 4channels
          width.get(0),
          height.get(0),
          0, // border
          GL_RGBA,
          GL_UNSIGNED_BYTE, // format & datatype of texture data
          img);
      glGenerateMipmap(GL_TEXTURE_2D);

      STBImage.stbi_image_free(img);
    }

    // use nearest neighbor interpolation to mimic minecraft pixel art
    glTexParameteri(
        GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST); // minification
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); // magnification
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    return textureId;
  }

  private int createShaderProgram(String vertexSrc, String fragmentSrc) {
    int vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader, vertexSrc);
    glCompileShader(vertexShader);
    checkShaderErrors(vertexShader, "VERTEX");

    int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragmentShader, fragmentSrc);
    glCompileShader(fragmentShader);
    checkShaderErrors(fragmentShader, "FRAGMENT");

    int program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);
    checkLinkErrors(program);

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    return program;
  }

  private void checkShaderErrors(int shader, String type) {
    if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
      throw new RuntimeException(type + " shader error: " + glGetShaderInfoLog(shader));
    }
  }

  private void checkLinkErrors(int program) {
    if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
      throw new RuntimeException("Program link error: " + glGetProgramInfoLog(program));
    }
  }
}
