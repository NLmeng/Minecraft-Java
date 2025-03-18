package com.game.minecraft.world.generations;

import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.chunks.Chunk;
import com.game.minecraft.world.chunks.ChunkCoordinate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

// TODO: seed the random number generator with a fixed seed (from World) for reproducibility.
public class OreDecorator {

  private static final Random random = new Random();

  private static final int[][] DIRECTIONS = {
    {1, 0, 0},
    {-1, 0, 0},
    {0, 1, 0},
    {0, -1, 0},
    {0, 0, 1},
    {0, 0, -1}
  };

  private static final double SEED_PROBABILITY = 0.001; // 0.1%

  /**
   * Decorates ores over a merged region of active (and not yet ore-decorated) chunks. This method
   * is intended to be run only once per chunk load.
   */
  public void decorateOresForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    Map<ChunkCoordinate, Chunk> snapshot = new HashMap<>();
    for (Map.Entry<ChunkCoordinate, Chunk> entry : activeChunks.entrySet()) {
      Chunk chunk = entry.getValue();
      if (!chunk.isOreDecorated()) {
        snapshot.put(entry.getKey(), chunk);
      }
    }
    if (snapshot.isEmpty()) return;

    int minChunkX = Integer.MAX_VALUE;
    int maxChunkX = Integer.MIN_VALUE;
    int minChunkZ = Integer.MAX_VALUE;
    int maxChunkZ = Integer.MIN_VALUE;
    for (ChunkCoordinate coord : snapshot.keySet()) {
      minChunkX = Math.min(minChunkX, coord.x());
      maxChunkX = Math.max(maxChunkX, coord.x());
      minChunkZ = Math.min(minChunkZ, coord.z());
      maxChunkZ = Math.max(maxChunkZ, coord.z());
    }
    int chunksX = maxChunkX - minChunkX + 1;
    int chunksZ = maxChunkZ - minChunkZ + 1;
    final int regionWidth = chunksX * Chunk.CHUNK_X;
    final int regionDepth = chunksZ * Chunk.CHUNK_Z;
    final int regionHeight = Chunk.CHUNK_Y;

    Blocks[][][] regionBlocks = new Blocks[regionWidth][regionHeight][regionDepth];
    for (Map.Entry<ChunkCoordinate, Chunk> entry : snapshot.entrySet()) {
      ChunkCoordinate coord = entry.getKey();
      Chunk chunk = entry.getValue();
      int offsetX = (coord.x() - minChunkX) * Chunk.CHUNK_X;
      int offsetZ = (coord.z() - minChunkZ) * Chunk.CHUNK_Z;
      Blocks[][][] chunkBlocks = chunk.getAllBlocks();
      for (int x = 0; x < Chunk.CHUNK_X; x++) {
        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          System.arraycopy(
              chunkBlocks[x][y], 0, regionBlocks[offsetX + x][y], offsetZ, Chunk.CHUNK_Z);
        }
      }
    }

    // For each (x,z) column, find the surface and try to seed an ore vein below it.
    for (int rx = 0; rx < regionWidth; rx++) {
      for (int rz = 0; rz < regionDepth; rz++) {
        int surfaceY = findSurfaceY(regionBlocks, rx, rz, regionHeight);
        if (surfaceY < 0) continue;

        // Compute vertical thresholds based on the surface.
        int bedrockTopLimit = regionHeight - (((surfaceY % 5) + 2));
        int subsurfaceTopLimit = bedrockTopLimit - (((surfaceY % 15) + 32));
        int upperOreTopLimit = bedrockTopLimit - (((surfaceY % 10) + 42));
        int middleOreTopLimit = bedrockTopLimit - (((surfaceY % 10) + 25));
        int deeperOreTopLimit = bedrockTopLimit - (((surfaceY % 10) + 12));

        // For each block below the surface, attempt to seed an ore vein.
        for (int y = surfaceY + 1; y < regionHeight; y++) {
          if (!isEligibleForOre(regionBlocks[rx][y][rz])) continue;
          if (Math.random() > SEED_PROBABILITY) continue;
          OreSettings settings =
              determineOreSettings(y, upperOreTopLimit, middleOreTopLimit, deeperOreTopLimit);
          if (settings == null) continue;
          if (isOre(regionBlocks[rx][y][rz])) continue;
          generateOreVein(regionBlocks, rx, y, rz, settings, rx, y, rz);
        }
      }
    }

    for (Map.Entry<ChunkCoordinate, Chunk> entry : snapshot.entrySet()) {
      ChunkCoordinate coord = entry.getKey();
      Chunk chunk = entry.getValue();
      int offsetX = (coord.x() - minChunkX) * Chunk.CHUNK_X;
      int offsetZ = (coord.z() - minChunkZ) * Chunk.CHUNK_Z;
      Blocks[][][] newChunkData = new Blocks[Chunk.CHUNK_X][Chunk.CHUNK_Y][Chunk.CHUNK_Z];
      for (int x = 0; x < Chunk.CHUNK_X; x++) {
        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          System.arraycopy(
              regionBlocks[offsetX + x][y], offsetZ, newChunkData[x][y], 0, Chunk.CHUNK_Z);
        }
      }
      chunk.setBlockData(newChunkData);
      chunk.setOreDecorated(true);
    }
  }

  // Returns the first non-null block's y-coordinate in column (rx, rz).
  private int findSurfaceY(Blocks[][][] region, int rx, int rz, int regionHeight) {
    for (int y = 0; y < regionHeight; y++) {
      if (region[rx][y][rz] != null) return y;
    }
    return -1;
  }

  // Checks if a block is eligible for ore replacement.
  private boolean isEligibleForOre(Blocks block) {
    return block != null && (block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.SAND);
  }

  // Checks if the block is already an ore.
  private boolean isOre(Blocks block) {
    return block == Blocks.COAL_ORE
        || block == Blocks.IRON_ORE
        || block == Blocks.GOLD_ORE
        || block == Blocks.DIAMOND_ORE;
  }

  /**
   * Determines ore settings based on vertical layer. Returns an OreSettings instance if ore should
   * be generated, or null.
   */
  private OreSettings determineOreSettings(int y, int upperOre, int middleOre, int deeperOre) {
    double r = Math.random();
    if (y > deeperOre) { // deep layer: diamond, gold, iron, coal
      if (r < 0.05) {
        return new OreSettings(Blocks.DIAMOND_ORE, 6, 2);
      } else if (r < 0.15) {
        return new OreSettings(Blocks.GOLD_ORE, 10, 3);
      } else if (r < 0.50) {
        return new OreSettings(Blocks.IRON_ORE, 12, 3);
      } else {
        return new OreSettings(Blocks.COAL_ORE, 20, 4);
      }
    } else if (y > middleOre) { // middle layer
      if (r < 0.10) {
        return new OreSettings(Blocks.GOLD_ORE, 10, 3);
      } else if (r < 0.45) {
        return new OreSettings(Blocks.IRON_ORE, 12, 3);
      } else {
        return new OreSettings(Blocks.COAL_ORE, 20, 4);
      }
    } else if (y > upperOre) { // upper layer: only coal
      return new OreSettings(Blocks.COAL_ORE, 20, 4);
    }
    return null;
  }

  /**
   * Strict flood-fill vein generation. Starting at (sx, sy, sz), replaces up to maxVeinSize
   * eligible blocks with oreType. Propagation is only allowed within a Manhattan distance of
   * maxRadius from the seed. seedX, seedY, seedZ are the original seed coordinates.
   */
  private void generateOreVein(
      Blocks[][][] region,
      int sx,
      int sy,
      int sz,
      OreSettings settings,
      int seedX,
      int seedY,
      int seedZ) {
    if (!inBounds(sx, sy, sz, region)) return;
    if (!isEligibleForOre(region[sx][sy][sz])) return;
    int count = 0;
    Queue<int[]> queue = new LinkedList<>();
    queue.add(new int[] {sx, sy, sz});
    while (!queue.isEmpty() && count < settings.maxVeinSize) {
      int[] pos = queue.poll();
      int x = pos[0], y = pos[1], z = pos[2];
      if (!inBounds(x, y, z, region)) continue;
      int dist = Math.abs(x - seedX) + Math.abs(y - seedY) + Math.abs(z - seedZ);
      if (dist > settings.maxRadius) continue;
      if (!isEligibleForOre(region[x][y][z])) continue;
      region[x][y][z] = settings.oreType;
      count++;
      for (int[] d : DIRECTIONS) {
        int nx = x + d[0], ny = y + d[1], nz = z + d[2];
        if (inBounds(nx, ny, nz, region) && Math.random() < 0.3) { // propagation chance
          queue.add(new int[] {nx, ny, nz});
        }
      }
    }
  }

  // Checks whether (x, y, z) is within the bounds of the region.
  private boolean inBounds(int x, int y, int z, Blocks[][][] region) {
    return x >= 0
        && x < region.length
        && y >= 0
        && y < region[0].length
        && z >= 0
        && z < region[0][0].length;
  }

  /**
   * Container for ore settings. oreType: the ore block type. maxVeinSize: the strict maximum number
   * of blocks in this vein (randomized within a range). maxRadius: the maximum Manhattan distance
   * from the seed that the vein is allowed to grow.
   */
  private static class OreSettings {
    final Blocks oreType;
    final int maxVeinSize;
    final int maxRadius;

    OreSettings(Blocks oreType, int maxVeinSize, int maxRadius) {
      this.oreType = oreType;
      this.maxVeinSize = randomBetween(1, maxVeinSize);
      this.maxRadius = maxRadius;
    }
  }

  private static int randomBetween(int min, int max) {
    return random.nextInt(max - min + 1) + min;
  }
}
