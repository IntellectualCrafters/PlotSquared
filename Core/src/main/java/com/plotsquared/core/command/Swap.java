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
package com.plotsquared.core.command;

import com.google.inject.Inject;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.PlotSwapEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.concurrent.CompletableFuture;

@CommandDeclaration(usage = "/plot swap <X;Z>",
        command = "swap",
        aliases = {"switch"},
        category = CommandCategory.CLAIMING,
        requiredType = RequiredType.PLAYER)
public class Swap extends SubCommand {

    @Inject
    private EventDispatcher eventDispatcher;

    @Override
    public CompletableFuture<Boolean> execute(
            PlotPlayer<?> player, String[] args,
            RunnableVal3<Command, Runnable, Runnable> confirm,
            RunnableVal2<Command, CommandResult> whenDone
    ) {
        Location location = player.getLocation();
        Plot plot = location.getPlotAbs();
        if (plot == null) {
            player.sendMessage(TranslatableCaption.of("errors.not_in_plot"));
            return CompletableFuture.completedFuture(false);
        }
        if (args.length != 1) {
            sendUsage(player);
            return CompletableFuture.completedFuture(false);
        }
        final Plot plotArg = Plot.getPlotFromString(player, args[0], true);
        if (plotArg == null) {
            return CompletableFuture.completedFuture(false);
        }
        final PlotSwapEvent event = this.eventDispatcher.callSwap(player, plot, plotArg);
        if (event.getEventResult() == Result.DENY) {
            if (event.sendErrorMessage()) {
                player.sendMessage(TranslatableCaption.of("swap.event_cancelled"));
            }
            return CompletableFuture.completedFuture(false);
        }
        if (!plot.isOwner(player.getUUID()) && !player.hasPermission(Permission.PERMISSION_ADMIN) && event.getEventResult() != Result.FORCE) {
            player.sendMessage(TranslatableCaption.of("permission.no_plot_perms"));
            return CompletableFuture.completedFuture(false);
        }
        final Plot target = event.target();
        if (plot.equals(target)) {
            player.sendMessage(TranslatableCaption.of("invalid.origin_cant_be_target"));
            return CompletableFuture.completedFuture(false);
        }
        if (!plot.getArea().isCompatible(target.getArea())) {
            player.sendMessage(TranslatableCaption.of("errors.plotworld_incompatible"));
            return CompletableFuture.completedFuture(false);
        }
        if (plot.isMerged() || target.isMerged()) {
            player.sendMessage(TranslatableCaption.of("swap.swap_merged"));
            return CompletableFuture.completedFuture(false);
        }

        // Set strings here as the plot objects are mutable (the PlotID changes after being moved).
        String p1 = plot.toString();
        String p2 = target.toString();

        return plot.getPlotModificationManager().move(target, player, () -> {
        }, true).thenApply(result -> {
            if (result) {
                player.sendMessage(
                        TranslatableCaption.of("swap.swap_success"),
                        TagResolver.builder()
                                .tag("origin", Tag.inserting(Component.text(p1)))
                                .tag("target", Tag.inserting(Component.text(p2)))
                                .build()
                );
                this.eventDispatcher.callPostSwap(player, plot, target);
                return true;
            } else {
                player.sendMessage(TranslatableCaption.of("swap.swap_overlap"));
                return false;
            }
        });
    }

    @Override
    public boolean onCommand(final PlotPlayer<?> player, String[] args) {
        return true;
    }

}
