package com.game.minecraft.world.generations;

import com.game.minecraft.world.chunks.Chunk;
import com.game.minecraft.world.chunks.ChunkCoordinate;
import java.util.Map;

/** Façade for simulation tasks. Internally delegates to TreeDecorator and WaterFlowSimulator. */
public class Simulator {

  // Singletons for static delegations
  private static final TreeDecorator treeDecorator = new TreeDecorator();
  private static final WaterFlowSimulator waterSim = new WaterFlowSimulator();
  private static final OreDecorator oreDecorator = new OreDecorator();

  /** Decorates the active region with trees (delegate). */
  public static void decorateTreesForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    treeDecorator.decorateTreesForActiveRegion(activeChunks);
  }

  /** Runs the water flow simulation across active region (delegate). */
  public static void simulateWaterFlowForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    waterSim.simulateWaterFlowForActiveRegion(activeChunks);
  }

  /** Decorates the active region with ores (delegate). */
  public static void decorateOresForActiveRegion(Map<ChunkCoordinate, Chunk> activeChunks) {
    oreDecorator.decorateOresForActiveRegion(activeChunks);
  }
}
