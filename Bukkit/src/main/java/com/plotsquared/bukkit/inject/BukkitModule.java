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
package com.plotsquared.bukkit.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.plotsquared.bukkit.BukkitPlatform;
import com.plotsquared.bukkit.listener.ServerListener;
import com.plotsquared.bukkit.listener.SingleWorldListener;
import com.plotsquared.bukkit.player.BukkitPlayerManager;
import com.plotsquared.bukkit.queue.BukkitChunkCoordinator;
import com.plotsquared.bukkit.queue.BukkitQueueCoordinator;
import com.plotsquared.bukkit.schematic.BukkitSchematicHandler;
import com.plotsquared.bukkit.util.BukkitChunkManager;
import com.plotsquared.bukkit.util.BukkitInventoryUtil;
import com.plotsquared.bukkit.util.BukkitRegionManager;
import com.plotsquared.bukkit.util.BukkitSetupUtils;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.bukkit.util.fawe.FaweRegionManager;
import com.plotsquared.bukkit.util.fawe.FaweSchematicHandler;
import com.plotsquared.core.PlotPlatform;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.generator.HybridGen;
import com.plotsquared.core.generator.IndependentPlotGenerator;
import com.plotsquared.core.inject.annotations.ConsoleActor;
import com.plotsquared.core.inject.annotations.DefaultGenerator;
import com.plotsquared.core.inject.factory.ChunkCoordinatorBuilderFactory;
import com.plotsquared.core.inject.factory.ChunkCoordinatorFactory;
import com.plotsquared.core.inject.factory.HybridPlotWorldFactory;
import com.plotsquared.core.inject.factory.ProgressSubscriberFactory;
import com.plotsquared.core.player.OfflinePlotPlayer;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.world.DefaultPlotAreaManager;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.plot.world.SinglePlotAreaManager;
import com.plotsquared.core.queue.ChunkCoordinator;
import com.plotsquared.core.queue.GlobalBlockQueue;
import com.plotsquared.core.queue.QueueProvider;
import com.plotsquared.core.queue.subscriber.DefaultProgressSubscriber;
import com.plotsquared.core.queue.subscriber.ProgressSubscriber;
import com.plotsquared.core.util.ChunkManager;
import com.plotsquared.core.util.EconHandler;
import com.plotsquared.core.util.InventoryUtil;
import com.plotsquared.core.util.PlayerManager;
import com.plotsquared.core.util.RegionManager;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.SetupUtils;
import com.plotsquared.core.util.WorldUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public class BukkitModule extends AbstractModule {

    private static final Logger LOGGER = LogManager.getLogger("PlotSquared/" + BukkitModule.class.getSimpleName());

    private final BukkitPlatform bukkitPlatform;

    public BukkitModule(final @NonNull BukkitPlatform bukkitPlatform) {
        this.bukkitPlatform = bukkitPlatform;
    }

    @Override
    protected void configure() {
        bind(PlayerManager.class).to(BukkitPlayerManager.class);
        bind(JavaPlugin.class).toInstance(bukkitPlatform);
        bind(PlotPlatform.class).toInstance(bukkitPlatform);
        bind(BukkitPlatform.class).toInstance(bukkitPlatform);
        bind(IndependentPlotGenerator.class).annotatedWith(DefaultGenerator.class).to(HybridGen.class);
        // Console actor
        @NonNull ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        WorldEditPlugin wePlugin = ((WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit"));
        bind(Actor.class).annotatedWith(ConsoleActor.class).toInstance(wePlugin.wrapCommandSender(console));
        bind(InventoryUtil.class).to(BukkitInventoryUtil.class);
        bind(SetupUtils.class).to(BukkitSetupUtils.class);
        bind(WorldUtil.class).to(BukkitUtil.class);
        install(new FactoryModuleBuilder()
                .implement(ProgressSubscriber.class, DefaultProgressSubscriber.class)
                .build(ProgressSubscriberFactory.class));
        bind(ChunkManager.class).to(BukkitChunkManager.class);
        if (PlotSquared.platform().isFaweHooking()) {
            bind(SchematicHandler.class).to(FaweSchematicHandler.class);
            bind(RegionManager.class).to(FaweRegionManager.class);
        } else {
            bind(SchematicHandler.class).to(BukkitSchematicHandler.class);
            bind(RegionManager.class).to(BukkitRegionManager.class);
        }
        bind(GlobalBlockQueue.class).toInstance(new GlobalBlockQueue(QueueProvider.of(BukkitQueueCoordinator.class)));
        if (Settings.Enabled_Components.WORLDS) {
            bind(PlotAreaManager.class).to(SinglePlotAreaManager.class);
            try {
                bind(SingleWorldListener.class).toInstance(new SingleWorldListener());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            bind(PlotAreaManager.class).to(DefaultPlotAreaManager.class);
        }
        install(new FactoryModuleBuilder().build(HybridPlotWorldFactory.class));
        install(new FactoryModuleBuilder()
                .implement(ChunkCoordinator.class, BukkitChunkCoordinator.class)
                .build(ChunkCoordinatorFactory.class));
        install(new FactoryModuleBuilder().build(ChunkCoordinatorBuilderFactory.class));
    }

    @Provides
    @Singleton
    @NonNull EconHandler provideEconHandler() {
        if (!Settings.Enabled_Components.ECONOMY || Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return EconHandler.nullEconHandler();
        }
        // Guice eagerly initializes singletons, so we need to bring the laziness ourselves
        return new LazyEconHandler();
    }

    private static final class LazyEconHandler extends EconHandler implements ServerListener.MutableEconHandler {
        private volatile EconHandler implementation;

        public void setImplementation(EconHandler econHandler) {
            this.implementation = econHandler;
        }

        @Override
        public boolean init() {
            return get().init();
        }

        @Override
        public double getBalance(final PlotPlayer<?> player) {
            return get().getBalance(player);
        }

        @Override
        public void withdrawMoney(final PlotPlayer<?> player, final double amount) {
            get().withdrawMoney(player, amount);
        }

        @Override
        public void depositMoney(final PlotPlayer<?> player, final double amount) {
            get().depositMoney(player, amount);
        }

        @Override
        public void depositMoney(final OfflinePlotPlayer player, final double amount) {
            get().depositMoney(player, amount);
        }

        @Override
        public boolean isEnabled(final PlotArea plotArea) {
            return get().isEnabled(plotArea);
        }

        @Override
        public @NonNull String format(final double balance) {
            return get().format(balance);
        }

        @Override
        public boolean isSupported() {
            return get().isSupported();
        }

        private EconHandler get() {
            return Objects.requireNonNull(this.implementation, "EconHandler not ready yet.");
        }

    }

}
