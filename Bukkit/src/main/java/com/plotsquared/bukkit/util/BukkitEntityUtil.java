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

import com.plotsquared.bukkit.player.BukkitPlayer;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.AnimalAttackFlag;
import com.plotsquared.core.plot.flag.implementations.AnimalCapFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.EntityCapFlag;
import com.plotsquared.core.plot.flag.implementations.HangingBreakFlag;
import com.plotsquared.core.plot.flag.implementations.HostileAttackFlag;
import com.plotsquared.core.plot.flag.implementations.HostileCapFlag;
import com.plotsquared.core.plot.flag.implementations.MiscBreakFlag;
import com.plotsquared.core.plot.flag.implementations.MiscCapFlag;
import com.plotsquared.core.plot.flag.implementations.MobCapFlag;
import com.plotsquared.core.plot.flag.implementations.PveFlag;
import com.plotsquared.core.plot.flag.implementations.PvpFlag;
import com.plotsquared.core.plot.flag.implementations.TamedAttackFlag;
import com.plotsquared.core.plot.flag.implementations.VehicleCapFlag;
import com.plotsquared.core.util.EntityUtil;
import com.plotsquared.core.util.entity.EntityCategories;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;

public class BukkitEntityUtil {

    public static final com.sk89q.worldedit.world.entity.EntityType FAKE_ENTITY_TYPE =
            new com.sk89q.worldedit.world.entity.EntityType("plotsquared:fake");

    public static boolean entityDamage(Entity damager, Entity victim) {
        return entityDamage(damager, victim, null);
    }

    public static boolean entityDamage(Entity damager, Entity victim, EntityDamageEvent.DamageCause cause) {
        Location dloc = BukkitUtil.adapt(damager.getLocation());
        Location vloc = BukkitUtil.adapt(victim.getLocation());
        PlotArea dArea = dloc.getPlotArea();
        PlotArea vArea;
        if (dArea != null && dArea.contains(vloc.getX(), vloc.getZ())) {
            vArea = dArea;
        } else {
            vArea = vloc.getPlotArea();
        }
        if (dArea == null && vArea == null) {
            return true;
        }

        Plot dplot;
        if (dArea != null) {
            dplot = dArea.getPlot(dloc);
        } else {
            dplot = null;
        }
        Plot vplot;
        if (vArea != null) {
            vplot = vArea.getPlot(vloc);
        } else {
            vplot = null;
        }

        Plot plot;
        String stub;
        boolean isPlot = true;
        if (dplot == null && vplot == null) {
            if (dArea == null) {
                return true;
            }
            plot = null;
            stub = "road";
            isPlot = false;
        } else {
            // Prioritize plots for close to seamless pvp zones
            if (victim.getTicksLived() > damager.getTicksLived()) {
                if (dplot == null || !(victim instanceof Player)) {
                    if (vplot == null) {
                        plot = dplot;
                    } else {
                        plot = vplot;
                    }
                } else {
                    plot = dplot;
                }
            } else if (dplot == null || !(victim instanceof Player)) {
                if (vplot == null) {
                    plot = dplot;
                } else {
                    plot = vplot;
                }
            } else if (vplot == null) {
                plot = dplot;
            } else {
                plot = vplot;
            }
            if (plot.hasOwner()) {
                stub = "other";
            } else {
                stub = "unowned";
            }
        }
        boolean roadFlags = vArea != null ? vArea.isRoadFlags() : dArea.isRoadFlags();
        PlotArea area = vArea != null ? vArea : dArea;

        Player player;
        if (damager instanceof Player) { // attacker is player
            player = (Player) damager;
        } else if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) { // shooter is player
                player = (Player) shooter;
            } else { // shooter is not player
                if (shooter instanceof BlockProjectileSource) {
                    Location sLoc = BukkitUtil
                            .adapt(((BlockProjectileSource) shooter).getBlock().getLocation());
                    dplot = dArea.getPlot(sLoc);
                }
                player = null;
            }
        } else { // Attacker is not player
            player = null;
        }
        if (player != null) {
            BukkitPlayer plotPlayer = BukkitUtil.adapt(player);

            final com.sk89q.worldedit.world.entity.EntityType entityType;

            // Create a fake entity type if the type does not have a name
            if (victim.getType().getName() == null) {
                entityType = FAKE_ENTITY_TYPE;
            } else {
                entityType = BukkitAdapter.adapt(victim.getType());
            }

            if (EntityCategories.HANGING.contains(entityType)) { // hanging
                if (plot != null && (plot.getFlag(HangingBreakFlag.class) || plot
                        .isAdded(plotPlayer.getUUID()))) {
                    if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                        if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                            plotPlayer.sendMessage(
                                    TranslatableCaption.of("done.building_restricted")
                            );
                            return false;
                        }
                    }
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_DESTROY + "." + stub))
                            )
                    );
                    return false;
                }
            } else if (victim.getType() == EntityType.ARMOR_STAND) {
                if (plot != null && (plot.getFlag(MiscBreakFlag.class) || plot
                        .isAdded(plotPlayer.getUUID()))) {
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_DESTROY + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_DESTROY + "." + stub))
                            )
                    );
                    if (plot != null) {
                        plot.debug(player.getName()
                                + " could not break armor stand because misc-break = false");
                    }
                    return false;
                }
            } else if (EntityCategories.HOSTILE.contains(entityType)) {
                if (isPlot) {
                    if (plot.getFlag(HostileAttackFlag.class) || plot.getFlag(PveFlag.class) || plot
                            .isAdded(plotPlayer.getUUID())) {
                        return true;
                    }
                } else if (roadFlags && (area.getRoadFlag(HostileAttackFlag.class) || area
                        .getFlag(PveFlag.class))) {
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_PVE + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_PVE + "." + stub))
                            )
                    );
                    if (plot != null) {
                        plot.debug(player.getName() + " could not attack " + entityType
                                + " because pve = false OR hostile-attack = false");
                    }
                    return false;
                }
            } else if (EntityCategories.TAMEABLE.contains(entityType)) { // victim is tameable
                if (isPlot) {
                    if (plot.getFlag(TamedAttackFlag.class) || plot.getFlag(PveFlag.class) || plot
                            .isAdded(plotPlayer.getUUID())) {
                        return true;
                    }
                } else if (roadFlags && (area.getRoadFlag(TamedAttackFlag.class) || area
                        .getFlag(PveFlag.class))) {
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_PVE + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_PVE + "." + stub))
                            )
                    );
                    if (plot != null) {
                        plot.debug(player.getName() + " could not attack " + entityType
                                + " because pve = false OR tamed-attack = false");
                    }
                    return false;
                }
            } else if (EntityCategories.PLAYER.contains(entityType)) {
                if (isPlot) {
                    if (!plot.getFlag(PvpFlag.class) && !plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_PVP + "." + stub)) {
                        plotPlayer.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_PVP + "." + stub))
                                )
                        );
                        plot.debug(player.getName() + " could not attack " + entityType
                                + " because pve = false");
                        return false;
                    } else {
                        return true;
                    }
                } else if (roadFlags && area.getRoadFlag(PvpFlag.class)) {
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_PVP + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_PVP + "." + stub))
                            )
                    );
                    return false;
                }
            } else if (EntityCategories.ANIMAL.contains(entityType)) { // victim is animal
                if (isPlot) {
                    if (plot.getFlag(AnimalAttackFlag.class) || plot.getFlag(PveFlag.class) || plot
                            .isAdded(plotPlayer.getUUID())) {
                        return true;
                    }
                } else if (roadFlags && (area.getRoadFlag(AnimalAttackFlag.class) || area
                        .getFlag(PveFlag.class))) {
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_PVE + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_PVE + "." + stub))
                            )
                    );
                    if (plot != null) {
                        plot.debug(player.getName() + " could not attack " + entityType
                                + " because pve = false OR animal-attack = false");
                    }
                    return false;
                }
            } else if (EntityCategories.VEHICLE
                    .contains(entityType)) { // Vehicles are managed in vehicle destroy event
                return true;
            } else { // victim is something else
                if (isPlot) {
                    if (plot.getFlag(PveFlag.class) || plot.isAdded(plotPlayer.getUUID())) {
                        return true;
                    }
                } else if (roadFlags && area.getRoadFlag(PveFlag.class)) {
                    return true;
                }
                if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_PVE + "." + stub)) {
                    plotPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Component.text(Permission.PERMISSION_ADMIN_PVE + "." + stub))
                            )
                    );
                    if (plot != null) {
                        plot.debug(player.getName() + " could not attack " + entityType
                                + " because pve = false");
                    }
                    return false;
                }
            }
            return true;
        } else if (dplot != null && (!dplot.equals(vplot) || Objects
                .equals(dplot.getOwnerAbs(), vplot.getOwnerAbs()))) {
            return vplot != null && vplot.getFlag(PveFlag.class);
        }
        //disable the firework damage. too much of a headache to support at the moment.
        if (vplot != null) {
            if (EntityDamageEvent.DamageCause.ENTITY_EXPLOSION == cause
                    && damager.getType() == EntityType.FIREWORK_ROCKET) {
                return false;
            }
        }
        if (vplot == null && roadFlags && area.getRoadFlag(PveFlag.class)) {
            return true;
        }
        return ((vplot != null && vplot.getFlag(PveFlag.class)) || !(damager instanceof Arrow
                && !(victim instanceof Creature)));
    }

    public static boolean checkEntity(Entity entity, Plot plot) {
        if (plot == null || !plot.hasOwner() || plot.getFlags().isEmpty() && plot.getArea()
                .getFlagContainer().getFlagMap().isEmpty()) {
            return false;
        }

        final com.sk89q.worldedit.world.entity.EntityType entityType =
                BukkitAdapter.adapt(entity.getType());

        if (EntityCategories.PLAYER.contains(entityType)) {
            return false;
        }

        if (EntityCategories.PROJECTILE.contains(entityType) || EntityCategories.OTHER
                .contains(entityType) || EntityCategories.HANGING.contains(entityType)) {
            return EntityUtil.checkEntity(plot, EntityCapFlag.ENTITY_CAP_UNLIMITED,
                    MiscCapFlag.MISC_CAP_UNLIMITED
            );
        }

        // Has to go go before vehicle as horses are both
        // animals and vehicles
        if (EntityCategories.ANIMAL.contains(entityType) || EntityCategories.VILLAGER
                .contains(entityType) || EntityCategories.TAMEABLE.contains(entityType)) {
            return EntityUtil
                    .checkEntity(plot, EntityCapFlag.ENTITY_CAP_UNLIMITED, MobCapFlag.MOB_CAP_UNLIMITED,
                            AnimalCapFlag.ANIMAL_CAP_UNLIMITED
                    );
        }

        if (EntityCategories.HOSTILE.contains(entityType)) {
            return EntityUtil
                    .checkEntity(plot, EntityCapFlag.ENTITY_CAP_UNLIMITED, MobCapFlag.MOB_CAP_UNLIMITED,
                            HostileCapFlag.HOSTILE_CAP_UNLIMITED
                    );
        }

        if (EntityCategories.VEHICLE.contains(entityType)) {
            return EntityUtil.checkEntity(plot, EntityCapFlag.ENTITY_CAP_UNLIMITED,
                    VehicleCapFlag.VEHICLE_CAP_UNLIMITED
            );
        }

        return EntityUtil.checkEntity(plot, EntityCapFlag.ENTITY_CAP_UNLIMITED);
    }

}
