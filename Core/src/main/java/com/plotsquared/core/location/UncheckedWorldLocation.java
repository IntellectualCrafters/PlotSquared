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
 *               Copyright (C) 2014 - 2022 IntellectualSites
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
package com.plotsquared.core.location;

import com.plotsquared.core.util.AnnotationHelper;
import com.sk89q.worldedit.math.BlockVector3;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Used internally for generation to reference locations in worlds that "don't exist yet". There is no guarantee that the world
 * name provided by {@link UncheckedWorldLocation#getWorldName()} exists on the server.
 *
 * @since 6.9.0
 */
@AnnotationHelper.ApiDescription(info = "Internal use only. Subject to changes at any time.")
public final class UncheckedWorldLocation extends Location {

    private final String worldName;

    /**
     * @since 6.9.0
     */
    private UncheckedWorldLocation(
            final @NonNull String worldName, final int x, final int y, final int z
    ) {
        super(World.nullWorld(), BlockVector3.at(x, y, z), 0f, 0f);
        this.worldName = worldName;
    }

    /**
     * Construct a new location with yaw and pitch equal to 0
     *
     * @param world World
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @return New location
     *
     * @since 6.9.0
     */
    @AnnotationHelper.ApiDescription(info = "Internal use only. Subject to changes at any time.")
    public static @NonNull UncheckedWorldLocation at(
            final @NonNull String world, final int x, final int y, final int z
    ) {
        return new UncheckedWorldLocation(world, x, y, z);
    }

    @Override
    @AnnotationHelper.ApiDescription(info = "Internal use only. Subject to changes at any time.")
    public @NonNull String getWorldName() {
        return this.worldName;
    }

}
