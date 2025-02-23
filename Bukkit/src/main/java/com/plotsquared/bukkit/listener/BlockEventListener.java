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
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.BlockBurnFlag;
import com.plotsquared.core.plot.flag.implementations.BlockIgnitionFlag;
import com.plotsquared.core.plot.flag.implementations.BreakFlag;
import com.plotsquared.core.plot.flag.implementations.ConcreteHardenFlag;
import com.plotsquared.core.plot.flag.implementations.CoralDryFlag;
import com.plotsquared.core.plot.flag.implementations.CropGrowFlag;
import com.plotsquared.core.plot.flag.implementations.DisablePhysicsFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.ExplosionFlag;
import com.plotsquared.core.plot.flag.implementations.GrassGrowFlag;
import com.plotsquared.core.plot.flag.implementations.IceFormFlag;
import com.plotsquared.core.plot.flag.implementations.IceMeltFlag;
import com.plotsquared.core.plot.flag.implementations.InstabreakFlag;
import com.plotsquared.core.plot.flag.implementations.KelpGrowFlag;
import com.plotsquared.core.plot.flag.implementations.LeafDecayFlag;
import com.plotsquared.core.plot.flag.implementations.LiquidFlowFlag;
import com.plotsquared.core.plot.flag.implementations.MycelGrowFlag;
import com.plotsquared.core.plot.flag.implementations.PlaceFlag;
import com.plotsquared.core.plot.flag.implementations.SnowFormFlag;
import com.plotsquared.core.plot.flag.implementations.SnowMeltFlag;
import com.plotsquared.core.plot.flag.implementations.SoilDryFlag;
import com.plotsquared.core.plot.flag.implementations.VineGrowFlag;
import com.plotsquared.core.plot.flag.types.BlockTypeWrapper;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.PlotFlagUtil;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.bukkit.Tag.CORALS;
import static org.bukkit.Tag.CORAL_BLOCKS;
import static org.bukkit.Tag.WALL_CORALS;

@SuppressWarnings("unused")
public class BlockEventListener implements Listener {
    private final PlotAreaManager plotAreaManager;
    private final WorldEdit worldEdit;

    @Inject
    public BlockEventListener(final @NonNull PlotAreaManager plotAreaManager, final @NonNull WorldEdit worldEdit) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void blockCreate(BlockPlaceEvent event) {
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Player player = event.getPlayer();
        BukkitPlayer pp = BukkitUtil.adapt(player);
        Plot plot = area.getPlot(location);
        if (plot != null) {
            if (area.notifyIfOutsideBuildArea(pp, location.getY())) {
                event.setCancelled(true);
                return;
            }
            if (!plot.hasOwner()) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                            )
                    );
                    event.setCancelled(true);
                    return;
                }
            } else if (!plot.isAdded(pp.getUUID())) {
                List<BlockTypeWrapper> place = plot.getFlag(PlaceFlag.class);
                if (place != null) {
                    Block block = event.getBlock();
                    if (place.contains(
                            BlockTypeWrapper.get(BukkitAdapter.asBlockType(block.getType())))) {
                        return;
                    }
                }
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                            )
                    );
                    event.setCancelled(true);
                    plot.debug(player.getName() + " could not place " + event.getBlock().getType()
                            + " because of the place = false");
                    return;
                }
            } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("done.building_restricted")
                    );
                    event.setCancelled(true);
                    return;
                }
            }
            if (plot.getFlag(DisablePhysicsFlag.class)) {
                Block block = event.getBlockPlaced();
                if (block.getType().hasGravity()) {
                    sendBlockChange(block.getLocation(), block.getBlockData());
                    plot.debug(event.getBlock().getType()
                            + " did not fall because of disable-physics = true");
                }
            }
        } else if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
            pp.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD)
                    )
            );
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockDestroy(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        if (plot != null && !plot.isMerging()) {
            BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
            // == rather than <= as we only care about the "ground level" not being destroyed
            if (event.getBlock().getY() == area.getMinGenHeight()) {
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL)
                            )
                    );
                    event.setCancelled(true);
                    return;
                }
            } else if (area.notifyIfOutsideBuildArea(plotPlayer, location.getY())) {
                event.setCancelled(true);
                return;
            }
            if (!plot.hasOwner()) {
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED, true)) {
                    event.setCancelled(true);
                }
                return;
            }
            if (!plot.isAdded(plotPlayer.getUUID())) {
                List<BlockTypeWrapper> destroy = plot.getFlag(BreakFlag.class);
                Block block = event.getBlock();
                final BlockType blockType = BukkitAdapter.asBlockType(block.getType());
                for (final BlockTypeWrapper blockTypeWrapper : destroy) {
                    if (blockTypeWrapper.accepts(blockType)) {
                        return;
                    }
                }
                if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                    return;
                }
                plotPlayer.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_OTHER)
                        )
                );
                event.setCancelled(true);
            } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("done.building_restricted")
                    );
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        BukkitPlayer pp = BukkitUtil.adapt(player);
        if (pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_ROAD)) {
            return;
        }
        if (this.worldEdit != null && pp.getAttribute("worldedit")) {
            if (player.getInventory().getItemInMainHand().getType() == Material
                    .getMaterial(this.worldEdit.getConfiguration().wandItem)) {
                return;
            }
        }
        pp.sendMessage(
                TranslatableCaption.of("permission.no_permission_event"),
                TagResolver.resolver(
                        "node",
                        Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_ROAD)
                )
        );
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        if (location.isPlotRoad()) {
            event.setCancelled(true);
            return;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return;
        }
        switch (event.getSource().getType().toString()) {
            case "GRASS_BLOCK":
                if (!plot.getFlag(GrassGrowFlag.class)) {
                    plot.debug("Grass could not grow because grass-grow = false");
                    event.setCancelled(true);
                }
                break;
            case "MYCELIUM":
                if (!plot.getFlag(MycelGrowFlag.class)) {
                    plot.debug("Mycelium could not grow because mycel-grow = false");
                    event.setCancelled(true);
                }
                break;
            case "WEEPING_VINES":
            case "TWISTING_VINES":
            case "CAVE_VINES":
            case "VINE":
            case "GLOW_BERRIES":
                if (!plot.getFlag(VineGrowFlag.class)) {
                    plot.debug("Vine could not grow because vine-grow = false");
                    event.setCancelled(true);
                }
                break;
            case "KELP":
                if (!plot.getFlag(KelpGrowFlag.class)) {
                    plot.debug("Kelp could not grow because kelp-grow = false");
                    event.setCancelled(true);
                }
            case "BUDDING_AMETHYST":
                if (!plot.getFlag(CropGrowFlag.class)) {
                    plot.debug("Amethyst clusters could not grow because crop-grow = false");
                    event.setCancelled(true);
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCauldronEmpty(CauldronLevelChangeEvent event) {
        Entity entity = event.getEntity();
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        // TODO Add flags for specific control over cauldron changes (rain, dripstone...)
        switch (event.getReason()) {
            case BANNER_WASH, ARMOR_WASH, EXTINGUISH -> {
                if (entity instanceof Player player) {
                    BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
                    if (plot != null) {
                        if (!plot.hasOwner()) {
                            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                                return;
                            }
                        } else if (!plot.isAdded(plotPlayer.getUUID())) {
                            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                                return;
                            }
                        } else {
                            return;
                        }
                    } else {
                        if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD)) {
                            return;
                        }
                        if (this.worldEdit != null && plotPlayer.getAttribute("worldedit")) {
                            if (player.getInventory().getItemInMainHand().getType() == Material
                                    .getMaterial(this.worldEdit.getConfiguration().wandItem)) {
                                return;
                            }
                        }
                    }
                }
                if (event.getReason() == CauldronLevelChangeEvent.ChangeReason.EXTINGUISH && event.getEntity() != null) {
                    event.getEntity().setFireTicks(0);
                }
                // Though the players fire ticks are modified,
                // the cauldron water level change is cancelled and the event should represent that.
                event.setCancelled(true);
            }
            default -> {
                // Bucket empty, Bucket fill, Bottle empty, Bottle fill are already handled in PlayerInteract event
                // Evaporation or Unknown reasons do not need to be cancelled as they are considered natural causes
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        if (event instanceof EntityBlockFormEvent) {
            return; // handled below
        }
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        if (location.isPlotRoad()) {
            event.setCancelled(true);
            return;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            return;
        }
        if (!area.buildRangeContainsY(location.getY())) {
            event.setCancelled(true);
            return;
        }
        if (org.bukkit.Tag.SNOW.isTagged(event.getNewState().getType())) {
            if (!plot.getFlag(SnowFormFlag.class)) {
                plot.debug("Snow could not form because snow-form = false");
                event.setCancelled(true);
            }
            return;
        }
        if (org.bukkit.Tag.ICE.isTagged(event.getNewState().getType())) {
            if (!plot.getFlag(IceFormFlag.class)) {
                plot.debug("Ice could not form because ice-form = false");
                event.setCancelled(true);
            }
        }
        if (event.getNewState().getType().toString().endsWith("CONCRETE")) {
            if (!plot.getFlag(ConcreteHardenFlag.class)) {
                plot.debug("Concrete powder could not harden because concrete-harden = false");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        String world = event.getBlock().getWorld().getName();
        if (!this.plotAreaManager.hasPlotArea(world)) {
            return;
        }
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        Class<? extends BooleanFlag<?>> flag;
        if (org.bukkit.Tag.SNOW.isTagged(event.getNewState().getType())) {
            flag = SnowFormFlag.class;
        } else if (org.bukkit.Tag.ICE.isTagged(event.getNewState().getType())) {
            flag = IceFormFlag.class;
        } else {
            return;
        }
        boolean allowed = plot.getFlag(flag);
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
            if (!plot.isAdded(plotPlayer.getUUID())) {
                if (allowed) {
                    return; // player is not added but forming <flag> is allowed
                }
                plot.debug(String.format(
                        "%s could not be formed because %s = false (entity is player)",
                        event.getNewState().getType(),
                        flag == SnowFormFlag.class ? "snow-form" : "ice-form"
                ));
                event.setCancelled(true); // player is not added and forming <flag> isn't allowed
            }
            return; // event is cancelled if not added and not allowed, otherwise forming <flag> is allowed
        }
        if (plot.hasOwner()) {
            if (allowed) {
                return;
            }
            plot.debug(String.format(
                    "%s could not be formed because %s = false (entity is not player)",
                    event.getNewState().getType(),
                    flag == SnowFormFlag.class ? "snow-form" : "ice-form"
            ));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        Plot plot = area.getPlot(location);
        if (plot != null) {
            if (plot.getFlag(InstabreakFlag.class)) {
                Block block = event.getBlock();
                BlockBreakEvent call = new BlockBreakEvent(block, player);
                Bukkit.getServer().getPluginManager().callEvent(call);
                if (!call.isCancelled()) {
                    if (Settings.Flags.INSTABREAK_CONSIDER_TOOL) {
                        block.breakNaturally(event.getItemInHand());
                    } else {
                        block.breakNaturally();
                    }
                }
            }
            // == rather than <= as we only care about the "ground level" not being destroyed
            if (location.getY() == area.getMinGenHeight()) {
                event.setCancelled(true);
                return;
            }
            if (!plot.hasOwner()) {
                BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
                if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                    return;
                }
                event.setCancelled(true);
                return;
            }
            BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
            if (!plot.isAdded(plotPlayer.getUUID())) {
                List<BlockTypeWrapper> destroy = plot.getFlag(BreakFlag.class);
                Block block = event.getBlock();
                if (destroy
                        .contains(BlockTypeWrapper.get(BukkitAdapter.asBlockType(block.getType())))
                        || plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                    return;
                }
                plot.debug(player.getName() + " could not break " + block.getType()
                        + " because it was not in the break flag");
                event.setCancelled(true);
                return;
            }
            return;
        }
        BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
        if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_ROAD)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        Material blockType = block.getType();
        if (org.bukkit.Tag.ICE.isTagged(blockType)) {
            if (!plot.getFlag(IceMeltFlag.class)) {
                plot.debug("Ice could not melt because ice-melt = false");
                event.setCancelled(true);
            }
            return;
        }
        if (org.bukkit.Tag.SNOW.isTagged(blockType)) {
            if (!plot.getFlag(SnowMeltFlag.class)) {
                plot.debug("Snow could not melt because snow-melt = false");
                event.setCancelled(true);
            }
            return;
        }
        if (blockType == Material.FARMLAND) {
            if (!plot.getFlag(SoilDryFlag.class)) {
                plot.debug("Soil could not dry because soil-dry = false");
                event.setCancelled(true);
            }
            return;
        }
        if (CORAL_BLOCKS.isTagged(blockType) || CORALS.isTagged(blockType) || WALL_CORALS.isTagged(blockType)) {
            if (!plot.getFlag(CoralDryFlag.class)) {
                plot.debug("Coral could not dry because coral-dry = false");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMoistureChange(MoistureChangeEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();

        if (area == null) {
            return;
        }

        Plot plot = area.getOwnedPlot(location);

        if (plot == null) {
            event.setCancelled(true);
            return;
        }

        if (block.getBlockData() instanceof Farmland farmland && event
                .getNewState()
                .getBlockData() instanceof Farmland newFarmland) {
            int currentMoisture = farmland.getMoisture();
            int newMoisture = newFarmland.getMoisture();

            // farmland gets moisturizes
            if (newMoisture > currentMoisture) {
                return;
            }

            if (plot.getFlag(SoilDryFlag.class)) {
                return;
            }

            plot.debug("Soil could not dry because soil-dry = false");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChange(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();

        // Check liquid flow flag inside of origin plot too
        final Location fromLocation = BukkitUtil.adapt(fromBlock.getLocation());
        final PlotArea fromArea = fromLocation.getPlotArea();
        if (fromArea != null) {
            final Plot fromPlot = fromArea.getOwnedPlot(fromLocation);
            if (fromPlot != null && fromPlot.getFlag(LiquidFlowFlag.class) == LiquidFlowFlag.FlowStatus.DISABLED && event
                    .getBlock()
                    .isLiquid()) {
                fromPlot.debug("Liquid could not flow because liquid-flow = disabled");
                event.setCancelled(true);
                return;
            }
        }

        Block toBlock = event.getToBlock();
        Location toLocation = BukkitUtil.adapt(toBlock.getLocation());
        PlotArea toArea = toLocation.getPlotArea();
        if (toArea == null) {
            if (fromBlock.getType() == Material.DRAGON_EGG && fromArea != null) {
                event.setCancelled(true);
            }
            return;
        }
        if (!toArea.buildRangeContainsY(toLocation.getY())) {
            event.setCancelled(true);
            return;
        }
        Plot toPlot = toArea.getOwnedPlot(toLocation);

        if (fromBlock.getType() == Material.DRAGON_EGG && fromArea != null) {
            final Plot fromPlot = fromArea.getOwnedPlot(fromLocation);

            if (fromPlot != null || toPlot != null) {
                if ((fromPlot == null || !fromPlot.equals(toPlot)) && (toPlot == null || !toPlot.equals(fromPlot))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (toPlot != null) {
            if (!toArea.contains(fromLocation.getX(), fromLocation.getZ()) || !Objects.equals(
                    toPlot,
                    toArea.getOwnedPlot(fromLocation)
            )) {
                event.setCancelled(true);
                return;
            }
            if (toPlot.getFlag(LiquidFlowFlag.class) == LiquidFlowFlag.FlowStatus.ENABLED && event.getBlock().isLiquid()) {
                return;
            }
            if (toPlot.getFlag(DisablePhysicsFlag.class)) {
                toPlot.debug(event.getBlock().getType() + " could not update because disable-physics = true");
                event.setCancelled(true);
                return;
            }
            if (toPlot.getFlag(LiquidFlowFlag.class) == LiquidFlowFlag.FlowStatus.DISABLED && event.getBlock().isLiquid()) {
                toPlot.debug("Liquid could not flow because liquid-flow = disabled");
                event.setCancelled(true);
            }
        } else if (!toArea.contains(fromLocation.getX(), fromLocation.getZ()) || !Objects.equals(
                null,
                toArea.getOwnedPlot(fromLocation)
        )) {
            event.setCancelled(true);
        } else if (event.getBlock().isLiquid()) {
            final org.bukkit.Location location = event.getBlock().getLocation();

            /*
                X = block location
                A-H = potential plot locations
               Z
               ^
               |    A B C
               o    D X E
               |    F G H
               v
                <-----O-----> x
             */
            if (BukkitUtil.adapt(location.clone().add(-1, 0, 1)  /* A */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(1, 0, 0)   /* B */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(1, 0, 1)   /* C */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(-1, 0, 0)  /* D */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(1, 0, 0)   /* E */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(-1, 0, -1) /* F */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(0, 0, -1)  /* G */).getPlot() != null
                    || BukkitUtil.adapt(location.clone().add(1, 0, 1)   /* H */).getPlot() != null) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());

        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }

        if (!area.buildRangeContainsY(location.getY())) {
            event.setCancelled(true);
            return;
        }

        Plot plot = location.getOwnedPlot();
        if (plot == null || !plot.getFlag(CropGrowFlag.class)) {
            if (plot != null) {
                plot.debug("Crop grow event was cancelled because crop-grow = false");
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        BlockFace face = event.getDirection();
        Vector relative = new Vector(face.getModX(), face.getModY(), face.getModZ());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                return;
            }
            for (Block block1 : event.getBlocks()) {
                Location bloc = BukkitUtil.adapt(block1.getLocation());
                if (bloc.isPlotArea() || bloc
                        .add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ())
                        .isPlotArea()) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (location.add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ()).isPlotArea()) {
                // Prevent pistons from extending if they are: bordering a plot
                // area, facing inside plot area, and not pushing any blocks
                event.setCancelled(true);
            }
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        for (Block block1 : event.getBlocks()) {
            Location bloc = BukkitUtil.adapt(block1.getLocation());
            Location newLoc = bloc.add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ());
            if (!area.contains(bloc.getX(), bloc.getZ()) || !area.contains(newLoc)) {
                event.setCancelled(true);
                return;
            }
            if (!plot.equals(area.getOwnedPlot(bloc)) || !plot.equals(area.getOwnedPlot(newLoc))) {
                event.setCancelled(true);
                return;
            }
            if (!area.buildRangeContainsY(bloc.getY()) || !area.buildRangeContainsY(newLoc.getY())) {
                event.setCancelled(true);
                return;
            }
        }
        if (!plot.equals(area.getOwnedPlot(location.add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ())))) {
            // This branch is only necessary to prevent pistons from extending
            // if they are: on a plot edge, facing outside the plot, and not
            // pushing any blocks
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        BlockFace face = event.getDirection();
        Vector relative = new Vector(face.getModX(), face.getModY(), face.getModZ());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                return;
            }
            for (Block block1 : event.getBlocks()) {
                Location bloc = BukkitUtil.adapt(block1.getLocation());
                Location newLoc = bloc.add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ());
                if (bloc.isPlotArea() || newLoc.isPlotArea()) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            event.setCancelled(true);
            return;
        }
        for (Block block1 : event.getBlocks()) {
            Location bloc = BukkitUtil.adapt(block1.getLocation());
            Location newLoc = bloc.add(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ());
            if (!area.contains(bloc.getX(), bloc.getZ()) || !area.contains(newLoc)) {
                event.setCancelled(true);
                return;
            }
            if (!plot.equals(area.getOwnedPlot(bloc)) || !plot.equals(area.getOwnedPlot(newLoc))) {
                event.setCancelled(true);
                return;
            }
            if (!area.buildRangeContainsY(bloc.getY()) || !area.buildRangeContainsY(newLoc.getY())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (!this.plotAreaManager.hasPlotArea(event.getBlock().getWorld().getName())) {
            return;
        }
        Material type = event.getItem().getType();
        switch (type.toString()) {
            case "SHULKER_BOX", "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX",
                    "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX",
                    "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX",
                    "RED_SHULKER_BOX", "BLACK_SHULKER_BOX", "CARVED_PUMPKIN", "WITHER_SKELETON_SKULL", "FLINT_AND_STEEL",
                    "BONE_MEAL", "SHEARS", "GLASS_BOTTLE", "GLOWSTONE", "COD_BUCKET", "PUFFERFISH_BUCKET", "SALMON_BUCKET",
                    "TROPICAL_FISH_BUCKET", "AXOLOTL_BUCKET", "BUCKET", "WATER_BUCKET", "LAVA_BUCKET", "TADPOLE_BUCKET" -> {
                if (event.getBlock().getType() == Material.DROPPER) {
                    return;
                }
                BlockFace targetFace = ((Dispenser) event.getBlock().getBlockData()).getFacing();
                Location location = BukkitUtil.adapt(event.getBlock().getRelative(targetFace).getLocation());
                if (location.isPlotRoad()) {
                    event.setCancelled(true);
                    return;
                }
                PlotArea area = location.getPlotArea();
                if (area != null && !area.buildRangeContainsY(location.getY())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!this.plotAreaManager.hasPlotArea(event.getWorld().getName())) {
            return;
        }
        List<org.bukkit.block.BlockState> blocks = event.getBlocks();
        if (blocks.isEmpty()) {
            return;
        }
        Location location = BukkitUtil.adapt(blocks.get(0).getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            for (int i = blocks.size() - 1; i >= 0; i--) {
                location = BukkitUtil.adapt(blocks.get(i).getLocation());
                if (location.isPlotArea()) {
                    blocks.remove(i);
                }
            }
            return;
        } else {
            Plot origin = area.getOwnedPlot(location);
            if (origin == null) {
                event.setCancelled(true);
                return;
            }
            for (int i = blocks.size() - 1; i >= 0; i--) {
                location = BukkitUtil.adapt(blocks.get(i).getLocation());
                if (!area.contains(location.getX(), location.getZ())) {
                    blocks.remove(i);
                    continue;
                }
                Plot plot = area.getOwnedPlot(location);
                if (!Objects.equals(plot, origin)) {
                    event.getBlocks().remove(i);
                    continue;
                }
                if (!area.buildRangeContainsY(location.getY())) {
                    event.getBlocks().remove(i);
                }
            }
        }
        Plot origin = area.getPlot(location);
        if (origin == null) {
            event.setCancelled(true);
            return;
        }
        for (int i = blocks.size() - 1; i >= 0; i--) {
            location = BukkitUtil.adapt(blocks.get(i).getLocation());
            Plot plot = area.getOwnedPlot(location);
            /*
             * plot → the base plot of the merged area
             * origin → the plot where the event gets called
             */

            // Are plot and origin different AND are both plots merged
            if (!Objects.equals(plot, origin) && (!plot.isMerged() && !origin.isMerged())) {
                event.getBlocks().remove(i);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBigBoom(BlockExplodeEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());
        String world = location.getWorldName();
        if (!this.plotAreaManager.hasPlotArea(world)) {
            return;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            Iterator<Block> iterator = event.blockList().iterator();
            while (iterator.hasNext()) {
                location = BukkitUtil.adapt(iterator.next().getLocation());
                if (location.isPlotArea()) {
                    iterator.remove();
                }
            }
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot == null || !plot.getFlag(ExplosionFlag.class)) {
            event.setCancelled(true);
            if (plot != null) {
                plot.debug("Explosion was cancelled because explosion = false");
            }
            return;
        }
        event.blockList().removeIf(blox -> !plot.equals(area.getOwnedPlot(BukkitUtil.adapt(blox.getLocation()))));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());

        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }

        Plot plot = location.getOwnedPlot();
        if (plot == null || !plot.getFlag(BlockBurnFlag.class)) {
            if (plot != null) {
                plot.debug("Block burning was cancelled because block-burn = false");
            }
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        Entity ignitingEntity = event.getIgnitingEntity();
        Block block = event.getBlock();
        BlockIgniteEvent.IgniteCause igniteCause = event.getCause();
        Location location1 = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location1.getPlotArea();
        if (area == null) {
            return;
        }
        if (igniteCause == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            event.setCancelled(true);
            return;
        }

        Plot plot = area.getOwnedPlot(location1);
        if (player != null) {
            BukkitPlayer pp = BukkitUtil.adapt(player);
            if (area.notifyIfOutsideBuildArea(pp, location1.getY())) {
                event.setCancelled(true);
                return;
            }
            if (plot == null) {
                if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, BlockIgnitionFlag.class, true) && !pp.hasPermission(
                        Permission.PERMISSION_ADMIN_BUILD_ROAD
                )) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD)
                            )
                    );
                    event.setCancelled(true);
                }
            } else if (!plot.hasOwner()) {
                if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, BlockIgnitionFlag.class, true) && !pp.hasPermission(
                        Permission.PERMISSION_ADMIN_BUILD_UNOWNED
                )) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                            )
                    );
                    event.setCancelled(true);
                }
            } else if (!plot.isAdded(pp.getUUID())) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                            )
                    );
                    event.setCancelled(true);
                }
            } else if (!plot.getFlag(BlockIgnitionFlag.class)) {
                event.setCancelled(true);
                plot.debug("Block ignition was cancelled because block-ignition = false");
            }
        } else {
            if (plot == null) {
                event.setCancelled(true);
                return;
            }
            if (ignitingEntity != null) {
                if (!plot.getFlag(BlockIgnitionFlag.class)) {
                    event.setCancelled(true);
                    plot.debug("Block ignition was cancelled because block-ignition = false");
                    return;
                }
                if (igniteCause == BlockIgniteEvent.IgniteCause.FIREBALL) {
                    if (ignitingEntity instanceof Fireball) {
                        Projectile fireball = (Projectile) ignitingEntity;
                        Location location = null;
                        if (fireball.getShooter() instanceof Entity shooter) {
                            location = BukkitUtil.adapt(shooter.getLocation());
                        } else if (fireball.getShooter() instanceof BlockProjectileSource) {
                            Block shooter =
                                    ((BlockProjectileSource) fireball.getShooter()).getBlock();
                            location = BukkitUtil.adapt(shooter.getLocation());
                        }
                        if (location != null && !plot.equals(location.getPlot())) {
                            event.setCancelled(true);
                        }
                    }
                }

            } else if (event.getIgnitingBlock() != null) {
                Block ignitingBlock = event.getIgnitingBlock();
                Plot plotIgnited = BukkitUtil.adapt(ignitingBlock.getLocation()).getPlot();
                if (igniteCause == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL && (
                        !plot.getFlag(BlockIgnitionFlag.class) || plotIgnited == null || !plotIgnited
                                .equals(plot)) || (igniteCause == BlockIgniteEvent.IgniteCause.SPREAD
                        || igniteCause == BlockIgniteEvent.IgniteCause.LAVA) && (
                        !plot.getFlag(BlockIgnitionFlag.class) || plotIgnited == null || !plotIgnited
                                .equals(plot))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        Location location = BukkitUtil.adapt(block.getLocation());

        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }

        Plot plot = location.getOwnedPlot();
        if (plot == null || !plot.getFlag(LeafDecayFlag.class)) {
            if (plot != null) {
                plot.debug("Leaf decaying was cancelled because leaf-decay = false");
            }
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        Block sponge = event.getBlock();
        Location location = BukkitUtil.adapt(sponge.getLocation());
        PlotArea area = location.getPlotArea();
        List<org.bukkit.block.BlockState> blocks = event.getBlocks();
        if (area == null) {
            blocks.removeIf(block -> BukkitUtil.adapt(block.getLocation()).isPlotArea());
        } else {
            Plot origin = area.getOwnedPlot(location);
            blocks.removeIf(block -> {
                Location blockLocation = BukkitUtil.adapt(block.getLocation());
                if (!area.contains(blockLocation.getX(), blockLocation.getZ())) {
                    return true;
                }
                Plot plot = area.getOwnedPlot(blockLocation);
                if (!Objects.equals(plot, origin)) {
                    return true;
                }
                return !area.buildRangeContainsY(location.getY());
            });
        }
        if (blocks.isEmpty()) {
            // Cancel event so the sponge block doesn't turn into a wet sponge
            // if no water is being absorbed
            event.setCancelled(true);
        }
    }

    /*
     * BlockMultiPlaceEvent is called unrelated to the BlockPlaceEvent itself and therefore doesn't respect the cancellation.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        // Check if the generic block place event would be cancelled
        blockCreate(event);
        if (event.isCancelled()) {
            return;
        }

        BukkitPlayer pp = BukkitUtil.adapt(event.getPlayer());
        Location placedLocation = BukkitUtil.adapt(event.getBlockReplacedState().getLocation());
        PlotArea area = placedLocation.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = placedLocation.getPlot();

        for (final BlockState state : event.getReplacedBlockStates()) {
            Location currentLocation = BukkitUtil.adapt(state.getLocation());
            if (!pp.hasPermission(
                    Permission.PERMISSION_ADMIN_BUILD_ROAD
            ) && !(Objects.equals(currentLocation.getPlot(), plot))) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD))
                );
                event.setCancelled(true);
                break;
            }
            if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT)) {
                continue;
            }
            if (area.notifyIfOutsideBuildArea(pp, currentLocation.getY())) {
                event.setCancelled(true);
                break;
            }
        }

    }

}
