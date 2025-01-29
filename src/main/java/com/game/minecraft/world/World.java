package com.game.minecraft.world;

import com.game.minecraft.utils.LRU;
import com.game.minecraft.utils.PersistStorage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: add save/load world to/from UUID.
public class World {

  private static final int MAX_CONCURRENT_CHUNKS = 1*1;
  private final Map<ChunkCoordinate, Chunk> chunkMap = new HashMap<>();
  private final LRU savedChunkData = new LRU(MAX_CONCURRENT_CHUNKS);

  private ChunkCoordinate currentPlayerChunk;
  private int chunkLayerRadius = 3;

  public void updatePlayerPosition(float playerX, float playerZ) {
    int playerChunkX = Math.floorDiv((int) playerX, Chunk.CHUNK_X);
    int playerChunkZ = Math.floorDiv((int) playerZ, Chunk.CHUNK_Z);

    ChunkCoordinate newPlayerChunk = new ChunkCoordinate(playerChunkX, playerChunkZ);

    if (!newPlayerChunk.equals(currentPlayerChunk)) {
      currentPlayerChunk = newPlayerChunk;
      manageChunks();
    }
  }

  private void manageChunks() {
    Set<ChunkCoordinate> requiredChunks =
        generateChunksInSpiral(currentPlayerChunk, chunkLayerRadius);

    for (ChunkCoordinate coord : requiredChunks) {
      chunkMap.computeIfAbsent(coord, this::createChunk);
    }

    chunkMap
        .keySet()
        .removeIf(
            coord -> {
              if (!requiredChunks.contains(coord)) {
                Chunk chunkToUnload = chunkMap.get(coord);
                if (chunkToUnload != null) {
                  saveChunkData(coord, chunkToUnload);
                }
                return true;
              }
              return false;
            });

    updateChunkNeighbors();
  }

  private void saveChunkData(ChunkCoordinate coord, Chunk chunk) {
    chunk.cleanup();
    savedChunkData.put(coord, chunk.copyBlockData());
  }

  private Chunk createChunk(ChunkCoordinate coord) {
    Blocks[][][] existingData = savedChunkData.get(coord);
    if (existingData == null) {
      existingData = PersistStorage.loadFromFile(coord);
      if (existingData != null) {
        savedChunkData.put(coord, existingData);
      }
    }

    if (existingData != null) {
      return createChunkFromSavedData(coord, existingData);
    }

    float x = coord.x() * Chunk.CHUNK_X;
    float z = coord.z() * Chunk.CHUNK_Z;
    Chunk newChunk = new Chunk(x, 0, z);
    newChunk.generateFlatTerrain();
    return newChunk;
  }

  private Chunk createChunkFromSavedData(ChunkCoordinate coord, Blocks[][][] blockData) {
    float x = coord.x() * Chunk.CHUNK_X;
    float z = coord.z() * Chunk.CHUNK_Z;
    Chunk loadedChunk = new Chunk(x, 0, z);

    loadedChunk.setBlockData(blockData);
    return loadedChunk;
  }

  private void updateChunkNeighbors() {
    for (ChunkCoordinate coord : chunkMap.keySet()) {
      Chunk currentChunk = chunkMap.get(coord);

      Chunk front = chunkMap.get(new ChunkCoordinate(coord.x(), coord.z() + 1));
      Chunk back = chunkMap.get(new ChunkCoordinate(coord.x(), coord.z() - 1));
      Chunk left = chunkMap.get(new ChunkCoordinate(coord.x() - 1, coord.z()));
      Chunk right = chunkMap.get(new ChunkCoordinate(coord.x() + 1, coord.z()));

      currentChunk.setNeighbor("front", front);
      currentChunk.setNeighbor("back", back);
      currentChunk.setNeighbor("left", left);
      currentChunk.setNeighbor("right", right);
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

  public Collection<Chunk> getLoadedChunks() {
    return chunkMap.values();
  }

  public int getChunkLayerRadius() {
    return chunkLayerRadius;
  }
}
