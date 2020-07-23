/*
 *       _____  _       _    _____                                _
 *      |  __ \| |     | |  / ____|                              | |
 *      | |__) | | ___ | |_| (___   __ _ _   _  __ _ _ __ ___  __| |
 *      |  ___/| |/ _ \| __|\___ \ / _` | | | |/ _` | '__/ _ \/ _` |
 *      | |    | | (_) | |_ ____) | (_| | |_| | (_| | | |  __/ (_| |
 *      |_|    |_|\___/ \__|_____/ \__, |\__,_|\__,_|_|  \___|\__,_|
 *                                    | |
 *                                    |_|
 *            PlotSquared plot management system for Minecraft
 *                  Copyright (C) 2020 IntellectualSites
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.plotsquared.core.command;

import com.google.inject.Inject;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Captions;
import com.plotsquared.core.configuration.ConfigurationSection;
import com.plotsquared.core.configuration.ConfigurationUtil;
import com.plotsquared.core.configuration.file.YamlConfiguration;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.generator.AugmentedUtils;
import com.plotsquared.core.generator.HybridPlotWorld;
import com.plotsquared.core.inject.annotations.WorldConfig;
import com.plotsquared.core.inject.annotations.WorldFile;
import com.plotsquared.core.inject.factory.HybridPlotWorldFactory;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotAreaTerrainType;
import com.plotsquared.core.plot.PlotAreaType;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.message.PlotMessage;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.setup.PlotAreaBuilder;
import com.plotsquared.core.util.FileUtils;
import com.plotsquared.core.util.MainUtil;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.Permissions;
import com.plotsquared.core.util.RegionManager;
import com.plotsquared.core.util.RegionUtil;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.SetupUtils;
import com.plotsquared.core.util.StringMan;
import com.plotsquared.core.util.WorldUtil;
import com.plotsquared.core.util.task.RunnableVal;
import com.plotsquared.core.util.task.RunnableVal3;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@CommandDeclaration(command = "area",
    permission = "plots.area",
    category = CommandCategory.ADMINISTRATION,
    requiredType = RequiredType.NONE,
    description = "Create a new PlotArea",
    aliases = "world",
    usage = "/plot area <create|info|list|tp|regen>",
    confirmation = true)
public class Area extends SubCommand {

    private final PlotAreaManager plotAreaManager;
    private final YamlConfiguration worldConfiguration;
    private final File worldFile;
    private final HybridPlotWorldFactory hybridPlotWorldFactory;
    private final SetupUtils setupUtils;
    private final WorldUtil worldUtil;
    private final RegionManager regionManager;

    private final Map<UUID, Map<String, Object>> metaData = new HashMap<>();

    @Inject public Area(@Nonnull final PlotAreaManager plotAreaManager,
                        @WorldConfig @Nonnull final YamlConfiguration worldConfiguration,
                        @WorldFile @Nonnull final File worldFile,
                        @Nonnull final HybridPlotWorldFactory hybridPlotWorldFactory,
                        @Nonnull final SetupUtils setupUtils,
                        @Nonnull final WorldUtil worldUtil,
                        @Nonnull final RegionManager regionManager) {
        this.plotAreaManager = plotAreaManager;
        this.worldConfiguration = worldConfiguration;
        this.worldFile = worldFile;
        this.hybridPlotWorldFactory = hybridPlotWorldFactory;
        this.setupUtils = setupUtils;
        this.worldUtil = worldUtil;
        this.regionManager = regionManager;
    }

    @Override public boolean onCommand(final PlotPlayer<?> player, String[] args) {
        if (args.length == 0) {
            Captions.COMMAND_SYNTAX.send(player, getUsage());
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "single":
                if (player instanceof ConsolePlayer) {
                    MainUtil.sendMessage(player, Captions.IS_CONSOLE);
                    return false;
                }
                if (!Permissions.hasPermission(player, Captions.PERMISSION_AREA_CREATE)) {
                    MainUtil.sendMessage(player, Captions.NO_PERMISSION, Captions.PERMISSION_AREA_CREATE);
                    return false;
                }
                if (args.length < 2) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_NEEDS_NAME);
                    return false;
                }
                final PlotArea existingArea = this.plotAreaManager.getPlotArea(player.getLocation().getWorldName(), args[1]);
                if (existingArea != null && existingArea.getId().equalsIgnoreCase(args[1])) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_NAME_TAKEN);
                    return false;
                }
                final LocalSession localSession = WorldEdit.getInstance().getSessionManager().getIfPresent(player.toActor());
                if (localSession == null) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_MISSING_SELECTION);
                    return false;
                }
                Region playerSelectedRegion = null;
                try {
                    playerSelectedRegion = localSession.getSelection(((Player) player.toActor()).getWorld());
                } catch (final Exception ignored) {}
                if (playerSelectedRegion == null) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_MISSING_SELECTION);
                    return false;
                }
                if (playerSelectedRegion.getWidth() != playerSelectedRegion.getLength()) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_NOT_SQUARE);
                    return false;
                }
                if (this.plotAreaManager.getPlotAreas(
                    Objects.requireNonNull(playerSelectedRegion.getWorld()).getName(), CuboidRegion.makeCuboid(playerSelectedRegion)).length != 0) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_OVERLAPPING);
                }
                // Alter the region
                final BlockVector3 playerSelectionMin = playerSelectedRegion.getMinimumPoint();
                final BlockVector3 playerSelectionMax = playerSelectedRegion.getMaximumPoint();
                // Create a new selection that spans the entire vertical range of the world
                final CuboidRegion selectedRegion = new CuboidRegion(playerSelectedRegion.getWorld(),
                    BlockVector3.at(playerSelectionMin.getX(), 0, playerSelectionMin.getZ()),
                    BlockVector3.at(playerSelectionMax.getX(), 255, playerSelectionMax.getZ()));
                // There's only one plot in the area...
                final PlotId plotId = PlotId.of(1, 1);
                final HybridPlotWorld hybridPlotWorld = this.hybridPlotWorldFactory.create(player.getLocation().getWorldName(), args[1],
                    Objects.requireNonNull(PlotSquared.platform()).getDefaultGenerator(), plotId, plotId);
                // Plot size is the same as the region width
                hybridPlotWorld.PLOT_WIDTH = hybridPlotWorld.SIZE = (short) selectedRegion.getWidth();
                // We use a schematic generator
                hybridPlotWorld.setTerrain(PlotAreaTerrainType.NONE);
                // It is always a partial plot world
                hybridPlotWorld.setType(PlotAreaType.PARTIAL);
                // We save the schematic :D
                hybridPlotWorld.PLOT_SCHEMATIC = true;
                // Set the road width to 0
                hybridPlotWorld.ROAD_WIDTH = hybridPlotWorld.ROAD_OFFSET_X = hybridPlotWorld.ROAD_OFFSET_Z = 0;
                // Set the plot height to the selection height
                hybridPlotWorld.PLOT_HEIGHT = hybridPlotWorld.ROAD_HEIGHT = hybridPlotWorld.WALL_HEIGHT = playerSelectionMin.getBlockY();
                // No sign plz
                hybridPlotWorld.setAllowSigns(false);
                final File parentFile = FileUtils.getFile(PlotSquared.platform().getDirectory(), "schematics" + File.separator +
                    "GEN_ROAD_SCHEMATIC" + File.separator + hybridPlotWorld.getWorldName() + File.separator +
                    hybridPlotWorld.getId());
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_COULD_NOT_MAKE_DIRECTORIES);
                    return false;
                }
                final File file = new File(parentFile, "plot.schem");
                try (final ClipboardWriter clipboardWriter = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
                    final BlockArrayClipboard clipboard = new BlockArrayClipboard(selectedRegion);
                    final EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(selectedRegion.getWorld(), -1);
                    final ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, selectedRegion, clipboard, selectedRegion.getMinimumPoint());
                    forwardExtentCopy.setCopyingBiomes(true);
                    forwardExtentCopy.setCopyingEntities(true);
                    Operations.complete(forwardExtentCopy);
                    clipboardWriter.write(clipboard);
                } catch (final Exception e) {
                    MainUtil.sendMessage(player, Captions.SINGLE_AREA_FAILED_TO_SAVE);
                    e.printStackTrace();
                    return false;
                }

                // Setup schematic
                try {
                    hybridPlotWorld.setupSchematics();
                } catch (final SchematicHandler.UnsupportedFormatException e) {
                    e.printStackTrace();
                }

                // Calculate the offset
                final BlockVector3 singlePos1 = selectedRegion.getMinimumPoint();

                // Now the schematic is saved, which is wonderful!
                PlotAreaBuilder singleBuilder = PlotAreaBuilder.ofPlotArea(hybridPlotWorld)
                        .plotManager(PlotSquared.platform().getPluginName())
                        .generatorName(PlotSquared.platform().getPluginName())
                        .maximumId(plotId)
                        .minimumId(plotId);
                Runnable singleRun = () -> {
                    final String path =
                        "worlds." + hybridPlotWorld.getWorldName() + ".areas." + hybridPlotWorld.getId() + '-'
                            + singleBuilder.minimumId() + '-' + singleBuilder.maximumId();
                    final int offsetX = singlePos1.getX();
                    final int offsetZ = singlePos1.getZ();
                    if (offsetX != 0) {
                        this.worldConfiguration.set(path + ".road.offset.x", offsetX);
                    }
                    if (offsetZ != 0) {
                        this.worldConfiguration.set(path + ".road.offset.z", offsetZ);
                    }
                    final String world = this.setupUtils.setupWorld(singleBuilder);
                    if (this.worldUtil.isWorld(world)) {
                        PlotSquared.get().loadWorld(world, null);
                        MainUtil.sendMessage(player, Captions.SINGLE_AREA_CREATED);
                    } else {
                        MainUtil.sendMessage(player,
                            "An error occurred while creating the world: " + hybridPlotWorld
                                .getWorldName());
                    }
                };
                singleRun.run();
                return true;
            case "c":
            case "setup":
            case "create":
                if (!Permissions.hasPermission(player, Captions.PERMISSION_AREA_CREATE)) {
                    Captions.NO_PERMISSION.send(player, Captions.PERMISSION_AREA_CREATE);
                    return false;
                }
                switch (args.length) {
                    case 1:
                        Captions.COMMAND_SYNTAX
                            .send(player, "/plot area create [world[:id]] [<modifier>=<value>]...");
                        return false;
                    case 2:
                        switch (args[1].toLowerCase()) {
                            case "pos1": { // Set position 1
                                HybridPlotWorld area = (HybridPlotWorld) metaData.computeIfAbsent(player.getUUID(), missingUUID -> new HashMap<>())
                                    .get("area_create_area");
                                if (area == null) {
                                    Captions.COMMAND_SYNTAX.send(player,
                                        "/plot area create [world[:id]] [<modifier>=<value>]...");
                                    return false;
                                }
                                Location location = player.getLocation();
                                metaData.computeIfAbsent(player.getUUID(), missingUUID -> new HashMap<>())
                                    .put("area_pos1", location);
                                Captions.SET_ATTRIBUTE.send(player, "area_pos1",
                                    location.getX() + "," + location.getZ());
                                MainUtil.sendMessage(player,
                                    "You will now set pos2: /plot area create pos2"
                                        + "\nNote: The chosen plot size may result in the created area not exactly matching your second position.");
                                return true;
                            }
                            case "pos2":  // Set position 2 and finish creation for type=2 (partial)
                                final HybridPlotWorld area = (HybridPlotWorld) metaData
                                    .computeIfAbsent(player.getUUID(), missingUUID -> new HashMap<>()).get("area_create_area");
                                if (area == null) {
                                    Captions.COMMAND_SYNTAX.send(player,
                                        "/plot area create [world[:id]] [<modifier>=<value>]...");
                                    return false;
                                }
                                Location pos1 = player.getLocation();
                                Location pos2 = (Location) metaData.computeIfAbsent(player.getUUID(), missingUUID -> new HashMap<>()).get("area_pos1");
                                int dx = Math.abs(pos1.getX() - pos2.getX());
                                int dz = Math.abs(pos1.getZ() - pos2.getZ());
                                int numX = Math.max(1,
                                    (dx + 1 + area.ROAD_WIDTH + area.SIZE / 2) / area.SIZE);
                                int numZ = Math.max(1,
                                    (dz + 1 + area.ROAD_WIDTH + area.SIZE / 2) / area.SIZE);
                                int ddx = dx - (numX * area.SIZE - area.ROAD_WIDTH);
                                int ddz = dz - (numZ * area.SIZE - area.ROAD_WIDTH);
                                int bx = Math.min(pos1.getX(), pos2.getX()) + ddx;
                                int bz = Math.min(pos1.getZ(), pos2.getZ()) + ddz;
                                int tx = Math.max(pos1.getX(), pos2.getX()) - ddx;
                                int tz = Math.max(pos1.getZ(), pos2.getZ()) - ddz;
                                int lower = (area.ROAD_WIDTH & 1) == 0 ?
                                    area.ROAD_WIDTH / 2 - 1 :
                                    area.ROAD_WIDTH / 2;
                                final int offsetX = bx - (area.ROAD_WIDTH == 0 ? 0 : lower);
                                final int offsetZ = bz - (area.ROAD_WIDTH == 0 ? 0 : lower);
                                final CuboidRegion region = RegionUtil.createRegion(bx, tx, bz, tz);
                                final Set<PlotArea> areas = this.plotAreaManager
                                    .getPlotAreasSet(area.getWorldName(), region);
                                if (!areas.isEmpty()) {
                                    Captions.CLUSTER_INTERSECTION
                                        .send(player, areas.iterator().next().toString());
                                    return false;
                                }
                                PlotAreaBuilder builder = PlotAreaBuilder.ofPlotArea(area)
                                        .plotManager(PlotSquared.platform().getPluginName())
                                        .generatorName(PlotSquared.platform().getPluginName())
                                        .minimumId(PlotId.of(1, 1))
                                        .maximumId(PlotId.of(numX, numZ));
                                final String path =
                                    "worlds." + area.getWorldName() + ".areas." + area.getId() + '-'
                                        + builder.minimumId() + '-' + builder.maximumId();
                                Runnable run = () -> {
                                    if (offsetX != 0) {
                                        this.worldConfiguration.set(path + ".road.offset.x", offsetX);
                                    }
                                    if (offsetZ != 0) {
                                        this.worldConfiguration.set(path + ".road.offset.z", offsetZ);
                                    }
                                    final String world = this.setupUtils.setupWorld(builder);
                                    if (this.worldUtil.isWorld(world)) {
                                        PlotSquared.get().loadWorld(world, null);
                                        Captions.SETUP_FINISHED.send(player);
                                        player.teleport(this.worldUtil.getSpawn(world),
                                            TeleportCause.COMMAND);
                                        if (area.getTerrain() != PlotAreaTerrainType.ALL) {
                                            this.regionManager.largeRegionTask(world, region,
                                                new RunnableVal<BlockVector2>() {
                                                    @Override public void run(BlockVector2 value) {
                                                        AugmentedUtils
                                                            .generate(null, world, value.getX(),
                                                                value.getZ(), null);
                                                    }
                                                }, null);
                                        }
                                    } else {
                                        MainUtil.sendMessage(player,
                                            "An error occurred while creating the world: " + area
                                                .getWorldName());
                                    }
                                };
                                if (hasConfirmation(player)) {
                                    CmdConfirm.addPending(player,
                                        getCommandString() + " create pos2 (Creates world)", run);
                                } else {
                                    run.run();
                                }
                                return true;
                        }
                    default: // Start creation
                        String[] split = args[1].split(":");
                        String id;
                        if (split.length == 2) {
                            id = split[1];
                        } else {
                            id = null;
                        }
                        PlotAreaBuilder builder = PlotAreaBuilder.newBuilder();
                        builder.worldName(split[0]);
                        final HybridPlotWorld pa = this.hybridPlotWorldFactory.create(builder.worldName(),
                            id, PlotSquared.platform().getDefaultGenerator(), null, null);
                        PlotArea other = this.plotAreaManager.getPlotArea(pa.getWorldName(), id);
                        if (other != null && Objects.equals(pa.getId(), other.getId())) {
                            Captions.SETUP_WORLD_TAKEN.send(player, pa.toString());
                            return false;
                        }
                        Set<PlotArea> areas = this.plotAreaManager.getPlotAreasSet(pa.getWorldName());
                        if (!areas.isEmpty()) {
                            PlotArea area = areas.iterator().next();
                            pa.setType(area.getType());
                        }
                        pa.SIZE = (short) (pa.PLOT_WIDTH + pa.ROAD_WIDTH);
                        for (int i = 2; i < args.length; i++) {
                            String[] pair = args[i].split("=");
                            if (pair.length != 2) {
                                Captions.COMMAND_SYNTAX.send(player, getCommandString()
                                    + " create [world[:id]] [<modifier>=<value>]...");
                                return false;
                            }
                            switch (pair[0].toLowerCase()) {
                                case "s":
                                case "size":
                                    pa.PLOT_WIDTH = Integer.parseInt(pair[1]);
                                    pa.SIZE = (short) (pa.PLOT_WIDTH + pa.ROAD_WIDTH);
                                    break;
                                case "g":
                                case "gap":
                                    pa.ROAD_WIDTH = Integer.parseInt(pair[1]);
                                    pa.SIZE = (short) (pa.PLOT_WIDTH + pa.ROAD_WIDTH);
                                    break;
                                case "h":
                                case "height":
                                    int value = Integer.parseInt(pair[1]);
                                    pa.PLOT_HEIGHT = value;
                                    pa.ROAD_HEIGHT = value;
                                    pa.WALL_HEIGHT = value;
                                    break;
                                case "f":
                                case "floor":
                                    pa.TOP_BLOCK =
                                        ConfigurationUtil.BLOCK_BUCKET.parseString(pair[1]);
                                    break;
                                case "m":
                                case "main":
                                    pa.MAIN_BLOCK =
                                        ConfigurationUtil.BLOCK_BUCKET.parseString(pair[1]);
                                    break;
                                case "w":
                                case "wall":
                                    pa.WALL_FILLING =
                                        ConfigurationUtil.BLOCK_BUCKET.parseString(pair[1]);
                                    break;
                                case "b":
                                case "border":
                                    pa.WALL_BLOCK =
                                        ConfigurationUtil.BLOCK_BUCKET.parseString(pair[1]);
                                    break;
                                case "terrain":
                                    pa.setTerrain(PlotAreaTerrainType.fromString(pair[1])
                                        .orElseThrow(() -> new IllegalArgumentException(
                                            pair[1] + " is not a valid terrain.")));
                                    builder.terrainType(pa.getTerrain());
                                    break;
                                case "type":
                                    pa.setType(PlotAreaType.fromString(pair[1]).orElseThrow(
                                        () -> new IllegalArgumentException(
                                            pair[1] + " is not a valid type.")));
                                    builder.plotAreaType(pa.getType());
                                    break;
                                default:
                                    Captions.COMMAND_SYNTAX.send(player, getCommandString()
                                        + " create [world[:id]] [<modifier>=<value>]...");
                                    return false;
                            }
                        }
                        if (pa.getType() != PlotAreaType.PARTIAL) {
                            if (this.worldUtil.isWorld(pa.getWorldName())) {
                                Captions.SETUP_WORLD_TAKEN.send(player, pa.getWorldName());
                                return false;
                            }
                            Runnable run = () -> {
                                String path = "worlds." + pa.getWorldName();
                                if (!this.worldConfiguration.contains(path)) {
                                    this.worldConfiguration.createSection(path);
                                }
                                ConfigurationSection section = this.worldConfiguration.getConfigurationSection(path);
                                pa.saveConfiguration(section);
                                pa.loadConfiguration(section);
                                builder.plotManager(PlotSquared.platform().getPluginName());
                                builder.generatorName(PlotSquared.platform().getPluginName());
                                String world = this.setupUtils.setupWorld(builder);
                                if (this.worldUtil.isWorld(world)) {
                                    Captions.SETUP_FINISHED.send(player);
                                    player.teleport(this.worldUtil.getSpawn(world),
                                        TeleportCause.COMMAND);
                                } else {
                                    MainUtil.sendMessage(player,
                                        "An error occurred while creating the world: " + pa
                                            .getWorldName());
                                }
                                try {
                                    this.worldConfiguration.save(this.worldFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            };
                            if (hasConfirmation(player)) {
                                CmdConfirm.addPending(player,
                                    getCommandString() + ' ' + StringMan.join(args, " "), run);
                            } else {
                                run.run();
                            }
                            return true;
                        }
                        if (pa.getId() == null) {
                            Captions.COMMAND_SYNTAX.send(player, getCommandString()
                                + " create [world[:id]] [<modifier>=<value>]...");
                            return false;
                        }
                        if (this.worldUtil.isWorld(pa.getWorldName())) {
                            if (!player.getLocation().getWorldName().equals(pa.getWorldName())) {
                                player.teleport(this.worldUtil.getSpawn(pa.getWorldName()),
                                    TeleportCause.COMMAND);
                            }
                        } else {
                            builder.terrainType(PlotAreaTerrainType.NONE);
                            builder.plotAreaType(PlotAreaType.NORMAL);
                            this.setupUtils.setupWorld(builder);
                            player.teleport(this.worldUtil.getSpawn(pa.getWorldName()),
                                TeleportCause.COMMAND);
                        }
                        metaData.computeIfAbsent(player.getUUID(), missingUUID -> new HashMap<>()).put("area_create_area", pa);
                        MainUtil.sendMessage(player,
                            "$1Go to the first corner and use: $2 " + getCommandString()
                                + " create pos1");
                        break;
                }
                return true;
            case "i":
            case "info": {
                if (!Permissions.hasPermission(player, Captions.PERMISSION_AREA_INFO)) {
                    Captions.NO_PERMISSION.send(player, Captions.PERMISSION_AREA_INFO);
                    return false;
                }
                PlotArea area;
                switch (args.length) {
                    case 1:
                        area = player.getApplicablePlotArea();
                        break;
                    case 2:
                        area = this.plotAreaManager.getPlotAreaByString(args[1]);
                        break;
                    default:
                        Captions.COMMAND_SYNTAX.send(player, getCommandString() + " info [area]");
                        return false;
                }
                if (area == null) {
                    if (args.length == 2) {
                        Captions.NOT_VALID_PLOT_WORLD.send(player, args[1]);
                    } else {
                        Captions.NOT_IN_PLOT_WORLD.send(player);
                    }
                    return false;
                }
                String name;
                double percent;
                int claimed = area.getPlotCount();
                int clusters = area.getClusters().size();
                String region;
                String generator = String.valueOf(area.getGenerator());
                if (area.getType() == PlotAreaType.PARTIAL) {
                    PlotId min = area.getMin();
                    PlotId max = area.getMax();
                    name = area.getWorldName() + ';' + area.getId() + ';' + min + ';' + max;
                    int size = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1);
                    percent = claimed == 0 ? 0 : size / (double) claimed;
                    region = area.getRegion().toString();
                } else {
                    name = area.getWorldName();
                    percent = claimed == 0 ? 0 : 100d * claimed / Integer.MAX_VALUE;
                    region = "N/A";
                }
                String value =
                    "&r$1NAME: " + name + "\n$1Type: $2" + area.getType() + "\n$1Terrain: $2" + area
                        .getTerrain() + "\n$1Usage: $2" + String.format("%.2f", percent) + '%'
                        + "\n$1Claimed: $2" + claimed + "\n$1Clusters: $2" + clusters
                        + "\n$1Region: $2" + region + "\n$1Generator: $2" + generator;
                MainUtil.sendMessage(player,
                    Captions.PLOT_INFO_HEADER.getTranslated() + '\n' + value + '\n'
                        + Captions.PLOT_INFO_FOOTER.getTranslated(), false);
                return true;
            }
            case "l":
            case "list":
                if (!Permissions.hasPermission(player, Captions.PERMISSION_AREA_LIST)) {
                    Captions.NO_PERMISSION.send(player, Captions.PERMISSION_AREA_LIST);
                    return false;
                }
                int page;
                switch (args.length) {
                    case 1:
                        page = 0;
                        break;
                    case 2:
                        if (MathMan.isInteger(args[1])) {
                            page = Integer.parseInt(args[1]) - 1;
                            break;
                        }
                    default:
                        Captions.COMMAND_SYNTAX.send(player, getCommandString() + " list [#]");
                        return false;
                }
                final List<PlotArea> areas = new ArrayList<>(Arrays.asList(this.plotAreaManager.getAllPlotAreas()));
                paginate(player, areas, 8, page,
                    new RunnableVal3<Integer, PlotArea, PlotMessage>() {
                        @Override public void run(Integer i, PlotArea area, PlotMessage message) {
                            String name;
                            double percent;
                            int claimed = area.getPlotCount();
                            int clusters = area.getClusters().size();
                            String region;
                            String generator = String.valueOf(area.getGenerator());
                            if (area.getType() == PlotAreaType.PARTIAL) {
                                PlotId min = area.getMin();
                                PlotId max = area.getMax();
                                name = area.getWorldName() + ';' + area.getId() + ';' + min + ';'
                                    + max;
                                int size = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1);
                                percent = claimed == 0 ? 0 : size / (double) claimed;
                                region = area.getRegion().toString();
                            } else {
                                name = area.getWorldName();
                                percent = claimed == 0 ?
                                    0 :
                                    Short.MAX_VALUE * Short.MAX_VALUE / (double) claimed;
                                region = "N/A";
                            }
                            PlotMessage tooltip = new PlotMessage().text("Claimed=").color("$1")
                                .text(String.valueOf(claimed)).color("$2").text("\nUsage=")
                                .color("$1").text(String.format("%.2f", percent) + '%').color("$2")
                                .text("\nClusters=").color("$1").text(String.valueOf(clusters))
                                .color("$2").text("\nRegion=").color("$1").text(region).color("$2")
                                .text("\nGenerator=").color("$1").text(generator).color("$2");

                            // type / terrain
                            String visit = "/plot area tp " + area.toString();
                            message.text("[").color("$3").text(String.valueOf(i)).command(visit)
                                .tooltip(visit).color("$1").text("]").color("$3").text(' ' + name)
                                .tooltip(tooltip).command(getCommandString() + " info " + area)
                                .color("$1").text(" - ").color("$2")
                                .text(area.getType() + ":" + area.getTerrain()).color("$3");
                        }
                    }, "/plot area list", Captions.AREA_LIST_HEADER_PAGED.getTranslated());
                return true;
            case "regen":
            case "clear":
            case "reset":
            case "regenerate": {
                if (!Permissions.hasPermission(player, Captions.PERMISSION_AREA_REGEN)) {
                    Captions.NO_PERMISSION.send(player, Captions.PERMISSION_AREA_REGEN);
                    return false;
                }
                final PlotArea area = player.getApplicablePlotArea();
                if (area == null) {
                    Captions.NOT_IN_PLOT_WORLD.send(player);
                    return false;
                }
                if (area.getType() != PlotAreaType.PARTIAL) {
                    MainUtil.sendMessage(player,
                        "$4Stop the server and delete: " + area.getWorldName() + "/region");
                    return false;
                }
                this.regionManager.largeRegionTask(area.getWorldName(), area.getRegion(),
                    new RunnableVal<BlockVector2>() {
                        @Override public void run(BlockVector2 value) {
                            AugmentedUtils
                                .generate(null, area.getWorldName(), value.getX(), value.getZ(),
                                    null);
                        }
                    }, () -> player.sendMessage("Regen complete"));
                return true;
            }
            case "goto":
            case "v":
            case "teleport":
            case "visit":
            case "tp":
                if (!Permissions.hasPermission(player, Captions.PERMISSION_AREA_TP)) {
                    Captions.NO_PERMISSION.send(player, Captions.PERMISSION_AREA_TP);
                    return false;
                }
                if (args.length != 2) {
                    Captions.COMMAND_SYNTAX.send(player, "/plot visit [area]");
                    return false;
                }
                PlotArea area = this.plotAreaManager.getPlotAreaByString(args[1]);
                if (area == null) {
                    Captions.NOT_VALID_PLOT_WORLD.send(player, args[1]);
                    return false;
                }
                Location center;
                if (area.getType() != PlotAreaType.PARTIAL) {
                    center = this.worldUtil.getSpawn(area.getWorldName());
                    player.teleport(center, TeleportCause.COMMAND);
                } else {
                    CuboidRegion region = area.getRegion();
                    center = Location.at(area.getWorldName(), region.getMinimumPoint().getX()
                        + (region.getMaximumPoint().getX() - region.getMinimumPoint().getX()) / 2,
                        0, region.getMinimumPoint().getZ()
                        + (region.getMaximumPoint().getZ() - region.getMinimumPoint().getZ()) / 2);
                    this.worldUtil.getHighestBlock(area.getWorldName(), center.getX(), center.getZ(), y ->
                            player.teleport(center.withY(1 + y), TeleportCause.COMMAND));
                }
                return true;
            case "delete":
            case "remove":
                MainUtil.sendMessage(player,
                    "$1World creation settings may be stored in multiple locations:"
                        + "\n$3 - $2Bukkit bukkit.yml" + "\n$3 - $2" + PlotSquared.platform()
                        .getPluginName() + " settings.yml"
                        + "\n$3 - $2Multiverse worlds.yml (or any world management plugin)"
                        + "\n$1Stop the server and delete it from these locations.");
                return true;
        }
        Captions.COMMAND_SYNTAX.send(player, getUsage());
        return false;
    }

}
