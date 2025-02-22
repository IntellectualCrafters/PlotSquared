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
import com.plotsquared.core.plot.world.PlotAreaManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

class GlobalPlotProvider implements PlotProvider {

    private final PlotAreaManager plotAreaManager;

    GlobalPlotProvider(final @NonNull PlotAreaManager plotAreaManager) {
        this.plotAreaManager = plotAreaManager;
    }

    @Override
    public Collection<Plot> getPlots() {
        final List<Plot> plots = new ArrayList<>();
        for (final PlotArea plotArea : this.plotAreaManager.getAllPlotAreas()) {
            plots.addAll(plotArea.getPlots());
        }
        return plots;
    }

    @Override
    public Stream<Plot> streamPlots() {
        return streamPlotsInPlotAreas(this.plotAreaManager.getAllPlotAreas());
    }

}
