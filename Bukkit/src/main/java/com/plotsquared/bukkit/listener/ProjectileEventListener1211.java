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

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.WindChargeFlag;
import com.plotsquared.core.util.PlotFlagUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * For events since 1.21.1
 * @since TODO
 */
public class ProjectileEventListener1211 implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onWindCharge(ProjectileHitEvent event) {

        Projectile entity = event.getEntity();
        if ((entity.getType() != EntityType.WIND_CHARGE) && (entity.getType() != EntityType.BREEZE_WIND_CHARGE)) {
            return;
        }

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
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, WindChargeFlag.class, false)) {
                entity.remove();
                event.setCancelled(true);
            }
            return;
        }

        if (!plot.hasOwner()) {
            entity.remove();
            event.setCancelled(true);
            return;
        }

        if (!plot.isAdded(pp.getUUID())) {
            if (!plot.getFlag(WindChargeFlag.class)) {
                plot.debug("Could not update blocks by wind charge because wind-charge = false");
                entity.remove();
                event.setCancelled(true);
            }
        }

    }

}
