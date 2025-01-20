package com.game.minecraft.world;

/** Enum representing unique blocks and their associated position within the atlas. */
public enum Blocks {
  GRASS(48, 240),
  DIRT(32, 240),
  STONE(16, 240),
  BEDROCK(16, 224);

  private final byte x;
  private final byte y;

  Blocks(int x, int y) {
    this.x = (byte) x;
    this.y = (byte) y;
  }

  public int getX() {
    return Byte.toUnsignedInt(x);
  }

  public int getY() {
    return Byte.toUnsignedInt(y);
  }
}
