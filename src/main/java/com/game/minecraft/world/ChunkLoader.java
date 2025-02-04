package com.game.minecraft.world;

import com.game.minecraft.utils.PersistStorage;
import com.game.minecraft.utils.SimpleNoise;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChunkLoader implements Runnable {
  static class ChunkLoadRequest {
    final ChunkCoordinate coord;

    ChunkLoadRequest(ChunkCoordinate coord) {
      if (coord == null) throw new IllegalArgumentException("Null coordinate");
      this.coord = coord;
    }
  }

  static class ChunkLoadResult {
    final ChunkCoordinate coord;
    final Blocks[][][] blockData;

    ChunkLoadResult(ChunkCoordinate coord, Blocks[][][] blockData) {
      if (coord == null || blockData == null) throw new IllegalArgumentException();
      this.coord = coord;
      this.blockData = blockData;
    }
  }

  private final BlockingQueue<ChunkLoadRequest> requestQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<ChunkLoadResult> resultQueue = new LinkedBlockingQueue<>();

  private volatile boolean running = true;

  public void requestLoad(ChunkCoordinate coord) {
    try {
      requestQueue.put(new ChunkLoadRequest(coord));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public ChunkLoadResult pollResult() {
    return resultQueue.poll();
  }

  public void stopLoader() {
    running = false;
  }

  @Override
  public void run() {
    while (running) {
      try {
        ChunkLoadRequest request = requestQueue.take();
        if (request == null) {
          continue;
        }

        Blocks[][][] data = PersistStorage.loadFromFile(request.coord);
        if (data == null) {
          data = generateNaturalTerrainAt(request.coord);
        }

        resultQueue.put(new ChunkLoadResult(request.coord, data));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public Blocks[][][] generateFlatTerrainAt(ChunkCoordinate coord) {
    Blocks[][][] blocks = new Blocks[Chunk.CHUNK_X][Chunk.CHUNK_Y][Chunk.CHUNK_Z];
    Random random = new Random();
    int dirtHeight = Chunk.CHUNK_Y - (60 + random.nextInt(6));

    for (int x = 0; x < Chunk.CHUNK_X; x++) {
      for (int z = 0; z < Chunk.CHUNK_Z; z++) {

        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          if (y >= Chunk.CHUNK_Y - 5) {
            blocks[x][y][z] = Blocks.BEDROCK;
          } else if (y >= dirtHeight) {
            blocks[x][y][z] = Blocks.DIRT;
          } else if (y >= Chunk.CHUNK_Y - 60) {
            blocks[x][y][z] = Blocks.STONE;
          } else if (y == dirtHeight - 1) {
            blocks[x][y][z] = Blocks.GRASS;
          }
        }
      }
    }
    blocks[0][dirtHeight - 1][0] = Blocks.BEDROCK;
    blocks[0][dirtHeight - 1][15] = Blocks.BEDROCK;
    blocks[15][dirtHeight - 1][15] = Blocks.BEDROCK;
    blocks[15][dirtHeight - 1][0] = Blocks.BEDROCK;

    return blocks;
  }

  public Blocks[][][] generateNaturalTerrainAt(ChunkCoordinate coord) {
    Blocks[][][] blocks = new Blocks[Chunk.CHUNK_X][Chunk.CHUNK_Y][Chunk.CHUNK_Z];

    final int baseHeight = 70; // avg ground level
    final int amplitude = 15; // max variation above/below the base
    final double frequency = 0.1; // smoothness

    int seed = coord.hashCode();
    SimpleNoise noiseGenerator = new SimpleNoise(seed);

    for (int x = 0; x < Chunk.CHUNK_X; x++) {
      for (int z = 0; z < Chunk.CHUNK_Z; z++) {
        // Adjust coordinates so that adjacent chunks match up.
        double noiseValue =
            noiseGenerator.noise(
                (x + coord.x() * Chunk.CHUNK_X) * frequency,
                (z + coord.z() * Chunk.CHUNK_Z) * frequency);
        int surfaceHeight = baseHeight + (int) (noiseValue * amplitude);

        // Clamp surfaceHeight to valid bounds.
        surfaceHeight = Math.min(Math.max(surfaceHeight, 1), Chunk.CHUNK_Y - 1);

        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          if (y < Chunk.CHUNK_Y - surfaceHeight) {
            blocks[x][y][z] = null;
          } else if (y == Chunk.CHUNK_Y - surfaceHeight) {
            blocks[x][y][z] = Blocks.GRASS;
          } else if (y < Chunk.CHUNK_Y - surfaceHeight + 3) {
            blocks[x][y][z] = Blocks.DIRT;
          } else if (y < Chunk.CHUNK_Y - 5) {
            blocks[x][y][z] = Blocks.STONE;
          } else {
            blocks[x][y][z] = Blocks.BEDROCK;
          }
        }
      }
    }

    return blocks;
  }
}
