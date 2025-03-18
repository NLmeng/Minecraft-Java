package com.game.minecraft;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryUtil.*;

import com.game.minecraft.camera.Camera;
import com.game.minecraft.camera.Renderer;
import com.game.minecraft.input.Keyboard;
import com.game.minecraft.input.Mouse;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

public class Window {

  private long windowHandle;
  private int width;
  private int height;
  private String title;

  private float lastFrameTime;
  private float deltaTime; // time since last frame

  public Window(int width, int height, String title) {
    this.width = width;
    this.height = height;
    this.title = title;
  }

  public void init() {
    GLFWErrorCallback.createPrint(System.err).set();

    if (!glfwInit()) {
      throw new IllegalStateException("Failed to initialize GLFW");
    }

    // use GLFW with core opengl 3.3
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

    windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
    if (windowHandle == NULL) {
      throw new RuntimeException("Failed to create GLFW window");
    }

    // Center window on primary monitor
    GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
    if (vidMode != null) {
      glfwSetWindowPos(
          windowHandle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
    }

    glfwMakeContextCurrent(windowHandle);

    glfwSwapInterval(1); // enable vsync

    glfwShowWindow(windowHandle);

    GL.createCapabilities();

    glEnable(GL_DEPTH_TEST); // add layers to objects

    // hide & capture the mouse
    glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    glfwSetCursorPosCallback(
        windowHandle,
        (win, xpos, ypos) -> {
          Mouse.handleMouseMove(xpos, ypos);
        });

    lastFrameTime = (float) glfwGetTime();
  }

  public void loop(Camera camera, Renderer renderer) {
    while (!glfwWindowShouldClose(windowHandle)) {
      float currentFrame = (float) glfwGetTime();
      deltaTime = currentFrame - lastFrameTime;
      lastFrameTime = currentFrame;

      glfwPollEvents();
      Keyboard.handleKeyboardInput(windowHandle, deltaTime);

      renderer.render(camera, width, height);

      // front buffer = displaying, back buffer = being rendered
      glfwSwapBuffers(windowHandle);
    }
  }

  public void cleanup(Renderer renderer) {
    renderer.shutdown();
    glfwTerminate();
    glfwSetErrorCallback(null).free();
  }
}
