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
import com.plotsquared.core.events.PlotMoveEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.CompletableFuture;

@CommandDeclaration(usage = "/plot move <X;Z>",
        command = "move",
        permission = "plots.move",
        category = CommandCategory.CLAIMING,
        requiredType = RequiredType.PLAYER)
public class Move extends SubCommand {

    private final PlotAreaManager plotAreaManager;
    private final EventDispatcher eventDispatcher;

    @Inject
    public Move(final @NonNull PlotAreaManager plotAreaManager, final @NonNull EventDispatcher eventDispatcher) {
        this.plotAreaManager = plotAreaManager;
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public CompletableFuture<Boolean> execute(
            PlotPlayer<?> player, String[] args,
            RunnableVal3<Command, Runnable, Runnable> confirm,
            RunnableVal2<Command, CommandResult> whenDone
    ) {
        Location location = player.getLocation();
        Plot plot1 = location.getPlotAbs();
        if (plot1 == null) {
            player.sendMessage(TranslatableCaption.of("errors.not_in_plot"));
            return CompletableFuture.completedFuture(false);
        }
        if (!plot1.isOwner(player.getUUID()) && !player.hasPermission(Permission.PERMISSION_ADMIN)) {
            player.sendMessage(TranslatableCaption.of("permission.no_plot_perms"));
            return CompletableFuture.completedFuture(false);
        }
        boolean override = false;
        if (args.length == 2 && args[1].equalsIgnoreCase("-f")) {
            args = new String[]{args[0]};
            override = player.hasPermission(Permission.PERMISSION_ADMIN); // Only allow force with admin permission
        }
        if (args.length != 1) {
            sendUsage(player);
            return CompletableFuture.completedFuture(false);
        }
        PlotArea area = this.plotAreaManager.getPlotAreaByString(args[0]);
        Plot tmpTargetPlot;
        if (area == null) {
            tmpTargetPlot = Plot.getPlotFromString(player, args[0], true);
            if (tmpTargetPlot == null) {
                return CompletableFuture.completedFuture(false);
            }
        } else {
            tmpTargetPlot = area.getPlotAbs(plot1.getId());
        }
        final PlotMoveEvent moveEvent = this.eventDispatcher.callMove(player, plot1, tmpTargetPlot);
        final Plot targetPlot = moveEvent.destination();
        if (!override) {
            override = moveEvent.getEventResult() == Result.FORCE;
        }

        if (moveEvent.getEventResult() == Result.DENY) {
            if (moveEvent.sendErrorMessage()) {
                player.sendMessage(TranslatableCaption.of("move.event_cancelled"));
            }
            return CompletableFuture.completedFuture(false);
        }

        if (plot1.equals(targetPlot)) {
            player.sendMessage(TranslatableCaption.of("invalid.origin_cant_be_target"));
            return CompletableFuture.completedFuture(false);
        }
        if (!plot1.getArea().isCompatible(targetPlot.getArea()) && !override) {
            player.sendMessage(TranslatableCaption.of("errors.plotworld_incompatible"));
            return CompletableFuture.completedFuture(false);
        }
        if (plot1.isMerged() || targetPlot.isMerged()) {
            player.sendMessage(TranslatableCaption.of("move.move_merged"));
            return CompletableFuture.completedFuture(false);
        }

        // Set strings here as the plot objects are mutable (the PlotID changes after being moved).
        PlotId oldPlotId = PlotId.of(plot1.getId().getX(), plot1.getId().getY());
        String p1 = plot1.toString();
        String p2 = targetPlot.toString();

        return plot1.getPlotModificationManager().move(targetPlot, player, () -> {
        }, false).thenApply(result -> {
            if (result) {
                player.sendMessage(
                        TranslatableCaption.of("move.move_success"),
                        TagResolver.builder()
                                .tag("origin", Tag.inserting(Component.text(p1)))
                                .tag("target", Tag.inserting(Component.text(p2)))
                                .build()
                );
                this.eventDispatcher.callPostMove(player, oldPlotId, targetPlot);
                return true;
            } else {
                player.sendMessage(TranslatableCaption.of("move.requires_unowned"));
                return false;
            }
        });
    }

    @Override
    public boolean onCommand(final PlotPlayer<?> player, String[] args) {
        return true;
    }

}
