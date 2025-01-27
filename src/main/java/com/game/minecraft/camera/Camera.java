package com.game.minecraft.camera;

import com.game.minecraft.input.Movement;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

  private Vector3f position;
  private Vector3f front; // where camera is looking at
  private Vector3f up; // direction vector of what is "up"

  // Euler angles
  private float yaw; // look left right
  private float pitch; // look up down
  private float roll; // look front back / might not need for first-person

  private float movementSpeed = 5.0f; // per second
  private float mouseSensitivity = 0.1f;

  public Camera() {
    position = new Vector3f(0f, -200f, 3f);
    front = new Vector3f(0f, 0f, -1f);
    up = new Vector3f(0f, 1f, 0f);

    yaw = -90.0f;
    pitch = 0.0f;
  }

  public void processKeyboard(Movement direction, float deltaTime) {
    float velocity = movementSpeed * deltaTime; // move same distance independent of fps

    switch (direction) {
      case FORWARD:
        position.add(new Vector3f(front).mul(velocity));
        break;
      case BACKWARD:
        position.sub(new Vector3f(front).mul(velocity));
        break;
        // Strafing: cross product of front and up gives vector pointing to right of camera view
      case LEFT:
        position.sub(new Vector3f(front).cross(up).normalize().mul(velocity));
        break;
      case RIGHT:
        position.add(new Vector3f(front).cross(up).normalize().mul(velocity));
        break;
      default:
        // TODO: error handle
        break;
    }
  }

  public void processMouseMovement(float xoffset, float yoffset) {
    xoffset *= mouseSensitivity;
    yoffset *= mouseSensitivity;

    yaw += xoffset;
    pitch += yoffset;

    // constrain to avoid flip
    if (pitch > 89.0f) pitch = 89.0f;
    if (pitch < -89.0f) pitch = -89.0f;

    updateCameraVectors();
  }

  public Matrix4f getViewMatrix() {
    return new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
  }

  public Vector3f getPosition() {
    return position;
  }

  public Vector3f getFront() {
    return front;
  }

  private void updateCameraVectors() {
    Vector3f newFront = new Vector3f();
    newFront.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
    newFront.y = (float) Math.sin(Math.toRadians(pitch));
    newFront.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
    front = newFront.normalize();
  }
}
