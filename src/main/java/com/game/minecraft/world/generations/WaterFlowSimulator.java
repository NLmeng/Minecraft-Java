package com.game.minecraft.world.generations;

import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.chunks.Chunk;
import com.game.minecraft.world.chunks.ChunkCoordinate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs a BFS-based water flow simulation over the active region. Merges chunks into a region,
 * runs the simulation, and writes the result back.
 */
public class WaterFlowSimulator {

  private static class FlowNode {
    final int x, y, z, distance;

    FlowNode(int x, int y, int z, int distance) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.distance = distance;
    }
  }

  private static final int[][] HORIZONTAL_DIRECTIONS = {
    {1, 0, 0},
    {-1, 0, 0},
    {0, 0, 1},
    {0, 0, -1}
  };

  public void simulateWaterFlowForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    // snapshot
    Map<ChunkCoordinate, Chunk> snapshot = new HashMap<>(activeChunks);

    // bounds
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

    // run simulation
    simulateWaterFlowRegion(regionBlocks, regionWidth, regionHeight, regionDepth);

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
    }
  }

  private void simulateWaterFlowRegion(Blocks[][][] blocks, int width, int height, int depth) {
    int[][][] flow = new int[width][height][depth];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < depth; z++) {
          flow[x][y][z] = -1;
        }
      }
    }

    boolean[][][] isSolid = new boolean[width][height][depth];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < depth; z++) {
          isSolid[x][y][z] = (blocks[x][y][z] != null && blocks[x][y][z].isSolid());
        }
      }
    }

    Deque<FlowNode> queue = new ArrayDeque<>();

    // Initialize the queue with existing water blocks.
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

    while (!queue.isEmpty()) {
      FlowNode node = queue.poll();
      int x = node.x;
      int y = node.y;
      int z = node.z;
      int d = node.distance;

      // Horizontal neighbors
      for (int[] dir : HORIZONTAL_DIRECTIONS) {
        int nx = x + dir[0];
        int ny = y + dir[1];
        int nz = z + dir[2];
        if (nx < 0 || nx >= width || nz < 0 || nz >= depth) {
          continue;
        }
        if (!isSolid[nx][ny][nz]) {
          if (d < 7 && (flow[nx][ny][nz] == -1 || flow[nx][ny][nz] > d + 1)) {
            blocks[nx][ny][nz] = Blocks.WATER1;
            flow[nx][ny][nz] = d + 1;
            queue.add(new FlowNode(nx, ny, nz, d + 1));
          }
        }
      }

      // Downward flow
      int by = y + 1;
      if (by < height && !isSolid[x][by][z]) {
        if (flow[x][by][z] == -1 || flow[x][by][z] > 0) {
          blocks[x][by][z] = Blocks.WATER1;
          flow[x][by][z] = 0;
          queue.addFirst(new FlowNode(x, by, z, 0));
        }
      }
    }
  }
}
