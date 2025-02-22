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
import com.plotsquared.bukkit.util.BukkitEntityUtil;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotHandler;
import com.plotsquared.core.plot.flag.implementations.FishingFlag;
import com.plotsquared.core.plot.flag.implementations.ProjectilesFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.PlotFlagUtil;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("unused")
public class ProjectileEventListener implements Listener {

    private final PlotAreaManager plotAreaManager;

    @Inject
    public ProjectileEventListener(final @NonNull PlotAreaManager plotAreaManager) {
        this.plotAreaManager = plotAreaManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        // Cancelling projectile hit events still results in area effect clouds.
        // We need to cancel the splash events to get rid of those.
        onProjectileHit(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion damager = event.getPotion();
        Location location = BukkitUtil.adapt(damager.getLocation());
        if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
            return;
        }
        int count = 0;
        for (LivingEntity victim : event.getAffectedEntities()) {
            if (!BukkitEntityUtil.entityDamage(damager, victim)) {
                event.setIntensity(victim, 0);
                count++;
            }
        }
        if (count > 0 && count == event.getAffectedEntities().size()) {
            event.setCancelled(true);
        } else {
            // Cancelling projectile hit events still results in potions
            // splashing in the world. We need to cancel the splash events to
            // avoid that.
            onProjectileHit(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile entity = event.getEntity();
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
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile entity = event.getEntity();
        Location location = BukkitUtil.adapt(entity.getLocation());
        if (!this.plotAreaManager.hasPlotArea(location.getWorldName())) {
            return;
        }
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        ProjectileSource shooter = entity.getShooter();
        if (shooter instanceof Player) {
            if (!((Player) shooter).isOnline()) {
                if (plot != null) {
                    if (plot.isAdded(((Player) shooter).getUniqueId()) || plot.getFlag(ProjectilesFlag.class)) {
                        return;
                    }
                } else if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, ProjectilesFlag.class, true)) {
                    return;
                }

                entity.remove();
                event.setCancelled(true);
                return;
            }

            PlotPlayer<?> pp = BukkitUtil.adapt((Player) shooter);
            if (plot == null) {
                if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, ProjectilesFlag.class, true) && !pp.hasPermission(
                        Permission.PERMISSION_ADMIN_PROJECTILE_UNOWNED
                )) {
                    entity.remove();
                    event.setCancelled(true);
                }
                return;
            }
            if (plot.isAdded(pp.getUUID()) || pp.hasPermission(Permission.PERMISSION_ADMIN_PROJECTILE_OTHER) || plot.getFlag(
                    ProjectilesFlag.class) || (entity.getType().equals(EntityType.FISHING_BOBBER) && plot.getFlag(
                    FishingFlag.class))) {
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
        }
    }

}
