package com.game.minecraft.utils;

// TODO: use or mimic OpenSimplex or Perlin Noise
public class SimpleNoise {
  private final int seed;

  public SimpleNoise(int seed) {
    this.seed = seed;
  }

  private double noise(int x, int y) { // pseudo-random [-1,1]
    int n = x + y * 57 + seed * 131;
    n = (n << 13) ^ n;
    // magic numbers to mix the bits and produce a pseudorandom value.
    double noiseValue =
        1.0 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0;
    return noiseValue;
  }

  // smooth noise using bilinear interpolation generating [-1,1]
  public double noise(double x, double y) {
    int xInt = (int) Math.floor(x);
    int yInt = (int) Math.floor(y);
    double xFrac = x - xInt;
    double yFrac = y - yInt;

    // corners
    double n00 = noise(xInt, yInt);
    double n10 = noise(xInt + 1, yInt);
    double n01 = noise(xInt, yInt + 1);
    double n11 = noise(xInt + 1, yInt + 1);

    // interpolate horizontally
    double i1 = interpolate(n00, n10, xFrac);
    double i2 = interpolate(n01, n11, xFrac);

    // interpolate vertically 
    return interpolate(i1, i2, yFrac);
  }

  private double interpolate(double a, double b, double t) {
    double ft = t * Math.PI;
    double f = (1 - Math.cos(ft)) * 0.5;
    return a * (1 - f) + b * f;
  }
}
