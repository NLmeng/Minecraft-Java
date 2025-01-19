package com.game.minecraft.input;

import static org.lwjgl.glfw.GLFW.*;

import com.game.minecraft.WindowRef;

public class Keyboard {

  public static void handleKeyboardInput(long windowHandle, float deltaTime) {
    if (glfwGetKey(windowHandle, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
      glfwSetWindowShouldClose(windowHandle, true);
    }

    if (WindowRef.camera != null) {
      if (glfwGetKey(windowHandle, GLFW_KEY_W) == GLFW_PRESS)
        WindowRef.camera.processKeyboard(Movement.FORWARD, deltaTime);
      if (glfwGetKey(windowHandle, GLFW_KEY_A) == GLFW_PRESS)
        WindowRef.camera.processKeyboard(Movement.LEFT, deltaTime);
      if (glfwGetKey(windowHandle, GLFW_KEY_S) == GLFW_PRESS)
        WindowRef.camera.processKeyboard(Movement.BACKWARD, deltaTime);
      if (glfwGetKey(windowHandle, GLFW_KEY_D) == GLFW_PRESS)
        WindowRef.camera.processKeyboard(Movement.RIGHT, deltaTime);
    }
  }
}
