package com.game.minecraft.world;

import com.game.minecraft.utils.PersistStorage;
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
        if (request == null) continue;
        Blocks[][][] data = PersistStorage.loadFromFile(request.coord);
        if (data == null) {
          data = generateTerrainAt(request.coord);
        }
        resultQueue.put(new ChunkLoadResult(request.coord, data));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // Biome Blending Helpers
  /////////////////////////////////////////////////////////////////////////////

  private static class BiomeParameters {
    int baseHeight;
    int amplitude;
    Blocks surfaceBlock;
    Blocks subsurfaceBlock;

    BiomeParameters(int baseHeight, int amplitude, Blocks surfaceBlock, Blocks subsurfaceBlock) {
      this.baseHeight = baseHeight;
      this.amplitude = amplitude;
      this.surfaceBlock = surfaceBlock;
      this.subsurfaceBlock = subsurfaceBlock;
    }
  }

  /**
   * A container that ties a biome to a “center” value and a blending range. The center value is
   * where this biome is considered active, and the range defines how far from that center the
   * biome’s influence falls off.
   */
  private static class BiomeOption {
    Biome biome;
    BiomeParameters params;
    double center; // the noise value at which this biome is “pure”
    double range; // how far from the center the influence drops to zero

    BiomeOption(Biome biome, BiomeParameters params, double center, double range) {
      this.biome = biome;
      this.params = params;
      this.center = center;
      this.range = range;
    }
  }

  /**
   * Determines biome parameters for a coordinate by blending across all available biomes. For each
   * biome option, compute a weight based on how close the low-frequency "biome noise" is to its
   * center.
   */
  private BiomeParameters getBlendedBiomeParameters(double worldX, double worldZ) {
    // TODO: randomize biome sizes
    double biomeFrequency = 0.005; // low frequency = large & continuous biomes
    int biomeOctaves = 3;
    double biomePersistence = 0.5;
    double biomeLacunarity = 2.0;

    double biomeNoise =
        PerlinNoise.getfBM2D(
            worldX * biomeFrequency,
            worldZ * biomeFrequency,
            biomeOctaves,
            biomePersistence,
            biomeLacunarity);

    // TODO: tune center and range between each blend
    BiomeOption[] options =
        new BiomeOption[] {
          new BiomeOption(
              Biome.OCEAN, // TODO: add water
              new BiomeParameters(Chunk.CHUNK_Y - 55, 3, null, Blocks.SAND),
              -0.5,
              0.3),
          new BiomeOption(
              Biome.PLAINS,
              new BiomeParameters(Chunk.CHUNK_Y - 60, 6, Blocks.GRASS, Blocks.DIRT),
              -0.1,
              0.3),
          new BiomeOption(
              Biome.DESERT,
              new BiomeParameters(Chunk.CHUNK_Y - 60, 4, Blocks.SAND, Blocks.SAND),
              0.2,
              0.3),
          new BiomeOption(
              Biome.MOUNTAIN,
              new BiomeParameters(Chunk.CHUNK_Y - 80, 15, Blocks.GRASS, Blocks.DIRT),
              0.6,
              0.3)
        };

    double totalWeight = 0;
    double blendedBaseHeight = 0;
    double blendedAmplitude = 0;
    double maxWeight = -1;
    BiomeParameters dominantParams = null;

    // compute a weight that decreases linearly from the center.
    for (BiomeOption option : options) {
      double distance = Math.abs(biomeNoise - option.center);
      double weight = Math.max(0, 1 - (distance / option.range)); // linear falloff
      totalWeight += weight;
      blendedBaseHeight += option.params.baseHeight * weight;
      blendedAmplitude += option.params.amplitude * weight;
      if (weight > maxWeight) {
        maxWeight = weight;
        dominantParams = option.params;
      }
    }

    // default to Plains
    if (totalWeight == 0) {
      return options[1].params;
    }

    int finalBaseHeight = (int) (blendedBaseHeight / totalWeight);
    int finalAmplitude = (int) (blendedAmplitude / totalWeight);
    Blocks finalSurfaceBlock = dominantParams.surfaceBlock;
    Blocks finalSubsurfaceBlock = dominantParams.subsurfaceBlock;

    return new BiomeParameters(
        finalBaseHeight, finalAmplitude, finalSurfaceBlock, finalSubsurfaceBlock);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Terrain Generation
  /////////////////////////////////////////////////////////////////////////////

  /** Generates terrain using fBM-based height mapping and blended biome-dependent parameters. */
  public Blocks[][][] generateTerrainAt(ChunkCoordinate coord) {
    Blocks[][][] blocks = new Blocks[Chunk.CHUNK_X][Chunk.CHUNK_Y][Chunk.CHUNK_Z];

    int octaves = 4;
    double persistence = 0.5;
    double lacunarity = 2.0;
    double frequency = 0.125; // Controls local detail (hills, valleys)

    for (int x = 0; x < Chunk.CHUNK_X; x++) {
      for (int z = 0; z < Chunk.CHUNK_Z; z++) {
        double worldX = x + coord.x() * Chunk.CHUNK_X;
        double worldZ = z + coord.z() * Chunk.CHUNK_Z;

        BiomeParameters bp = getBlendedBiomeParameters(worldX, worldZ);
        int baseHeight = bp.baseHeight;
        int amplitude = bp.amplitude;
        Blocks surfaceBlock = bp.surfaceBlock;
        Blocks subsurfaceBlock = bp.subsurfaceBlock;

        double noiseValue =
            PerlinNoise.getfBM2D(
                worldX * frequency, worldZ * frequency, octaves, persistence, lacunarity);
        int surface = baseHeight + (int) (noiseValue * amplitude);

        for (int y = Chunk.CHUNK_Y - 5; y < Chunk.CHUNK_Y; y++) {
          blocks[x][y][z] = Blocks.BEDROCK;
        }
        if (surface >= 0 && surface < Chunk.CHUNK_Y - 5) {
          blocks[x][surface][z] = surfaceBlock;
        }
        for (int y = surface + 1; y < Chunk.CHUNK_Y - 5; y++) {
          blocks[x][y][z] = subsurfaceBlock;
        }
        // TODO: add features (caves, water, trees, etc.)
      }
    }
    return blocks;
  }
}
