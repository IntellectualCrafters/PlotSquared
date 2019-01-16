package com.github.intellectualsites.plotsquared.bukkit.generator;

import com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil;
import com.github.intellectualsites.plotsquared.bukkit.util.block.GenChunk;
import com.github.intellectualsites.plotsquared.plot.PlotSquared;
import com.github.intellectualsites.plotsquared.plot.generator.GeneratorWrapper;
import com.github.intellectualsites.plotsquared.plot.generator.HybridPlotWorld;
import com.github.intellectualsites.plotsquared.plot.generator.IndependentPlotGenerator;
import com.github.intellectualsites.plotsquared.plot.object.*;
import com.github.intellectualsites.plotsquared.plot.object.worlds.SingleWorldGenerator;
import com.github.intellectualsites.plotsquared.plot.util.ChunkManager;
import com.github.intellectualsites.plotsquared.plot.util.MainUtil;
import com.github.intellectualsites.plotsquared.plot.util.MathMan;
import com.github.intellectualsites.plotsquared.plot.util.block.GlobalBlockQueue;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.github.intellectualsites.plotsquared.plot.util.block.ScopedLocalBlockQueue;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.*;

public class BukkitPlotGenerator extends ChunkGenerator
    implements GeneratorWrapper<ChunkGenerator> {

    private final GenChunk chunkSetter;
    private final IndependentPlotGenerator plotGenerator;
    private final ChunkGenerator platformGenerator;
    private final boolean full;
    private List<BlockPopulator> populators;
    private boolean loaded = false;

    public BukkitPlotGenerator(IndependentPlotGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("Generator may not be null!");
        }
        this.plotGenerator = generator;
        this.platformGenerator = this;
        populators = new ArrayList<>();
        this.populators.add(new BlockPopulator() {

            private LocalBlockQueue queue;

            @Override public void populate(World world, Random r, Chunk c) {
                if (queue == null) {
                    queue = GlobalBlockQueue.IMP.getNewQueue(world.getName(), false);
                }
                PlotArea area = PlotSquared.get().getPlotArea(world.getName(), null);
                ChunkWrapper wrap = new ChunkWrapper(area.worldname, c.getX(), c.getZ());
                ScopedLocalBlockQueue chunk = queue.getForChunk(wrap.x, wrap.z);
                if (BukkitPlotGenerator.this.plotGenerator.populateChunk(chunk, area)) {
                    queue.flush();
                }
            }
        });
        this.chunkSetter = new GenChunk(null, null);
        this.full = true;
        MainUtil.initCache();
    }

    public BukkitPlotGenerator(final String world, final ChunkGenerator cg) {
        if (cg instanceof BukkitPlotGenerator) {
            throw new IllegalArgumentException("ChunkGenerator: " + cg.getClass().getName()
                + " is already a BukkitPlotGenerator!");
        }
        this.full = false;
        PlotSquared.debug("BukkitPlotGenerator does not fully support: " + cg);
        this.platformGenerator = cg;
        this.plotGenerator = new IndependentPlotGenerator() {
            @Override public void processSetup(SetupObject setup) {
            }

            @Override public void initialize(PlotArea area) {
            }

            @Override public PlotManager getNewPlotManager() {
                return PlotSquared.get().IMP.getDefaultGenerator().getNewPlotManager();
            }

            @Override public String getName() {
                return cg.getClass().getName();
            }

            @Override
            public PlotArea getNewPlotArea(String world, String id, PlotId min, PlotId max) {
                return PlotSquared.get().IMP.getDefaultGenerator()
                    .getNewPlotArea(world, id, min, max);
            }

            @Override public BlockBucket[][] generateBlockBucketChunk(PlotArea settings) {
                BlockBucket[][] blockBuckets = new BlockBucket[16][];
                HybridPlotWorld hpw = (HybridPlotWorld) settings;
                // Bedrock
                if (hpw.PLOT_BEDROCK) {
                    for (short x = 0; x < 16; x++) {
                        for (short z = 0; z < 16; z++) {
                            blockBuckets[0][(z << 4) | x] =
                                BlockBucket.withSingle(PlotBlock.get("bedrock"));
                        }
                    }
                }
                for (short x = 0; x < 16; x++) {
                    for (short z = 0; z < 16; z++) {
                        for (int y = 1; y < hpw.PLOT_HEIGHT; y++) {
                            blockBuckets[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = hpw.MAIN_BLOCK;
                        }
                        blockBuckets[hpw.PLOT_HEIGHT >> 4][((hpw.PLOT_HEIGHT & 0xF) << 8) | (z << 4)
                            | x] = hpw.MAIN_BLOCK;
                    }
                }
                return blockBuckets;
            }

            @Override
            public void generateChunk(final ScopedLocalBlockQueue result, PlotArea settings) {
                World w = BukkitUtil.getWorld(world);
                Location min = result.getMin();
                int cx = min.getX() >> 4;
                int cz = min.getZ() >> 4;
                Random r = new Random(MathMan.pair((short) cx, (short) cz));
                BiomeGrid grid = new BiomeGrid() {
                    @Override public void setBiome(int x, int z, Biome biome) {
                        result.setBiome(x, z, biome.name());
                    }

                    @Override public Biome getBiome(int arg0, int arg1) {
                        return Biome.FOREST;
                    }
                };
                try {
                    // ChunkData will spill a bit
                    ChunkData data = cg.generateChunkData(w, r, cx, cz, grid);
                    if (data != null) {
                        return;
                    }
                } catch (Throwable ignored) {
                }
                /* TODO: Redo this
                // Populator spillage
                short[][] tmp = cg.generateExtBlockSections(w, r, cx, cz, grid);
                if (tmp != null) {
                    for (int i = 0; i < tmp.length; i++) {
                        short[] section = tmp[i];
                        if (section == null) {
                            if (i < 7) {
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int y = i << 4; y < (i << 4) + 16; y++) {
                                            result.setBlock(x, y, z, PlotBlock.get("air"));
                                        }
                                    }
                                }
                            }
                            continue;
                        }
                        for (int j = 0; j < section.length; j++) {
                            int x = MainUtil.x_loc[i][j];
                            int y = MainUtil.y_loc[i][j];
                            int z = MainUtil.z_loc[i][j];
                            result.setBlock(x, y, z, section[j], (byte) 0);
                        }
                    }
                }
                */
                for (BlockPopulator populator : cg.getDefaultPopulators(w)) {
                    populator.populate(w, r, w.getChunkAt(cx, cz));
                }
            }
        };
        this.chunkSetter = new GenChunk(null, new ChunkWrapper(world, 0, 0));
        MainUtil.initCache();
    }

    @Override public void augment(PlotArea area) {
        BukkitAugmentedGenerator.get(BukkitUtil.getWorld(area.worldname));
    }

    @Override public boolean isFull() {
        return this.full;
    }

    @Override public IndependentPlotGenerator getPlotGenerator() {
        return this.plotGenerator;
    }

    @Override public ChunkGenerator getPlatformGenerator() {
        return this.platformGenerator;
    }

    @Override public List<BlockPopulator> getDefaultPopulators(World world) {
        try {
            if (!this.loaded) {
                String name = world.getName();
                PlotSquared.get().loadWorld(name, this);
                Set<PlotArea> areas = PlotSquared.get().getPlotAreas(name);
                if (!areas.isEmpty()) {
                    PlotArea area = areas.iterator().next();
                    if (!area.MOB_SPAWNING) {
                        if (!area.SPAWN_EGGS) {
                            world.setSpawnFlags(false, false);
                        }
                        world.setAmbientSpawnLimit(0);
                        world.setAnimalSpawnLimit(0);
                        world.setMonsterSpawnLimit(0);
                        world.setWaterAnimalSpawnLimit(0);
                    } else {
                        world.setSpawnFlags(true, true);
                        world.setAmbientSpawnLimit(-1);
                        world.setAnimalSpawnLimit(-1);
                        world.setMonsterSpawnLimit(-1);
                        world.setWaterAnimalSpawnLimit(-1);
                    }
                }
                this.loaded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<BlockPopulator> toAdd = new ArrayList<>();
        List<BlockPopulator> existing = world.getPopulators();
        if (populators == null && platformGenerator != null) {
            populators = new ArrayList<>(platformGenerator.getDefaultPopulators(world));
        }
        for (BlockPopulator populator : this.populators) {
            if (!existing.contains(populator)) {
                toAdd.add(populator);
            }
        }
        return toAdd;
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int cx, int cz, BiomeGrid grid) {
        GenChunk result = this.chunkSetter;
        if (this.getPlotGenerator() instanceof SingleWorldGenerator) {
            if (result.getCd() != null) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        grid.setBiome(x, z, Biome.PLAINS);
                    }
                }
                return result.getCd();
            }
        }
        // Set the chunk location
        result.setChunk(new ChunkWrapper(world.getName(), cx, cz));
        // Set the result data
        result.setCd(createChunkData(world));
        result.grid = grid;
        result.result = generateExtBlockSections(world, random, cx, cz, grid);

        // Catch any exceptions (as exceptions usually thrown)
        try {
            // Fill the result data if necessary
            if (this.platformGenerator != this) {
                return this.platformGenerator.generateChunkData(world, random, cx, cz, grid);
            } else {
                generate(world, result);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // Return the result data
        return result.getCd();
    }

    private void generate(World world, ScopedLocalBlockQueue result) {
        // Load if improperly loaded
        if (!this.loaded) {
            String name = world.getName();
            PlotSquared.get().loadWorld(name, this);
            this.loaded = true;
        }
        // Process the chunk
        if (ChunkManager.preProcessChunk(result)) {
            return;
        }
        PlotArea area = PlotSquared.get().getPlotArea(world.getName(), null);
        try {
            this.plotGenerator.generateChunk(this.chunkSetter, area);
        } catch (Throwable e) {
            // Recover from generator error
            e.printStackTrace();
        }
        ChunkManager.postProcessChunk(result);
    }


    public PlotBlock[][] generateExtBlockSections(World world, Random r, int cx, int cz,
        BiomeGrid grid) {
        GenChunk result = this.chunkSetter;
        // Set the chunk location
        result.setChunk(new ChunkWrapper(world.getName(), cx, cz));
        // Set the result data
        result.result = new PlotBlock[16][];
        result.grid = grid;
        // Catch any exceptions (as exceptions usually thrown)
        try {
            if (this.platformGenerator != this) {
                final ChunkData chunkData =
                    this.platformGenerator.generateChunkData(world, r, cx, cz, grid);
                final PlotBlock[][] blocks = new PlotBlock[world.getMaxHeight() / 16][];
                // section ID = Y >> 4
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < world.getMaxHeight(); y++) {
                            if (blocks[y >> 4] == null) {
                                blocks[y >> 4] = new PlotBlock[4096];
                            }
                            blocks[y >> 4][((y & 0xF) << 8) | (z << 4) | x] =
                                PlotBlock.get(chunkData.getType(x, y, z));
                        }
                    }
                }
            } else {
                generate(world, result);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // Return the result data
        return result.result;
    }


    /**
     * Allow spawning everywhere.
     *
     * @param world Ignored
     * @param x     Ignored
     * @param z     Ignored
     * @return always true
     */
    @Override public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override public String toString() {
        if (this.platformGenerator == this) {
            return this.plotGenerator.getName();
        }
        if (this.platformGenerator == null) {
            return "null";
        } else {
            return this.platformGenerator.getClass().getName();
        }
    }

    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return toString().equals(obj.toString()) || toString().equals(obj.getClass().getName());
    }
}
