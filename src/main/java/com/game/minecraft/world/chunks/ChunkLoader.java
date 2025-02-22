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
          data = generateTerrainsAt(request.coord);
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
    Biome biome;
    Blocks surfaceBlock;
    Blocks subsurfaceBlock;

    BiomeParameters(
        Biome biome, int baseHeight, int amplitude, Blocks surfaceBlock, Blocks subsurfaceBlock) {
      this.biome = biome;
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
    BiomeParameters params;
    double center; // the noise value at which this biome is “pure”
    double range; // how far from the center the influence drops to zero

    BiomeOption(BiomeParameters params, double center, double range) {
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
    double baseBiomeFrequency = 0.005; // base frequency for large biomes
    double randomFactor = PerlinNoise.getfBM2D(worldX * 0.01, worldZ * 0.01, 2, 0.5, 2.0);
    double randomMultiplier = 0.1 + ((randomFactor + 1) / 2) * 10; // [0.1, 10.1]
    double biomeFrequency = baseBiomeFrequency * randomMultiplier;

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
              new BiomeParameters(Biome.OCEAN, Chunk.CHUNK_Y - 60, 3, Blocks.WATER1, Blocks.SAND),
              -0.5,
              0.3),
          new BiomeOption(
              new BiomeParameters(Biome.PLAINS, Chunk.CHUNK_Y - 60, 6, Blocks.GRASS, Blocks.DIRT),
              -0.1,
              0.3),
          new BiomeOption(
              new BiomeParameters(Biome.DESERT, Chunk.CHUNK_Y - 60, 4, Blocks.SAND, Blocks.SAND),
              0.2,
              0.3),
          new BiomeOption(
              new BiomeParameters(
                  Biome.MOUNTAIN, Chunk.CHUNK_Y - 85, 15, Blocks.GRASS, Blocks.DIRT),
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
        dominantParams.biome,
        finalBaseHeight,
        finalAmplitude,
        finalSurfaceBlock,
        finalSubsurfaceBlock);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Terrain Generation
  /////////////////////////////////////////////////////////////////////////////

  // NOTE: y=0 is the top and y=Chunk.CHUNK_Y-1 is the bottom
  private static final int SEA_LEVEL = Chunk.CHUNK_Y - 60;

  /** Generates terrain using fBM-based height mapping and blended biome-dependent parameters. */
  public Blocks[][][] generateTerrainsAt(ChunkCoordinate coord) {
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

        double noiseValue =
            PerlinNoise.getfBM2D(
                worldX * frequency, worldZ * frequency, octaves, persistence, lacunarity);

        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          blocks[x][y][z] = generateBlockBasedOn(y, bp, noiseValue);
        }
      }
    }
    return blocks;
  }

  private Blocks generateBlockBasedOn(int height, BiomeParameters bp, double noise) {
    int baseHeight = bp.baseHeight;
    int amplitude = bp.amplitude;
    int surface = baseHeight + (int) (noise * amplitude);

    switch (bp.biome) {
      case OCEAN:
        return getOceanBlock(height, bp, surface);
      case PLAINS:
        return getPlainsBlock(height, bp, surface);
      case MOUNTAIN:
        return getMountainBlock(height, bp, surface);
      case DESERT:
        return getDesertBlock(height, bp, surface);
      default:
        return null;
    }
    // TODO: add features (caves, water, trees, etc.)
  }

  private Blocks getOceanBlock(int height, BiomeParameters bp, int surface) {
    if (!heightIsBeneath(height, surface)) return null;

    int bedrockTopLimit = Chunk.CHUNK_Y - (((int) surface % 5) + 2);
    int subsurfaceTopLimit = bedrockTopLimit - (((int) surface % 10) + 25);

    if (heightIsBeneath(height, bedrockTopLimit)) return Blocks.BEDROCK;
    if (heightIsBeneath(height, subsurfaceTopLimit)) return bp.subsurfaceBlock;
    if (heightIsBeneath(height, surface) && heightIsBeneath(height, SEA_LEVEL - 1))
      return bp.surfaceBlock;

    return null;
  }

  private Blocks getPlainsBlock(int height, BiomeParameters bp, int surface) {
    if (height == surface) return bp.surfaceBlock;
    if (!heightIsBeneath(height, surface)) return null;

    int bedrockTopLimit = Chunk.CHUNK_Y - (((int) surface % 5) + 2);

    if (!heightIsBeneath(height, bedrockTopLimit)) return bp.subsurfaceBlock;
    if (heightIsBeneath(height, bedrockTopLimit)) return Blocks.BEDROCK;
    return null;
  }

  private Blocks getMountainBlock(int height, BiomeParameters bp, int surface) {
    if (height == surface) return bp.surfaceBlock;
    if (!heightIsBeneath(height, surface)) return null;

    int bedrockTopLimit = Chunk.CHUNK_Y - (((int) surface % 5) + 2);

    if (!heightIsBeneath(height, bedrockTopLimit)) return bp.subsurfaceBlock;
    if (heightIsBeneath(height, bedrockTopLimit)) return Blocks.BEDROCK;
    return null;
  }

  private Blocks getDesertBlock(int height, BiomeParameters bp, int surface) {
    if (height == surface) return bp.surfaceBlock;
    if (!heightIsBeneath(height, surface)) return null;

    int bedrockTopLimit = Chunk.CHUNK_Y - (((int) surface % 5) + 2);

    if (!heightIsBeneath(height, bedrockTopLimit)) return bp.subsurfaceBlock;
    if (heightIsBeneath(height, bedrockTopLimit)) return Blocks.BEDROCK;
    return null;
  }

  private boolean heightIsBeneath(int height, int limit) {
    return height > limit;
  }
}
