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

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class SearchPlotProvider implements PlotProvider {

    private final String searchTerm;

    SearchPlotProvider(final @NonNull String searchTerm) {
        this.searchTerm = searchTerm;
    }

    /**
     * Fuzzy plot search with spaces separating terms.
     * - Terms: type, alias, world, owner, trusted, member
     *
     * @param search Search string
     * @return Search results
     */
    @NonNull
    private static List<Plot> getPlotsBySearch(final @NonNull String search) {
        String[] split = search.split(" ");
        int size = split.length * 2;

        List<UUID> uuids = new ArrayList<>();
        PlotId id = null;

        for (String term : split) {
            try {
                UUID uuid = PlotSquared.get().getImpromptuUUIDPipeline()
                        .getSingle(term, Settings.UUID.BLOCKING_TIMEOUT);
                if (uuid == null) {
                    uuid = UUID.fromString(term);
                }
                uuids.add(uuid);
            } catch (Exception ignored) {
                id = PlotId.fromString(term);
            }
        }

        ArrayList<ArrayList<Plot>> plotList =
                IntStream.range(0, size).mapToObj(i -> new ArrayList<Plot>())
                        .collect(Collectors.toCollection(() -> new ArrayList<>(size)));

        PlotArea area = null;
        String alias = null;
        for (Plot plot : PlotQuery.newQuery().allPlots()) {
            int count = 0;
            if (!uuids.isEmpty()) {
                for (UUID uuid : uuids) {
                    if (plot.isOwner(uuid)) {
                        count += 2;
                    } else if (plot.isAdded(uuid)) {
                        count++;
                    }
                }
            }
            if (id != null) {
                if (plot.getId().equals(id)) {
                    count++;
                }
            }
            if (area != null && plot.getArea().equals(area)) {
                count++;
            }
            if (alias != null && alias.equals(plot.getAlias())) {
                count += 2;
            }
            if (count != 0) {
                plotList.get(count - 1).add(plot);
            }
        }

        List<Plot> plots = new ArrayList<>();
        for (int i = plotList.size() - 1; i >= 0; i--) {
            if (!plotList.get(i).isEmpty()) {
                plots.addAll(plotList.get(i));
            }
        }
        return plots;
    }

    @Override
    public Collection<Plot> getPlots() {
        return getPlotsBySearch(this.searchTerm);
    }

    @Override
    public Stream<Plot> streamPlots() {
        return getPlots().stream(); // TODO: look into potentially optimizations here?
    }

}
