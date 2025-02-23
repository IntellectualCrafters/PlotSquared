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
package com.plotsquared.bukkit.listener;

import com.google.inject.Inject;
import com.plotsquared.bukkit.player.BukkitPlayer;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.DisableBlockUpdateFlag;
import com.plotsquared.core.plot.flag.implementations.DisablePhysicsFlag;
import com.plotsquared.core.plot.flag.implementations.RedstoneFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.PlotFlagUtil;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public class HighFreqBlockEventListener implements Listener {

    private static final Set<Material> PISTONS = Set.of(
            Material.PISTON,
            Material.STICKY_PISTON
    );
    private static final Set<Material> PHYSICS_BLOCKS = Set.of(
            Material.TURTLE_EGG,
            Material.TURTLE_SPAWN_EGG
    );

    private final PlotAreaManager plotAreaManager;
    private final WorldEdit worldEdit;

    @Inject
    public HighFreqBlockEventListener(final @NonNull PlotAreaManager plotAreaManager, final @NonNull WorldEdit worldEdit) {
        this.plotAreaManager = plotAreaManager;
        this.worldEdit = worldEdit;
    }

    public static void sendBlockChange(final org.bukkit.Location bloc, final BlockData data) {
        TaskManager.runTaskLater(() -> {
            String world = bloc.getWorld().getName();
            int x = bloc.getBlockX();
            int z = bloc.getBlockZ();
            int distance = Bukkit.getViewDistance() * 16;

            for (final PlotPlayer<?> player : PlotSquared.platform().playerManager().getPlayers()) {
                Location location = player.getLocation();
                if (location.getWorldName().equals(world)) {
                    if (16 * Math.abs(location.getX() - x) / 16 > distance || 16 * Math.abs(location.getZ() - z) / 16 > distance) {
                        continue;
                    }
                    ((BukkitPlayer) player).player.sendBlockChange(bloc, data);
                }
            }
        }, TaskTime.ticks(3L));
    }

    @EventHandler
    public void onRedstoneEvent(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, RedstoneFlag.class, false)) {
                event.setNewCurrent(0);
            }
            return;
        }
        if (!plot.getFlag(RedstoneFlag.class)) {
            event.setNewCurrent(0);
            plot.debug("Redstone event was cancelled because redstone = false");
            return;
        }
        if (Settings.Redstone.DISABLE_OFFLINE) {
            boolean disable = false;
            if (!DBFunc.SERVER.equals(plot.getOwner())) {
                if (plot.isMerged()) {
                    disable = true;
                    for (UUID owner : plot.getOwners()) {
                        if (PlotSquared.platform().playerManager().getPlayerIfExists(owner) != null) {
                            disable = false;
                            break;
                        }
                    }
                } else {
                    disable = PlotSquared.platform().playerManager().getPlayerIfExists(plot.getOwnerAbs()) == null;
                }
            }
            if (disable) {
                for (UUID trusted : plot.getTrusted()) {
                    if (PlotSquared.platform().playerManager().getPlayerIfExists(trusted) != null) {
                        disable = false;
                        break;
                    }
                }
                if (disable) {
                    event.setNewCurrent(0);
                    plot.debug("Redstone event was cancelled because no trusted player was in the plot");
                    return;
                }
            }
        }
        if (Settings.Redstone.DISABLE_UNOCCUPIED) {
            for (final PlotPlayer<?> player : PlotSquared.platform().playerManager().getPlayers()) {
                if (plot.equals(player.getCurrentPlot())) {
                    return;
                }
            }
            event.setNewCurrent(0);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPhysicsEvent(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Block sourceBlock = event.getSourceBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlotAbs(location);
        if (plot == null) {
            return;
        }
        if (event.getChangedType().hasGravity() && plot.getFlag(DisablePhysicsFlag.class)) {
            event.setCancelled(true);
            sendBlockChange(event.getBlock().getLocation(), event.getBlock().getBlockData());
            plot.debug("Prevented block physics and resent block change because disable-physics = true");
            return;
        }
        if (plot.getFlag(DisableBlockUpdateFlag.class)) {
            if (!event.getChangedType().hasGravity()) {
                if (sourceBlock.getType().isAir() || block.getType().isAir()) {
                    return;
                }
                if (!sourceBlock.getType().name().toLowerCase(Locale.ROOT).contains("snow")
                        || !block.getLocation().add(0, -1, 0).getBlock().getType().equals(Material.GRASS_BLOCK)) {
                    event.setCancelled(true);
                    plot.debug("Prevented block update because disable-block-update = true");
                }
            }
        }
        if (event.getChangedType() == Material.COMPARATOR) {
            if (!plot.getFlag(RedstoneFlag.class)) {
                event.setCancelled(true);
                plot.debug("Prevented comparator update because redstone = false");
            }
            return;
        }
        if (PHYSICS_BLOCKS.contains(event.getChangedType())) {
            if (plot.getFlag(DisablePhysicsFlag.class)) {
                event.setCancelled(true);
                plot.debug("Prevented block physics because disable-physics = true");
            }
            return;
        }
        if (Settings.Redstone.DETECT_INVALID_EDGE_PISTONS) {
            if (PISTONS.contains(block.getType())) {
                org.bukkit.block.data.Directional piston = (org.bukkit.block.data.Directional) block.getBlockData();
                final BlockFace facing = piston.getFacing();
                location = location.add(facing.getModX(), facing.getModY(), facing.getModZ());
                Plot newPlot = area.getOwnedPlotAbs(location);
                if (plot.equals(newPlot)) {
                    return;
                }
                if (!plot.isMerged() || !plot.getConnectedPlots().contains(newPlot)) {
                    event.setCancelled(true);
                    plot.debug("Prevented piston update because of invalid edge piston detection");
                }
            }
        }
    }

}
