/*
 *       _____  _       _    _____                                _
 *      |  __ \| |     | |  / ____|                              | |
 *      | |__) | | ___ | |_| (___   __ _ _   _  __ _ _ __ ___  __| |
 *      |  ___/| |/ _ \| __|\___ \ / _` | | | |/ _` | '__/ _ \/ _` |
 *      | |    | | (_) | |_ ____) | (_| | |_| | (_| | | |  __/ (_| |
 *      |_|    |_|\___/ \__|_____/ \__, |\__,_|\__,_|_|  \___|\__,_|
 *                                    | |
 *                                    |_|
 *            PlotSquared plot management system for Minecraft
 *                  Copyright (C) 2020 IntellectualSites
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.bukkit.permissions;

import com.plotsquared.bukkit.player.BukkitPlayer;
import com.plotsquared.core.permissions.ConsolePermissionProfile;
import com.plotsquared.core.permissions.PermissionHandler;
import com.plotsquared.core.permissions.PermissionProfile;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.OfflinePlotPlayer;
import com.plotsquared.core.player.PlotPlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class BukkitPermissionHandler implements PermissionHandler {

    @Override public void initialize() {
    }

    @Nonnull @Override public Optional<PermissionProfile> getPermissionProfile(
        @Nonnull PlotPlayer<?> playerPlotPlayer) {
        if (playerPlotPlayer instanceof BukkitPlayer) {
            final BukkitPlayer bukkitPlayer = (BukkitPlayer) playerPlotPlayer;
            return Optional.of(new BukkitPermissionProfile(bukkitPlayer.getPlatformPlayer()));
        } else if (playerPlotPlayer instanceof ConsolePlayer) {
            return Optional.of(ConsolePermissionProfile.INSTANCE);
        }
        return Optional.empty();
    }

    @Nonnull @Override public Optional<PermissionProfile> getPermissionProfile(
        @Nonnull OfflinePlotPlayer offlinePlotPlayer) {
        return Optional.empty();
    }

    @Nonnull @Override public Set<PermissionHandlerCapability> getCapabilities() {
        return EnumSet.of(PermissionHandlerCapability.ONLINE_PERMISSIONS);
    }


    private static final class BukkitPermissionProfile implements PermissionProfile {

        private final WeakReference<Player> playerReference;

        private BukkitPermissionProfile(@Nonnull final Player player) {
            this.playerReference = new WeakReference<>(player);
        }

        @Override public boolean hasPermission(@Nullable final String world,
                                               @Nonnull final String permission) {
            final Player player = this.playerReference.get();
            return player != null && player.hasPermission(permission);
        }

    }

}
