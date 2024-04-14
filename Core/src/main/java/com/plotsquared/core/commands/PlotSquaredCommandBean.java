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
package com.plotsquared.core.commands;

import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.player.PlotPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandBean;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.Requirements;

import java.util.List;

public abstract class PlotSquaredCommandBean extends CommandBean<PlotPlayer<?>> {

    private final RequirementApplicable.RequirementApplicableFactory<PlotPlayer<?>, CommandRequirement>
            requirementApplicableFactory = RequirementApplicable.factory(CommandRequirement.REQUIREMENTS_KEY);

    /**
     * Returns the category of the command.
     *
     * @return the category
     */
    public abstract @NonNull CommandCategory category();

    /**
     * Returns the requirements for the command to be executable.
     *
     * @return the requirements
     */
    public abstract @NonNull List<@NonNull CommandRequirement> requirements();

    /**
     * Prepares the given {@code builder}.
     *
     * <p>This should be implemented by abstract classes that extend {@link PlotSquaredCommandBean} to offer shared behavior
     * for a subset of plot commands.</p>
     *
     * @param builder the builder
     * @return the prepared builder
     */
    protected Command.@NonNull Builder<PlotPlayer<?>> prepare(final Command.@NonNull Builder<PlotPlayer<?>> builder) {
        return builder;
    }

    @Override
    protected final @NonNull CommandProperties properties() {
        return CommandProperties.of("platsquared", "plat");
    }

    @Override
    protected final Command.@NonNull Builder<PlotPlayer<?>> configure(final Command.@NonNull Builder<PlotPlayer<?>> builder) {
        return this.configurePlotCommand(this.prepare(builder.meta(PlotSquaredCommandMeta.META_CATEGORY, this.category())))
                .apply(this.requirementApplicableFactory.create(Requirements.of(this.requirements())));
    }

    protected abstract Command.@NonNull Builder<PlotPlayer<?>> configurePlotCommand(
            Command.@NonNull Builder<PlotPlayer<?>> builder
    );
}
