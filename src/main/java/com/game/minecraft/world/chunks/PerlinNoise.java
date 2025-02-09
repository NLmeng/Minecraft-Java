package com.game.minecraft.world;

// credits:
// https://rtouti.github.io/graphics/perlin-noise-algorithm
// http://mrl.nyu.edu/~perlin/paper445.pdf
// https://thebookofshaders.com/13/

public class PerlinNoise {
  static final int perm[] = {
    36, 204, 50, 197, 13, 231, 144, 39, 155, 73, 195, 162, 51, 5, 3, 111, 49, 20, 66, 71, 112, 153,
    160, 42, 135, 250, 180, 170, 219, 29, 128, 104, 37, 76, 65, 181, 88, 114, 86, 12, 163, 48, 186,
    235, 137, 63, 236, 11, 117, 230, 132, 118, 183, 38, 140, 172, 45, 158, 46, 165, 26, 80, 166, 83,
    97, 218, 156, 138, 53, 4, 207, 147, 32, 206, 173, 16, 56, 103, 174, 145, 122, 161, 237, 171, 59,
    168, 58, 101, 227, 248, 40, 222, 220, 159, 96, 167, 198, 142, 200, 25, 244, 70, 139, 199, 208,
    75, 148, 7, 107, 60, 108, 33, 210, 175, 8, 67, 192, 27, 209, 253, 143, 52, 178, 254, 255, 77,
    74, 64, 146, 98, 81, 229, 213, 134, 79, 252, 246, 232, 68, 249, 223, 193, 136, 119, 191, 169,
    205, 203, 30, 1, 54, 69, 62, 10, 240, 224, 217, 185, 221, 251, 28, 177, 43, 34, 242, 17, 225,
    19, 87, 106, 120, 123, 234, 35, 2, 6, 149, 84, 72, 91, 179, 124, 22, 94, 151, 152, 9, 141, 89,
    41, 0, 129, 57, 78, 182, 14, 93, 126, 245, 105, 150, 215, 125, 241, 115, 247, 188, 189, 55, 44,
    131, 176, 113, 154, 99, 228, 21, 157, 82, 85, 61, 196, 47, 164, 130, 243, 201, 226, 121, 18,
    116, 92, 100, 15, 216, 211, 23, 187, 133, 95, 214, 190, 127, 202, 24, 238, 90, 212, 194, 109,
    31, 102, 184, 233, 110, 239
  };
  static final int permFull[] = new int[512];

  static {
    for (int i = 0; i < 256; i++) permFull[i] = permFull[256 + i] = perm[i];
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
