package com.game.minecraft.world;

import java.util.Random;

// credits:
// https://rtouti.github.io/graphics/perlin-noise-algorithm
// http://mrl.nyu.edu/~perlin/paper445.pdf
// https://thebookofshaders.com/13/

public class PerlinNoise {
  private static final int[] perm = new int[256];
  private static final int[] permFull = new int[512];
  private static final Random rand = new Random();

  static {
    setSeed(0);
  }

  public static void setSeed(long seed) {
    rand.setSeed(seed);

    for (int i = 0; i < 256; i++) {
      perm[i] = i;
    }

    for (int i = 255; i > 0; i--) {
      int index = rand.nextInt(i + 1);
      int temp = perm[i];
      perm[i] = perm[index];
      perm[index] = temp;
    }

    System.arraycopy(perm, 0, permFull, 0, 256);
    System.arraycopy(perm, 0, permFull, 256, 256);
  }

  /**
   * Computes 2D Perlin noise. Determine grid cell of the xy. Calculate relative coords inside that
   * cell. Apply fade to use in lerp. Use permutation to hash coords of 4 corners. Calculate
   * gradient vector and compute dot product with vector from corner to xy. lerp the produce of the
   * 4 dot products.
   */
  public static double getNoise2D(double x, double y) {
    int X = (int) Math.floor(x) & 255;
    int Y = (int) Math.floor(y) & 255;

    double xfractional = x - Math.floor(x);
    double yfractional = y - Math.floor(y);

    double u = fade(xfractional);
    double v = fade(yfractional);

    int topleft = permFull[X + permFull[Y]];
    int botleft = permFull[X + permFull[Y + 1]];
    int topright = permFull[X + 1 + permFull[Y]];
    int botright = permFull[X + 1 + permFull[Y + 1]];

    double dotAA = grad(xfractional, yfractional, topleft);
    double dotBA = grad(xfractional - 1, yfractional, topright);
    double dotAB = grad(xfractional, yfractional - 1, botleft);
    double dotBB = grad(xfractional - 1, yfractional - 1, botright);

    return lerp(lerp(dotAA, dotBA, u), lerp(dotAB, dotBB, u), v);
  }

  /**
   * Computes fractal Brownian motion at a given 2D point by summing several octaves (layers) of
   * Perlin noise, each with increasing frequency and decreasing amplitude.
   */
  public static double getfBM2D(
      double x, double y, int octaves, double persistence, double lacunarity) {
    double total = 0.0;
    double amplitude = 0.5; // smoothness / flatness
    double frequency = 1.0; // stretchness / finer details
    double maxAmplitude = 0.0;

    for (int i = 0; i < octaves; i++) {
      double noiseValue = getNoise2D(x * frequency, y * frequency);
      total += noiseValue * amplitude;

      maxAmplitude += amplitude;

      amplitude *= persistence;
      frequency *= lacunarity;
    }

    return total / maxAmplitude;
  }

  static double fade(double t) {
    // using ease curve, t=0 or t=1 has first and second derivative 0
    return (t * (t * 6 - 15) + 10) * t * t * t;
  }

  static double lerp(double a, double b, double t) {
    // linear interpolation formula
    return t * (b - a) + a;
  }

  static double grad(double x, double y, int hash) {
    // select one of 4 gradient directions
    int h = hash & 3;
    switch (h) {
      case 0:
        return x + y;
      case 1:
        return y - x;
      case 2:
        return x - y;
      case 3:
        return -x - y;
      default:
        return 0;
    }
  }
}
