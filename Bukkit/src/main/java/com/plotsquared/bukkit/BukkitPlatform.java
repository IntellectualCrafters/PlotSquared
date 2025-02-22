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
package com.plotsquared.bukkit;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.plotsquared.bukkit.generator.BukkitPlotGenerator;
import com.plotsquared.bukkit.inject.BackupModule;
import com.plotsquared.bukkit.inject.BukkitModule;
import com.plotsquared.bukkit.inject.PermissionModule;
import com.plotsquared.bukkit.inject.WorldManagerModule;
import com.plotsquared.bukkit.listener.BlockEventListener;
import com.plotsquared.bukkit.listener.BlockEventListener117;
import com.plotsquared.bukkit.listener.ChunkListener;
import com.plotsquared.bukkit.listener.EntityEventListener;
import com.plotsquared.bukkit.listener.ProjectileEventListener1211;
import com.plotsquared.bukkit.listener.EntitySpawnListener;
import com.plotsquared.bukkit.listener.HighFreqBlockEventListener;
import com.plotsquared.bukkit.listener.PaperListener;
import com.plotsquared.bukkit.listener.PlayerEventListener;
import com.plotsquared.bukkit.listener.PlayerEventListener1201;
import com.plotsquared.bukkit.listener.ProjectileEventListener;
import com.plotsquared.bukkit.listener.ServerListener;
import com.plotsquared.bukkit.listener.SingleWorldListener;
import com.plotsquared.bukkit.listener.SpigotListener;
import com.plotsquared.bukkit.listener.WorldEvents;
import com.plotsquared.bukkit.placeholder.PAPIPlaceholders;
import com.plotsquared.bukkit.placeholder.PlaceholderFormatter;
import com.plotsquared.bukkit.player.BukkitPlayerManager;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.bukkit.util.BukkitWorld;
import com.plotsquared.bukkit.util.SetGenCB;
import com.plotsquared.bukkit.util.TranslationUpdateManager;
import com.plotsquared.bukkit.util.UpdateUtility;
import com.plotsquared.bukkit.util.task.BukkitTaskManager;
import com.plotsquared.bukkit.util.task.PaperTimeConverter;
import com.plotsquared.bukkit.util.task.SpigotTimeConverter;
import com.plotsquared.bukkit.uuid.EssentialsUUIDService;
import com.plotsquared.bukkit.uuid.LuckPermsUUIDService;
import com.plotsquared.bukkit.uuid.OfflinePlayerUUIDService;
import com.plotsquared.bukkit.uuid.PaperUUIDService;
import com.plotsquared.bukkit.uuid.SQLiteUUIDService;
import com.plotsquared.bukkit.uuid.SquirrelIdUUIDService;
import com.plotsquared.core.PlotPlatform;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.backup.BackupManager;
import com.plotsquared.core.components.ComponentPresetManager;
import com.plotsquared.core.configuration.ConfigurationNode;
import com.plotsquared.core.configuration.ConfigurationSection;
import com.plotsquared.core.configuration.ConfigurationUtil;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.Storage;
import com.plotsquared.core.configuration.caption.ChatFormatter;
import com.plotsquared.core.configuration.file.YamlConfiguration;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.events.RemoveRoadEntityEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.generator.GeneratorWrapper;
import com.plotsquared.core.generator.IndependentPlotGenerator;
import com.plotsquared.core.generator.SingleWorldGenerator;
import com.plotsquared.core.inject.annotations.BackgroundPipeline;
import com.plotsquared.core.inject.annotations.DefaultGenerator;
import com.plotsquared.core.inject.annotations.ImpromptuPipeline;
import com.plotsquared.core.inject.annotations.WorldConfig;
import com.plotsquared.core.inject.annotations.WorldFile;
import com.plotsquared.core.inject.modules.PlotSquaredModule;
import com.plotsquared.core.listener.PlotListener;
import com.plotsquared.core.listener.WESubscriber;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotAreaTerrainType;
import com.plotsquared.core.plot.PlotAreaType;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.comment.CommentManager;
import com.plotsquared.core.plot.flag.implementations.ServerPlotFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.plot.world.SinglePlotArea;
import com.plotsquared.core.plot.world.SinglePlotAreaManager;
import com.plotsquared.core.setup.PlotAreaBuilder;
import com.plotsquared.core.setup.SettingsNodesWrapper;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.FileUtils;
import com.plotsquared.core.util.PlatformWorldManager;
import com.plotsquared.core.util.PlayerManager;
import com.plotsquared.core.util.PremiumVerification;
import com.plotsquared.core.util.ReflectionUtils;
import com.plotsquared.core.util.SetupUtils;
import com.plotsquared.core.util.WorldUtil;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.core.uuid.CacheUUIDService;
import com.plotsquared.core.uuid.UUIDPipeline;
import com.plotsquared.core.uuid.offline.OfflineModeUUIDService;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.serverlib.ServerLib;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.plotsquared.core.util.PremiumVerification.getDownloadID;
import static com.plotsquared.core.util.PremiumVerification.getResourceID;
import static com.plotsquared.core.util.PremiumVerification.getUserID;
import static com.plotsquared.core.util.ReflectionUtils.getRefClass;

@SuppressWarnings("unused")
@Singleton
public final class BukkitPlatform extends JavaPlugin implements Listener, PlotPlatform<Player> {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + BukkitPlatform.class.getSimpleName());
    private static final int BSTATS_ID = 1404;

    static {
        try {
            Settings.load(new File(PlotSquared.platform().getDirectory(), "settings.yml"));
        } catch (Throwable ignored) {
        }
    }

    private int[] version;
    private String pluginName;
    private SingleWorldListener singleWorldListener;
    private Method methodUnloadChunk0;
    private boolean methodUnloadSetup = false;
    private boolean metricsStarted;
    private boolean faweHook = false;

    private Injector injector;

    @Inject
    private PlotAreaManager plotAreaManager;
    @Inject
    private EventDispatcher eventDispatcher;
    @Inject
    private PlotListener plotListener;
    @Inject
    @WorldConfig
    private YamlConfiguration worldConfiguration;
    @Inject
    @WorldFile
    private File worldfile;
    @Inject
    private BukkitPlayerManager playerManager;
    @Inject
    private BackupManager backupManager;
    @Inject
    @ImpromptuPipeline
    private UUIDPipeline impromptuPipeline;
    @Inject
    @BackgroundPipeline
    private UUIDPipeline backgroundPipeline;
    @Inject
    private PlatformWorldManager<World> worldManager;
    private Locale serverLocale;

    @SuppressWarnings("StringSplitter")
    @Override
    public int @NonNull [] serverVersion() {
        if (this.version == null) {
            try {
                this.version = new int[3];
                String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
                this.version[0] = Integer.parseInt(split[0]);
                this.version[1] = Integer.parseInt(split[1]);
                if (split.length == 3) {
                    this.version[2] = Integer.parseInt(split[2]);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new int[]{1, 13, 0};
            }
        }
        return this.version;
    }

    @Override
    public int versionMinHeight() {
        return serverVersion()[1] >= 18 ? -64 : 0;
    }

    @Override
    public int versionMaxHeight() {
        return serverVersion()[1] >= 18 ? 319 : 255;
    }

    @Override
    public @NonNull String serverImplementation() {
        return Bukkit.getVersion();
    }

    @Override
    @SuppressWarnings("deprecation") // Paper deprecation
    public void onEnable() {
        this.pluginName = getDescription().getName();

        final TaskTime.TimeConverter timeConverter;
        if (PaperLib.isPaper()) {
            timeConverter = new PaperTimeConverter();
        } else {
            timeConverter = new SpigotTimeConverter();
        }

        // Stuff that needs to be created before the PlotSquared instance
        PlotPlayer.registerConverter(Player.class, BukkitUtil::adapt);
        TaskManager.setPlatformImplementation(new BukkitTaskManager(this, timeConverter));

        final PlotSquared plotSquared = new PlotSquared(this, "Bukkit");

        // FastAsyncWorldEdit
        if (Settings.FAWE_Components.FAWE_HOOK) {
            Plugin fawe = getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (fawe != null) {
                try {
                    Class.forName("com.fastasyncworldedit.bukkit.regions.plotsquared.FaweQueueCoordinator");
                    faweHook = true;
                } catch (Exception ignored) {
                    LOGGER.error("Incompatible version of FastAsyncWorldEdit to enable hook, please upgrade: https://ci.athion" +
                            ".net/job/FastAsyncWorldEdit/");
                }
            }
        }

        // We create the injector after PlotSquared has been initialized, so that we have access
        // to generated instances and settings
        this.injector = Guice
                .createInjector(
                        Stage.PRODUCTION,
                        new PermissionModule(),
                        new WorldManagerModule(),
                        new PlotSquaredModule(),
                        new BukkitModule(this),
                        new BackupModule()
                );
        this.injector.injectMembers(this);

        try {
            this.injector.getInstance(TranslationUpdateManager.class).upgradeTranslationFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.serverLocale = Locale.forLanguageTag(Settings.Enabled_Components.DEFAULT_LOCALE);

        if (PremiumVerification.isPremium() && Settings.Enabled_Components.UPDATE_NOTIFICATIONS) {
            injector.getInstance(UpdateUtility.class).updateChecker();
        }

        if (PremiumVerification.isPremium()) {
            LOGGER.info("PlotSquared version licensed to Spigot user {}", getUserID());
            LOGGER.info("https://www.spigotmc.org/resources/{}", getResourceID());
            LOGGER.info("Download ID: {}", getDownloadID());
            LOGGER.info("Thanks for supporting us :)");
        } else {
            LOGGER.info("Couldn't verify purchase :(");
        }

        // Database
        if (Settings.Enabled_Components.DATABASE) {
            plotSquared.setupDatabase();
        }

        // Check if we need to convert old flag values, etc
        if (!plotSquared.getConfigurationVersion().equalsIgnoreCase("v5")) {
            // Perform upgrade
            if (DBFunc.dbManager.convertFlags()) {
                LOGGER.info("Flags were converted successfully!");
                // Update the config version
                try {
                    plotSquared.setConfigurationVersion("v5");
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Comments
        CommentManager.registerDefaultInboxes();

        // Do stuff that was previously done in PlotSquared
        // Kill entities
        if (Settings.Enabled_Components.KILL_ROAD_MOBS || Settings.Enabled_Components.KILL_ROAD_VEHICLES) {
            this.runEntityTask();
        }

        // WorldEdit
        if (Settings.Enabled_Components.WORLDEDIT_RESTRICTIONS) {
            try {
                WorldEdit.getInstance().getEventBus().register(this.injector().getInstance(WESubscriber.class));
                LOGGER.info("{} hooked into WorldEdit", this.pluginName());
            } catch (Throwable e) {
                LOGGER.error(
                        "Incompatible version of WorldEdit, please upgrade: https://builds.enginehub.org/job/worldedit?branch=master");
            }
        }

        if (Settings.Enabled_Components.EVENTS) {
            getServer().getPluginManager().registerEvents(injector().getInstance(PlayerEventListener.class), this);
            if ((serverVersion()[1] == 21 && serverVersion()[2] >= 1) || serverVersion()[1] > 21) {
                getServer().getPluginManager().registerEvents(injector().getInstance(ProjectileEventListener1211.class), this);
            }
            if ((serverVersion()[1] == 20 && serverVersion()[2] >= 1) || serverVersion()[1] > 20) {
                getServer().getPluginManager().registerEvents(injector().getInstance(PlayerEventListener1201.class), this);
            }
            getServer().getPluginManager().registerEvents(injector().getInstance(BlockEventListener.class), this);
            if (Settings.HIGH_FREQUENCY_LISTENER) {
                getServer().getPluginManager().registerEvents(injector().getInstance(HighFreqBlockEventListener.class), this);
            }
            if (serverVersion()[1] >= 17) {
                getServer().getPluginManager().registerEvents(injector().getInstance(BlockEventListener117.class), this);
            }
            getServer().getPluginManager().registerEvents(injector().getInstance(EntityEventListener.class), this);
            getServer().getPluginManager().registerEvents(injector().getInstance(ProjectileEventListener.class), this);
            getServer().getPluginManager().registerEvents(injector().getInstance(ServerListener.class), this);
            getServer().getPluginManager().registerEvents(injector().getInstance(EntitySpawnListener.class), this);
            if (PaperLib.isPaper() && Settings.Paper_Components.PAPER_LISTENERS) {
                getServer().getPluginManager().registerEvents(injector().getInstance(PaperListener.class), this);
            } else {
                getServer().getPluginManager().registerEvents(injector().getInstance(SpigotListener.class), this);
            }
            this.plotListener.startRunnable();
        }

        // Required
        getServer().getPluginManager().registerEvents(injector().getInstance(WorldEvents.class), this);
        if (Settings.Enabled_Components.CHUNK_PROCESSOR) {
            getServer().getPluginManager().registerEvents(injector().getInstance(ChunkListener.class), this);
        }

        // Commands
        if (Settings.Enabled_Components.COMMANDS) {
            this.registerCommands();
        }

        // Permissions
        this.permissionHandler().initialize();

        if (Settings.Enabled_Components.COMPONENT_PRESETS) {
            try {
                injector().getInstance(ComponentPresetManager.class);
            } catch (final Exception e) {
                LOGGER.error("Failed to initialize the preset system", e);
            }
        }

        // World generators:
        final ConfigurationSection section = this.worldConfiguration.getConfigurationSection("worlds");
        final WorldUtil worldUtil = injector().getInstance(WorldUtil.class);

        if (section != null) {
            for (String world : section.getKeys(false)) {
                if (world.equals("CheckingPlotSquaredGenerator")) {
                    continue;
                }
                if (worldUtil.isWorld(world)) {
                    this.setGenerator(world);
                }
            }
            TaskManager.runTaskLater(() -> {
                for (String world : section.getKeys(false)) {
                    if (world.equals("CheckingPlotSquaredGenerator")) {
                        continue;
                    }
                    if (!worldUtil.isWorld(world) && !world.equals("*")) {
                        if (Settings.DEBUG) {
                            LOGGER.warn(
                                    "`{}` was not properly loaded - {} will now try to load it properly",
                                    world,
                                    this.pluginName()
                            );
                            LOGGER.warn(
                                    "- Are you trying to delete this world? Remember to remove it from the worlds.yml, bukkit.yml and multiverse worlds.yml");
                            LOGGER.warn("- Your world management plugin may be faulty (or non existent)");
                            LOGGER.warn("- The named world is not a plot world");
                            LOGGER.warn("This message may also be a false positive and could be ignored.");
                        }
                        this.setGenerator(world);
                    }
                }
            }, TaskTime.ticks(1L));
        }

        plotSquared.startExpiryTasks();

        // Once the server has loaded force updating all generators known to PlotSquared
        TaskManager.runTaskLater(() -> PlotSquared.platform().setupUtils().updateGenerators(true), TaskTime.ticks(1L));

        // Services are accessed in order
        final CacheUUIDService cacheUUIDService = new CacheUUIDService(Settings.UUID.UUID_CACHE_SIZE);
        this.impromptuPipeline.registerService(cacheUUIDService);
        this.backgroundPipeline.registerService(cacheUUIDService);
        this.impromptuPipeline.registerConsumer(cacheUUIDService);
        this.backgroundPipeline.registerConsumer(cacheUUIDService);

        // Now, if the server is in offline mode we can only use profiles and direct UUID
        // access, and so we skip the player profile stuff as well as SquirrelID (Mojang lookups)
        if (Settings.UUID.OFFLINE) {
            final OfflineModeUUIDService offlineModeUUIDService = new OfflineModeUUIDService();
            this.impromptuPipeline.registerService(offlineModeUUIDService);
            this.backgroundPipeline.registerService(offlineModeUUIDService);
            LOGGER.info("(UUID) Using the offline mode UUID service");
        }

        if (Settings.UUID.SERVICE_BUKKIT) {
            final OfflinePlayerUUIDService offlinePlayerUUIDService = new OfflinePlayerUUIDService();
            this.impromptuPipeline.registerService(offlinePlayerUUIDService);
            this.backgroundPipeline.registerService(offlinePlayerUUIDService);
        }

        final SQLiteUUIDService sqLiteUUIDService = new SQLiteUUIDService("user_cache.db");

        final SQLiteUUIDService legacyUUIDService;
        if (Settings.UUID.LEGACY_DATABASE_SUPPORT && FileUtils
                .getFile(PlotSquared.platform().getDirectory(), "usercache.db")
                .exists()) {
            legacyUUIDService = new SQLiteUUIDService("usercache.db");
        } else {
            legacyUUIDService = null;
        }

        final LuckPermsUUIDService luckPermsUUIDService;
        if (Settings.UUID.SERVICE_LUCKPERMS && Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsUUIDService = new LuckPermsUUIDService();
            LOGGER.info("(UUID) Using LuckPerms as a complementary UUID service");
        } else {
            luckPermsUUIDService = null;
        }

        final EssentialsUUIDService essentialsUUIDService;
        if (Settings.UUID.SERVICE_ESSENTIALSX && Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            essentialsUUIDService = new EssentialsUUIDService();
            LOGGER.info("(UUID) Using EssentialsX as a complementary UUID service");
        } else {
            essentialsUUIDService = null;
        }

        if (!Settings.UUID.OFFLINE) {
            // If running Paper we'll also try to use their profiles
            if (Bukkit.getOnlineMode() && PaperLib.isPaper() && Settings.UUID.SERVICE_PAPER) {
                final PaperUUIDService paperUUIDService = new PaperUUIDService();
                this.impromptuPipeline.registerService(paperUUIDService);
                this.backgroundPipeline.registerService(paperUUIDService);
                LOGGER.info("(UUID) Using Paper as a complementary UUID service");
            }

            this.impromptuPipeline.registerService(sqLiteUUIDService);
            this.backgroundPipeline.registerService(sqLiteUUIDService);
            this.impromptuPipeline.registerConsumer(sqLiteUUIDService);
            this.backgroundPipeline.registerConsumer(sqLiteUUIDService);

            if (legacyUUIDService != null) {
                this.impromptuPipeline.registerService(legacyUUIDService);
                this.backgroundPipeline.registerService(legacyUUIDService);
            }

            // Plugin providers
            if (luckPermsUUIDService != null) {
                this.impromptuPipeline.registerService(luckPermsUUIDService);
                this.backgroundPipeline.registerService(luckPermsUUIDService);
            }
            if (essentialsUUIDService != null) {
                this.impromptuPipeline.registerService(essentialsUUIDService);
                this.backgroundPipeline.registerService(essentialsUUIDService);
            }

            if (Settings.UUID.IMPROMPTU_SERVICE_MOJANG_API) {
                final SquirrelIdUUIDService impromptuMojangService = new SquirrelIdUUIDService(Settings.UUID.IMPROMPTU_LIMIT);
                this.impromptuPipeline.registerService(impromptuMojangService);
            }
            final SquirrelIdUUIDService backgroundMojangService = new SquirrelIdUUIDService(Settings.UUID.BACKGROUND_LIMIT);
            this.backgroundPipeline.registerService(backgroundMojangService);
        } else {
            this.impromptuPipeline.registerService(sqLiteUUIDService);
            this.backgroundPipeline.registerService(sqLiteUUIDService);
            this.impromptuPipeline.registerConsumer(sqLiteUUIDService);
            this.backgroundPipeline.registerConsumer(sqLiteUUIDService);

            if (legacyUUIDService != null) {
                this.impromptuPipeline.registerService(legacyUUIDService);
                this.backgroundPipeline.registerService(legacyUUIDService);
            }
        }

        this.impromptuPipeline.storeImmediately("*", DBFunc.EVERYONE);

        if (Settings.UUID.BACKGROUND_CACHING_ENABLED) {
            this.startUuidCaching(sqLiteUUIDService, cacheUUIDService);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            injector.getInstance(PAPIPlaceholders.class).register();
            if (Settings.Enabled_Components.EXTERNAL_PLACEHOLDERS) {
                ChatFormatter.formatters.add(injector().getInstance(PlaceholderFormatter.class));
            }
            LOGGER.info("PlotSquared hooked into PlaceholderAPI");
        }

        this.startMetrics();

        if (Settings.Enabled_Components.WORLDS) {
            TaskManager.getPlatformImplementation().taskRepeat(this::unload, TaskTime.seconds(10L));
            try {
                singleWorldListener = injector().getInstance(SingleWorldListener.class);
                Bukkit.getPluginManager().registerEvents(singleWorldListener, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Clean up potential memory leak
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                for (final PlotPlayer<? extends Player> player : this.playerManager().getPlayers()) {
                    if (player.getPlatformPlayer() == null || !player.getPlatformPlayer().isOnline()) {
                        this.playerManager().removePlayer(player);
                    }
                }
            } catch (final Exception e) {
                getLogger().warning("Failed to clean up players: " + e.getMessage());
            }
        }, 100L, 100L);

        // Check if we are in a safe environment
        ServerLib.checkUnsafeForks();
    }

    private void unload() {
        if (!this.methodUnloadSetup) {
            this.methodUnloadSetup = true;
            try {
                ReflectionUtils.RefClass classCraftWorld = getRefClass("{cb}.CraftWorld");
                this.methodUnloadChunk0 = classCraftWorld.getRealClass().getDeclaredMethod(
                        "unloadChunk0",
                        int.class,
                        int.class,
                        boolean.class
                );
                this.methodUnloadChunk0.setAccessible(true);
            } catch (Throwable event) {
                event.printStackTrace();
            }
        }

        if (this.plotAreaManager instanceof SinglePlotAreaManager) {
            long start = System.currentTimeMillis();
            final SinglePlotArea area = ((SinglePlotAreaManager) this.plotAreaManager).getArea();

            outer:
            for (final World world : Bukkit.getWorlds()) {
                final String name = world.getName();
                final char char0 = name.charAt(0);
                if (!Character.isDigit(char0) && char0 != '-') {
                    continue;
                }

                if (!world.getPlayers().isEmpty()) {
                    continue;
                }

                PlotId id;
                try {
                    id = PlotId.fromString(name);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                final Plot plot = area.getOwnedPlot(id);
                if (plot != null) {
                    if (!plot.getFlag(ServerPlotFlag.class) || PlotSquared
                            .platform()
                            .playerManager()
                            .getPlayerIfExists(plot.getOwner()) == null) {
                        if (world.getKeepSpawnInMemory()) {
                            world.setKeepSpawnInMemory(false);
                            return;
                        }
                        final Chunk[] chunks = world.getLoadedChunks();
                        if (chunks.length == 0) {
                            if (!Bukkit.unloadWorld(world, true)) {
                                LOGGER.warn("Failed to unload {}", world.getName());
                            }
                            return;
                        } else {
                            int index = 0;
                            do {
                                final Chunk chunkI = chunks[index++];
                                boolean result;
                                if (methodUnloadChunk0 != null) {
                                    try {
                                        result = (boolean) methodUnloadChunk0.invoke(world, chunkI.getX(), chunkI.getZ(), true);
                                    } catch (Throwable e) {
                                        methodUnloadChunk0 = null;
                                        e.printStackTrace();
                                        continue outer;
                                    }
                                } else {
                                    result = world.unloadChunk(chunkI.getX(), chunkI.getZ(), true);
                                }
                                if (!result) {
                                    continue outer;
                                }
                                if (System.currentTimeMillis() - start > 5) {
                                    return;
                                }
                            } while (index < chunks.length);
                        }
                    }
                }
            }
        }
    }

    private void startUuidCaching(
            final @NonNull SQLiteUUIDService sqLiteUUIDService,
            final @NonNull CacheUUIDService cacheUUIDService
    ) {
        // Record all unique UUID's and put them into a queue
        final Set<UUID> uuidSet = new HashSet<>();
        PlotSquared.get().forEachPlotRaw(plot -> {
            uuidSet.add(plot.getOwnerAbs());
            uuidSet.addAll(plot.getMembers());
            uuidSet.addAll(plot.getTrusted());
            uuidSet.addAll(plot.getDenied());
        });
        final Queue<UUID> uuidQueue = new LinkedBlockingQueue<>(uuidSet);

        LOGGER.info("(UUID) {} UUIDs will be cached", uuidQueue.size());

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            // Begin by reading all the SQLite cache at once
            cacheUUIDService.accept(sqLiteUUIDService.getAll());
            // Now fetch names for all known UUIDs
            final int totalSize = uuidQueue.size();
            int read = 0;
            LOGGER.info("(UUID) PlotSquared will fetch UUIDs in groups of {}", Settings.UUID.BACKGROUND_LIMIT);
            final List<UUID> uuidList = new ArrayList<>(Settings.UUID.BACKGROUND_LIMIT);

            // Used to indicate that the second retrieval has been attempted
            boolean secondRun = false;

            while (!uuidQueue.isEmpty() || !uuidList.isEmpty()) {
                if (!uuidList.isEmpty() && secondRun) {
                    LOGGER.warn("(UUID) Giving up on last batch. Fetching new batch instead");
                    uuidList.clear();
                }
                if (uuidList.isEmpty()) {
                    // Retrieve the secondRun variable to indicate that we're retrieving a
                    // fresh batch
                    secondRun = false;
                    // Populate the request list
                    for (int i = 0; i < Settings.UUID.BACKGROUND_LIMIT && !uuidQueue.isEmpty(); i++) {
                        uuidList.add(uuidQueue.poll());
                        read++;
                    }
                } else {
                    // If the list isn't empty then this is a second run for
                    // an old batch, so we re-use the patch
                    secondRun = true;
                }
                try {
                    PlotSquared.get().getBackgroundUUIDPipeline().getNames(uuidList).get();
                    // Clear the list if we successfully index all the names
                    uuidList.clear();
                    // Print progress
                    final double percentage = ((double) read / (double) totalSize) * 100.0D;
                    if (Settings.DEBUG) {
                        LOGGER.info("(UUID) PlotSquared has cached {} of UUIDs", String.format("%.1f%%", percentage));
                    }
                } catch (final InterruptedException | ExecutionException e) {
                    LOGGER.error("(UUID) Failed to retrieve last batch. Will try again", e);
                }
            }
            LOGGER.info("(UUID) PlotSquared has cached all UUIDs");
        }, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        PlotSquared.get().disable();
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public void shutdown() {
        this.getServer().getPluginManager().disablePlugin(this);
    }

    @Override
    public void shutdownServer() {
        getServer().shutdown();
    }

    private void registerCommands() {
        final BukkitCommand bukkitCommand = new BukkitCommand();
        final PluginCommand plotCommand = getCommand("plots");
        if (plotCommand != null) {
            plotCommand.setExecutor(bukkitCommand);
            plotCommand.setAliases(Arrays.asList("p", "ps", "plotme", "plot"));
            plotCommand.setTabCompleter(bukkitCommand);
        }
    }

    @Override
    public @NonNull File getDirectory() {
        return getDataFolder();
    }

    @Override
    public @NonNull File worldContainer() {
        return Bukkit.getWorldContainer();
    }

    @SuppressWarnings("deprecation")
    private void runEntityTask() {
        TaskManager.runTaskRepeat(() -> this.plotAreaManager.forEachPlotArea(plotArea -> {
            final World world = Bukkit.getWorld(plotArea.getWorldName());
            try {
                if (world == null) {
                    return;
                }
                List<Entity> entities = world.getEntities();
                Iterator<Entity> iterator = entities.iterator();
                while (iterator.hasNext()) {
                    Entity entity = iterator.next();
                    //noinspection ConstantValue - getEntitySpawnReason annotated as NotNull, but is not NotNull. lol.
                    if (PaperLib.isPaper() && entity.getEntitySpawnReason() != null && "CUSTOM".equals(entity.getEntitySpawnReason().name())) {
                        continue;
                    }
                    // Fallback for Spigot not having Entity#getEntitySpawnReason
                    if (entity.getMetadata("ps_custom_spawned").stream().anyMatch(MetadataValue::asBoolean)) {
                        continue;
                    }
                    switch (entity.getType().toString()) {
                        case "EGG":
                        case "FISHING_HOOK":
                        case "ENDER_SIGNAL":
                        case "AREA_EFFECT_CLOUD":
                        case "EXPERIENCE_ORB":
                        case "LEASH_HITCH":
                        case "FIREWORK":
                        case "LIGHTNING":
                        case "WITHER_SKULL":
                        case "UNKNOWN":
                        case "PLAYER":
                            // non moving / unmovable
                            continue;
                        case "THROWN_EXP_BOTTLE":
                        case "SPLASH_POTION":
                        case "SNOWBALL":
                        case "SHULKER_BULLET":
                        case "SPECTRAL_ARROW":
                        case "ENDER_PEARL":
                        case "ARROW":
                        case "LLAMA_SPIT":
                        case "TRIDENT":
                            // managed elsewhere | projectile
                            continue;
                        case "ITEM_FRAME":
                        case "PAINTING":
                            // Not vehicles
                            continue;
                        case "ARMOR_STAND":
                            // Temporarily classify as vehicle
                        case "MINECART":
                        case "MINECART_CHEST":
                        case "CHEST_MINECART":
                        case "MINECART_COMMAND":
                        case "COMMAND_BLOCK_MINECART":
                        case "MINECART_FURNACE":
                        case "FURNACE_MINECART":
                        case "MINECART_HOPPER":
                        case "HOPPER_MINECART":
                        case "MINECART_MOB_SPAWNER":
                        case "SPAWNER_MINECART":
                        case "ENDER_CRYSTAL":
                        case "MINECART_TNT":
                        case "TNT_MINECART":
                        case "CHEST_BOAT":
                        case "BOAT":
                            if (Settings.Enabled_Components.KILL_ROAD_VEHICLES) {
                                com.plotsquared.core.location.Location location = BukkitUtil.adapt(entity.getLocation());
                                Plot plot = location.getPlot();
                                if (plot == null) {
                                    if (location.isPlotArea()) {
                                        if (entity.hasMetadata("ps-tmp-teleport")) {
                                            continue;
                                        }
                                        this.removeRoadEntity(entity, iterator);
                                    }
                                    continue;
                                }
                                List<MetadataValue> meta = entity.getMetadata("plot");
                                if (meta.isEmpty()) {
                                    continue;
                                }
                                Plot origin = (Plot) meta.get(0).value();
                                if (!plot.equals(origin.getBasePlot(false))) {
                                    if (entity.hasMetadata("ps-tmp-teleport")) {
                                        continue;
                                    }
                                    this.removeRoadEntity(entity, iterator);
                                }
                            }
                            continue;
                        case "SMALL_FIREBALL":
                        case "FIREBALL":
                        case "DRAGON_FIREBALL":
                        case "DROPPED_ITEM":
                            if (Settings.Enabled_Components.KILL_ROAD_ITEMS
                                    && plotArea.getOwnedPlotAbs(BukkitUtil.adapt(entity.getLocation())) == null) {
                                this.removeRoadEntity(entity, iterator);
                            }
                            // dropped item
                            continue;
                        case "PRIMED_TNT":
                        case "FALLING_BLOCK":
                            // managed elsewhere
                            continue;
                        case "SHULKER":
                            if (Settings.Enabled_Components.KILL_ROAD_MOBS && (Settings.Enabled_Components.KILL_NAMED_ROAD_MOBS || entity.getCustomName() == null)) {
                                LivingEntity livingEntity = (LivingEntity) entity;
                                List<MetadataValue> meta = entity.getMetadata("shulkerPlot");
                                if (!meta.isEmpty()) {
                                    if (livingEntity.isLeashed() && !Settings.Enabled_Components.KILL_OWNED_ROAD_MOBS) {
                                        continue;
                                    }
                                    if (entity.hasMetadata("keep")) {
                                        continue;
                                    }

                                    PlotId originalPlotId = (PlotId) meta.get(0).value();
                                    if (originalPlotId != null) {
                                        com.plotsquared.core.location.Location pLoc = BukkitUtil.adapt(entity.getLocation());
                                        PlotArea area = pLoc.getPlotArea();
                                        if (area != null) {
                                            Plot currentPlot = area.getPlotAbs(pLoc);
                                            if (currentPlot == null || !originalPlotId.equals(currentPlot.getId())) {
                                                if (entity.hasMetadata("ps-tmp-teleport")) {
                                                    continue;
                                                }
                                                this.removeRoadEntity(entity, iterator);
                                            }
                                        }
                                    }
                                } else {
                                    //This is to apply the metadata to already spawned shulkers (see EntitySpawnListener.java)
                                    com.plotsquared.core.location.Location pLoc = BukkitUtil.adapt(entity.getLocation());
                                    PlotArea area = pLoc.getPlotArea();
                                    if (area != null) {
                                        Plot currentPlot = area.getPlotAbs(pLoc);
                                        if (currentPlot != null) {
                                            entity.setMetadata(
                                                    "shulkerPlot",
                                                    new FixedMetadataValue((Plugin) PlotSquared.platform(), currentPlot.getId())
                                            );
                                        }
                                    }
                                }
                            }
                            continue;
                        case "ZOMBIFIED_PIGLIN":
                        case "PIGLIN_BRUTE":
                        case "LLAMA":
                        case "DONKEY":
                        case "MULE":
                        case "ZOMBIE_HORSE":
                        case "SKELETON_HORSE":
                        case "HUSK":
                        case "ELDER_GUARDIAN":
                        case "WITHER_SKELETON":
                        case "STRAY":
                        case "ZOMBIE_VILLAGER":
                        case "EVOKER":
                        case "EVOKER_FANGS":
                        case "VEX":
                        case "VINDICATOR":
                        case "POLAR_BEAR":
                        case "BAT":
                        case "BLAZE":
                        case "CAVE_SPIDER":
                        case "CHICKEN":
                        case "COW":
                        case "CREEPER":
                        case "ENDERMAN":
                        case "ENDERMITE":
                        case "ENDER_DRAGON":
                        case "GHAST":
                        case "GIANT":
                        case "GUARDIAN":
                        case "HORSE":
                        case "IRON_GOLEM":
                        case "MAGMA_CUBE":
                        case "MUSHROOM_COW":
                        case "OCELOT":
                        case "PIG":
                        case "PIG_ZOMBIE":
                        case "RABBIT":
                        case "SHEEP":
                        case "SILVERFISH":
                        case "SKELETON":
                        case "SLIME":
                        case "SNOWMAN":
                        case "SPIDER":
                        case "SQUID":
                        case "VILLAGER":
                        case "WITCH":
                        case "WITHER":
                        case "WOLF":
                        case "ZOMBIE":
                        case "PARROT":
                        case "SALMON":
                        case "DOLPHIN":
                        case "TROPICAL_FISH":
                        case "DROWNED":
                        case "COD":
                        case "TURTLE":
                        case "PUFFERFISH":
                        case "PHANTOM":
                        case "ILLUSIONER":
                        case "CAT":
                        case "PANDA":
                        case "FOX":
                        case "PILLAGER":
                        case "TRADER_LLAMA":
                        case "WANDERING_TRADER":
                        case "RAVAGER":
                        case "BEE":
                        case "HOGLIN":
                        case "PIGLIN":
                        case "ZOGLIN":
                        default: {
                            if (Settings.Enabled_Components.KILL_ROAD_MOBS) {
                                Location location = entity.getLocation();
                                if (BukkitUtil.adapt(location).isPlotRoad()) {
                                    if (entity instanceof LivingEntity livingEntity) {
                                        if ((Settings.Enabled_Components.KILL_OWNED_ROAD_MOBS || !livingEntity.isLeashed())
                                                || !entity.hasMetadata("keep")) {
                                            Entity passenger = entity.getPassenger();
                                            if ((Settings.Enabled_Components.KILL_OWNED_ROAD_MOBS
                                                    || !((passenger instanceof Player) || livingEntity.isLeashed()))
                                                    && (Settings.Enabled_Components.KILL_NAMED_ROAD_MOBS || entity.getCustomName() == null)
                                                    && entity.getMetadata("keep").isEmpty()) {
                                                if (entity.hasMetadata("ps-tmp-teleport")) {
                                                    continue;
                                                }
                                                this.removeRoadEntity(entity, iterator);
                                            }
                                        }
                                    } else {
                                        Entity passenger = entity.getPassenger();
                                        if ((Settings.Enabled_Components.KILL_OWNED_ROAD_MOBS || !(passenger instanceof Player))
                                                && (Settings.Enabled_Components.KILL_NAMED_ROAD_MOBS && entity.getCustomName() != null)
                                                && entity.getMetadata("keep").isEmpty()) {
                                            if (entity.hasMetadata("ps-tmp-teleport")) {
                                                continue;
                                            }
                                            this.removeRoadEntity(entity, iterator);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }), TaskTime.seconds(1L));
    }

    private void removeRoadEntity(Entity entity, Iterator<Entity> entityIterator) {
        RemoveRoadEntityEvent event = eventDispatcher.callRemoveRoadEntity(BukkitAdapter.adapt(entity));

        if (event.getEventResult() == Result.DENY) {
            return;
        }

        entityIterator.remove();
        entity.remove();
    }

    @Override
    public @Nullable
    final ChunkGenerator getDefaultWorldGenerator(
            final @NonNull String worldName,
            final @Nullable String id
    ) {
        final IndependentPlotGenerator result;
        if (id != null && id.equalsIgnoreCase("single")) {
            result = injector().getInstance(SingleWorldGenerator.class);
        } else {
            result = injector().getInstance(Key.get(IndependentPlotGenerator.class, DefaultGenerator.class));
            if (!PlotSquared.get().setupPlotWorld(worldName, id, result)) {
                return null;
            }
        }
        return (ChunkGenerator) result.specify(worldName);
    }

    @Override
    public @Nullable GeneratorWrapper<?> getGenerator(
            final @NonNull String world,
            final @Nullable String name
    ) {
        if (name == null) {
            return null;
        }
        final Plugin genPlugin = Bukkit.getPluginManager().getPlugin(name);
        if (genPlugin != null && genPlugin.isEnabled()) {
            ChunkGenerator gen = genPlugin.getDefaultWorldGenerator(world, "");
            if (gen instanceof GeneratorWrapper<?>) {
                return (GeneratorWrapper<?>) gen;
            }
            return new BukkitPlotGenerator(world, gen, this.plotAreaManager);
        } else {
            return new BukkitPlotGenerator(
                    world,
                    injector().getInstance(Key.get(IndependentPlotGenerator.class, DefaultGenerator.class)),
                    this.plotAreaManager
            );
        }
    }

    @Override
    public void startMetrics() {
        if (this.metricsStarted) {
            return;
        }
        this.metricsStarted = true;
        Metrics metrics = new Metrics(this, BSTATS_ID); // bstats
        metrics.addCustomChart(new DrilldownPie("area_types", () -> {
            final Map<String, Map<String, Integer>> map = new HashMap<>();
            for (final PlotAreaType plotAreaType : PlotAreaType.values()) {
                final Map<String, Integer> terrainTypes = new HashMap<>();
                for (final PlotAreaTerrainType plotAreaTerrainType : PlotAreaTerrainType.values()) {
                    terrainTypes.put(plotAreaTerrainType.name().toLowerCase(), 0);
                }
                map.put(plotAreaType.name().toLowerCase(), terrainTypes);
            }
            for (final PlotArea plotArea : this.plotAreaManager.getAllPlotAreas()) {
                final Map<String, Integer> terrainTypeMap = map.get(plotArea.getType().name().toLowerCase());
                terrainTypeMap.put(
                        plotArea.getTerrain().name().toLowerCase(),
                        terrainTypeMap.get(plotArea.getTerrain().name().toLowerCase()) + 1
                );
            }
            return map;
        }));
        metrics.addCustomChart(new SimplePie(
                "premium",
                () -> PremiumVerification.isPremium() ? "Premium" : "Non-Premium"
        ));
        metrics.addCustomChart(new SimplePie("worlds", () -> Settings.Enabled_Components.WORLDS ? "true" : "false"));
        metrics.addCustomChart(new SimplePie("economy", () -> Settings.Enabled_Components.ECONOMY ? "true" : "false"));
        metrics.addCustomChart(new SimplePie(
                "plot_expiry",
                () -> Settings.Enabled_Components.PLOT_EXPIRY ? "true" : "false"
        ));
        metrics.addCustomChart(new SimplePie("database_type", () -> Storage.MySQL.USE ? "MySQL" : "SQLite"));
        metrics.addCustomChart(new SimplePie(
                "worldedit_implementation",
                () -> Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null ? "FastAsyncWorldEdit" : "WorldEdit"
        ));
        metrics.addCustomChart(new SimplePie("offline_mode", () -> Settings.UUID.OFFLINE ? "true" : "false"));
        metrics.addCustomChart(new SimplePie("offline_mode_force", () -> Settings.UUID.FORCE_LOWERCASE ? "true" : "false"));
    }

    @Override
    public void unregister(final @NonNull PlotPlayer<?> player) {
        PlotSquared.platform().playerManager().removePlayer(player.getUUID());
    }

    @Override
    public void setGenerator(final @NonNull String worldName) {
        World world = BukkitUtil.getWorld(worldName);
        if (world == null) {
            // create world
            ConfigurationSection worldConfig = this.worldConfiguration.getConfigurationSection("worlds." + worldName);
            String manager = worldConfig.getString("generator.plugin", pluginName());
            PlotAreaBuilder builder =
                    PlotAreaBuilder.newBuilder().plotManager(manager).generatorName(worldConfig.getString(
                                    "generator.init",
                                    manager
                            ))
                            .plotAreaType(ConfigurationUtil.getType(worldConfig)).terrainType(ConfigurationUtil.getTerrain(
                                    worldConfig))
                            .settingsNodesWrapper(new SettingsNodesWrapper(new ConfigurationNode[0], null)).worldName(worldName);
            injector().getInstance(SetupUtils.class).setupWorld(builder);
            world = Bukkit.getWorld(worldName);
        } else {
            try {
                if (!this.plotAreaManager.hasPlotArea(worldName)) {
                    SetGenCB.setGenerator(BukkitUtil.getWorld(worldName));
                }
            } catch (final Exception e) {
                LOGGER.error("Failed to reload world: {} | {}", world, e.getMessage());
                Bukkit.getServer().unloadWorld(world, false);
                return;
            }
        }
        assert world != null;
        ChunkGenerator gen = world.getGenerator();
        if (gen instanceof BukkitPlotGenerator) {
            PlotSquared.get().loadWorld(worldName, (BukkitPlotGenerator) gen);
        } else if (gen != null) {
            PlotSquared.get().loadWorld(worldName, new BukkitPlotGenerator(worldName, gen, this.plotAreaManager));
        } else if (this.worldConfiguration.contains("worlds." + worldName)) {
            PlotSquared.get().loadWorld(worldName, null);
        }
    }

    @Override
    public @NonNull String serverNativePackage() {
        final String name = Bukkit.getServer().getClass().getPackage().getName();
        String ver = name.substring(name.lastIndexOf('.') + 1);
        // org.bukkit.craftbukkit is no longer suffixed by a version
        return ver.equals("craftbukkit") ? "" : ver;
    }

    @Override
    public @NonNull GeneratorWrapper<?> wrapPlotGenerator(
            final @NonNull String world,
            final @NonNull IndependentPlotGenerator generator
    ) {
        return new BukkitPlotGenerator(world, generator, this.plotAreaManager);
    }

    @SuppressWarnings("deprecation") // Paper deprecation
    @Override
    public @NonNull String pluginsFormatted() {
        StringBuilder msg = new StringBuilder();
        List<Plugin> plugins = new ArrayList<>();
        Collections.addAll(plugins, Bukkit.getServer().getPluginManager().getPlugins());
        plugins.sort(Comparator.comparing(Plugin::getName));
        msg.append("Plugins (").append(plugins.size()).append("): \n");
        for (Plugin p : plugins) {
            msg.append(" - ").append(p.getName()).append(":").append("\n")
                    .append("  • Version: ").append(p.getDescription().getVersion()).append("\n")
                    .append("  • Enabled: ").append(p.isEnabled()).append("\n")
                    .append("  • Main: ").append(p.getDescription().getMain()).append("\n")
                    .append("  • Authors: ").append(p.getDescription().getAuthors()).append("\n")
                    .append("  • Load Before: ").append(p.getDescription().getLoadBefore()).append("\n")
                    .append("  • Dependencies: ").append(p.getDescription().getDepend()).append("\n")
                    .append("  • Soft Dependencies: ").append(p.getDescription().getSoftDepend()).append("\n");
            List<RegisteredServiceProvider<?>> providers = Bukkit.getServicesManager().getRegistrations(p);
            if (!providers.isEmpty()) {
                msg.append("  • Provided Services: \n");
                for (RegisteredServiceProvider<?> provider : providers) {
                    msg.append("    • ")
                            .append(provider.getService().getName()).append(" = ")
                            .append(provider.getProvider().getClass().getName())
                            .append(" (priority: ").append(provider.getPriority()).append(")")
                            .append("\n");
                }
            }
        }
        return msg.toString();
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "deprecation"}) // Paper deprecation
    public @NonNull String worldEditImplementations() {
        StringBuilder msg = new StringBuilder();
        if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            msg.append("FastAsyncWorldEdit: ").append(Bukkit
                    .getPluginManager()
                    .getPlugin("FastAsyncWorldEdit")
                    .getDescription()
                    .getVersion());
        } else if (Bukkit.getPluginManager().getPlugin("AsyncWorldEdit") != null) {
            msg.append("AsyncWorldEdit: ").append(Bukkit
                    .getPluginManager()
                    .getPlugin("AsyncWorldEdit")
                    .getDescription()
                    .getVersion()).append("\n");
            msg.append("WorldEdit: ").append(Bukkit.getPluginManager().getPlugin("WorldEdit").getDescription().getVersion());
        } else {
            msg.append("WorldEdit: ").append(Bukkit.getPluginManager().getPlugin("WorldEdit").getDescription().getVersion());
        }
        return msg.toString();
    }

    @Override
    public com.plotsquared.core.location.@NonNull World<?> getPlatformWorld(final @NonNull String worldName) {
        return BukkitWorld.of(worldName);
    }

    @Override
    public @NonNull Audience consoleAudience() {
        return BukkitUtil.BUKKIT_AUDIENCES.console();
    }

    @Override
    public @NonNull String pluginName() {
        return this.pluginName;
    }

    public SingleWorldListener getSingleWorldListener() {
        return this.singleWorldListener;
    }

    @Override
    public @NonNull Injector injector() {
        return this.injector;
    }

    @Override
    public @NonNull PlotAreaManager plotAreaManager() {
        return this.plotAreaManager;
    }

    @NonNull
    @Override
    public Locale getLocale() {
        return this.serverLocale;
    }

    @Override
    public void setLocale(final @NonNull Locale locale) {
        throw new UnsupportedOperationException("Cannot replace server locale");
    }

    @Override
    public @NonNull PlatformWorldManager<?> worldManager() {
        return this.worldManager;
    }

    @Override
    @NonNull
    public PlayerManager<? extends PlotPlayer<Player>, ? extends Player> playerManager() {
        return this.playerManager;
    }

    @Override
    public void copyCaptionMaps() {
        /* Make this prettier at some point */
        final String[] languages = new String[]{"en"};
        for (final String language : languages) {
            if (!new File(new File(this.getDataFolder(), "lang"), String.format("messages_%s.json", language)).exists()) {
                this.saveResource(String.format("lang/messages_%s.json", language), false);
                LOGGER.info("Copied language file 'messages_{}.json'", language);
            }
        }
    }

    @NonNull
    @Override
    public String toLegacyPlatformString(final @NonNull Component component) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }

    @Override
    public boolean isFaweHooking() {
        return faweHook;
    }

}
