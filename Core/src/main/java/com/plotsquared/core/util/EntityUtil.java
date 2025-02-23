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
package com.plotsquared.core.util;

import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import org.checkerframework.checker.nullness.qual.NonNull;

import static com.plotsquared.core.util.entity.EntityCategories.CAP_ANIMAL;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_ENTITY;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MISC;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MOB;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MONSTER;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_VEHICLE;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_VILLAGER;

/**
 * Entity related general utility methods
 */
public class EntityUtil {

    private EntityUtil() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    private static int capNumeral(final @NonNull String flagName) {
        return switch (flagName) {
            case "mob-cap" -> CAP_MOB;
            case "hostile-cap" -> CAP_MONSTER;
            case "animal-cap" -> CAP_ANIMAL;
            case "vehicle-cap" -> CAP_VEHICLE;
            case "misc-cap" -> CAP_MISC;
            case "villager-cap" -> CAP_VILLAGER;
            default -> CAP_ENTITY;
        };
    }

    @SuppressWarnings("unchecked")
    public static boolean checkEntity(Plot plot, PlotFlag<Integer, ?>... flags) {
        if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
            return true;
        }
        int[] mobs = null;
        for (PlotFlag<Integer, ?> flag : flags) {
            final int i = capNumeral(flag.getName());
            int cap = plot.getFlag(flag);
            if (cap == Integer.MAX_VALUE) {
                continue;
            }
            if (cap == 0) {
                return true;
            }
            if (mobs == null) {
                mobs = plot.countEntities();
            }
            if (mobs[i] >= cap) {
                plot.setMeta("EntityCount", mobs);
                plot.setMeta("EntityCountTime", System.currentTimeMillis());
                plot.debug("Prevented spawning of mob because it would exceed " + flag.getName());
                return true;
            }
        }
        if (mobs != null) {
            for (PlotFlag<Integer, ?> flag : flags) {
                final int i = capNumeral(flag.getName());
                mobs[i]++;
            }
            plot.setMeta("EntityCount", mobs);
            plot.setMeta("EntityCountTime", System.currentTimeMillis());
        }
        return false;
    }

}
