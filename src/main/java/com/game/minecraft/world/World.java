package com.game.minecraft.world;

import com.game.minecraft.utils.LRU;
import com.game.minecraft.utils.PersistStorage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: add save/load world to/from UUID.
public class World {

  private static final String INSTANCE_WORLD_NAME = UUID.randomUUID().toString().replace("-", "");

  private final Map<ChunkCoordinate, Chunk> activeChunks = new ConcurrentHashMap<>();
  private final LRU cachedChunk;
  private Set<ChunkCoordinate> requiredChunks;

  private ChunkCoordinate currentPlayerChunk;
  private int chunkLayerRadius = 3;

  private Thread loaderThread;
  private ChunkLoader chunkLoader;

  public World() {
    cachedChunk = new LRU(calculateMaxConcurrentChunks());
    chunkLoader = new ChunkLoader();
    loaderThread = new Thread(chunkLoader, "ChunkLoader");
    loaderThread.start();
    PersistStorage.setWorldInstanceName(INSTANCE_WORLD_NAME);
  }

  public void shutdown() {
    chunkLoader.stopLoader();
    loaderThread.interrupt();
  }

  private int calculateMaxConcurrentChunks() {
    int baseChunks = (2 * chunkLayerRadius + 1) * (2 * chunkLayerRadius + 1);
    return (int) (baseChunks * 1.25);
  }

  public void updatePlayerPosition(float playerX, float playerZ) {
    int playerChunkX = Math.floorDiv((int) playerX, Chunk.CHUNK_X);
    int playerChunkZ = Math.floorDiv((int) playerZ, Chunk.CHUNK_Z);

    ChunkCoordinate newPlayerChunk = new ChunkCoordinate(playerChunkX, playerChunkZ);

    if (!newPlayerChunk.equals(currentPlayerChunk)) {
      currentPlayerChunk = newPlayerChunk;
      manageChunks();
    }

    checkChunkLoaderResults();
  }

  private void checkChunkLoaderResults() {
    ChunkLoader.ChunkLoadResult result;
    while ((result = chunkLoader.pollResult()) != null) {
      if (result.blockData != null) {
        cachedChunk.put(result.coord, result.blockData);
        if (requiredChunks != null
            && requiredChunks.contains(result.coord)
            && !activeChunks.containsKey(result.coord)) {
          Blocks[][][] data = cachedChunk.get(result.coord);
          if (data != null) {
            activeChunks.put(result.coord, createChunkFromCache(result.coord, data));
          }
        }
      }
    }
  }

  private void manageChunks() {
    requiredChunks = generateChunksInSpiral(currentPlayerChunk, chunkLayerRadius);

    for (ChunkCoordinate coord : requiredChunks) {
      if (!activeChunks.containsKey(coord)) {
        Blocks[][][] existingData = cachedChunk.get(coord);
        if (existingData != null) {
          activeChunks.put(coord, createChunkFromCache(coord, existingData));
        } else {
          chunkLoader.requestLoad(coord);
        }
      }
    }

    activeChunks
        .keySet()
        .removeIf(
            coord -> {
              if (!requiredChunks.contains(coord)) {
                Chunk chunkToUnload = activeChunks.get(coord);
                if (chunkToUnload != null) {
                  saveChunkData(coord, chunkToUnload);
                }
                return true;
              }
              return false;
            });

    updateChunkNeighbors();
  }

  private Chunk createChunkFromCache(ChunkCoordinate coord, Blocks[][][] blocks) {
    float x = coord.x() * Chunk.CHUNK_X;
    float z = coord.z() * Chunk.CHUNK_Z;
    Chunk newChunk = new Chunk(x, 0, z);

    newChunk.setBlockData(blocks);
    return newChunk;
  }

  private void saveChunkData(ChunkCoordinate coord, Chunk chunk) {
    chunk.cleanup();
    cachedChunk.put(coord, chunk.copyBlockData());
  }

  private void updateChunkNeighbors() {
    for (ChunkCoordinate coord : activeChunks.keySet()) {
      Chunk currentChunk = activeChunks.get(coord);

      Chunk front = activeChunks.get(new ChunkCoordinate(coord.x(), coord.z() + 1));
      Chunk back = activeChunks.get(new ChunkCoordinate(coord.x(), coord.z() - 1));
      Chunk left = activeChunks.get(new ChunkCoordinate(coord.x() - 1, coord.z()));
      Chunk right = activeChunks.get(new ChunkCoordinate(coord.x() + 1, coord.z()));

      currentChunk.setNeighbor(Direction.FRONT, front);
      currentChunk.setNeighbor(Direction.BACK, back);
      currentChunk.setNeighbor(Direction.LEFT, left);
      currentChunk.setNeighbor(Direction.RIGHT, right);
    }
  }

  private static final int[][] DIRECTIONS = {
    {1, 0}, // right
    {0, 1}, // up
    {-1, 0}, // left
    {0, -1} // down
  };

  /**
   * Starting at "origin", move in a direction "layer" numbers of time. Take a turn. After two
   * turns, increment "layer" - meaning go in the each directions one block longer. Stop when reach
   * "layerLimit" squared. To keep coordinates squared with a center, do 2k+1.
   */
  public Set<ChunkCoordinate> generateChunksInSpiral(ChunkCoordinate origin, int layerLimit) {
    Set<ChunkCoordinate> result = new HashSet<>();
    int x = origin.x();
    int z = origin.z();
    result.add(new ChunkCoordinate(x, z));

    int layer = 1;
    int layerProcessed = 0;
    int numTurned = 0;
    int directionIndex = 0;
    while (result.size() < (2 * layerLimit + 1) * (2 * layerLimit + 1)) {
      x += DIRECTIONS[directionIndex][0];
      z += DIRECTIONS[directionIndex][1];
      result.add(new ChunkCoordinate(x, z));

      layerProcessed++;
      if (layerProcessed == layer) {
        numTurned++;
        layerProcessed = 0;
        directionIndex = (directionIndex + 1) % 4;
      }
      if (numTurned == 2) {
        numTurned = 0;
        layer++;
      }
    }
    return result;
  }

  public void setChunkLayerRadius(int radius) {
    this.chunkLayerRadius = radius;
    if (currentPlayerChunk != null) {
      manageChunks();
    }
  }

  public Collection<Chunk> getActiveChunks() {
    return activeChunks.values();
  }

  public int getChunkLayerRadius() {
    return chunkLayerRadius;
  }

  public String getID() {
    return INSTANCE_WORLD_NAME;
  }
}
