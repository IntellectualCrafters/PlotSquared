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
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.UntrustedVisitFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.PlayerManager;
import com.plotsquared.core.util.TabCompletions;
import com.plotsquared.core.util.query.PlotQuery;
import com.plotsquared.core.util.query.SortingStrategy;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@CommandDeclaration(command = "visit",
        permission = "plots.visit",
        usage = "/plot visit <player> | <alias> | <plot> [area]|[#] [#]",
        aliases = {"v", "tp", "teleport", "goto", "warp"},
        requiredType = RequiredType.PLAYER,
        category = CommandCategory.TELEPORT)
public class Visit extends Command {

    private final PlotAreaManager plotAreaManager;

    @Inject
    public Visit(final @NonNull PlotAreaManager plotAreaManager) {
        super(MainCommand.getInstance(), true);
        this.plotAreaManager = plotAreaManager;
    }

    private void visit(
            final @NonNull PlotPlayer<?> player, final @NonNull PlotQuery query, final PlotArea sortByArea,
            final RunnableVal3<Command, Runnable, Runnable> confirm, final RunnableVal2<Command, CommandResult> whenDone,
            int page, final boolean isQueryingBasePlot
    ) {
        if (!isQueryingBasePlot && query.hasMinimumMatches(2)) {
            query.whereBasePlot();
        }

        // without specified argument
        if (page == Integer.MIN_VALUE) {
            page = 1;
        }

        PlotArea relativeArea = sortByArea;
        if (Settings.Teleport.PER_WORLD_VISIT && sortByArea == null) {
            relativeArea = player.getApplicablePlotArea();
        }

        if (relativeArea != null) {
            query.relativeToArea(relativeArea).withSortingStrategy(SortingStrategy.SORT_BY_CREATION);
        } else {
            query.withSortingStrategy(SortingStrategy.SORT_BY_TEMP);
        }

        final List<Plot> plots = query.asList();

        // Conversion of reversed page argument
        if (page < 0) {
            page = (plots.size() + 1) + page;
        }

        if (plots.isEmpty()) {
            player.sendMessage(TranslatableCaption.of("invalid.found_no_plots"));
            return;
        } else if (page > plots.size() || page < 1) {
            player.sendMessage(
                    TranslatableCaption.of("invalid.number_not_in_range"),
                    TagResolver.builder()
                            .tag("min", Tag.inserting(Component.text(1)))
                            .tag("max", Tag.inserting(Component.text(plots.size())))
                            .build()
            );
            return;
        }

        final Plot plot = plots.get(page - 1);
        if (!plot.hasOwner()) {
            if (!player.hasPermission(Permission.PERMISSION_VISIT_UNOWNED)) {
                player.sendMessage(
                        TranslatableCaption.of("permission.no_permission"),
                        TagResolver.resolver("node", Tag.inserting(Component.text("plots.visit.unowned")))
                );
                return;
            }
        } else if (plot.isOwner(player.getUUID())) {
            if (!player.hasPermission(Permission.PERMISSION_VISIT_OWNED) && !player.hasPermission(Permission.PERMISSION_HOME)) {
                player.sendMessage(
                        TranslatableCaption.of("permission.no_permission"),
                        TagResolver.resolver("node", Tag.inserting(Component.text("plots.visit.owned")))
                );
                return;
            }
        } else if (plot.isAdded(player.getUUID())) {
            if (!player.hasPermission(Permission.PERMISSION_SHARED)) {
                player.sendMessage(
                        TranslatableCaption.of("permission.no_permission"),
                        TagResolver.resolver("node", Tag.inserting(Component.text("plots.visit.shared")))
                );
                return;
            }
        } else {
            // allow visit, if UntrustedVisit flag is set, or if the player has either the plot.visit.other or
            // plot.admin.visit.untrusted permission
            if (!plot.getFlag(UntrustedVisitFlag.class) && !player.hasPermission(Permission.PERMISSION_VISIT_OTHER)
                    && !player.hasPermission(Permission.PERMISSION_ADMIN_VISIT_UNTRUSTED)) {
                player.sendMessage(
                        TranslatableCaption.of("permission.no_permission"),
                        TagResolver.resolver("node", Tag.inserting(Component.text("plots.visit.other")))
                );
                return;
            }
            if (plot.isDenied(player.getUUID())) {
                if (!player.hasPermission(Permission.PERMISSION_VISIT_DENIED)) {
                    player.sendMessage(
                            TranslatableCaption.of("permission.no_permission"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_VISIT_DENIED)
                            )
                    );
                    return;
                }
            }
        }

        confirm.run(this, () -> plot.teleportPlayer(player, TeleportCause.COMMAND_VISIT, result -> {
            if (result) {
                whenDone.run(Visit.this, CommandResult.SUCCESS);
            } else {
                whenDone.run(Visit.this, CommandResult.FAILURE);
            }
        }), () -> whenDone.run(Visit.this, CommandResult.FAILURE));
    }

    @Override
    public CompletableFuture<Boolean> execute(
            final PlotPlayer<?> player,
            String[] args,
            final RunnableVal3<Command, Runnable, Runnable> confirm,
            final RunnableVal2<Command, CommandResult> whenDone
    ) throws CommandException {
        if (args.length > 3) {
            sendUsage(player);
            return CompletableFuture.completedFuture(false);
        }

        if (args.length == 1 && args[0].contains(":")) {
            args = args[0].split(":");
        }

        PlotArea sortByArea;

        int page = Integer.MIN_VALUE;

        switch (args.length) {
            // /p v <player> <area> <page>
            case 3:
                if (isInvalidPageNr(args[2])) {
                    sendInvalidPageNrMsg(player);
                    return CompletableFuture.completedFuture(false);
                }
                page = getPageNr(args[2]);
                // /p v <player> <area> [page]
                // /p v <player> [page]
            case 2:
                // If "case 3" is already through or the argument is not a page number:
                // -> /p v <player> <area> [page]
                if (page != Integer.MIN_VALUE || isInvalidPageNr(args[1])) {
                    sortByArea = this.plotAreaManager.getPlotAreaByString(args[1]);
                    if (sortByArea == null) {
                        sendInvalidPageNrMsg(player);
                        return CompletableFuture.completedFuture(false);
                    }

                    final PlotArea finalSortByArea = sortByArea;
                    int finalPage1 = page;
                    PlayerManager.getUUIDsFromString(args[0], (uuids, throwable) -> {
                        if (throwable instanceof TimeoutException) {
                            player.sendMessage(TranslatableCaption.of("players.fetching_players_timeout"));
                        } else if (throwable != null || uuids.size() != 1) {
                            player.sendMessage(
                                    TranslatableCaption.of("commandconfig.command_syntax"),
                                    TagResolver.resolver("value", Tag.inserting(Component.text(getUsage())))
                            );
                        } else {
                            final UUID uuid = uuids.toArray(new UUID[0])[0];
                            PlotQuery query = PlotQuery.newQuery();
                            if (Settings.Teleport.VISIT_MERGED_OWNERS) {
                                query.whereBasePlot().ownersInclude(uuid);
                            } else {
                                query.whereBasePlot().ownedBy(uuid);
                            }
                            this.visit(
                                    player,
                                    query,
                                    finalSortByArea,
                                    confirm,
                                    whenDone,
                                    finalPage1,
                                    true
                            );
                        }
                    });
                    break;
                }
                // -> /p v <player> <page>
                if (isInvalidPageNr(args[1])) {
                    sendInvalidPageNrMsg(player);
                    return CompletableFuture.completedFuture(false);
                }
                page = getPageNr(args[1]);
                // /p v <player> [page]
                // /p v <uuid> [page]
                // /p v <plot> [page]
                // /p v <alias>
            case 1:
                final String[] finalArgs = args;
                int finalPage = page;
                if (args[0].length() >= 2 && !args[0].contains(";") && !args[0].contains(",")) {
                    PlotSquared.get().getImpromptuUUIDPipeline().getSingle(args[0], (uuid, throwable) -> {
                        if (throwable instanceof TimeoutException) {
                            // The request timed out
                            player.sendMessage(TranslatableCaption.of("players.fetching_players_timeout"));
                            return;
                        }
                        if (uuid == null){
                            // player not found, so we assume it's an alias if no page was provided
                            if (finalPage == Integer.MIN_VALUE) {
                                this.visit(
                                        player,
                                        PlotQuery.newQuery().withAlias(finalArgs[0]),
                                        player.getApplicablePlotArea(),
                                        confirm,
                                        whenDone,
                                        1,
                                        false
                                );
                            } else {
                                player.sendMessage(
                                        TranslatableCaption.of("errors.invalid_player"),
                                        TagResolver.resolver("value", Tag.inserting(Component.text(finalArgs[0])))
                                );
                            }
                            return;
                        }
                        final PlotQuery query = Settings.Teleport.VISIT_MERGED_OWNERS
                                ? PlotQuery.newQuery().ownersInclude(uuid)
                                : PlotQuery.newQuery().ownedBy(uuid);
                        if (!query.anyMatch()) {
                            // It was a valid UUID but the player has no plots
                            player.sendMessage(TranslatableCaption.of("errors.player_no_plots"));
                            return;
                        }
                        this.visit(player, query.whereBasePlot(), null, confirm, whenDone, finalPage, true);
                    });
                } else {
                    // Try to parse a plot
                    final Plot plot = Plot.getPlotFromString(player, finalArgs[0], true);
                    if (plot != null) {
                        this.visit(player, PlotQuery.newQuery().withPlot(plot), null, confirm, whenDone, 1, false);
                    }
                }
                break;
            case 0:
                // /p v is invalid
                player.sendMessage(
                        TranslatableCaption.of("commandconfig.command_syntax"),
                        TagResolver.resolver("value", Tag.inserting(Component.text(getUsage())))
                );
                return CompletableFuture.completedFuture(false);
            default:
        }

        return CompletableFuture.completedFuture(true);
    }

    private boolean isInvalidPageNr(String arg) {
        if (MathMan.isInteger(arg)) {
            return false;
        } else if (arg.equals("last") || arg.equals("n")) {
            return false;
        }
        return true;
    }

    private int getPageNr(String arg) {
        if (MathMan.isInteger(arg)) {
            return Integer.parseInt(arg);
        } else if (arg.equals("last") || arg.equals("n")) {
            return -1;
        }
        return Integer.MIN_VALUE;
    }

    private void sendInvalidPageNrMsg(PlotPlayer<?> player) {
        player.sendMessage(
                TranslatableCaption.of("invalid.not_valid_number"),
                TagResolver.resolver("value", Tag.inserting(Component.text("(1, ∞)")))
        );
        player.sendMessage(
                TranslatableCaption.of("commandconfig.command_syntax"),
                TagResolver.resolver("value", Tag.inserting(Component.text(getUsage())))
        );
    }

    @Override
    public Collection<Command> tab(PlotPlayer<?> player, String[] args, boolean space) {
        final List<Command> completions = new ArrayList<>();
        switch (args.length - 1) {
            case 0 -> completions.addAll(TabCompletions.completePlayers(player, args[0], Collections.emptyList()));
            case 1 -> {
                completions.addAll(
                        TabCompletions.completeAreas(args[1]));
                completions.addAll(TabCompletions.asCompletions("last"));
                if (args[1].isEmpty()) {
                    // if no input is given, only suggest 1 - 3
                    completions.addAll(
                            TabCompletions.asCompletions("1", "2", "3"));
                    break;
                }
                completions.addAll(
                        TabCompletions.completeNumbers(args[1], 10, 999));
            }
            case 2 -> {
                completions.addAll(TabCompletions.asCompletions("last"));
                if (args[2].isEmpty()) {
                    // if no input is given, only suggest 1 - 3
                    completions.addAll(
                            TabCompletions.asCompletions("1", "2", "3"));
                    break;
                }
                completions.addAll(
                        TabCompletions.completeNumbers(args[2], 10, 999));
            }
        }

        return completions;
    }

}
