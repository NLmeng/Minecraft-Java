package com.game.minecraft;

import com.game.minecraft.camera.Camera;
import com.game.minecraft.camera.Renderer;

public class Main {
  public static void main(String[] args) {
    // create window and intialize glfw, opengl
    Window window = new Window(800, 600, "Minecraft");
    window.init();

    Camera camera = new Camera();
    WindowRef.camera = camera;

    Renderer renderer = new Renderer();
    renderer.init();

    window.loop(camera, renderer);

    window.cleanup();
  }
}
