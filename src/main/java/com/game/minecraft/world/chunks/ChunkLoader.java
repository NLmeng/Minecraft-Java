package com.game.minecraft.world.chunks;

import com.game.minecraft.utils.PersistStorage;
import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.generations.ChunkTerrainGenerator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Accept chunk-load requests. Load from PersistStorage if available, otherwise delegate terrain
 * generation to ChunkTerrainGenerator. Produce chunk-load results asynchronously.
 */
public class ChunkLoader implements Runnable {

  public static class ChunkLoadRequest {
    final ChunkCoordinate coord;

    ChunkLoadRequest(ChunkCoordinate coord) {
      if (coord == null) {
        throw new IllegalArgumentException("Null coordinate");
      }
      this.coord = coord;
    }

    public ChunkCoordinate getCoord() {
      return coord;
    }
  }

  public static class ChunkLoadResult {
    final ChunkCoordinate coord;
    final Blocks[][][] blockData;

    ChunkLoadResult(ChunkCoordinate coord, Blocks[][][] blockData) {
      if (coord == null || blockData == null) {
        throw new IllegalArgumentException("Null arguments in load result");
      }
      this.coord = coord;
      this.blockData = blockData;
    }

    public ChunkCoordinate getCoord() {
      return coord;
    }

    public Blocks[][][] getBlockData() {
      return blockData;
    }
  }

  private final BlockingQueue<ChunkLoadRequest> requestQueue = new LinkedBlockingQueue<>();
  private final BlockingQueue<ChunkLoadResult> resultQueue = new LinkedBlockingQueue<>();
  private final ChunkTerrainGenerator terrainGenerator;

  private volatile boolean running = true;

  public ChunkLoader() {
    this.terrainGenerator = new ChunkTerrainGenerator();
  }

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
          data = terrainGenerator.generateTerrainsAt(request.coord);
        }

        resultQueue.put(new ChunkLoadResult(request.coord, data));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
