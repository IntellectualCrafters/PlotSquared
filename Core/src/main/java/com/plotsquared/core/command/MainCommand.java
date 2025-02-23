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

import com.google.inject.Injector;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.MetaDataAccess;
import com.plotsquared.core.player.PlayerMetaDataKeys;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.world.SinglePlotArea;
import com.plotsquared.core.util.EconHandler;
import com.plotsquared.core.util.PlotExpression;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * PlotSquared command class.
 */
@CommandDeclaration(command = "plot",
        aliases = {"plots", "p", "plotsquared", "plot2", "p2", "ps", "2", "plotme", "plotz", "ap"})
public class MainCommand extends Command {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + MainCommand.class.getSimpleName());

    private static MainCommand instance;
    public Help help;
    public Toggle toggle;

    private MainCommand() {
        super(null, true);
        instance = this;
    }

    public static MainCommand getInstance() {
        if (instance == null) {
            instance = new MainCommand();

            final Injector injector = PlotSquared.platform().injector();
            final List<Class<? extends Command>> commands = new LinkedList<>();
            commands.add(Caps.class);
            commands.add(Buy.class);
            if (Settings.Web.LEGACY_WEBINTERFACE) {
                LOGGER.warn("Legacy webinterface is used. Please note that it will be removed in future.");
            }
            commands.add(Load.class);
            commands.add(Confirm.class);
            commands.add(Template.class);
            commands.add(Download.class);
            commands.add(Setup.class);
            commands.add(Area.class);
            commands.add(DebugSaveTest.class);
            commands.add(DebugLoadTest.class);
            commands.add(CreateRoadSchematic.class);
            commands.add(DebugAllowUnsafe.class);
            commands.add(RegenAllRoads.class);
            commands.add(Claim.class);
            commands.add(Auto.class);
            commands.add(HomeCommand.class);
            commands.add(Visit.class);
            commands.add(Set.class);
            commands.add(Clear.class);
            commands.add(Delete.class);
            commands.add(Trust.class);
            commands.add(Add.class);
            commands.add(Leave.class);
            commands.add(Deny.class);
            commands.add(Remove.class);
            commands.add(Info.class);
            commands.add(Near.class);
            commands.add(ListCmd.class);
            commands.add(Debug.class);
            commands.add(SchematicCmd.class);
            commands.add(PluginCmd.class);
            commands.add(Purge.class);
            commands.add(Reload.class);
            commands.add(Merge.class);
            commands.add(DebugPaste.class);
            commands.add(Unlink.class);
            commands.add(Kick.class);
            commands.add(Inbox.class);
            commands.add(Comment.class);
            commands.add(DatabaseCommand.class);
            commands.add(Swap.class);
            commands.add(Music.class);
            commands.add(DebugRoadRegen.class);
            commands.add(DebugExec.class);
            commands.add(FlagCommand.class);
            commands.add(Target.class);
            commands.add(Move.class);
            commands.add(Condense.class);
            commands.add(Copy.class);
            commands.add(Trim.class);
            commands.add(Done.class);
            commands.add(Continue.class);
            commands.add(Middle.class);
            commands.add(Grant.class);
            commands.add(Owner.class);
            commands.add(Desc.class);
            commands.add(Biome.class);
            commands.add(Alias.class);
            commands.add(SetHome.class);
            commands.add(Cluster.class);
            commands.add(DebugImportWorlds.class);
            commands.add(Backup.class);

            if (Settings.Ratings.USE_LIKES) {
                commands.add(Like.class);
                commands.add(Dislike.class);
            } else {
                commands.add(Rate.class);
            }

            for (final Class<? extends Command> command : commands) {
                try {
                    injector.getInstance(command);
                } catch (final Exception e) {
                    LOGGER.error("Failed to register command {}", command.getCanonicalName(), e);
                }
            }

            // Referenced commands
            instance.toggle = injector.getInstance(Toggle.class);
            instance.help = new Help(instance);
        }
        return instance;
    }

    public static boolean onCommand(final PlotPlayer<?> player, String... args) {
        final EconHandler econHandler = PlotSquared.platform().econHandler();
        if (args.length >= 1 && args[0].contains(":")) {
            String[] split2 = args[0].split(":");
            if (split2.length == 2) {
                // Ref: c:v, this will push value to the last spot in the array
                // ex. /p h:2 SomeUsername
                // > /p h SomeUsername 2
                String[] tmp = new String[args.length + 1];
                tmp[0] = split2[0];
                tmp[args.length] = split2[1];
                if (args.length >= 2) {
                    System.arraycopy(args, 1, tmp, 1, args.length - 1);
                }
                args = tmp;
            }
        }
        try {
            getInstance().execute(player, args, new RunnableVal3<>() {
                @Override
                public void run(final Command cmd, final Runnable success, final Runnable failure) {
                    if (cmd.hasConfirmation(player)) {
                        CmdConfirm.addPending(player, cmd.getUsage(), () -> {
                            PlotArea area = player.getApplicablePlotArea();
                            if (area != null && econHandler.isEnabled(area) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON)) {
                                PlotExpression priceEval =
                                        area.getPrices().get(cmd.getFullId());
                                double price = priceEval != null ? priceEval.evaluate(0d) : 0d;
                                if (econHandler.getMoney(player) < price) {
                                    if (failure != null) {
                                        failure.run();
                                    }
                                    return;
                                }
                            }
                            if (success != null) {
                                success.run();
                            }
                        });
                        return;
                    }
                    PlotArea area = player.getApplicablePlotArea();
                    if (area != null && econHandler.isEnabled(area) && !player.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_ECON)) {
                        PlotExpression priceEval = area.getPrices().get(cmd.getFullId());
                        double price = priceEval != null ? priceEval.evaluate(0d) : 0d;
                        if (price != 0d && econHandler.getMoney(player) < price) {
                            if (failure != null) {
                                failure.run();
                            }
                            return;
                        }
                    }
                    if (success != null) {
                        success.run();
                    }
                }
            }, new RunnableVal2<>() {
                @Override
                public void run(Command cmd, CommandResult result) {
                    // Post command stuff!?
                }
            }).thenAccept(result -> {
                // TODO: Something with the command result
            });
        } catch (CommandException e) {
            e.perform(player);
        }
        // Always true
        return true;
    }

    @Override
    public CompletableFuture<Boolean> execute(
            final PlotPlayer<?> player, String[] args,
            RunnableVal3<Command, Runnable, Runnable> confirm,
            RunnableVal2<Command, CommandResult> whenDone
    ) {
        prepareArguments(new CommandExecutionData(player, args, confirm, whenDone, null))
                .thenCompose(executionData -> {
                    if (executionData.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    var data = executionData.get();
                    try {
                        return super.execute(data.player(), data.args(), data.confirm(), data.whenDone());
                    } catch (CommandException e) {
                        throw e;
                    } catch (Throwable e) {
                        LOGGER.error("A error occurred while executing plot command", e);
                        String message = e.getMessage();
                        if (message != null) {
                            data.player().sendMessage(
                                    TranslatableCaption.of("errors.error"),
                                    TagResolver.resolver("value", Tag.inserting(Component.text(message)))
                            );
                        } else {
                            data.player().sendMessage(
                                    TranslatableCaption.of("errors.error_console"));
                        }
                    } finally {
                        if (data.postCommandData() != null) {
                            resetCommandScope(data.player(), data.postCommandData());
                        }
                    }
                    return CompletableFuture.completedFuture(true);
                });
        return CompletableFuture.completedFuture(true);
    }

    private CompletableFuture<Optional<CommandExecutionData>> prepareArguments(CommandExecutionData data) {
        if (data.args().length >= 2) {
            PlotArea area = data.player().getApplicablePlotArea();
            Plot newPlot = Plot.fromString(area, data.args()[0]);
            return preparePlotArgument(newPlot, data, area)
                    .thenApply(d -> d.flatMap(x -> prepareFlagArgument(x, area)));
        } else {
            return CompletableFuture.completedFuture(Optional.of(data));
        }
    }

    private CompletableFuture<Optional<CommandExecutionData>> preparePlotArgument(@Nullable Plot newPlot,
                                                                        @Nonnull CommandExecutionData data,
                                                                                  @Nullable PlotArea area) {
        if (newPlot != null && (data.player() instanceof ConsolePlayer
                || (area != null && area.equals(newPlot.getArea()))
                || data.player().hasPermission(Permission.PERMISSION_ADMIN)
                || data.player().hasPermission(Permission.PERMISSION_ADMIN_AREA_SUDO))
                && !newPlot.isDenied(data.player().getUUID())) {
            return fetchPlotCenterLocation(newPlot)
                    .thenApply(newLoc -> {
                        if (!data.player().canTeleport(newLoc)) {
                            data.player().sendMessage(TranslatableCaption.of("border.denied"));
                            return Optional.empty();
                        }
                        // Save meta
                        var originalCommandMeta = setCommandScope(data.player(), new TemporaryCommandMeta(newLoc, newPlot));
                        return Optional.of(new CommandExecutionData(
                                data.player(),
                                Arrays.copyOfRange(data.args(), 1, data.args().length), // Trimmed command
                                data.confirm(),
                                data.whenDone(),
                                originalCommandMeta
                        ));
                    });
        }
        return CompletableFuture.completedFuture(Optional.of(data));
    }

    private Optional<CommandExecutionData> prepareFlagArgument(@Nonnull CommandExecutionData data, @Nonnull PlotArea area) {
        if (data.args().length >= 2 && !data.args()[0].isEmpty() && data.args()[0].charAt(0) == '-') {
            if ("f".equals(data.args()[0].substring(1))) {
                return Optional.of(new CommandExecutionData(
                        data.player(),
                        Arrays.copyOfRange(data.args(), 1, data.args().length), // Trimmed command
                        createForcedConfirmation(data.player(), area),
                        data.whenDone(),
                        data.postCommandData()
                ));
            } else {
                data.player().sendMessage(TranslatableCaption.of("errors.invalid_command_flag"));
                return Optional.empty();
            }
        }
        return Optional.of(data);
    }

    private CompletableFuture<Location> fetchPlotCenterLocation(Plot plot) {
        if (plot.getArea() instanceof SinglePlotArea && !plot.isLoaded()) {
            return CompletableFuture.completedFuture(Location.at("", 0, 0, 0));
        }
        CompletableFuture<Location> future = new CompletableFuture<>();
        plot.getCenter(future::complete);
        return future;
    }

    private @Nonnull RunnableVal3<Command, Runnable, Runnable> createForcedConfirmation(@Nonnull PlotPlayer<?> player,
                                                                                        @Nullable PlotArea area) {
        return new RunnableVal3<>() {
            @Override
            public void run(Command cmd, Runnable success, Runnable failure) {
                if (area != null && PlotSquared.platform().econHandler().isEnabled(area)
                        && Optional.of(area.getPrices().get(cmd.getFullId()))
                        .map(priceEval -> priceEval.evaluate(0d))
                        .filter(price -> price != 0d)
                        .filter(price -> PlotSquared.platform().econHandler().getMoney(player) < price)
                        .isPresent()) {
                    if (failure != null) {
                        failure.run();
                    }
                    return;
                }
                if (success != null) {
                    success.run();
                }
            }
        };
    }

    private @Nonnull TemporaryCommandMeta setCommandScope(@Nonnull PlotPlayer<?> player, @Nonnull TemporaryCommandMeta commandMeta) {
        Objects.requireNonNull(commandMeta.location());
        Objects.requireNonNull(commandMeta.plot());
        Location location;
        Plot plot;
        try (final MetaDataAccess<Location> locationMetaDataAccess
                     = player.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
            location = locationMetaDataAccess.get().orElse(null);
            locationMetaDataAccess.set(commandMeta.location());
        }
        try (final MetaDataAccess<Plot> plotMetaDataAccess
                     = player.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
            plot = plotMetaDataAccess.get().orElse(null);
            plotMetaDataAccess.set(commandMeta.plot());
        }
        return new TemporaryCommandMeta(location, plot);
    }

    private void resetCommandScope(@Nonnull PlotPlayer<?> player, @Nonnull TemporaryCommandMeta commandMeta) {
        try (final MetaDataAccess<Location> locationMetaDataAccess
                     = player.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
            if (commandMeta.location() == null) {
                locationMetaDataAccess.remove();
            } else {
                locationMetaDataAccess.set(commandMeta.location());
            }
        }
        try (final MetaDataAccess<Plot> plotMetaDataAccess
                     = player.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
            if (commandMeta.plot() == null) {
                plotMetaDataAccess.remove();
            } else {
                plotMetaDataAccess.set(commandMeta.plot());
            }
        }
    }

    private record CommandExecutionData(@Nonnull PlotPlayer<?> player, @Nonnull String[] args,
                    @Nonnull RunnableVal3<Command, Runnable, Runnable> confirm,
                    @Nonnull RunnableVal2<Command, CommandResult> whenDone,
                    @Nullable TemporaryCommandMeta postCommandData) {}

    private record TemporaryCommandMeta(@Nullable Location location, @Nullable Plot plot) {}

    @Override
    public boolean canExecute(PlotPlayer<?> player, boolean message) {
        return true;
    }

}
