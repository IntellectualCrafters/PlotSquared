package com.github.intellectualsites.plotsquared.plot.commands;

import com.github.intellectualsites.plotsquared.commands.Command;
import com.github.intellectualsites.plotsquared.commands.CommandDeclaration;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.object.Location;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotPlayer;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal2;
import com.github.intellectualsites.plotsquared.plot.object.RunnableVal3;
import com.github.intellectualsites.plotsquared.plot.util.MainUtil;
import com.github.intellectualsites.plotsquared.plot.util.Permissions;

import java.util.concurrent.CompletableFuture;

@CommandDeclaration(usage = "/plot swap <X;Z>",
    command = "swap",
    description = "Swap two plots",
    aliases = {"switch"},
    category = CommandCategory.CLAIMING,
    requiredType = RequiredType.PLAYER)
public class Swap extends SubCommand {

    @Override public CompletableFuture<Boolean> execute(PlotPlayer player, String[] args,
        RunnableVal3<Command, Runnable, Runnable> confirm,
        RunnableVal2<Command, CommandResult> whenDone) {
        Location location = player.getLocation();
        Plot plot1 = location.getPlotAbs();
        if (plot1 == null) {
            return CompletableFuture.completedFuture(!MainUtil.sendMessage(player, Captions.NOT_IN_PLOT));
        }
        if (!plot1.isOwner(player.getUUID()) && !Permissions
            .hasPermission(player, Captions.PERMISSION_ADMIN.getTranslated())) {
            MainUtil.sendMessage(player, Captions.NO_PLOT_PERMS);
            return CompletableFuture.completedFuture(false);
        }
        if (args.length != 1) {
            Captions.COMMAND_SYNTAX.send(player, getUsage());
            return CompletableFuture.completedFuture(false);
        }
        Plot plot2 = MainUtil.getPlotFromString(player, args[0], true);
        if (plot2 == null) {
            return CompletableFuture.completedFuture(false);
        }
        if (plot1.equals(plot2)) {
            MainUtil.sendMessage(player, Captions.NOT_VALID_PLOT_ID);
            MainUtil.sendMessage(player, Captions.COMMAND_SYNTAX, "/plot copy <X;Z>");
            return CompletableFuture.completedFuture(false);
        }
        if (!plot1.getArea().isCompatible(plot2.getArea())) {
            Captions.PLOTWORLD_INCOMPATIBLE.send(player);
            return CompletableFuture.completedFuture(false);
        }
        if (plot1.isMerged() || plot2.isMerged()) {
            Captions.SWAP_MERGED.send(player);
            return CompletableFuture.completedFuture(false);
        }

        return plot1.move(plot2, () -> {}, true)
            .thenApply(result -> {
                if (result) {
                    MainUtil.sendMessage(player, Captions.SWAP_SUCCESS);
                    return true;
                } else {
                    MainUtil.sendMessage(player, Captions.SWAP_OVERLAP);
                    return false;
                }
            });
    }

    @Override public boolean onCommand(final PlotPlayer player, String[] args) {
        return true;
    }
}
