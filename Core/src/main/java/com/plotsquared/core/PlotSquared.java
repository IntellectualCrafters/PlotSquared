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
package com.plotsquared.core;

import com.google.inject.Key;
import com.plotsquared.core.configuration.ConfigurationSection;
import com.plotsquared.core.configuration.ConfigurationUtil;
import com.plotsquared.core.configuration.MemorySection;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.Storage;
import com.plotsquared.core.configuration.caption.CaptionMap;
import com.plotsquared.core.configuration.caption.DummyCaptionMap;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.configuration.caption.load.CaptionLoader;
import com.plotsquared.core.configuration.caption.load.DefaultCaptionProvider;
import com.plotsquared.core.configuration.file.YamlConfiguration;
import com.plotsquared.core.configuration.serialization.ConfigurationSerialization;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.database.SQLManager;
import com.plotsquared.core.generator.GeneratorWrapper;
import com.plotsquared.core.generator.HybridPlotWorld;
import com.plotsquared.core.generator.HybridUtils;
import com.plotsquared.core.generator.IndependentPlotGenerator;
import com.plotsquared.core.inject.annotations.PlotDatabase;
import com.plotsquared.core.inject.factory.HybridPlotWorldFactory;
import com.plotsquared.core.listener.PlotListener;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlayerMetaDataKeys;
import com.plotsquared.core.plot.BlockBucket;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotAreaTerrainType;
import com.plotsquared.core.plot.PlotAreaType;
import com.plotsquared.core.plot.PlotCluster;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.PlotManager;
import com.plotsquared.core.plot.expiration.ExpireManager;
import com.plotsquared.core.plot.expiration.ExpiryTask;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.plot.world.SinglePlotArea;
import com.plotsquared.core.plot.world.SinglePlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.FileUtils;
import com.plotsquared.core.util.LegacyConverter;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.ReflectionUtils;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.uuid.UUIDPipeline;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.util.eventbus.EventHandler;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An implementation of the core, with a static getter for easy access.
 */
@SuppressWarnings({"WeakerAccess"})
public class PlotSquared {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + PlotSquared.class.getSimpleName());
    private static @MonotonicNonNull PlotSquared instance;

    // Implementation
    private final PlotPlatform<?> platform;
    // Current thread
    private final Thread thread;
    // UUID pipelines
    private final UUIDPipeline impromptuUUIDPipeline =
            new UUIDPipeline(Executors.newCachedThreadPool());
    private final UUIDPipeline backgroundUUIDPipeline =
            new UUIDPipeline(Executors.newSingleThreadExecutor());
    // Localization
    private final Map<String, CaptionMap> captionMaps = new HashMap<>();
    public HashMap<String, HashMap<PlotId, Plot>> plots_tmp;
    private CaptionLoader captionLoader;
    // WorldEdit instance
    private WorldEdit worldedit;
    private File configFile;
    private File worldsFile;
    private YamlConfiguration worldConfiguration;
    // Temporary hold the plots/clusters before the worlds load
    private HashMap<String, Set<PlotCluster>> clustersTmp;
    private YamlConfiguration config;
    // Platform / Version / Update URL
    private PlotVersion version;
    // Files and configuration
    private File jarFile = null; // This file
    private File storageFile;
    private EventDispatcher eventDispatcher;
    private PlotListener plotListener;

    private boolean weInitialised;

    /**
     * Initialize PlotSquared with the desired Implementation class.
     *
     * @param iPlotMain Implementation of {@link PlotPlatform} used
     * @param platform  The platform being used
     */
    public PlotSquared(
            final @NonNull PlotPlatform<?> iPlotMain,
            final @NonNull String platform
    ) {
        if (instance != null) {
            throw new IllegalStateException("Cannot re-initialize the PlotSquared singleton");
        }
        instance = this;

        this.thread = Thread.currentThread();
        this.platform = iPlotMain;
        Settings.PLATFORM = platform;

        // Initialize the class
        PlayerMetaDataKeys.load();

        //
        // Register configuration serializable classes
        //
        ConfigurationSerialization.registerClass(BlockBucket.class, "BlockBucket");

        // load configs before reading from settings
        if (!setupConfigs()) {
            return;
        }

        this.captionLoader = CaptionLoader.of(
                Locale.ENGLISH,
                CaptionLoader.patternExtractor(Pattern.compile("messages_(.*)\\.json")),
                DefaultCaptionProvider.forClassLoaderFormatString(
                        this.getClass().getClassLoader(),
                        "lang/messages_%s.json" // the path in our jar file
                ),
                TranslatableCaption.DEFAULT_NAMESPACE
        );
        // Load caption map
        try {
            this.loadCaptionMap();
        } catch (final Exception e) {
            LOGGER.error("Failed to load caption map", e);
            LOGGER.error("Shutting down server to prevent further issues");
            this.platform.shutdownServer();
            throw new RuntimeException("Abort loading PlotSquared");
        }

        // Setup the global flag container
        GlobalFlagContainer.setup();

        try {
            String ver = this.platform.serverNativePackage();
            new ReflectionUtils(ver.isEmpty() ? null : ver);
            try {
                URL logurl = PlotSquared.class.getProtectionDomain().getCodeSource().getLocation();
                this.jarFile = new File(
                        URI.create(
                                logurl.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file"))
                                .getPath());
            } catch (URISyntaxException | SecurityException e) {
                LOGGER.error(e);
                this.jarFile = new File(this.platform.getDirectory().getParentFile(), "PlotSquared.jar");
                if (!this.jarFile.exists()) {
                    this.jarFile = new File(
                            this.platform.getDirectory().getParentFile(),
                            "PlotSquared-" + platform + ".jar"
                    );
                }
            }

            this.worldedit = WorldEdit.getInstance();
            WorldEdit.getInstance().getEventBus().register(new WEPlatformReadyListener());

            // Create Event utility class
            this.eventDispatcher = new EventDispatcher(this.worldedit);
            // Create plot listener
            this.plotListener = new PlotListener(this.eventDispatcher);

            // Copy files
            copyFile("town.template", Settings.Paths.TEMPLATES);
            copyFile("bridge.template", Settings.Paths.TEMPLATES);
            copyFile("skyblock.template", Settings.Paths.TEMPLATES);
            showDebug();
        } catch (Throwable e) {
            LOGGER.error(e);
        }
    }

    /**
     * Gets an instance of PlotSquared.
     *
     * @return instance of PlotSquared
     */
    public static @NonNull PlotSquared get() {
        return PlotSquared.instance;
    }

    /**
     * Get the platform specific implementation of PlotSquared
     *
     * @return Platform implementation
     */
    public static @NonNull PlotPlatform<?> platform() {
        if (instance != null && instance.platform != null) {
            return instance.platform;
        }
        throw new IllegalStateException("Plot platform implementation is missing");
    }

    public void loadCaptionMap() throws Exception {
        this.platform.copyCaptionMaps();
        // Setup localization
        CaptionMap captionMap;
        if (Settings.Enabled_Components.PER_USER_LOCALE) {
            captionMap = this.captionLoader.loadAll(this.platform.getDirectory().toPath().resolve("lang"));
        } else {
            String fileName = "messages_" + Settings.Enabled_Components.DEFAULT_LOCALE + ".json";
            captionMap = this.captionLoader.loadOrCreateSingle(this.platform
                    .getDirectory()
                    .toPath()
                    .resolve("lang")
                    .resolve(fileName));
        }
        this.captionMaps.put(TranslatableCaption.DEFAULT_NAMESPACE, captionMap);
        LOGGER.info(
                "Loaded caption map for namespace 'plotsquared': {}",
                this.captionMaps.get(TranslatableCaption.DEFAULT_NAMESPACE).getClass().getCanonicalName()
        );
    }

    /**
     * Get the platform specific {@link PlotAreaManager} instance
     *
     * @return Plot area manager
     */
    public @NonNull PlotAreaManager getPlotAreaManager() {
        return this.platform.plotAreaManager();
    }

    public void startExpiryTasks() {
        if (Settings.Enabled_Components.PLOT_EXPIRY) {
            ExpireManager expireManager = PlotSquared.platform().expireManager();
            expireManager.runAutomatedTask();
            for (Settings.Auto_Clear settings : Settings.AUTO_CLEAR.getInstances()) {
                ExpiryTask task = new ExpiryTask(settings, this.getPlotAreaManager());
                expireManager.addTask(task);
            }
        }
    }

    public boolean isMainThread(final @NonNull Thread thread) {
        return this.thread == thread;
    }

    /**
     * Check if `version` is &gt;= `version2`.
     *
     * @param version  First version
     * @param version2 Second version
     * @return {@code true} if `version` is &gt;= `version2`
     */
    public boolean checkVersion(
            final int[] version,
            final int... version2
    ) {
        return version[0] > version2[0] || version[0] == version2[0] && version[1] > version2[1]
                || version[0] == version2[0] && version[1] == version2[1] && version[2] >= version2[2];
    }

    /**
     * Gets the current PlotSquared version.
     *
     * @return current version in config or null
     */
    public @NonNull PlotVersion getVersion() {
        return this.version;
    }

    /**
     * Gets the server platform this plugin is running on this is running on.
     *
     * <p>This will be either <b>Bukkit</b> or <b>Sponge</b></p>
     *
     * @return the server implementation
     */
    public @NonNull String getPlatform() {
        return Settings.PLATFORM;
    }

    /**
     * Add a global reference to a plot world.
     * <p>
     * You can remove the reference by calling {@link #removePlotArea(PlotArea)}
     * </p>
     *
     * @param plotArea the {@link PlotArea} to add.
     */
    @SuppressWarnings("unchecked")
    public void addPlotArea(final @NonNull PlotArea plotArea) {
        HashMap<PlotId, Plot> plots;
        if (plots_tmp == null || (plots = plots_tmp.remove(plotArea.toString())) == null) {
            if (plotArea.getType() == PlotAreaType.PARTIAL) {
                plots = this.plots_tmp != null ? this.plots_tmp.get(plotArea.getWorldName()) : null;
                if (plots != null) {
                    Iterator<Entry<PlotId, Plot>> iterator = plots.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry<PlotId, Plot> next = iterator.next();
                        PlotId id = next.getKey();
                        if (plotArea.contains(id)) {
                            next.getValue().setArea(plotArea);
                            iterator.remove();
                        }
                    }
                }
            }
        } else {
            for (Plot entry : plots.values()) {
                entry.setArea(plotArea);
            }
        }
        Set<PlotCluster> clusters;
        if (clustersTmp == null || (clusters = clustersTmp.remove(plotArea.toString())) == null) {
            if (plotArea.getType() == PlotAreaType.PARTIAL) {
                clusters = this.clustersTmp != null ?
                        this.clustersTmp.get(plotArea.getWorldName()) :
                        null;
                if (clusters != null) {
                    Iterator<PlotCluster> iterator = clusters.iterator();
                    while (iterator.hasNext()) {
                        PlotCluster next = iterator.next();
                        if (next.intersects(plotArea.getMin(), plotArea.getMax())) {
                            next.setArea(plotArea);
                            iterator.remove();
                        }
                    }
                }
            }
        } else {
            for (PlotCluster cluster : clusters) {
                cluster.setArea(plotArea);
            }
        }
        getPlotAreaManager().addPlotArea(plotArea);
        plotArea.setupBorder();
        if (!Settings.Enabled_Components.PERSISTENT_ROAD_REGEN) {
            return;
        }
        File file = new File(
                this.platform.getDirectory() + File.separator + "persistent_regen_data_" + plotArea.getId()
                        + "_" + plotArea.getWorldName());
        if (!file.exists()) {
            return;
        }
        TaskManager.runTaskAsync(() -> {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                List<Object> list = (List<Object>) ois.readObject();
                ArrayList<int[]> regionInts = (ArrayList<int[]>) list.get(0);
                ArrayList<int[]> chunkInts = (ArrayList<int[]>) list.get(1);
                HashSet<BlockVector2> regions = new HashSet<>();
                Set<BlockVector2> chunks = new HashSet<>();
                regionInts.forEach(l -> regions.add(BlockVector2.at(l[0], l[1])));
                chunkInts.forEach(l -> chunks.add(BlockVector2.at(l[0], l[1])));
                int height = (int) list.get(2);
                LOGGER.info(
                        "Incomplete road regeneration found. Restarting in world {} with height {}",
                        plotArea.getWorldName(),
                        height
                );
                LOGGER.info("- Regions: {}", regions.size());
                LOGGER.info("- Chunks: {}", chunks.size());
                HybridUtils.UPDATE = true;
                PlotSquared.platform().hybridUtils().scheduleRoadUpdate(plotArea, regions, height, chunks);
            } catch (IOException | ClassNotFoundException e) {
                LOGGER.error("Error restarting road regeneration", e);
            } finally {
                if (!file.delete()) {
                    LOGGER.error("Error deleting persistent_regen_data_{}. Please delete this file manually", plotArea.getId());
                }
            }
        });
    }

    /**
     * Remove a plot world reference.
     *
     * @param area the {@link PlotArea} to remove
     */
    public void removePlotArea(final @NonNull PlotArea area) {
        getPlotAreaManager().removePlotArea(area);
        setPlotsTmp(area);
    }

    public void removePlotAreas(final @NonNull String world) {
        for (final PlotArea area : this.getPlotAreaManager().getPlotAreasSet(world)) {
            if (area.getWorldName().equals(world)) {
                removePlotArea(area);
            }
        }
    }

    private void setPlotsTmp(final @NonNull PlotArea area) {
        if (this.plots_tmp == null) {
            this.plots_tmp = new HashMap<>();
        }
        HashMap<PlotId, Plot> map =
                this.plots_tmp.computeIfAbsent(area.toString(), k -> new HashMap<>());
        for (Plot plot : area.getPlots()) {
            map.put(plot.getId(), plot);
        }
        if (this.clustersTmp == null) {
            this.clustersTmp = new HashMap<>();
        }
        this.clustersTmp.put(area.toString(), area.getClusters());
    }

    public Set<PlotCluster> getClusters(final @NonNull String world) {
        final Set<PlotCluster> set = new HashSet<>();
        for (final PlotArea area : this.getPlotAreaManager().getPlotAreasSet(world)) {
            set.addAll(area.getClusters());
        }
        return Collections.unmodifiableSet(set);

    }

    public List<Plot> sortPlotsByTemp(Collection<Plot> plots) {
        int max = 0;
        int overflowCount = 0;
        for (Plot plot : plots) {
            if (plot.temp > 0) {
                if (plot.temp > max) {
                    max = plot.temp;
                }
            } else {
                overflowCount++;
            }
        }
        Plot[] array = new Plot[max + 1];
        List<Plot> overflow = new ArrayList<>(overflowCount);
        for (Plot plot : plots) {
            if (plot.temp <= 0) {
                overflow.add(plot);
            } else {
                array[plot.temp] = plot;
            }
        }
        ArrayList<Plot> result = new ArrayList<>(plots.size());
        for (Plot plot : array) {
            if (plot != null) {
                result.add(plot);
            }
        }
        overflow.sort(Comparator.comparingInt(Plot::hashCode));
        result.addAll(overflow);
        return result;
    }

    /**
     * Sort plots by hashcode.
     *
     * @param plots the collection of plots to sort
     * @return the sorted collection
     */
    private ArrayList<Plot> sortPlotsByHash(Collection<Plot> plots) {
        int hardmax = 256000;
        int max = 0;
        int overflowSize = 0;
        for (Plot plot : plots) {
            int hash = MathMan.getPositiveId(plot.hashCode());
            if (hash > max) {
                if (hash >= hardmax) {
                    overflowSize++;
                } else {
                    max = hash;
                }
            }
        }
        hardmax = Math.min(hardmax, max);
        Plot[] cache = new Plot[hardmax + 1];
        List<Plot> overflow = new ArrayList<>(overflowSize);
        ArrayList<Plot> extra = new ArrayList<>();
        for (Plot plot : plots) {
            int hash = MathMan.getPositiveId(plot.hashCode());
            if (hash < hardmax) {
                if (hash >= 0) {
                    cache[hash] = plot;
                } else {
                    extra.add(plot);
                }
            } else if (Math.abs(plot.getId().getX()) > 15446 || Math.abs(plot.getId().getY()) > 15446) {
                extra.add(plot);
            } else {
                overflow.add(plot);
            }
        }
        Plot[] overflowArray = overflow.toArray(new Plot[0]);
        sortPlotsByHash(overflowArray);
        ArrayList<Plot> result = new ArrayList<>(cache.length + overflowArray.length);
        for (Plot plot : cache) {
            if (plot != null) {
                result.add(plot);
            }
        }
        Collections.addAll(result, overflowArray);
        result.addAll(extra);
        return result;
    }

    /**
     * Unchecked, use {@link #sortPlots(Collection, SortType, PlotArea)} instead which will in turn call this.
     *
     * @param input an array of plots to sort
     */
    @SuppressWarnings("unchecked")
    private void sortPlotsByHash(final @NonNull Plot @NonNull [] input) {
        List<Plot>[] bucket = new ArrayList[32];
        Arrays.fill(bucket, new ArrayList<>());
        boolean maxLength = false;
        int placement = 1;
        while (!maxLength) {
            maxLength = true;
            for (Plot plot : input) {
                int tmp = MathMan.getPositiveId(plot.hashCode()) / placement;
                bucket[tmp & 31].add(plot);
                if (maxLength && tmp > 0) {
                    maxLength = false;
                }
            }
            int a = 0;
            for (int i = 0; i < 32; i++) {
                for (Plot plot : bucket[i]) {
                    input[a++] = plot;
                }
                bucket[i].clear();
            }
            placement *= 32;
        }
    }

    private @NonNull List<Plot> sortPlotsByTimestamp(final @NonNull Collection<Plot> plots) {
        int hardMax = 256000;
        int max = 0;
        int overflowSize = 0;
        for (final Plot plot : plots) {
            int hash = MathMan.getPositiveId(plot.hashCode());
            if (hash > max) {
                if (hash >= hardMax) {
                    overflowSize++;
                } else {
                    max = hash;
                }
            }
        }
        hardMax = Math.min(hardMax, max);
        Plot[] cache = new Plot[hardMax + 1];
        List<Plot> overflow = new ArrayList<>(overflowSize);
        ArrayList<Plot> extra = new ArrayList<>();
        for (Plot plot : plots) {
            int hash = MathMan.getPositiveId(plot.hashCode());
            if (hash < hardMax) {
                if (hash >= 0) {
                    cache[hash] = plot;
                } else {
                    extra.add(plot);
                }
            } else if (Math.abs(plot.getId().getX()) > 15446 || Math.abs(plot.getId().getY()) > 15446) {
                extra.add(plot);
            } else {
                overflow.add(plot);
            }
        }
        Plot[] overflowArray = overflow.toArray(new Plot[0]);
        sortPlotsByHash(overflowArray);
        ArrayList<Plot> result = new ArrayList<>(cache.length + overflowArray.length);
        for (Plot plot : cache) {
            if (plot != null) {
                result.add(plot);
            }
        }
        Collections.addAll(result, overflowArray);
        result.addAll(extra);
        return result;
    }

    /**
     * Sort plots by creation timestamp.
     *
     * @param input Plots to sort
     * @return Sorted list
     */
    private @NonNull List<Plot> sortPlotsByModified(final @NonNull Collection<Plot> input) {
        List<Plot> list;
        if (input instanceof List) {
            list = (List<Plot>) input;
        } else {
            list = new ArrayList<>(input);
        }
        ExpireManager expireManager = PlotSquared.platform().expireManager();
        list.sort(Comparator.comparingLong(a -> expireManager.getTimestamp(a.getOwnerAbs())));
        return list;
    }

    /**
     * Sort a collection of plots by world (with a priority world), then
     * by hashcode.
     *
     * @param plots        the plots to sort
     * @param type         The sorting method to use for each world (timestamp, or hash)
     * @param priorityArea Use null, "world", or "gibberish" if you
     *                     want default world order
     * @return ArrayList of plot
     */
    public @NonNull List<Plot> sortPlots(
            final @NonNull Collection<Plot> plots,
            final @NonNull SortType type,
            final @Nullable PlotArea priorityArea
    ) {
        // group by world
        // sort each
        HashMap<PlotArea, Collection<Plot>> map = new HashMap<>();
        int totalSize = Arrays.stream(this.getPlotAreaManager().getAllPlotAreas()).mapToInt(PlotArea::getPlotCount).sum();
        if (plots.size() == totalSize) {
            for (PlotArea area : getPlotAreaManager().getAllPlotAreas()) {
                map.put(area, area.getPlots());
            }
        } else {
            for (PlotArea area : getPlotAreaManager().getAllPlotAreas()) {
                map.put(area, new ArrayList<>(0));
            }
            Collection<Plot> lastList = null;
            PlotArea lastWorld = null;
            for (Plot plot : plots) {
                if (lastWorld == plot.getArea()) {
                    lastList.add(plot);
                } else {
                    lastWorld = plot.getArea();
                    lastList = map.get(lastWorld);
                    lastList.add(plot);
                }
            }
        }
        List<PlotArea> areas = Arrays.asList(getPlotAreaManager().getAllPlotAreas());
        areas.sort((a, b) -> {
            if (priorityArea != null) {
                if (a.equals(priorityArea)) {
                    return -1;
                } else if (b.equals(priorityArea)) {
                    return 1;
                }
            }
            return a.hashCode() - b.hashCode();
        });
        ArrayList<Plot> toReturn = new ArrayList<>(plots.size());
        for (PlotArea area : areas) {
            switch (type) {
                case CREATION_DATE -> toReturn.addAll(sortPlotsByTemp(map.get(area)));
                case CREATION_DATE_TIMESTAMP -> toReturn.addAll(sortPlotsByTimestamp(map.get(area)));
                case DISTANCE_FROM_ORIGIN -> toReturn.addAll(sortPlotsByHash(map.get(area)));
                case LAST_MODIFIED -> toReturn.addAll(sortPlotsByModified(map.get(area)));
                default -> {
                }
            }
        }
        return toReturn;
    }

    public void setPlots(final @NonNull Map<String, HashMap<PlotId, Plot>> plots) {
        if (this.plots_tmp == null) {
            this.plots_tmp = new HashMap<>();
        }
        for (final Entry<String, HashMap<PlotId, Plot>> entry : plots.entrySet()) {
            final String world = entry.getKey();
            final PlotArea plotArea = this.getPlotAreaManager().getPlotArea(world, null);
            if (plotArea == null) {
                Map<PlotId, Plot> map = this.plots_tmp.computeIfAbsent(world, k -> new HashMap<>());
                map.putAll(entry.getValue());
            } else {
                for (Plot plot : entry.getValue().values()) {
                    plot.setArea(plotArea);
                    plotArea.addPlot(plot);
                }
            }
        }
    }

    /**
     * Unregisters a plot from local memory without calling the database.
     *
     * @param plot      the plot to remove
     * @param callEvent If to call an event about the plot being removed
     * @return {@code true} if plot existed | {@code false} if it didn't
     */
    public boolean removePlot(
            final @NonNull Plot plot,
            final boolean callEvent
    ) {
        if (plot == null) {
            return false;
        }
        if (callEvent) {
            eventDispatcher.callDelete(plot);
        }
        if (plot.getArea().removePlot(plot.getId())) {
            PlotId last = (PlotId) plot.getArea().getMeta("lastPlot");
            int last_max = Math.max(Math.abs(last.getX()), Math.abs(last.getY()));
            int this_max = Math.max(Math.abs(plot.getId().getX()), Math.abs(plot.getId().getY()));
            if (this_max < last_max) {
                plot.getArea().setMeta("lastPlot", plot.getId());
            }
            if (callEvent) {
                eventDispatcher.callPostDelete(plot);
            }
            return true;
        }
        return false;
    }

    /**
     * This method is called by the PlotGenerator class normally.
     * <ul>
     * <li>Initializes the PlotArea and PlotManager classes
     * <li>Registers the PlotArea and PlotManager classes
     * <li>Loads (and/or generates) the PlotArea configuration
     * <li>Sets up the world border if configured
     * </ul>
     *
     * <p>If loading an augmented plot world:
     * <ul>
     * <li>Creates the AugmentedPopulator classes
     * <li>Injects the AugmentedPopulator classes if required
     * </ul>
     *
     * @param world         the world to load
     * @param baseGenerator The generator for that world, or null
     */
    public void loadWorld(
            final @NonNull String world,
            final @Nullable GeneratorWrapper<?> baseGenerator
    ) {
        if (world.equals("CheckingPlotSquaredGenerator")) {
            return;
        }
        // Don't check the return result -> breaks runtime loading of single plot areas on creation
        this.getPlotAreaManager().addWorld(world);
        Set<String> worlds;
        if (this.worldConfiguration.contains("worlds")) {
            worlds = this.worldConfiguration.getConfigurationSection("worlds").getKeys(false);
        } else {
            worlds = new HashSet<>();
        }
        String path = "worlds." + world;
        ConfigurationSection worldSection = this.worldConfiguration.getConfigurationSection(path);
        PlotAreaType type;
        if (worldSection != null) {
            type = ConfigurationUtil.getType(worldSection);
        } else {
            type = PlotAreaType.NORMAL;
        }
        if (type == PlotAreaType.NORMAL) {
            if (getPlotAreaManager().getPlotAreas(world, null).length != 0) {
                return;
            }
            IndependentPlotGenerator plotGenerator;
            if (baseGenerator != null && baseGenerator.isFull()) {
                plotGenerator = baseGenerator.getPlotGenerator();
            } else if (worldSection != null) {
                String secondaryGeneratorName = worldSection.getString("generator.plugin");
                GeneratorWrapper<?> secondaryGenerator =
                        this.platform.getGenerator(world, secondaryGeneratorName);
                if (secondaryGenerator != null && secondaryGenerator.isFull()) {
                    plotGenerator = secondaryGenerator.getPlotGenerator();
                } else {
                    String primaryGeneratorName = worldSection.getString("generator.init");
                    GeneratorWrapper<?> primaryGenerator =
                            this.platform.getGenerator(world, primaryGeneratorName);
                    if (primaryGenerator != null && primaryGenerator.isFull()) {
                        plotGenerator = primaryGenerator.getPlotGenerator();
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
            // Conventional plot generator
            PlotArea plotArea = plotGenerator.getNewPlotArea(world, null, null, null);
            PlotManager plotManager = plotArea.getPlotManager();
            LOGGER.info("Detected world load for '{}'", world);
            LOGGER.info("- generator: {}>{}", baseGenerator, plotGenerator);
            LOGGER.info("- plot world: {}", plotArea.getClass().getCanonicalName());
            LOGGER.info("- plot area manager: {}", plotManager.getClass().getCanonicalName());
            if (!this.worldConfiguration.contains(path)) {
                this.worldConfiguration.createSection(path);
                worldSection = this.worldConfiguration.getConfigurationSection(path);
            }
            plotArea.saveConfiguration(worldSection);
            plotArea.loadDefaultConfiguration(worldSection);
            try {
                this.worldConfiguration.save(this.worldsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Now add it
            addPlotArea(plotArea);
            plotGenerator.initialize(plotArea);
        } else {
            if (!worlds.contains(world)) {
                return;
            }
            ConfigurationSection areasSection = worldSection.getConfigurationSection("areas");
            if (areasSection == null) {
                if (getPlotAreaManager().getPlotAreas(world, null).length != 0) {
                    return;
                }
                LOGGER.info("Detected world load for '{}'", world);
                String gen_string = worldSection.getString("generator.plugin", platform.pluginName());
                if (type == PlotAreaType.PARTIAL) {
                    Set<PlotCluster> clusters =
                            this.clustersTmp != null ? this.clustersTmp.get(world) : new HashSet<>();
                    if (clusters == null) {
                        throw new IllegalArgumentException("No cluster exists for world: " + world);
                    }
                    ArrayDeque<PlotArea> toLoad = new ArrayDeque<>();
                    for (PlotCluster cluster : clusters) {
                        PlotId pos1 = cluster.getP1(); // Cluster pos1
                        PlotId pos2 = cluster.getP2(); // Cluster pos2
                        String name = cluster.getName(); // Cluster name
                        String fullId = name + "-" + pos1 + "-" + pos2;
                        worldSection.createSection("areas." + fullId);
                        DBFunc.replaceWorld(world, world + ";" + name, pos1, pos2); // NPE
                        LOGGER.info("- {}-{}-{}", name, pos1, pos2);
                        GeneratorWrapper<?> areaGen = this.platform.getGenerator(world, gen_string);
                        if (areaGen == null) {
                            throw new IllegalArgumentException("Invalid Generator: " + gen_string);
                        }
                        PlotArea pa =
                                areaGen.getPlotGenerator().getNewPlotArea(world, name, pos1, pos2);
                        pa.saveConfiguration(worldSection);
                        pa.loadDefaultConfiguration(worldSection);
                        try {
                            this.worldConfiguration.save(this.worldsFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        LOGGER.info("| generator: {}>{}", baseGenerator, areaGen);
                        LOGGER.info("| plot world: {}", pa.getClass().getCanonicalName());
                        LOGGER.info("| manager: {}", pa.getPlotManager().getClass().getCanonicalName());
                        LOGGER.info("Note: Area created for cluster '{}' (invalid or old configuration?)", name);
                        areaGen.getPlotGenerator().initialize(pa);
                        areaGen.augment(pa);
                        toLoad.add(pa);
                    }
                    for (PlotArea area : toLoad) {
                        addPlotArea(area);
                    }
                    return;
                }
                GeneratorWrapper<?> areaGen = this.platform.getGenerator(world, gen_string);
                if (areaGen == null) {
                    throw new IllegalArgumentException("Invalid Generator: " + gen_string);
                }
                PlotArea pa = areaGen.getPlotGenerator().getNewPlotArea(world, null, null, null);
                LOGGER.info("- generator: {}>{}", baseGenerator, areaGen);
                LOGGER.info("- plot world: {}", pa.getClass().getCanonicalName());
                LOGGER.info("- plot area manager: {}", pa.getPlotManager().getClass().getCanonicalName());
                if (!this.worldConfiguration.contains(path)) {
                    this.worldConfiguration.createSection(path);
                    worldSection = this.worldConfiguration.getConfigurationSection(path);
                }
                pa.saveConfiguration(worldSection);
                pa.loadDefaultConfiguration(worldSection);
                try {
                    this.worldConfiguration.save(this.worldsFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                areaGen.getPlotGenerator().initialize(pa);
                areaGen.augment(pa);
                addPlotArea(pa);
                return;
            }
            if (type == PlotAreaType.AUGMENTED) {
                throw new IllegalArgumentException(
                        "Invalid type for multi-area world. Expected `PARTIAL`, got `"
                                + PlotAreaType.AUGMENTED + "`");
            }
            for (String areaId : areasSection.getKeys(false)) {
                LOGGER.info("- {}", areaId);
                String[] split = areaId.split("(?<=[^;-])-");
                if (split.length != 3) {
                    throw new IllegalArgumentException("Invalid Area identifier: " + areaId
                            + ". Expected form `<name>-<pos1>-<pos2>`");
                }
                String name = split[0];
                PlotId pos1 = PlotId.fromString(split[1]);
                PlotId pos2 = PlotId.fromString(split[2]);
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Invalid Area identifier: " + areaId
                            + ". Expected form `<name>-<x1;z1>-<x2;z2>`");
                }
                final PlotArea existing = this.getPlotAreaManager().getPlotArea(world, name);
                if (existing != null && name.equals(existing.getId())) {
                    continue;
                }
                ConfigurationSection section = areasSection.getConfigurationSection(areaId);
                YamlConfiguration clone = new YamlConfiguration();
                for (String key : section.getKeys(true)) {
                    if (section.get(key) instanceof MemorySection) {
                        continue;
                    }
                    if (!clone.contains(key)) {
                        clone.set(key, section.get(key));
                    }
                }
                for (String key : worldSection.getKeys(true)) {
                    if (worldSection.get(key) instanceof MemorySection) {
                        continue;
                    }
                    if (!key.startsWith("areas") && !clone.contains(key)) {
                        clone.set(key, worldSection.get(key));
                    }
                }
                String gen_string = clone.getString("generator.plugin", platform.pluginName());
                GeneratorWrapper<?> areaGen = this.platform.getGenerator(world, gen_string);
                if (areaGen == null) {
                    throw new IllegalArgumentException("Invalid Generator: " + gen_string);
                }
                PlotArea pa = areaGen.getPlotGenerator().getNewPlotArea(world, name, pos1, pos2);
                pa.saveConfiguration(clone);
                // netSections is the combination of
                for (String key : clone.getKeys(true)) {
                    if (clone.get(key) instanceof MemorySection) {
                        continue;
                    }
                    if (!worldSection.contains(key)) {
                        worldSection.set(key, clone.get(key));
                    } else {
                        Object value = worldSection.get(key);
                        if (!Objects.equals(value, clone.get(key))) {
                            section.set(key, clone.get(key));
                        }
                    }
                }
                pa.loadDefaultConfiguration(clone);
                try {
                    this.worldConfiguration.save(this.worldsFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LOGGER.info("Detected area load for '{}'", world);
                LOGGER.info("| generator: {}>{}", baseGenerator, areaGen);
                LOGGER.info("| plot world: {}", pa);
                LOGGER.info("| manager: {}", pa.getPlotManager());
                areaGen.getPlotGenerator().initialize(pa);
                areaGen.augment(pa);
                addPlotArea(pa);
            }
        }
    }

    /**
     * Setup the configuration for a plot world based on world arguments.
     * <p>
     *
     * <i>e.g. /mv create &lt;world&gt; normal -g PlotSquared:&lt;args&gt;</i>
     *
     * @param world     The name of the world
     * @param args      The arguments
     * @param generator the plot generator
     * @return boolean | if valid arguments were provided
     */
    public boolean setupPlotWorld(
            final @NonNull String world,
            final @Nullable String args,
            final @NonNull IndependentPlotGenerator generator
    ) {
        if (args != null && !args.isEmpty()) {
            // save configuration

            final List<String> validArguments = Arrays
                    .asList("s=", "size=", "g=", "gap=", "h=", "height=", "minh=", "minheight=", "maxh=", "maxheight=",
                            "f=", "floor=", "m=", "main=", "w=", "wall=", "b=", "border="
                    );

            // Calculate the number of expected arguments
            int expected = (int) validArguments.stream()
                    .filter(validArgument -> args.toLowerCase(Locale.ENGLISH).contains(validArgument))
                    .count();

            String[] split = args.toLowerCase(Locale.ENGLISH).split(",(?![^\\(\\[]*[\\]\\)])");

            if (split.length > expected) {
                // This means we have multi-block block buckets
                String[] combinedArgs = new String[expected];
                int index = 0;

                StringBuilder argBuilder = new StringBuilder();
                outer:
                for (final String string : split) {
                    for (final String validArgument : validArguments) {
                        if (string.contains(validArgument)) {
                            if (!argBuilder.toString().isEmpty()) {
                                combinedArgs[index++] = argBuilder.toString();
                                argBuilder = new StringBuilder();
                            }
                            argBuilder.append(string);
                            continue outer;
                        }
                    }
                    if (argBuilder.toString().charAt(argBuilder.length() - 1) != '=') {
                        argBuilder.append(",");
                    }
                    argBuilder.append(string);
                }

                if (!argBuilder.toString().isEmpty()) {
                    combinedArgs[index] = argBuilder.toString();
                }

                split = combinedArgs;
            }

            final HybridPlotWorldFactory hybridPlotWorldFactory = this.platform
                    .injector()
                    .getInstance(HybridPlotWorldFactory.class);
            final HybridPlotWorld plotWorld = hybridPlotWorldFactory.create(world, null, generator, null, null);

            for (String element : split) {
                String[] pair = element.split("=");
                if (pair.length != 2) {
                    LOGGER.error("No value provided for '{}'", element);
                    return false;
                }
                String key = pair[0].toLowerCase();
                String value = pair[1];
                try {
                    String base = "worlds." + world + ".";
                    switch (key) {
                        case "s", "size" -> this.worldConfiguration.set(
                                base + "plot.size",
                                ConfigurationUtil.INTEGER.parseString(value).shortValue()
                        );
                        case "g", "gap" -> this.worldConfiguration.set(
                                base + "road.width",
                                ConfigurationUtil.INTEGER.parseString(value).shortValue()
                        );
                        case "h", "height" -> {
                            this.worldConfiguration.set(
                                    base + "road.height",
                                    ConfigurationUtil.INTEGER.parseString(value).shortValue()
                            );
                            this.worldConfiguration.set(
                                    base + "plot.height",
                                    ConfigurationUtil.INTEGER.parseString(value).shortValue()
                            );
                            this.worldConfiguration.set(
                                    base + "wall.height",
                                    ConfigurationUtil.INTEGER.parseString(value).shortValue()
                            );
                        }
                        case "minh", "minheight" -> this.worldConfiguration.set(
                                base + "world.min_gen_height",
                                ConfigurationUtil.INTEGER.parseString(value).shortValue()
                        );
                        case "maxh", "maxheight" -> this.worldConfiguration.set(
                                base + "world.max_gen_height",
                                ConfigurationUtil.INTEGER.parseString(value).shortValue()
                        );
                        case "f", "floor" -> this.worldConfiguration.set(
                                base + "plot.floor",
                                ConfigurationUtil.BLOCK_BUCKET.parseString(value).toString()
                        );
                        case "m", "main" -> this.worldConfiguration.set(
                                base + "plot.filling",
                                ConfigurationUtil.BLOCK_BUCKET.parseString(value).toString()
                        );
                        case "w", "wall" -> this.worldConfiguration.set(
                                base + "wall.filling",
                                ConfigurationUtil.BLOCK_BUCKET.parseString(value).toString()
                        );
                        case "b", "border" -> this.worldConfiguration.set(
                                base + "wall.block",
                                ConfigurationUtil.BLOCK_BUCKET.parseString(value).toString()
                        );
                        default -> {
                            LOGGER.error("Key not found: {}", element);
                            return false;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Invalid value '{}' for arg '{}'", value, element);
                    e.printStackTrace();
                    return false;
                }
            }
            try {
                ConfigurationSection section =
                        this.worldConfiguration.getConfigurationSection("worlds." + world);
                plotWorld.saveConfiguration(section);
                plotWorld.loadDefaultConfiguration(section);
                this.worldConfiguration.save(this.worldsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Copies a file from inside the jar to a location
     *
     * @param file   Name of the file inside PlotSquared.jar
     * @param folder The output location relative to /plugins/PlotSquared/
     */
    public void copyFile(
            final @NonNull String file,
            final @NonNull String folder
    ) {
        try {
            File output = this.platform.getDirectory();
            if (!output.exists()) {
                output.mkdirs();
            }
            File newFile = FileUtils.getFile(output, folder + File.separator + file);
            if (newFile.exists()) {
                return;
            }
            try (InputStream stream = this.platform.getClass().getResourceAsStream(file)) {
                byte[] buffer = new byte[2048];
                if (stream == null) {
                    try (ZipInputStream zis = new ZipInputStream(
                            new FileInputStream(this.jarFile))) {
                        ZipEntry ze = zis.getNextEntry();
                        while (ze != null) {
                            String name = ze.getName();
                            if (name.equals(file)) {
                                new File(newFile.getParent()).mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                ze = null;
                            } else {
                                ze = zis.getNextEntry();
                            }
                        }
                        zis.closeEntry();
                    }
                    return;
                }
                newFile.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not save {}", file);
            e.printStackTrace();
        }
    }

    /**
     * Safely closes the database connection.
     */
    public void disable() {
        try {
            eventDispatcher.unregisterAll();
            checkRoadRegenPersistence();
            // Validate that all data in the db is correct
            final HashSet<Plot> plots = new HashSet<>();
            try {
                forEachPlotRaw(plots::add);
            } catch (final Exception ignored) {
            }
            DBFunc.validatePlots(plots);

            // Close the connection
            final DataSource dataSource = platform().injector().getInstance(Key.get(DataSource.class, PlotDatabase.class));
            if (dataSource instanceof Closeable closeable) {
                closeable.close();
            }
        } catch (IOException | NullPointerException throwable) {
            LOGGER.error("Could not close database connection", throwable);
            throwable.printStackTrace();
        }
    }

    /**
     * Handle road regen persistence
     */
    private void checkRoadRegenPersistence() {
        if (!HybridUtils.UPDATE || !Settings.Enabled_Components.PERSISTENT_ROAD_REGEN || (
                HybridUtils.regions.isEmpty() && HybridUtils.chunks.isEmpty())) {
            return;
        }
        LOGGER.info("Road regeneration incomplete. Saving incomplete regions to disk");
        LOGGER.info("- regions: {}", HybridUtils.regions.size());
        LOGGER.info("- chunks: {}", HybridUtils.chunks.size());
        ArrayList<int[]> regions = new ArrayList<>();
        ArrayList<int[]> chunks = new ArrayList<>();
        for (BlockVector2 r : HybridUtils.regions) {
            regions.add(new int[]{r.getBlockX(), r.getBlockZ()});
        }
        for (BlockVector2 c : HybridUtils.chunks) {
            chunks.add(new int[]{c.getBlockX(), c.getBlockZ()});
        }
        List<Object> list = new ArrayList<>();
        list.add(regions);
        list.add(chunks);
        list.add(HybridUtils.height);
        File file = new File(
                this.platform.getDirectory() + File.separator + "persistent_regen_data_" + HybridUtils.area
                        .getId() + "_" + HybridUtils.area.getWorldName());
        if (file.exists() && !file.delete()) {
            LOGGER.error("persistent_regene_data file already exists and could not be deleted");
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(
                Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE_NEW))) {
            oos.writeObject(list);
        } catch (IOException e) {
            LOGGER.error("Error creating persistent_region_data file", e);
        }
    }

    /**
     * Set up the database connection.
     */
    public void setupDatabase() {
        try {
            if (DBFunc.dbManager != null) {
                DBFunc.dbManager.close();
            }
            DBFunc.dbManager = platform().injector().getInstance(SQLManager.class);
            this.plots_tmp = DBFunc.getPlots();
            if (getPlotAreaManager() instanceof SinglePlotAreaManager) {
                SinglePlotArea area = ((SinglePlotAreaManager) getPlotAreaManager()).getArea();
                addPlotArea(area);
                ConfigurationSection section = worldConfiguration.getConfigurationSection("worlds.*");
                if (section == null) {
                    section = worldConfiguration.createSection("worlds.*");
                }
                area.saveConfiguration(section);
                area.loadDefaultConfiguration(section);
            }
            this.clustersTmp = DBFunc.getClusters();
            LOGGER.info("Connection to database established. Type: {}", Storage.MySQL.USE ? "MySQL" : "SQLite");
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to open database connection ({}). Disabling PlotSquared",
                    Storage.MySQL.USE ? "MySQL" : "SQLite"
            );
            LOGGER.error("==== Here is an ugly stacktrace, if you are interested in those things ===");
            LOGGER.error("", e);
            LOGGER.error("==== End of stacktrace ====");
            LOGGER.error(
                    "Please go to the {} 'storage.yml' and configure the database correctly",
                    platform.pluginName()
            );
            this.platform.shutdown(); //shutdown used instead of disable because of database error
        }
    }

    /**
     * Setup the default configuration.
     */
    public void setupConfig() {
        String lastVersionString = this.getConfig().getString("version");
        if (lastVersionString != null) {
            String[] split = lastVersionString.split("\\.");
            int[] lastVersion = new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1]),
                    Integer.parseInt(split[2])};
            if (checkVersion(new int[]{3, 4, 0}, lastVersion)) {
                Settings.convertLegacy(configFile);
                if (getConfig().contains("worlds")) {
                    ConfigurationSection worldSection =
                            getConfig().getConfigurationSection("worlds");
                    worldConfiguration.set("worlds", worldSection);
                    try {
                        worldConfiguration.save(worldsFile);
                    } catch (IOException e) {
                        LOGGER.error("Failed to save worlds.yml", e);
                        e.printStackTrace();
                    }
                }
                Settings.save(configFile);
            }
        }
        Settings.load(configFile);
        //Sets the version information for the settings.yml file
        try (InputStream stream = getClass().getResourceAsStream("/plugin.properties")) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
                String versionString = br.readLine();
                String commitString = br.readLine();
                String dateString = br.readLine();
                this.version = PlotVersion.tryParse(versionString, commitString, dateString);
            }
        } catch (IOException throwable) {
            throwable.printStackTrace();
        }
        Settings.save(configFile);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Setup all configuration files<br>
     * - Config: settings.yml<br>
     * - Storage: storage.yml<br>
     *
     * @return success or not
     */
    public boolean setupConfigs() {
        File folder = new File(this.platform.getDirectory(), "config");
        if (!folder.exists() && !folder.mkdirs()) {
            LOGGER.error("Failed to create the {} config folder. Please create it manually", this.platform.getDirectory());
        }
        try {
            this.worldsFile = new File(folder, "worlds.yml");
            if (!this.worldsFile.exists() && !this.worldsFile.createNewFile()) {
                LOGGER.error("Could not create the worlds file. Please create 'worlds.yml' manually");
            }
            this.worldConfiguration = YamlConfiguration.loadConfiguration(this.worldsFile);

            if (this.worldConfiguration.contains("worlds")) {
                if (!this.worldConfiguration.contains("configuration_version") || (
                        !this.worldConfiguration.getString("configuration_version")
                                .equalsIgnoreCase(LegacyConverter.CONFIGURATION_VERSION) && !this.worldConfiguration
                                .getString("configuration_version").equalsIgnoreCase("v5"))) {
                    // Conversion needed
                    LOGGER.info("A legacy configuration file was detected. Conversion will be attempted.");
                    try {
                        com.google.common.io.Files
                                .copy(this.worldsFile, new File(folder, "worlds.yml.old"));
                        LOGGER.info("A copy of worlds.yml has been saved in the file worlds.yml.old");
                        final ConfigurationSection worlds =
                                this.worldConfiguration.getConfigurationSection("worlds");
                        final LegacyConverter converter = new LegacyConverter(worlds);
                        converter.convert();
                        this.worldConfiguration.set("worlds", worlds);
                        this.setConfigurationVersion(LegacyConverter.CONFIGURATION_VERSION);
                        LOGGER.info(
                                "The conversion has finished. PlotSquared will now be disabled and the new configuration file will be used at next startup. Please review the new worlds.yml file. Please note that schematics will not be converted, as we are now using WorldEdit to handle schematics. You need to re-generate the schematics.");
                    } catch (final Exception e) {
                        LOGGER.error("Failed to convert the legacy configuration file. See stack trace for information.", e);
                    }
                    // Disable plugin
                    this.platform.shutdown();
                    return false;
                }
            } else {
                this.worldConfiguration.set("configuration_version", LegacyConverter.CONFIGURATION_VERSION);
            }
        } catch (IOException ignored) {
            LOGGER.error("Failed to save worlds.yml");
        }
        try {
            this.configFile = new File(folder, "settings.yml");
            if (!this.configFile.exists() && !this.configFile.createNewFile()) {
                LOGGER.error("Could not create the settings file. Please create 'settings.yml' manually");
            }
            this.config = YamlConfiguration.loadConfiguration(this.configFile);
            setupConfig();
        } catch (IOException ignored) {
            LOGGER.error("Failed to save settings.yml");
        }
        try {
            this.storageFile = new File(folder, "storage.yml");
            if (!this.storageFile.exists() && !this.storageFile.createNewFile()) {
                LOGGER.error("Could not create the storage settings file. Please create 'storage.yml' manually");
            }
            YamlConfiguration.loadConfiguration(this.storageFile);
            setupStorage();
        } catch (IOException ignored) {
            LOGGER.error("Failed to save storage.yml");
        }
        return true;
    }

    public @NonNull String getConfigurationVersion() {
        return this.worldConfiguration.get("configuration_version", LegacyConverter.CONFIGURATION_VERSION)
                .toString();
    }

    public void setConfigurationVersion(final @NonNull String newVersion) throws IOException {
        this.worldConfiguration.set("configuration_version", newVersion);
        this.worldConfiguration.save(this.worldsFile);
    }

    /**
     * Setup the storage file (load + save missing nodes).
     */
    private void setupStorage() {
        Storage.load(storageFile);
        Storage.save(storageFile);
        YamlConfiguration.loadConfiguration(storageFile);
    }

    /**
     * Show startup debug information.
     */
    private void showDebug() {
        if (Settings.DEBUG) {
            Map<String, Object> components = Settings.getFields(Settings.Enabled_Components.class);
            for (Entry<String, Object> component : components.entrySet()) {
                LOGGER.info("Key: {} | Value: {}", component.getKey(), component.getValue());
            }
        }
    }

    public void forEachPlotRaw(final @NonNull Consumer<Plot> consumer) {
        for (final PlotArea area : this.getPlotAreaManager().getAllPlotAreas()) {
            area.getPlots().forEach(consumer);
        }
        if (this.plots_tmp != null) {
            for (final HashMap<PlotId, Plot> entry : this.plots_tmp.values()) {
                entry.values().forEach(consumer);
            }
        }
    }

    /**
     * Check if the chunk uses vanilla/non-PlotSquared generation
     *
     * @param world            World name
     * @param chunkCoordinates Chunk coordinates
     * @return {@code true} if the chunk uses non-standard generation, {@code false} if not
     */
    public boolean isNonStandardGeneration(
            final @NonNull String world,
            final @NonNull BlockVector2 chunkCoordinates
    ) {
        final Location location = Location.at(world, chunkCoordinates.getBlockX() << 4, 64, chunkCoordinates.getBlockZ() << 4);
        final PlotArea area = getPlotAreaManager().getApplicablePlotArea(location);
        if (area == null) {
            return true;
        }
        return area.getTerrain() != PlotAreaTerrainType.NONE;
    }

    public @NonNull YamlConfiguration getConfig() {
        return config;
    }

    public @NonNull UUIDPipeline getImpromptuUUIDPipeline() {
        return this.impromptuUUIDPipeline;
    }

    public @NonNull UUIDPipeline getBackgroundUUIDPipeline() {
        return this.backgroundUUIDPipeline;
    }

    public @NonNull WorldEdit getWorldEdit() {
        return this.worldedit;
    }

    public @NonNull File getConfigFile() {
        return this.configFile;
    }

    public @NonNull File getWorldsFile() {
        return this.worldsFile;
    }

    public @NonNull YamlConfiguration getWorldConfiguration() {
        return this.worldConfiguration;
    }

    /**
     * Get the caption map belonging to a namespace. If none exists, a dummy
     * caption map will be returned.
     * <p>
     * You can register a caption map by calling {@link #registerCaptionMap(String, CaptionMap)}
     * </p>
     *
     * @param namespace Namespace
     * @return Map instance
     */
    public @NonNull CaptionMap getCaptionMap(final @NonNull String namespace) {
        return this.captionMaps.computeIfAbsent(
                namespace.toLowerCase(Locale.ENGLISH),
                missingNamespace -> new DummyCaptionMap()
        );
    }

    /**
     * Register a caption map. The namespace needs to be equal to the namespace used for
     * the {@link TranslatableCaption}s inside the map.
     *
     * @param namespace  Namespace
     * @param captionMap Map instance
     */
    public void registerCaptionMap(
            final @NonNull String namespace,
            final @NonNull CaptionMap captionMap
    ) {
        if (namespace.equalsIgnoreCase(TranslatableCaption.DEFAULT_NAMESPACE)) {
            throw new IllegalArgumentException("Cannot replace default caption map");
        }
        this.captionMaps.put(namespace.toLowerCase(Locale.ENGLISH), captionMap);
    }

    public @NonNull EventDispatcher getEventDispatcher() {
        return this.eventDispatcher;
    }

    public @NonNull PlotListener getPlotListener() {
        return this.plotListener;
    }

    /**
     * Get if the {@link PlatformReadyEvent} has been sent by WorldEdit. There is no way to query this within WorldEdit itself.
     */
    public boolean isWeInitialised() {
        return weInitialised;
    }

    /**
     * Different ways of sorting {@link Plot plots}
     */
    public enum SortType {
        /**
         * Sort plots by their creation, using their index in the database
         */
        CREATION_DATE,
        /**
         * Sort plots by their creation timestamp
         */
        CREATION_DATE_TIMESTAMP,
        /**
         * Sort plots by when they were last modified
         */
        LAST_MODIFIED,
        /**
         * Sort plots based on their distance from the origin of the world
         */
        DISTANCE_FROM_ORIGIN
    }

    private final class WEPlatformReadyListener {

        @SuppressWarnings("unused")
        @Subscribe(priority = EventHandler.Priority.VERY_EARLY)
        public void onPlatformReady(PlatformReadyEvent event) {
            weInitialised = true;
            WorldEdit.getInstance().getEventBus().unregister(WEPlatformReadyListener.this);
        }

    }

}
