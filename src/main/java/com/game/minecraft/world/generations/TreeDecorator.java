package com.game.minecraft.world.generations;

import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.chunks.Chunk;
import com.game.minecraft.world.chunks.ChunkCoordinate;
import java.util.HashMap;
import java.util.Map;

// TODO: maybe make this once per chunk load like ore decorator
/**
 * Merges chunks into a region, decorates the region with trees, then writes back the modified data.
 */
public class TreeDecorator {

  public void decorateTreesForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    // snapshot
    Map<ChunkCoordinate, Chunk> snapshot = new HashMap<>();
    for (Map.Entry<ChunkCoordinate, Chunk> entry : activeChunks.entrySet()) {
      Chunk chunk = entry.getValue();
      if (!chunk.isTreeDecorated()) {
        snapshot.put(entry.getKey(), chunk);
      }
    }
    if (snapshot.isEmpty()) return;

    // bound
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

    // merge
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

    // decorate
    decorateTreesForRegion(regionBlocks, regionWidth, regionDepth);

    // writeback
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
      chunk.setTreeDecorated(true);
    }
  }

  private void decorateTreesForRegion(Blocks[][][] regionBlocks, int regionWidth, int regionDepth) {
    final int regionHeight = Chunk.CHUNK_Y;

    // tree configs
    double treeFrequency = 0.05;
    double treeThreshold = 0.5;
    int minTrunkHeight = 4;
    int maxTrunkHeight = 6;
    int baseCanopyLayers = 3;
    int baseCanopyRadius = 2;

    for (int rx = 0; rx < regionWidth; rx++) {
      for (int rz = 0; rz < regionDepth; rz++) {
        int surfaceY = findSurfaceY(regionBlocks, rx, rz, regionHeight);
        if (surfaceY < 0) {
          continue;
        }

        Blocks blockBelow = regionBlocks[rx][surfaceY][rz];
        if (blockBelow != Blocks.GRASS && blockBelow != Blocks.DIRT) {
          continue;
        }

        double worldX = rx;
        double worldZ = rz;
        double treeNoise =
            PerlinNoise.getfBM2D(worldX * treeFrequency, worldZ * treeFrequency, 1, 1.0, 2.0);
        if (treeNoise < treeThreshold) {
          continue;
        }

        // Use local maximum check to avoid tree clusters.
        if (!isLocalMax(rx, rz, regionWidth, regionDepth, treeNoise, treeFrequency)) {
          continue;
        }

        double trunkNoise =
            PerlinNoise.getfBM2D(
                worldX * treeFrequency * 2, worldZ * treeFrequency * 2, 1, 1.0, 2.0);
        int trunkHeight = minTrunkHeight + (int) ((maxTrunkHeight - minTrunkHeight) * trunkNoise);

        double canopyVar = PerlinNoise.getfBM2D(worldX * 0.2, worldZ * 0.2, 1, 1.0, 2.0);
        int extraLayers = (int) (canopyVar * 2);
        int dynamicCanopyLayers = baseCanopyLayers + extraLayers;
        int extraRadius = (int) (canopyVar * 2);
        int dynamicBaseCanopyRad = baseCanopyRadius + extraRadius;

        int requiredClear = trunkHeight + dynamicCanopyLayers;
        if (!isClearAbove(regionBlocks, rx, rz, surfaceY, requiredClear)) {
          continue;
        }

        // Build:
        for (int y = surfaceY - 1; y >= surfaceY - trunkHeight && y >= 0; y--) {
          regionBlocks[rx][y][rz] = Blocks.OAKWOOD;
        }
        int trunkTop = surfaceY - trunkHeight;

        for (int layer = 0; layer < dynamicCanopyLayers; layer++) {
          int canopyY = trunkTop - 1 - layer;
          int radius = dynamicBaseCanopyRad - layer;
          if (radius < 0) {
            radius = 0;
          }
          for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
              int ax = rx + dx;
              int az = rz + dz;
              if (ax < 0 || ax >= regionWidth || az < 0 || az >= regionDepth) {
                continue;
              }
              if (canopyY >= 0 && regionBlocks[ax][canopyY][az] == null) {
                regionBlocks[ax][canopyY][az] = Blocks.GREY_LEAVES;
              }
            }
          }
        }
      }
    }
  }

  private int findSurfaceY(Blocks[][][] region, int rx, int rz, int regionHeight) {
    for (int y = 0; y < regionHeight; y++) {
      if (region[rx][y][rz] != null) {
        return y;
      }
    }
    return -1;
  }

  private boolean isLocalMax(
      int rx, int rz, int regionWidth, int regionDepth, double treeNoise, double freq) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (dx == 0 && dz == 0) {
          continue;
        }
        int nx = rx + dx;
        int nz = rz + dz;
        if (nx < 0 || nx >= regionWidth || nz < 0 || nz >= regionDepth) {
          continue;
        }
        double neighborNoise = PerlinNoise.getfBM2D(nx * freq, nz * freq, 1, 1.0, 2.0);
        if (neighborNoise >= treeNoise) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean isClearAbove(
      Blocks[][][] region, int rx, int rz, int surfaceY, int requiredClear) {
    for (int y = surfaceY - 1; y >= surfaceY - requiredClear && y >= 0; y--) {
      if (region[rx][y][rz] != null) {
        return false;
      }
    }
    return true;
  }
}
