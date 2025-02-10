package com.game.minecraft.world;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Simulator {

  private static class FlowNode {
    int x, y, z, distance;

    FlowNode(int x, int y, int z, int distance) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.distance = distance;
    }
  }

  public static void simulateWaterFlowForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    // capture a snapshot of the active chunks to avoid concurrent issue
    Map<ChunkCoordinate, Chunk> chunksSnapshot = new HashMap<>(activeChunks);

    int minChunkX = Integer.MAX_VALUE;
    int maxChunkX = Integer.MIN_VALUE;
    int minChunkZ = Integer.MAX_VALUE;
    int maxChunkZ = Integer.MIN_VALUE;
    for (ChunkCoordinate coord : chunksSnapshot.keySet()) {
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

    // region = all active chunks combined
    Blocks[][][] regionBlocks = new Blocks[regionWidth][regionHeight][regionDepth];

    for (Map.Entry<ChunkCoordinate, Chunk> entry : chunksSnapshot.entrySet()) {
      ChunkCoordinate coord = entry.getKey();
      Chunk chunk = entry.getValue();
      int offsetX = (coord.x() - minChunkX) * Chunk.CHUNK_X;
      int offsetZ = (coord.z() - minChunkZ) * Chunk.CHUNK_Z;
      Blocks[][][] chunkBlocks = chunk.getAllBlocks();
      for (int x = 0; x < Chunk.CHUNK_X; x++) {
        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          for (int z = 0; z < Chunk.CHUNK_Z; z++) {
            regionBlocks[offsetX + x][y][offsetZ + z] = chunkBlocks[x][y][z];
          }
        }
      }
    }

    // run the water simulation on the region and write back
    simulateWaterFlowRegion(regionBlocks, regionWidth, regionHeight, regionDepth);
    for (Map.Entry<ChunkCoordinate, Chunk> entry : chunksSnapshot.entrySet()) {
      ChunkCoordinate coord = entry.getKey();
      Chunk chunk = entry.getValue();
      int offsetX = (coord.x() - minChunkX) * Chunk.CHUNK_X;
      int offsetZ = (coord.z() - minChunkZ) * Chunk.CHUNK_Z;
      Blocks[][][] newChunkData = new Blocks[Chunk.CHUNK_X][Chunk.CHUNK_Y][Chunk.CHUNK_Z];
      for (int x = 0; x < Chunk.CHUNK_X; x++) {
        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          for (int z = 0; z < Chunk.CHUNK_Z; z++) {
            newChunkData[x][y][z] = regionBlocks[offsetX + x][y][offsetZ + z];
          }
        }
      }
      chunk.setBlockData(newChunkData);
    }
  }

  private static void simulateWaterFlowRegion(
      Blocks[][][] blocks, int width, int height, int depth) {
    int[][][] flow = new int[width][height][depth];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < depth; z++) {
          flow[x][y][z] = -1;
        }
      }
    }

    Queue<FlowNode> queue = new LinkedList<>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < depth; z++) {
          if (blocks[x][y][z] == Blocks.WATER1) {
            flow[x][y][z] = 0;
            queue.add(new FlowNode(x, y, z, 0));
          }
        }
      }
    }

    // Breadth-first flood fill for water flow
    while (!queue.isEmpty()) {
      FlowNode node = queue.poll();
      int x = node.x, y = node.y, z = node.z, d = node.distance;

      // Horizontal flow: allow water to flow to neighbors with limit to 7 blocks and a reset if
      // block underneath is water
      int[][] directions = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] dir : directions) {
        int nx = x + dir[0], ny = y + dir[1], nz = z + dir[2];
        if (nx < 0 || nx >= width || nz < 0 || nz >= depth) continue;
        if (ny != y) continue; // only horizontal neighbors
        if (blocks[nx][ny][nz] == null || !blocks[nx][ny][nz].isSolid()) {
          if (d < 7 && (flow[nx][ny][nz] == -1 || flow[nx][ny][nz] > d + 1)) {
            blocks[nx][ny][nz] = Blocks.WATER1;
            flow[nx][ny][nz] = d + 1;
            queue.add(new FlowNode(nx, ny, nz, d + 1));
          }
        }
      }

      // Downward flow: allow water to flow down without limit
      int by = y + 1;
      if (by < height) {
        if (blocks[x][by][z] == null || !blocks[x][by][z].isSolid()) {
          if (flow[x][by][z] == -1 || flow[x][by][z] > 0) {
            blocks[x][by][z] = Blocks.WATER1;
            flow[x][by][z] = 0;
            queue.add(new FlowNode(x, by, z, 0));
          }
        }
      }
    }
  }
}
