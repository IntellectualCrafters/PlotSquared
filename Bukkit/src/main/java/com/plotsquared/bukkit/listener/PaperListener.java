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

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import com.destroystokyo.paper.event.entity.SlimePathfindEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.google.inject.Inject;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.command.Command;
import com.plotsquared.core.command.MainCommand;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.FlagContainer;
import com.plotsquared.core.plot.flag.implementations.BeaconEffectsFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.FishingFlag;
import com.plotsquared.core.plot.flag.implementations.ProjectilesFlag;
import com.plotsquared.core.plot.flag.implementations.TileDropFlag;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.PlotFlagUtil;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Events specific to Paper. Some toit nups here
 */
@SuppressWarnings("unused")
public class PaperListener implements Listener {

    private final PlotAreaManager plotAreaManager;
    private Chunk lastChunk;

    @Inject
    public PaperListener(final @NonNull PlotAreaManager plotAreaManager) {
        this.plotAreaManager = plotAreaManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockDestroy(final BlockDestroyEvent event) {
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        if (plot != null) {
            event.setWillDrop(plot.getFlag(TileDropFlag.class));
        }
    }

    @EventHandler
    public void onEntityPathfind(EntityPathfindEvent event) {
        if (!Settings.Paper_Components.ENTITY_PATHING) {
            return;
        }
        handleEntityMovement(event, event.getEntity().getLocation(), event.getLoc());
    }

    @EventHandler
    public void onEntityPathfind(SlimePathfindEvent event) {
        if (!Settings.Paper_Components.ENTITY_PATHING) {
            return;
        }
        Slime slime = event.getEntity();

        Block b = slime.getTargetBlockExact(4);
        if (b == null) {
            return;
        }

        handleEntityMovement(event, event.getEntity().getLocation(),  b.getLocation());
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (!Settings.Paper_Components.ENTITY_MOVEMENT) {
            return;
        }
        if (!event.hasExplicitlyChangedBlock()) {
            return;
        }
        handleEntityMovement(event, event.getFrom(), event.getTo());
    }

    private static void handleEntityMovement(Cancellable event, org.bukkit.Location from, org.bukkit.Location target) {
        Location toLoc = BukkitUtil.adapt(target);
        Location fromLoc = BukkitUtil.adapt(from);
        PlotArea tarea = toLoc.getPlotArea();
        if (tarea == null) {
            return;
        }
        PlotArea farea = fromLoc.getPlotArea();
        if (farea == null) {
            return;
        }
        if (tarea != farea) {
            event.setCancelled(true);
            return;
        }
        Plot tplot = toLoc.getPlot();
        Plot fplot = fromLoc.getPlot();
        if (tplot == null ^ fplot == null) {
            event.setCancelled(true);
            return;
        }
        if (tplot == null || tplot.getId().equals(fplot.getId())) {
            return;
        }
        if (fplot.isMerged() && fplot.getConnectedPlots().contains(tplot)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPreCreatureSpawnEvent(PreCreatureSpawnEvent event) {
        if (!Settings.Paper_Components.CREATURE_SPAWN) {
            return;
        }
        Location location = BukkitUtil.adapt(event.getSpawnLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        // Armour-stands are handled elsewhere and should not be handled by area-wide entity-spawn options
        if (event.getType() == EntityType.ARMOR_STAND) {
            return;
        }
        // If entities are spawning... the chunk should be loaded?
        Entity[] entities = event.getSpawnLocation().getChunk().getEntities();
        if (entities.length >= Settings.Chunk_Processor.MAX_ENTITIES) {
            event.setShouldAbortSpawn(true);
            event.setCancelled(true);
            return;
        }
        CreatureSpawnEvent.SpawnReason reason = event.getReason();
        switch (reason.toString()) {
            case "DISPENSE_EGG", "EGG", "OCELOT_BABY", "SPAWNER_EGG" -> {
                if (!area.isSpawnEggs()) {
                    event.setShouldAbortSpawn(true);
                    event.setCancelled(true);
                    return;
                }
            }
            case "REINFORCEMENTS", "NATURAL", "MOUNT", "PATROL", "RAID", "SHEARED", "SILVERFISH_BLOCK", "ENDER_PEARL", "TRAP", "VILLAGE_DEFENSE", "VILLAGE_INVASION", "BEEHIVE", "CHUNK_GEN" -> {
                if (!area.isMobSpawning()) {
                    event.setShouldAbortSpawn(true);
                    event.setCancelled(true);
                    return;
                }
            }
            case "BREEDING" -> {
                if (!area.isSpawnBreeding()) {
                    event.setShouldAbortSpawn(true);
                    event.setCancelled(true);
                    return;
                }
            }
            case "BUILD_IRONGOLEM", "BUILD_SNOWMAN", "BUILD_WITHER", "CUSTOM" -> {
                if (!area.isSpawnCustom()) {
                    event.setShouldAbortSpawn(true);
                    event.setCancelled(true);
                    return;
                }
            }
            case "SPAWNER" -> {
                if (!area.isMobSpawnerSpawning()) {
                    event.setShouldAbortSpawn(true);
                    event.setCancelled(true);
                    return;
                }
            }
        }
        Plot plot = location.getOwnedPlotAbs();
        if (plot == null) {
            EntityType type = event.getType();
            // PreCreatureSpawnEvent **should** not be called for DROPPED_ITEM, just for the sake of consistency
            if (type == EntityType.ITEM) {
                if (Settings.Enabled_Components.KILL_ROAD_ITEMS) {
                    event.setCancelled(true);
                }
                return;
            }
            if (!area.isMobSpawning()) {
                if (type == EntityType.PLAYER) {
                    return;
                }
                if (type.isAlive()) {
                    event.setShouldAbortSpawn(true);
                    event.setCancelled(true);
                }
            }
            if (!area.isMiscSpawnUnowned() && !type.isAlive()) {
                event.setShouldAbortSpawn(true);
                event.setCancelled(true);
            }
            return;
        }
        if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
            event.setShouldAbortSpawn(true);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerNaturallySpawnCreaturesEvent(PlayerNaturallySpawnCreaturesEvent event) {
        if (Settings.Paper_Components.CANCEL_CHUNK_SPAWN) {
            Location location = BukkitUtil.adapt(event.getPlayer().getLocation());
            PlotArea area = location.getPlotArea();
            if (area != null && !area.isMobSpawning()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPreSpawnerSpawnEvent(PreSpawnerSpawnEvent event) {
        if (Settings.Paper_Components.SPAWNER_SPAWN) {
            Location location = BukkitUtil.adapt(event.getSpawnerLocation());
            PlotArea area = location.getPlotArea();
            if (area != null && !area.isMobSpawnerSpawning()) {
                event.setCancelled(true);
                event.setShouldAbortSpawn(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Settings.Paper_Components.TILE_ENTITY_CHECK || !Settings.Enabled_Components.CHUNK_PROCESSOR) {
            return;
        }
        if (!(event.getBlock().getState(false) instanceof TileState)) {
            return;
        }
        final Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        final PlotArea plotArea = location.getPlotArea();
        if (plotArea == null) {
            return;
        }
        final int tileEntityCount = event.getBlock().getChunk().getTileEntities(false).length;
        if (tileEntityCount >= Settings.Chunk_Processor.MAX_TILES) {
            final PlotPlayer<?> plotPlayer = BukkitUtil.adapt(event.getPlayer());
            plotPlayer.sendMessage(
                    TranslatableCaption.of("errors.tile_entity_cap_reached"),
                    TagResolver.resolver("amount", Tag.inserting(Component.text(Settings.Chunk_Processor.MAX_TILES)))
            );
            event.setCancelled(true);
            event.setBuild(false);
        }
    }

    /**
     * Unsure if this will be any performance improvement over the spigot version,
     * but here it is anyway :)
     *
     * @param event Paper's PlayerLaunchProjectileEvent
     */
    @EventHandler
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        if (!Settings.Paper_Components.PLAYER_PROJECTILE) {
            return;
        }
        Projectile entity = event.getProjectile();
        ProjectileSource shooter = entity.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }
        Location location = BukkitUtil.adapt(entity.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        PlotPlayer<Player> pp = BukkitUtil.adapt((Player) shooter);
        Plot plot = location.getOwnedPlot();

        if (plot == null) {
            if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, ProjectilesFlag.class, true) && !pp.hasPermission(
                    Permission.PERMISSION_ADMIN_PROJECTILE_ROAD
            )) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_PROJECTILE_ROAD)
                        )
                );
                entity.remove();
                event.setCancelled(true);
            }
        } else if (!plot.hasOwner()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED)
                        )
                );
                entity.remove();
                event.setCancelled(true);
            }
        } else if (!plot.isAdded(pp.getUUID())) {
            if (entity.getType().equals(EntityType.FISHING_BOBBER)) {
                if (plot.getFlag(FishingFlag.class)) {
                    return;
                }
            }
            if (!plot.getFlag(ProjectilesFlag.class)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER)
                            )
                    );
                    entity.remove();
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onAsyncTabCompletion(final AsyncTabCompleteEvent event) {
        if (!Settings.Paper_Components.ASYNC_TAB_COMPLETION) {
            return;
        }
        String buffer = event.getBuffer();
        if (!(event.getSender() instanceof Player)) {
            return;
        }
        if ((!event.isCommand() && !buffer.startsWith("/")) || buffer.indexOf(' ') == -1) {
            return;
        }
        if (buffer.startsWith("/")) {
            buffer = buffer.substring(1);
        }
        final String[] unprocessedArgs = buffer.split(Pattern.quote(" "));
        if (unprocessedArgs.length == 1) {
            return; // We don't do anything in this case
        } else if (!Settings.Enabled_Components.TAB_COMPLETED_ALIASES
                .contains(unprocessedArgs[0].toLowerCase(Locale.ENGLISH))) {
            return;
        }
        final String[] args = new String[unprocessedArgs.length - 1];
        System.arraycopy(unprocessedArgs, 1, args, 0, args.length);
        try {
            final PlotPlayer<?> player = BukkitUtil.adapt((Player) event.getSender());
            final Collection<Command> objects = MainCommand.getInstance().tab(player, args, buffer.endsWith(" "));
            if (objects == null) {
                return;
            }
            final List<String> result = new ArrayList<>();
            for (final com.plotsquared.core.command.Command o : objects) {
                result.add(o.toString());
            }
            event.setCompletions(result);
            event.setHandled(true);
        } catch (final Exception ignored) {
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBeaconEffect(final BeaconEffectEvent event) {
        Block block = event.getBlock();
        Location beaconLocation = BukkitUtil.adapt(block.getLocation());
        Plot beaconPlot = beaconLocation.getPlot();

        PlotArea area = beaconLocation.getPlotArea();
        if (area == null) {
            return;
        }

        Player player = event.getPlayer();
        Location playerLocation = BukkitUtil.adapt(player.getLocation());

        PlotPlayer<Player> plotPlayer = BukkitUtil.adapt(player);
        Plot playerStandingPlot = playerLocation.getPlot();
        if (playerStandingPlot == null) {
            FlagContainer container = area.getRoadFlagContainer();
            if (!getBooleanFlagValue(container, BeaconEffectsFlag.class, true) ||
                    (beaconPlot != null && Settings.Enabled_Components.DISABLE_BEACON_EFFECT_OVERFLOW)) {
                event.setCancelled(true);
            }
            return;
        }

        FlagContainer container = playerStandingPlot.getFlagContainer();
        boolean plotBeaconEffects = getBooleanFlagValue(container, BeaconEffectsFlag.class, true);
        if (playerStandingPlot.equals(beaconPlot)) {
            if (!plotBeaconEffects) {
                event.setCancelled(true);
            }
            return;
        }

        if (!plotBeaconEffects || Settings.Enabled_Components.DISABLE_BEACON_EFFECT_OVERFLOW) {
            event.setCancelled(true);
        }
    }

    private boolean getBooleanFlagValue(
            @NonNull FlagContainer container,
            @NonNull Class<? extends BooleanFlag<?>> flagClass,
            boolean defaultValue
    ) {
        BooleanFlag<?> flag = container.getFlag(flagClass);
        return flag == null ? defaultValue : flag.getValue();
    }

}
