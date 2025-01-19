package com.game.minecraft.input;

import com.game.minecraft.WindowRef;

public class Mouse {

  private static boolean firstMouse = true;
  private static double lastX;
  private static double lastY;

  public static void handleMouseMove(double xpos, double ypos) {
    if (firstMouse) {
      lastX = xpos;
      lastY = ypos;
      firstMouse = false;
    }

    double xoffset = xpos - lastX;
    double yoffset = lastY - ypos;

    lastX = xpos;
    lastY = ypos;

    if (WindowRef.camera != null) { // option1:use windowref, option2: let window handle
      WindowRef.camera.processMouseMovement((float) xoffset, (float) yoffset);
    }
  }
}
