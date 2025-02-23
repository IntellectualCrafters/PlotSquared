/*
 * PlotSquared, a land and world management plugin for Minecraft.
 * Copyright (C) IntellectualSites <https://intellectualsites.com>
 * Copyright (C) IntellectualSites team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.bukkit.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.plotsquared.core.generator.AugmentedUtils;
import com.plotsquared.core.inject.factory.ProgressSubscriberFactory;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.location.PlotLoc;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotManager;
import com.plotsquared.core.queue.GlobalBlockQueue;
import com.plotsquared.core.queue.QueueCoordinator;
import com.plotsquared.core.queue.ZeroedDelegateScopedQueueCoordinator;
import com.plotsquared.core.util.ChunkManager;
import com.plotsquared.core.util.RegionManager;
import com.plotsquared.core.util.WorldUtil;
import com.plotsquared.core.util.entity.EntityCategories;
import com.plotsquared.core.util.task.RunnableVal;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.plotsquared.core.util.entity.EntityCategories.CAP_ANIMAL;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_ENTITY;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MISC;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_VILLAGER;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MOB;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MONSTER;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_VEHICLE;

@Singleton
public class BukkitRegionManager extends RegionManager {

    private final GlobalBlockQueue blockQueue;

    @Inject
    public BukkitRegionManager(
            @NonNull WorldUtil worldUtil, @NonNull GlobalBlockQueue blockQueue, @NonNull
    ProgressSubscriberFactory subscriberFactory
    ) {
        super(worldUtil, blockQueue, subscriberFactory);
        this.blockQueue = blockQueue;
    }

    @Override
    public boolean handleClear(
            @NonNull Plot plot,
            @Nullable Runnable whenDone,
            @NonNull PlotManager manager,
            @Nullable PlotPlayer<?> player
    ) {
        return false;
    }

    @Override
    public int[] countEntities(@NonNull Plot plot) {
        int[] existing = (int[]) plot.getMeta("EntityCount");
        if (existing != null && (System.currentTimeMillis() - (long) plot.getMeta("EntityCountTime") < 1000)) {
            return existing;
        }
        PlotArea area = plot.getArea();
        World world = BukkitUtil.getWorld(area.getWorldName());
        Location bot = plot.getBottomAbs();
        Location top = plot.getTopAbs();
        int bx = bot.getX() >> 4;
        int bz = bot.getZ() >> 4;

        int tx = top.getX() >> 4;
        int tz = top.getZ() >> 4;

        int size = tx - bx << 4;

        Set<Chunk> chunks = new HashSet<>();
        for (int X = bx; X <= tx; X++) {
            for (int Z = bz; Z <= tz; Z++) {
                if (world.isChunkLoaded(X, Z)) {
                    chunks.add(world.getChunkAt(X, Z));
                }
            }
        }

        boolean doWhole = false;
        List<Entity> entities = null;
        if (size > 200 && chunks.size() > 200) {
            entities = world.getEntities();
            if (entities.size() < 16 + size / 8) {
                doWhole = true;
            }
        }

        int[] count = new int[7];
        if (doWhole) {
            for (Entity entity : entities) {
                org.bukkit.Location location = entity.getLocation();
                PaperLib.getChunkAtAsync(location).thenAccept(chunk -> {
                    if (chunks.contains(chunk)) {
                        int X = chunk.getX();
                        int Z = chunk.getZ();
                        if (X > bx && X < tx && Z > bz && Z < tz) {
                            count(count, entity);
                        } else {
                            Plot other = area.getPlot(BukkitUtil.adapt(location));
                            if (plot.equals(other)) {
                                count(count, entity);
                            }
                        }
                    }
                });
            }
        } else {
            for (Chunk chunk : chunks) {
                int X = chunk.getX();
                int Z = chunk.getZ();
                Entity[] entities1 = chunk.getEntities();
                for (Entity entity : entities1) {
                    if (X == bx || X == tx || Z == bz || Z == tz) {
                        Plot other = area.getPlot(BukkitUtil.adapt(entity.getLocation()));
                        if (plot.equals(other)) {
                            count(count, entity);
                        }
                    } else {
                        count(count, entity);
                    }
                }
            }
        }
        return count;
    }

    @Override
    public boolean regenerateRegion(
            final @NonNull Location pos1,
            final @NonNull Location pos2,
            final boolean ignoreAugment,
            final @Nullable Runnable whenDone
    ) {
        final BukkitWorld world = (BukkitWorld) worldUtil.getWeWorld(pos1.getWorldName());

        final int p1x = pos1.getX();
        final int p1z = pos1.getZ();
        final int p2x = pos2.getX();
        final int p2z = pos2.getZ();
        final int bcx = p1x >> 4;
        final int bcz = p1z >> 4;
        final int tcx = p2x >> 4;
        final int tcz = p2z >> 4;

        final QueueCoordinator queue = blockQueue.getNewQueue(world);
        final QueueCoordinator regenQueue = blockQueue.getNewQueue(world);
        queue.addReadChunks(new CuboidRegion(pos1.getBlockVector3(), pos2.getBlockVector3()).getChunks());
        queue.setChunkConsumer(chunk -> {

            int x = chunk.getX();
            int z = chunk.getZ();
            int xxb = x << 4;
            int zzb = z << 4;
            int xxt = xxb + 15;
            int zzt = zzb + 15;
            if (xxb >= p1x && xxt <= p2x && zzb >= p1z && zzt <= p2z) {
                AugmentedUtils.bypass(ignoreAugment, () -> regenQueue.regenChunk(chunk.getX(), chunk.getZ()));
                return;
            }
            boolean checkX1 = false;

            int xxb2;

            if (x == bcx) {
                xxb2 = p1x - 1;
                checkX1 = true;
            } else {
                xxb2 = xxb;
            }
            boolean checkX2 = false;
            int xxt2;
            if (x == tcx) {
                xxt2 = p2x + 1;
                checkX2 = true;
            } else {
                xxt2 = xxt;
            }
            boolean checkZ1 = false;
            int zzb2;
            if (z == bcz) {
                zzb2 = p1z - 1;
                checkZ1 = true;
            } else {
                zzb2 = zzb;
            }
            boolean checkZ2 = false;
            int zzt2;
            if (z == tcz) {
                zzt2 = p2z + 1;
                checkZ2 = true;
            } else {
                zzt2 = zzt;
            }
            final ContentMap map = new ContentMap();
            if (checkX1) {
                map.saveRegion(world, xxb, xxb2, zzb2, zzt2); //
            }
            if (checkX2) {
                map.saveRegion(world, xxt2, xxt, zzb2, zzt2); //
            }
            if (checkZ1) {
                map.saveRegion(world, xxb2, xxt2, zzb, zzb2); //
            }
            if (checkZ2) {
                map.saveRegion(world, xxb2, xxt2, zzt2, zzt); //
            }
            if (checkX1 && checkZ1) {
                map.saveRegion(world, xxb, xxb2, zzb, zzb2); //
            }
            if (checkX2 && checkZ1) {
                map.saveRegion(world, xxt2, xxt, zzb, zzb2); // ?
            }
            if (checkX1 && checkZ2) {
                map.saveRegion(world, xxb, xxb2, zzt2, zzt); // ?
            }
            if (checkX2 && checkZ2) {
                map.saveRegion(world, xxt2, xxt, zzt2, zzt); //
            }
            CuboidRegion currentPlotClear = new CuboidRegion(pos1.getBlockVector3(), pos2.getBlockVector3());
            map.saveEntitiesOut(Bukkit.getWorld(world.getName()).getChunkAt(x, z), currentPlotClear);
            AugmentedUtils.bypass(
                    ignoreAugment,
                    () -> ChunkManager.setChunkInPlotArea(null, new RunnableVal<ZeroedDelegateScopedQueueCoordinator>() {
                        @Override
                        public void run(ZeroedDelegateScopedQueueCoordinator value) {
                            Location min = value.getMin();
                            int bx = min.getX();
                            int bz = min.getZ();
                            for (int x1 = 0; x1 < 16; x1++) {
                                for (int z1 = 0; z1 < 16; z1++) {
                                    PlotLoc plotLoc = new PlotLoc(bx + x1, bz + z1);
                                    BaseBlock[] ids = map.allBlocks.get(plotLoc);
                                    if (ids != null) {
                                        int minY = value.getMin().getY();
                                        for (int yIndex = 0; yIndex < ids.length; yIndex++) {
                                            int y = yIndex + minY;
                                            BaseBlock id = ids[yIndex];
                                            if (id != null) {
                                                value.setBlock(x1, y, z1, id);
                                            } else {
                                                value.setBlock(x1, y, z1, BlockTypes.AIR.getDefaultState());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }, world.getName(), chunk)
            );
            //map.restoreBlocks(worldObj, 0, 0);
            map.restoreEntities(Bukkit.getWorld(world.getName()));
        });
        regenQueue.setCompleteTask(whenDone);
        queue.setCompleteTask(regenQueue::enqueue);
        queue.enqueue();
        return true;
    }

    @Override
    public void clearAllEntities(@NonNull Location pos1, @NonNull Location pos2) {
        String world = pos1.getWorldName();

        final World bukkitWorld = BukkitUtil.getWorld(world);
        final List<Entity> entities;
        if (bukkitWorld != null) {
            entities = new ArrayList<>(bukkitWorld.getEntities());
        } else {
            entities = new ArrayList<>();
        }

        int bx = pos1.getX();
        int bz = pos1.getZ();
        int tx = pos2.getX();
        int tz = pos2.getZ();
        for (Entity entity : entities) {
            if (!(entity instanceof Player)) {
                org.bukkit.Location location = entity.getLocation();
                if (location.getX() >= bx && location.getX() <= tx && location.getZ() >= bz && location.getZ() <= tz) {
                    if (entity.hasMetadata("ps-tmp-teleport")) {
                        continue;
                    }
                    entity.remove();
                }
            }
        }
    }

    private void count(int[] count, @NonNull Entity entity) {
        final com.sk89q.worldedit.world.entity.EntityType entityType = BukkitAdapter.adapt(entity.getType());

        if (EntityCategories.PLAYER.contains(entityType)) {
            return;
        } else if (EntityCategories.PROJECTILE.contains(entityType) || EntityCategories.OTHER.contains(entityType) || EntityCategories.HANGING
                .contains(entityType)) {
            count[CAP_MISC]++;
        } else if (EntityCategories.VILLAGER.contains(entityType)) {
            count[CAP_MOB]++;
            count[CAP_VILLAGER]++;
        } else if (EntityCategories.ANIMAL.contains(entityType) || EntityCategories.TAMEABLE
                .contains(entityType)) {
            count[CAP_MOB]++;
            count[CAP_ANIMAL]++;
        } else if (EntityCategories.VEHICLE.contains(entityType)) {
            count[CAP_VEHICLE]++;
        } else if (EntityCategories.HOSTILE.contains(entityType)) {
            count[CAP_MOB]++;
            count[CAP_MONSTER]++;
        }
        count[CAP_ENTITY]++;
    }

}
