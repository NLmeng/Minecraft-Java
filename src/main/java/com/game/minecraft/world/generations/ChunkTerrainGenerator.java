package com.game.minecraft.world.generations;

import com.game.minecraft.world.Blocks;
import com.game.minecraft.world.chunks.Biome;
import com.game.minecraft.world.chunks.Chunk;
import com.game.minecraft.world.chunks.ChunkCoordinate;

/**
 * Generate new (deep copy) chunk data (blocks[][][]) if not found on disk. Apply biome blending,
 * terrain shape, caves distribution.
 */
public class ChunkTerrainGenerator {

  private static class BiomeParameters {
    final Biome biome;
    final int baseHeight;
    final int amplitude;
    final Blocks surfaceBlock;
    final Blocks subsurfaceBlock;
    final Blocks innerUpperBlock;

    BiomeParameters(
        Biome biome,
        int baseHeight,
        int amplitude,
        Blocks surfaceBlock,
        Blocks subsurfaceBlock,
        Blocks innerUpperBlock) {
      this.biome = biome;
      this.baseHeight = baseHeight;
      this.amplitude = amplitude;
      this.surfaceBlock = surfaceBlock;
      this.subsurfaceBlock = subsurfaceBlock;
      this.innerUpperBlock = innerUpperBlock;
    }
  }

  // Ties biome to a “center” noise value and how quickly it falls off.
  private static class BiomeOption {
    final BiomeParameters params;
    final double center;
    final double range;

    BiomeOption(BiomeParameters params, double center, double range) {
      this.params = params;
      this.center = center;
      this.range = range;
    }
  }

  // NOTE: y=0 is top and vice-versa
  private static final int SEA_LEVEL = Chunk.CHUNK_Y - 60;

  /** Generates new terrain block data for the requested coordinate. */
  public Blocks[][][] generateTerrainsAt(ChunkCoordinate coord) {
    Blocks[][][] blocks = new Blocks[Chunk.CHUNK_X][Chunk.CHUNK_Y][Chunk.CHUNK_Z];

    // Noise config
    int octaves = 4;
    double persistence = 0.5;
    double lacunarity = 2.0;
    double frequency = 0.125; // local detail

    // Cave noise config
    int caveOctaves = 3;
    double cavePersistence = 0.5;
    double caveLacunarity = 2.0;
    double caveFrequency = 0.01;
    double caveThreshold = 0.3;

    for (int x = 0; x < Chunk.CHUNK_X; x++) {
      for (int z = 0; z < Chunk.CHUNK_Z; z++) {
        double worldX = x + coord.x() * (double) Chunk.CHUNK_X;
        double worldZ = z + coord.z() * (double) Chunk.CHUNK_Z;

        // Blend biome parameters for (worldX, worldZ)
        BiomeParameters bp = getBlendedBiomeParameters(worldX, worldZ);

        // Terrain shape (height map)
        double terrainNoise =
            PerlinNoise.getfBM2D(
                worldX * frequency, worldZ * frequency, octaves, persistence, lacunarity);

        int surface = bp.baseHeight + (int) (terrainNoise * bp.amplitude);

        if (bp.biome == Biome.OCEAN) {
          double oceanScale =
              PerlinNoise.getfBM2D(worldX * 0.000001, worldZ * 0.000001, 1, 1.0, 2.0);
          oceanScale = (oceanScale + 1) / 2; // Normalize to [0,1]

          // Determine additional depth based on oceanScale using a piecewise linear function.
          // For very small ocean areas (oceanScale near 0), only a slight depression is applied.
          // For very large ocean areas (oceanScale near 1), the depression reaches up to
          int minAdditionalDepth = 10;
          int maxAdditionalDepth = 100;
          int additionalDepth;
          if (oceanScale < 0.3) {
            additionalDepth = (int) (minAdditionalDepth * (oceanScale / 0.3));
          } else {
            additionalDepth =
                minAdditionalDepth
                    + (int)
                        ((maxAdditionalDepth - minAdditionalDepth) * ((oceanScale - 0.3) / 0.7));
          }

          int depressedSurface = surface - additionalDepth;
          // When oceanScale is near 1, the depressed surface is almost fully applied.
          surface = (int) (surface * (1 - oceanScale) + depressedSurface * oceanScale);
        }

        int bedrockTopLimit = Chunk.CHUNK_Y - (((int) surface % 5) + 2);
        int subsurfaceTopLimit = bedrockTopLimit - (((int) surface % 15) + 32);
        int upperOreTopLimit = bedrockTopLimit - (((int) surface % 10) + 42);

        for (int y = 0; y < Chunk.CHUNK_Y; y++) {
          Blocks block = generateBlockBasedOn(y, bp, surface, bedrockTopLimit, subsurfaceTopLimit);

          // Carve caves
          double caveNoise =
              PerlinNoise.getfBM3D(
                  worldX * caveFrequency,
                  y * caveFrequency,
                  worldZ * caveFrequency,
                  caveOctaves,
                  cavePersistence,
                  caveLacunarity);

          if (heightIsBeneath(y, surface)
              && caveNoise > caveThreshold
              && block != Blocks.BEDROCK
              && block != Blocks.WATER1) {
            block = null;
          }

          blocks[x][y][z] = block;

          if (block == null || block == Blocks.WATER1 || block == Blocks.BEDROCK) {
            continue;
          }
          if (bp.biome == Biome.OCEAN && heightIsBeneath(y, subsurfaceTopLimit)) {
            continue;
          }
          if (heightIsBeneath(y, upperOreTopLimit)) {
            block = bp.innerUpperBlock;
          }

          blocks[x][y][z] = block;
        }
      }
    }
    return blocks;
  }

  // Biome Blending
  private BiomeParameters getBlendedBiomeParameters(double worldX, double worldZ) {
    double baseBiomeFrequency = 0.005;
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

    // TODO: rework Ocean to make it naturally deeper and larger; rework water-related biomes to be
    // more specific (ponds, rivers, etc.)
    // "blend" candidates
    BiomeOption[] options = {
      new BiomeOption(
          new BiomeParameters(
              Biome.OCEAN, Chunk.CHUNK_Y - 60, 3, Blocks.WATER1, Blocks.SAND, Blocks.STONE),
          -0.5,
          0.3),
      new BiomeOption(
          new BiomeParameters(
              Biome.PLAINS, Chunk.CHUNK_Y - 60, 6, Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
          -0.1,
          0.3),
      new BiomeOption(
          new BiomeParameters(
              Biome.DESERT, Chunk.CHUNK_Y - 60, 4, Blocks.SAND, Blocks.SAND, Blocks.STONE),
          0.2,
          0.3),
      new BiomeOption(
          new BiomeParameters(
              Biome.MOUNTAIN, Chunk.CHUNK_Y - 85, 15, Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
          0.6,
          0.3)
    };

    double totalWeight = 0.0;
    double blendedBaseHeight = 0.0;
    double blendedAmplitude = 0.0;
    double maxWeight = -1.0;
    BiomeParameters dominantParams = null;

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

    // Default = Plains
    if (totalWeight == 0) {
      return options[1].params;
    }

    int finalBaseHeight = (int) (blendedBaseHeight / totalWeight);
    int finalAmplitude = (int) (blendedAmplitude / totalWeight);

    // if (dominantParams.biome == Biome.OCEAN) {
    //   double oceanScale = PerlinNoise.getfBM2D(worldX * 0.000001, worldZ * 0.000001, 1, 1.0,
    // 2.0);
    //   oceanScale = (oceanScale + 1) / 2;
    //   finalAmplitude = (int) (finalAmplitude * oceanScale);
    //   finalBaseHeight = finalBaseHeight - (int) ((1 - oceanScale) * 10);
    // }

    return new BiomeParameters(
        dominantParams.biome,
        finalBaseHeight,
        finalAmplitude,
        dominantParams.surfaceBlock,
        dominantParams.subsurfaceBlock,
        dominantParams.innerUpperBlock);
  }

  // Block assignment according to biome
  private Blocks generateBlockBasedOn(
      int height, BiomeParameters bp, int surface, int bedrockTopLimit, int subsurfaceTopLimit) {
    switch (bp.biome) {
      case OCEAN:
        return getOceanBlock(height, bp, surface, bedrockTopLimit, subsurfaceTopLimit);
      case PLAINS:
        return getPlainsBlock(height, bp, surface, bedrockTopLimit, subsurfaceTopLimit);
      case MOUNTAIN:
        return getMountainBlock(height, bp, surface, bedrockTopLimit, subsurfaceTopLimit);
      case DESERT:
        return getDesertBlock(height, bp, surface, bedrockTopLimit, subsurfaceTopLimit);
      default:
        return null; // fallback
    }
  }

  private Blocks getOceanBlock(
      int height, BiomeParameters bp, int surface, int bedrockTop, int subsurfaceTop) {
    if (!heightIsBeneath(height, surface)) {
      return null;
    }
    if (heightIsBeneath(height, bedrockTop)) {
      return Blocks.BEDROCK;
    }
    if (heightIsBeneath(height, subsurfaceTop)) {
      return bp.subsurfaceBlock;
    }
    if (heightIsBeneath(height, surface) && heightIsBeneath(height, SEA_LEVEL - 1)) {
      return bp.surfaceBlock;
    }
    return null;
  }

  private Blocks getPlainsBlock(
      int height, BiomeParameters bp, int surface, int bedrockTop, int subsurfaceTop) {
    if (height == surface) {
      return bp.surfaceBlock;
    }
    if (!heightIsBeneath(height, surface)) {
      return null;
    }
    if (!heightIsBeneath(height, bedrockTop)) {
      return bp.subsurfaceBlock;
    }
    if (heightIsBeneath(height, bedrockTop)) {
      return Blocks.BEDROCK;
    }
    return null;
  }

  private Blocks getMountainBlock(
      int height, BiomeParameters bp, int surface, int bedrockTop, int subsurfaceTop) {
    if (height == surface) {
      return bp.surfaceBlock;
    }
    if (!heightIsBeneath(height, surface)) {
      return null;
    }
    if (!heightIsBeneath(height, bedrockTop)) {
      return bp.subsurfaceBlock;
    }
    if (heightIsBeneath(height, bedrockTop)) {
      return Blocks.BEDROCK;
    }
    return null;
  }

  private Blocks getDesertBlock(
      int height, BiomeParameters bp, int surface, int bedrockTop, int subsurfaceTop) {
    if (height == surface) {
      return bp.surfaceBlock;
    }
    if (!heightIsBeneath(height, surface)) {
      return null;
    }
    if (!heightIsBeneath(height, bedrockTop)) {
      return bp.subsurfaceBlock;
    }
    if (heightIsBeneath(height, bedrockTop)) {
      return Blocks.BEDROCK;
    }
    return null;
  }

  private boolean heightIsBeneath(int height, int limit) {
    return height > limit;
  }
}
