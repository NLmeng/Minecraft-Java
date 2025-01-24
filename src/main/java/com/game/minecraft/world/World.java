package com.game.minecraft.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class World {

  private final Map<ChunkCoordinate, Chunk> chunkMap = new HashMap<>();
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
        calculateRequiredChunks(currentPlayerChunk, chunkLayerRadius);

    for (ChunkCoordinate coord : requiredChunks) {
      chunkMap.computeIfAbsent(coord, this::createChunk);
    }

    chunkMap.keySet().removeIf(coord -> !requiredChunks.contains(coord));
  }

  private Set<ChunkCoordinate> calculateRequiredChunks(ChunkCoordinate origin, int layers) {
    Set<ChunkCoordinate> result = new HashSet<>();
    result.add(origin);

    for (int layer = 1; layer <= layers; layer++) {
      for (int dx = -layer; dx <= layer; dx++) {
        result.add(new ChunkCoordinate(origin.x() + dx, origin.z() - layer)); // Top edge
        result.add(new ChunkCoordinate(origin.x() + dx, origin.z() + layer)); // Bottom edge
      }

      for (int dz = -layer + 1; dz <= layer - 1; dz++) {
        result.add(new ChunkCoordinate(origin.x() - layer, origin.z() + dz)); // Left edge
        result.add(new ChunkCoordinate(origin.x() + layer, origin.z() + dz)); // Right edge
      }
    }
    return result;
  }

  private Chunk createChunk(ChunkCoordinate coord) {
    float x = coord.x() * Chunk.CHUNK_X;
    float z = coord.z() * Chunk.CHUNK_Z;
    return new Chunk(x, 0, z);
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
