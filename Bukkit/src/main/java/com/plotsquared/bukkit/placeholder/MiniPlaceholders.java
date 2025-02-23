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
package com.plotsquared.bukkit.placeholder;

import com.google.common.eventbus.Subscribe;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.util.placeholders.Placeholder;
import com.plotsquared.core.util.placeholders.PlaceholderRegistry;
import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.utils.TagsUtils;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class MiniPlaceholders {

    private Expansion expansion = null;
    private final PlaceholderRegistry registry;

    public MiniPlaceholders(final @NonNull PlaceholderRegistry registry) {
        this.registry = registry;
        this.createExpansion();
        PlotSquared.get().getEventDispatcher().registerListener(this);
    }

    @Subscribe
    public void onNewPlaceholder(final PlaceholderRegistry.@NonNull PlaceholderAddedEvent event) {
        // We cannot register placeholders on the fly, so we have to replace the expansion.
        this.createExpansion();
    }

    private synchronized void createExpansion() {
        if (this.expansion != null && this.expansion.registered()) {
            this.expansion.unregister();
        }
        final Expansion.Builder builder = Expansion.builder("plotsquared");
        for (final Placeholder placeholder : this.registry.getPlaceholders()) {
            builder.audiencePlaceholder(placeholder.getKey(), (audience, argumentQueue, context) -> {
                final PlotPlayer<?> plotPlayer;
                if (audience instanceof Player player) {
                    plotPlayer = BukkitUtil.adapt(player);
                } else {
                    plotPlayer = ConsolePlayer.getConsole();
                }
                return TagsUtils.staticTag(placeholder.getValue(plotPlayer));
            });
        }
        this.expansion = builder.build();
        this.expansion.register();
    }
}
