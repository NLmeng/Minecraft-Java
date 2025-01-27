package com.game.minecraft.world;

/** Enum representing unique blocks and their associated position within the atlas. */
public enum Blocks {
  GRASS(224, 16, 32, 0, 48, 0), // Top: grass, Bottom: dirt, Sides: grass block
  DIRT(32, 0, 32, 0, 32, 0),
  STONE(16, 0, 16, 0, 16, 0),
  BEDROCK(16, 16, 16, 16, 16, 16);
  private final byte topX, topY;
  private final byte bottomX, bottomY;
  private final byte sideX, sideY;
  private boolean isSolid;

  Blocks(int topX, int topY, int bottomX, int bottomY, int sideX, int sideY) {
    this.topX = (byte) topX;
    this.topY = (byte) topY;
    this.bottomX = (byte) bottomX;
    this.bottomY = (byte) bottomY;
    this.sideX = (byte) sideX;
    this.sideY = (byte) sideY;
    this.isSolid = true;
  }

  public boolean isSolid() {
    return isSolid;
  }

  public void setSolidStateAs(boolean state) {
    isSolid = state;
  }

  public int getTopX() {
    return Byte.toUnsignedInt(topX);
  }

  public int getTopY() {
    return Byte.toUnsignedInt(topY);
  }

  public int getBottomX() {
    return Byte.toUnsignedInt(bottomX);
  }

  public int getBottomY() {
    return Byte.toUnsignedInt(bottomY);
  }

  public int getSideX() {
    return Byte.toUnsignedInt(sideX);
  }

  public int getSideY() {
    return Byte.toUnsignedInt(sideY);
  }
}
