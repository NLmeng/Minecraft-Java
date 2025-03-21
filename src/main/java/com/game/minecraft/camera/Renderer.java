package com.game.minecraft.camera;

import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import com.game.minecraft.utils.FileReader;
import com.game.minecraft.world.World;
import com.game.minecraft.world.chunks.Chunk;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class Renderer {

  private final String VERTEX_SHADER_SRC = FileReader.loadFromResource("shaders/vertex.glsl");
  private final String FRAGMENT_SHADER_SRC = FileReader.loadFromResource("shaders/fragment.glsl");
  private final int MAX_BUILD_PER_FRAME = 2;

  private int shaderProgram;
  private int atlasTextureId;
  private int uMVP;
  private int uIsWater;

  private World world;

  private float FOV = 70f;
  private float ZNEAR = 0.1f;
  private float zFar = 1000f;

  public void init() {
    shaderProgram = createShaderProgram(VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
    uMVP = glGetUniformLocation(shaderProgram, "uMVP");
    uIsWater = glGetUniformLocation(shaderProgram, "uIsWater");
    atlasTextureId = loadFullAtlas("assets/atlas.png");

    world = new World();
    setRenderDistance(3);

    glEnable(GL_DEPTH_TEST); // add 3d layers to models
  }

  public void render(Camera camera, int width, int height) {
    glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glUseProgram(shaderProgram);
    glBindTexture(GL_TEXTURE_2D, atlasTextureId);

    Matrix4f projection =
        new Matrix4f()
            .perspective(
                (float) Math.toRadians(FOV), // fov
                (float) width / height, // aspect ratio
                ZNEAR, // clipping
                zFar); // render distance

    Matrix4f view = camera.getViewMatrix(); // cameras position & orient

    Matrix4f projectionView = projection.mul(view, new Matrix4f());

    world.updatePlayerPosition(camera.getPosition().x, camera.getPosition().z);

    // render solid/opaques
    int buildsThisFrame = 0;
    for (Chunk chunk : world.getActiveChunks()) {
      if (chunk.isDirty() && buildsThisFrame < MAX_BUILD_PER_FRAME) {
        chunk.buildMesh();
        buildsThisFrame++;
      }
      renderObject(
          projectionView,
          chunk.getModelMatrix4f(),
          chunk.getOpaqueVaoId(),
          chunk.getOpaqueVertexCount());
    }
    // render water
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glUniform1i(uIsWater, 1);
    for (Chunk chunk : world.getActiveChunks()) {
      renderObject(
          projectionView,
          chunk.getModelMatrix4f(),
          chunk.getWaterVaoId(),
          chunk.getWaterVertexCount());
    }
    glUniform1i(uIsWater, 0);
    glDisable(GL_BLEND);

    glBindVertexArray(0);
    glUseProgram(0);
  }

  private void renderObject(
      Matrix4f projectionView, Matrix4f modelMatrix, int vaoId, int vertexCount) {
    Matrix4f mvp = projectionView.mul(modelMatrix, new Matrix4f());
    setMVPUniform(mvp);
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

  public void setRenderDistance(int layerRadius) {
    world.setChunkLayerRadius(layerRadius);
    zFar = layerRadius * Chunk.CHUNK_X + Chunk.CHUNK_X;
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

  public void shutdown() {
    world.shutdown();
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
