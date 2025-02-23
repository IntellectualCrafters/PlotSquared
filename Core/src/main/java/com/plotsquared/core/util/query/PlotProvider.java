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
package com.plotsquared.core.util.query;

import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

interface PlotProvider {

    Collection<Plot> getPlots();

    Stream<Plot> streamPlots();

    default Stream<Plot> streamPlotsInPlotAreas(PlotArea[] areas) {
        if (areas == null || areas.length == 0) {
            return Stream.of();
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<>() {
            private int areaIndex = -1;
            private Iterator<Plot> currentAreaPlots;
            @Override
            public boolean hasNext() {
                if (currentAreaPlots == null || !currentAreaPlots.hasNext()) {
                    if (areaIndex >= areas.length) {
                        return false;
                    }
                    currentAreaPlots = areas[++areaIndex].getPlots().iterator();
                }
                return true;
            }

            @Override
            public Plot next() {
                if (currentAreaPlots == null) {
                    currentAreaPlots = areas[++areaIndex].getPlots().iterator();
                }
                return currentAreaPlots.next();
            }
        }, Spliterator.IMMUTABLE), false);
    }

}
