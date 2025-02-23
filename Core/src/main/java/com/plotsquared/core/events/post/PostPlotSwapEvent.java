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

/**
 * Called after a plot swap was performed and succeeded.
 * <p>
 * <b>Note:</b> {@link Plot#getId()} of the {@link #target() target Plot} will be the {@link com.plotsquared.core.plot.PlotId}
 * of the {@link #getPlot() issuing plot} at this event stage (as the swap already happened). If you need the PlotId of the
 * origin plot (where the player was standing on when issuing the command, e.g.) before the swap, use the {@link #target()}
 * Plot, as the IDs in the plot objects were mutated. (The target plot is now the present origin plot)
 *
 * @see com.plotsquared.core.events.PlotSwapEvent
 * @since TODO
 */
public class PostPlotSwapEvent extends PlotPlayerEvent {

    private final Plot target;

    public PostPlotSwapEvent(final PlotPlayer<?> initiator, final Plot plot, final Plot target) {
        super(initiator, plot);
        this.target = target;
    }

    /**
     * The plot that initiated the plot swap.
     *
     * @return The plot.
     */
    @Override
    public Plot getPlot() {
        return super.getPlot(); // delegate for overriding the documentation
    }

    /**
     * The plot that was swapped with {@link #getPlot()}. (The command argument)
     *
     * @return The plot.
     * @since TODO
     */
    public Plot target() {
        return target;
    }

}
