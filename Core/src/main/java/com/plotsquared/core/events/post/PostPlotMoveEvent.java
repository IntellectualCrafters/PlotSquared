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
package com.plotsquared.core.events.post;

import com.plotsquared.core.events.PlotPlayerEvent;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;

/**
 * Called after a plot move was performed and succeeded.
 *
 * @see com.plotsquared.core.events.PlotMoveEvent
 * @since TODO
 */
public class PostPlotMoveEvent extends PlotPlayerEvent {

    private final PlotId oldPlot;

    public PostPlotMoveEvent(final PlotPlayer<?> initiator, final PlotId oldPlot, final Plot plot) {
        super(initiator, plot);
        this.oldPlot = oldPlot;
    }

    /**
     * @return The id of the old plot location.
     * @since TODO
     */
    public PlotId oldPlot() {
        return oldPlot;
    }

}
