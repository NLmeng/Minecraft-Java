package com.game.minecraft.world;

/** Enum representing unique blocks and their associated position within the atlas. */
public enum Blocks {
  // (y, x) = (row / up-down, column / left-right) in the atlas
  // (0,2) + (0,3) + (1,14) Bottom: dirt, Sides: grass block, Top: grass
  GRASS(14, 1, 2, 0, 3, 0),
  // (0,2)
  DIRT(2, 0, 2, 0, 2, 0),
  // (4,8)
  CLAY(8, 4, 8, 4, 8, 4),
  // (1,3)
  GRAVEL(3, 1, 3, 1, 3, 1),
  // (1,2)
  SAND(2, 1, 2, 1, 2, 1),
  // (0,1)
  STONE(1, 0, 1, 0, 1, 0),
  // (1,1)
  BEDROCK(1, 1, 1, 1, 1, 1),
  // (2,5)
  OBSIDIAN(5, 2, 5, 2, 5, 2),
  // (1,0)
  COBBLESTONE(0, 1, 0, 1, 0, 1),
  // (2,4)
  MOSS_COBBLESTONE(4, 2, 4, 2, 4, 2),
  // (2,0)
  GOLD_ORE(0, 2, 0, 2, 0, 2),
  // (2,1)
  IRON_ORE(1, 2, 1, 2, 1, 2),
  // (2,2)
  COAL_ORE(2, 2, 2, 2, 2, 2),
  // (3,2)
  DIAMOND_ORE(2, 3, 2, 3, 2, 3),
  // (10, 0)
  LAPIS_ORE(0, 10, 0, 10, 0, 10),
  // (3,3)
  REDSTONE_ORE(3, 3, 3, 3, 3, 3),
  // (1,4) + (1,5)
  OAKWOOD(5, 1, 5, 1, 4, 1),
  ;
  private final byte topX, topY;
  private final byte bottomX, bottomY;
  private final byte sideX, sideY;
  private boolean isSolid;
  private static final int PIXEL_OFFSET = 16;

  Blocks(int topX, int topY, int bottomX, int bottomY, int sideX, int sideY) {
    this.topX = (byte) (topX * PIXEL_OFFSET);
    this.topY = (byte) (topY * PIXEL_OFFSET);
    this.bottomX = (byte) (bottomX * PIXEL_OFFSET);
    this.bottomY = (byte) (bottomY * PIXEL_OFFSET);
    this.sideX = (byte) (sideX * PIXEL_OFFSET);
    this.sideY = (byte) (sideY * PIXEL_OFFSET);
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
