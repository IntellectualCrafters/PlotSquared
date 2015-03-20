package com.intellectualcrafters.plot.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class PlotHandler {
    public static HashSet<UUID> getOwners(Plot plot) {
        if (plot.owner_ == null) {
            return new HashSet<UUID>();
        }
        if (plot.settings.isMerged()) {
            HashSet<UUID> owners = new HashSet<UUID>();
            Plot top = MainUtil.getTopPlot(plot);
            ArrayList<PlotId> ids = MainUtil.getPlotSelectionIds(plot.id, top.id);
            for (PlotId id : ids) {
                UUID owner = MainUtil.getPlot(plot.world, id).owner_;
                if (owner != null) {
                    owners.add(owner);
                }
            }
            return owners;
        }
        return new HashSet<>(Arrays.asList(plot.owner_));
    }
    
    public static boolean isOwner(Plot plot, UUID uuid) {
        if (plot.owner_ == null) {
            return false;
        }
        if (plot.settings.isMerged()) {
            Plot top = MainUtil.getTopPlot(plot);
            ArrayList<PlotId> ids = MainUtil.getPlotSelectionIds(plot.id, top.id);
            for (PlotId id : ids) {
                UUID owner = MainUtil.getPlot(plot.world, id).owner_;
                if (owner != null && owner.equals(uuid)) {
                    return true;
                }
            }
        }
        return plot.owner_.equals(uuid);
    }
    
    public static boolean isOnline(Plot plot) {
        if (plot.owner_ == null) {
            return false;
        }
        if (plot.settings.isMerged()) {
            Plot top = MainUtil.getTopPlot(plot);
            ArrayList<PlotId> ids = MainUtil.getPlotSelectionIds(plot.id, top.id);
            for (PlotId id : ids) {
                UUID owner = MainUtil.getPlot(plot.world, id).owner_;
                if (owner != null) {
                    if (UUIDHandler.getPlayer(owner) != null) {
                        return true;
                    }
                }
            }
            return false;
        }
        return UUIDHandler.getPlayer(plot.owner_) != null;
    }
    
    public static boolean sameOwners(Plot plot1, Plot plot2) {
        if (plot1.owner_ == null || plot2.owner_ == null) {
            return false;
        }
        HashSet<UUID> owners = getOwners(plot1);
        owners.retainAll(getOwners(plot2));
        return owners.size() > 0;
    }
}
