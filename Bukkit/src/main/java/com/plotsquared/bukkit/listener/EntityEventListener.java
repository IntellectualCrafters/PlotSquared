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
import com.plotsquared.bukkit.BukkitPlatform;
import com.plotsquared.bukkit.player.BukkitPlayer;
import com.plotsquared.bukkit.util.BukkitEntityUtil;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.listener.PlayerBlockEventType;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotHandler;
import com.plotsquared.core.plot.flag.implementations.DisablePhysicsFlag;
import com.plotsquared.core.plot.flag.implementations.EntityChangeBlockFlag;
import com.plotsquared.core.plot.flag.implementations.ExplosionFlag;
import com.plotsquared.core.plot.flag.implementations.InvincibleFlag;
import com.plotsquared.core.plot.flag.implementations.ProjectileChangeBlockFlag;
import com.plotsquared.core.plot.flag.implementations.WeavingDeathPlace;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.PlotFlagUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unused")
public class EntityEventListener implements Listener {

    private final BukkitPlatform platform;
    private final PlotAreaManager plotAreaManager;
    private final EventDispatcher eventDispatcher;
    private float lastRadius;

    @Inject
    public EntityEventListener(
            final @NonNull BukkitPlatform platform,
            final @NonNull PlotAreaManager plotAreaManager,
            final @NonNull EventDispatcher eventDispatcher
    ) {
        this.platform = platform;
        this.plotAreaManager = plotAreaManager;
        this.eventDispatcher = eventDispatcher;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        onEntityDamageByEntityCommon(event.getCombuster(), event.getEntity(), EntityDamageEvent.DamageCause.FIRE_TICK, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        onEntityDamageByEntityCommon(event.getDamager(), event.getEntity(), event.getCause(), event);
    }

    private void onEntityDamageByEntityCommon(
            final Entity damager,
            final Entity victim,
            final EntityDamageEvent.DamageCause cause,
            final Cancellable event
    ) {
        Location location = BukkitUtil.adapt(damager.getLocation());
        if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
            return;
        }
        if (!BukkitEntityUtil.entityDamage(damager, victim, cause)) {
            if (event.isCancelled()) {
                if (victim instanceof Ageable ageable) {
                    if (ageable.getAge() == -24000) {
                        ageable.setAge(0);
                        ageable.setAdult();
                    }
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void creatureSpawnEvent(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        Location location = BukkitUtil.adapt(entity.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        // Armour-stands are handled elsewhere and should not be handled by area-wide entity-spawn options
        if (entity.getType() == EntityType.ARMOR_STAND) {
            return;
        }
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        switch (reason.toString()) {
            case "DISPENSE_EGG", "EGG", "OCELOT_BABY", "SPAWNER_EGG" -> {
                if (!area.isSpawnEggs()) {
                    event.setCancelled(true);
                    return;
                }
            }
            case "REINFORCEMENTS", "NATURAL", "MOUNT", "PATROL", "RAID", "SHEARED", "SILVERFISH_BLOCK", "ENDER_PEARL",
                    "TRAP", "VILLAGE_DEFENSE", "VILLAGE_INVASION", "BEEHIVE", "CHUNK_GEN", "NETHER_PORTAL",
                    "FROZEN", "SPELL", "DEFAULT" -> {
                if (!area.isMobSpawning()) {
                    event.setCancelled(true);
                    return;
                }
            }
            case "BREEDING", "DUPLICATION" -> {
                if (!area.isSpawnBreeding()) {
                    event.setCancelled(true);
                    return;
                }
            }
            case "CUSTOM" -> {
                if (!area.isSpawnCustom()) {
                    event.setCancelled(true);
                    return;
                }
                // No need to clutter metadata if running paper
                if (!PaperLib.isPaper()) {
                    entity.setMetadata("ps_custom_spawned", new FixedMetadataValue(this.platform, true));
                }
                return; // Don't cancel if mob spawning is disabled
            }
            case "BUILD_IRONGOLEM", "BUILD_SNOWMAN", "BUILD_WITHER" -> {
                if (!area.isSpawnCustom()) {
                    event.setCancelled(true);
                    return;
                }
            }
            case "SPAWNER" -> {
                if (!area.isMobSpawnerSpawning()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        Plot plot = area.getOwnedPlotAbs(location);
        if (plot == null) {
            if (!area.isMobSpawning()) {
                event.setCancelled(true);
            }
            return;
        }
        if (BukkitEntityUtil.checkEntity(entity, plot.getBasePlot(false))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityFall(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK) {
            return;
        }
        Block block = event.getBlock();
        World world = block.getWorld();
        String worldName = world.getName();
        if (!this.plotAreaManager.hasPlotArea(worldName)) {
            return;
        }
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlotAbs(location);
        if (plot == null || plot.getFlag(DisablePhysicsFlag.class)) {
            event.setCancelled(true);
            if (plot != null) {
                if (block.getType().hasGravity()) {
                    BlockEventListener.sendBlockChange(block.getLocation(), block.getBlockData());
                }
                plot.debug("Falling block event was cancelled because disable-physics = true");
            }
            return;
        }
        if (event.getTo().hasGravity()) {
            Entity entity = event.getEntity();
            List<MetadataValue> meta = entity.getMetadata("plot");
            if (meta.isEmpty()) {
                return;
            }
            Plot origin = (Plot) meta.get(0).value();
            if (origin != null && !origin.equals(plot)) {
                event.setCancelled(true);
                entity.remove();
            }
        } else if (event.getTo() == Material.AIR) {
            event.getEntity().setMetadata("plot", new FixedMetadataValue((Plugin) PlotSquared.platform(), plot));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWeavingEffect(EntityChangeBlockEvent event) {
        if (event.getTo() != Material.COBWEB) {
            return;
        }
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, WeavingDeathPlace.class, false)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!plot.getFlag(WeavingDeathPlace.class)) {
            plot.debug(event.getTo() + " could not spawn because weaving-death-place = false");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }
        Location location = BukkitUtil.adapt(event.getEntity().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, InvincibleFlag.class, true)) {
                event.setCancelled(true);
            }
            return;
        }
        if (plot.getFlag(InvincibleFlag.class)) {
            plot.debug(event.getEntity().getName() + " could not take damage because invincible = true");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBigBoom(EntityExplodeEvent event) {
        Location location = BukkitUtil.adapt(event.getLocation());
        PlotArea area = location.getPlotArea();
        boolean plotArea = location.isPlotArea();
        if (!plotArea) {
            if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
                return;
            }
            return;
        }
        Plot plot = area.getOwnedPlot(location);
        if (plot != null) {
            if (plot.getFlag(ExplosionFlag.class)) {
                List<MetadataValue> meta = event.getEntity().getMetadata("plot");
                Plot origin;
                if (meta.isEmpty()) {
                    origin = plot;
                } else {
                    origin = (Plot) meta.get(0).value();
                }
                if (this.lastRadius != 0) {
                    List<Entity> nearby = event.getEntity().getNearbyEntities(this.lastRadius, this.lastRadius, this.lastRadius);
                    for (Entity near : nearby) {
                        if (near instanceof TNTPrimed || near.getType().equals(EntityType.TNT_MINECART)) {
                            if (!near.hasMetadata("plot")) {
                                near.setMetadata("plot", new FixedMetadataValue((Plugin) PlotSquared.platform(), plot));
                            }
                        }
                    }
                    this.lastRadius = 0;
                }
                Iterator<Block> iterator = event.blockList().iterator();
                while (iterator.hasNext()) {
                    Block block = iterator.next();
                    location = BukkitUtil.adapt(block.getLocation());
                    if (!area.contains(location.getX(), location.getZ()) || !origin.equals(area.getOwnedPlot(location))) {
                        iterator.remove();
                    }
                }
                return;
            } else {
                plot.debug("Explosion was cancelled because explosion = false");
            }
        }
        event.setCancelled(true);
        //Spawn Explosion Particles when enabled in settings
        if (Settings.General.ALWAYS_SHOW_EXPLOSIONS) {
            event.getLocation().getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, event.getLocation(), 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPeskyMobsChangeTheWorldLikeWTFEvent(EntityChangeBlockEvent event) {
        Entity e = event.getEntity();
        Material type = event.getBlock().getType();
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        if (e instanceof FallingBlock) {
            // allow falling blocks converting to blocks and vice versa
            return;
        } else if (e instanceof Boat) {
            // allow boats destroying lily pads
            if (type == Material.LILY_PAD) {
                return;
            }
        } else if (e instanceof Player player) {
            BukkitPlayer pp = BukkitUtil.adapt(player);
            if (type.toString().equals("POWDER_SNOW")) {
                // Burning player evaporating powder snow. Use same checks as
                // trampling farmland
                BlockType blockType = BukkitAdapter.asBlockType(type);
                if (!this.eventDispatcher.checkPlayerBlockEvent(pp,
                        PlayerBlockEventType.TRIGGER_PHYSICAL, location, blockType, true
                )) {
                    event.setCancelled(true);
                }
                return;
            } else {
                // already handled by other flags (mainly the 'use' flag):
                // - player tilting big dripleaf by standing on it
                // - player picking glow berries from cave vine
                // - player trampling farmland
                // - player standing on or clicking redstone ore
                return;
            }
        } else if (e instanceof Projectile entity) {
            // Exact same as the ProjectileHitEvent listener, except that we let
            // the entity-change-block determine what to do with shooters that
            // aren't players and aren't blocks
            Plot plot = area.getPlot(location);
            ProjectileSource shooter = entity.getShooter();
            if (shooter instanceof Player) {
                PlotPlayer<?> pp = BukkitUtil.adapt((Player) shooter);
                if (plot == null) {
                    if (area.isRoadFlags() && !area.getRoadFlag(ProjectileChangeBlockFlag.class) && !pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED)) {
                        entity.remove();
                        event.setCancelled(true);
                    }
                    return;
                }
                if (plot.isAdded(pp.getUUID()) || plot.getFlag(ProjectileChangeBlockFlag.class) || pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER)) {
                    return;
                }
                entity.remove();
                event.setCancelled(true);
                return;
            }
            if (!(shooter instanceof Entity) && shooter != null) {
                if (plot == null) {
                    entity.remove();
                    event.setCancelled(true);
                    return;
                }
                Location sLoc =
                        BukkitUtil.adapt(((BlockProjectileSource) shooter).getBlock().getLocation());
                if (!area.contains(sLoc.getX(), sLoc.getZ())) {
                    entity.remove();
                    event.setCancelled(true);
                    return;
                }
                Plot sPlot = area.getOwnedPlotAbs(sLoc);
                if (sPlot == null || !PlotHandler.sameOwners(plot, sPlot)) {
                    entity.remove();
                    event.setCancelled(true);
                }
                return;
            }
            // fall back to entity-change-block flag
        }

        Plot plot = area.getOwnedPlot(location);
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, EntityChangeBlockFlag.class, false)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!plot.getFlag(EntityChangeBlockFlag.class)) {
            plot.debug(e.getType() + " could not change block because entity-change-block = false");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrime(ExplosionPrimeEvent event) {
        this.lastRadius = event.getRadius() + 1;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        Vehicle entity = event.getVehicle();
        Location location = BukkitUtil.adapt(entity.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getOwnedPlotAbs(location);
        if (plot == null || BukkitEntityUtil.checkEntity(entity, plot)) {
            entity.remove();
            return;
        }
        if (Settings.Enabled_Components.KILL_ROAD_VEHICLES) {
            entity.setMetadata("plot", new FixedMetadataValue((Plugin) PlotSquared.platform(), plot));
        }
    }

}
