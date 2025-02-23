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
package com.plotsquared.bukkit.listener;

import com.destroystokyo.paper.MaterialTags;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.plotsquared.bukkit.player.BukkitPlayer;
import com.plotsquared.bukkit.util.BukkitEntityUtil;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.bukkit.util.UpdateUtility;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.listener.PlayerBlockEventType;
import com.plotsquared.core.listener.PlotListener;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.MetaDataAccess;
import com.plotsquared.core.player.PlayerMetaDataKeys;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.PlotInventory;
import com.plotsquared.core.plot.flag.FlagContainer;
import com.plotsquared.core.plot.flag.implementations.AnimalInteractFlag;
import com.plotsquared.core.plot.flag.implementations.BlockedCmdsFlag;
import com.plotsquared.core.plot.flag.implementations.ChatFlag;
import com.plotsquared.core.plot.flag.implementations.DenyPortalTravelFlag;
import com.plotsquared.core.plot.flag.implementations.DenyPortalsFlag;
import com.plotsquared.core.plot.flag.implementations.DenyTeleportFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.DropProtectionFlag;
import com.plotsquared.core.plot.flag.implementations.EditSignFlag;
import com.plotsquared.core.plot.flag.implementations.HangingBreakFlag;
import com.plotsquared.core.plot.flag.implementations.HangingPlaceFlag;
import com.plotsquared.core.plot.flag.implementations.HostileInteractFlag;
import com.plotsquared.core.plot.flag.implementations.InteractionInteractFlag;
import com.plotsquared.core.plot.flag.implementations.ItemDropFlag;
import com.plotsquared.core.plot.flag.implementations.KeepInventoryFlag;
import com.plotsquared.core.plot.flag.implementations.LecternReadBookFlag;
import com.plotsquared.core.plot.flag.implementations.MiscInteractFlag;
import com.plotsquared.core.plot.flag.implementations.PlayerInteractFlag;
import com.plotsquared.core.plot.flag.implementations.PreventCreativeCopyFlag;
import com.plotsquared.core.plot.flag.implementations.TamedInteractFlag;
import com.plotsquared.core.plot.flag.implementations.TileDropFlag;
import com.plotsquared.core.plot.flag.implementations.UntrustedVisitFlag;
import com.plotsquared.core.plot.flag.implementations.VehicleBreakFlag;
import com.plotsquared.core.plot.flag.implementations.VehicleUseFlag;
import com.plotsquared.core.plot.flag.implementations.VillagerInteractFlag;
import com.plotsquared.core.plot.world.PlotAreaManager;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.MathMan;
import com.plotsquared.core.util.PlotFlagUtil;
import com.plotsquared.core.util.PremiumVerification;
import com.plotsquared.core.util.entity.EntityCategories;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.help.HelpTopic;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Player Events involving plots.
 */
@SuppressWarnings("unused")
public class PlayerEventListener implements Listener {

    private static final Set<Material> MINECARTS = Set.of(
            Material.MINECART,
            Material.TNT_MINECART,
            Material.CHEST_MINECART,
            Material.COMMAND_BLOCK_MINECART,
            Material.FURNACE_MINECART,
            Material.HOPPER_MINECART
    );
    private static final Set<Material> BOOKS = Set.of(
            Material.BOOK,
            Material.KNOWLEDGE_BOOK,
            Material.WRITABLE_BOOK,
            Material.WRITTEN_BOOK
    );
    private static final Set<String> DYES;
    static {
        Set<String> mutableDyes = new HashSet<>(Set.of(
                "WHITE_DYE",
                "LIGHT_GRAY_DYE",
                "GRAY_DYE",
                "BLACK_DYE",
                "BROWN_DYE",
                "RED_DYE",
                "ORANGE_DYE",
                "YELLOW_DYE",
                "LIME_DYE",
                "GREEN_DYE",
                "CYAN_DYE",
                "LIGHT_BLUE_DYE",
                "BLUE_DYE",
                "PURPLE_DYE",
                "MAGENTA_DYE",
                "PINK_DYE",
                "GLOW_INK_SAC"
        ));
        int[] version = PlotSquared.platform().serverVersion();
        if (version[1] >= 20) {
            mutableDyes.add("HONEYCOMB");
        }
        DYES = Set.copyOf(mutableDyes);
    }

    private static final Set<String> INTERACTABLE_MATERIALS;

    static {
        // @formatter:off
        // "temporary" fix for https://hub.spigotmc.org/jira/browse/SPIGOT-7813
        // can (and should) be removed when 1.21 support is dropped
        // List of all interactable 1.21 materials
        INTERACTABLE_MATERIALS = Material.CHEST.isInteractable() ? null :  Set.of(
                "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE", "CHISELED_BOOKSHELF", "DECORATED_POT", "CHEST", "CRAFTING_TABLE",
                "FURNACE", "JUKEBOX", "OAK_FENCE", "SPRUCE_FENCE", "BIRCH_FENCE", "JUNGLE_FENCE", "ACACIA_FENCE", "CHERRY_FENCE",
                "DARK_OAK_FENCE", "MANGROVE_FENCE", "BAMBOO_FENCE", "CRIMSON_FENCE", "WARPED_FENCE", "PUMPKIN",
                "NETHER_BRICK_FENCE", "ENCHANTING_TABLE", "DRAGON_EGG", "ENDER_CHEST", "COMMAND_BLOCK", "BEACON", "ANVIL",
                "CHIPPED_ANVIL", "DAMAGED_ANVIL", "LIGHT", "REPEATING_COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "SHULKER_BOX",
                "WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX",
                "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX",
                "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX",
                "BLACK_SHULKER_BOX", "REPEATER", "COMPARATOR", "HOPPER", "DISPENSER", "DROPPER", "LECTERN", "LEVER",
                "DAYLIGHT_DETECTOR", "TRAPPED_CHEST", "TNT", "NOTE_BLOCK", "STONE_BUTTON", "POLISHED_BLACKSTONE_BUTTON",
                "OAK_BUTTON", "SPRUCE_BUTTON", "BIRCH_BUTTON", "JUNGLE_BUTTON", "ACACIA_BUTTON", "CHERRY_BUTTON",
                "DARK_OAK_BUTTON", "MANGROVE_BUTTON", "BAMBOO_BUTTON", "CRIMSON_BUTTON", "WARPED_BUTTON", "IRON_DOOR", "OAK_DOOR",
                "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR", "ACACIA_DOOR", "CHERRY_DOOR", "DARK_OAK_DOOR", "MANGROVE_DOOR",
                "BAMBOO_DOOR", "CRIMSON_DOOR", "WARPED_DOOR", "COPPER_DOOR", "EXPOSED_COPPER_DOOR", "WEATHERED_COPPER_DOOR",
                "OXIDIZED_COPPER_DOOR", "WAXED_COPPER_DOOR", "WAXED_EXPOSED_COPPER_DOOR", "WAXED_WEATHERED_COPPER_DOOR",
                "WAXED_OXIDIZED_COPPER_DOOR", "IRON_TRAPDOOR", "OAK_TRAPDOOR", "SPRUCE_TRAPDOOR", "BIRCH_TRAPDOOR",
                "JUNGLE_TRAPDOOR", "ACACIA_TRAPDOOR", "CHERRY_TRAPDOOR", "DARK_OAK_TRAPDOOR", "MANGROVE_TRAPDOOR",
                "BAMBOO_TRAPDOOR", "CRIMSON_TRAPDOOR", "WARPED_TRAPDOOR", "COPPER_TRAPDOOR", "EXPOSED_COPPER_TRAPDOOR",
                "WEATHERED_COPPER_TRAPDOOR", "OXIDIZED_COPPER_TRAPDOOR", "WAXED_COPPER_TRAPDOOR", "WAXED_EXPOSED_COPPER_TRAPDOOR",
                "WAXED_WEATHERED_COPPER_TRAPDOOR", "WAXED_OXIDIZED_COPPER_TRAPDOOR", "OAK_FENCE_GATE", "SPRUCE_FENCE_GATE",
                "BIRCH_FENCE_GATE", "JUNGLE_FENCE_GATE", "ACACIA_FENCE_GATE", "CHERRY_FENCE_GATE", "DARK_OAK_FENCE_GATE",
                "MANGROVE_FENCE_GATE", "BAMBOO_FENCE_GATE", "CRIMSON_FENCE_GATE", "WARPED_FENCE_GATE", "STRUCTURE_BLOCK",
                "JIGSAW", "OAK_SIGN", "SPRUCE_SIGN", "BIRCH_SIGN", "JUNGLE_SIGN", "ACACIA_SIGN", "CHERRY_SIGN", "DARK_OAK_SIGN",
                "MANGROVE_SIGN", "BAMBOO_SIGN", "CRIMSON_SIGN", "WARPED_SIGN", "OAK_HANGING_SIGN", "SPRUCE_HANGING_SIGN",
                "BIRCH_HANGING_SIGN", "JUNGLE_HANGING_SIGN", "ACACIA_HANGING_SIGN", "CHERRY_HANGING_SIGN",
                "DARK_OAK_HANGING_SIGN", "MANGROVE_HANGING_SIGN", "BAMBOO_HANGING_SIGN", "CRIMSON_HANGING_SIGN",
                "WARPED_HANGING_SIGN", "CAKE", "WHITE_BED", "ORANGE_BED", "MAGENTA_BED", "LIGHT_BLUE_BED", "YELLOW_BED",
                "LIME_BED", "PINK_BED", "GRAY_BED", "LIGHT_GRAY_BED", "CYAN_BED", "PURPLE_BED", "BLUE_BED", "BROWN_BED",
                "GREEN_BED", "RED_BED", "BLACK_BED", "CRAFTER", "BREWING_STAND", "CAULDRON", "FLOWER_POT", "LOOM", "COMPOSTER",
                "BARREL", "SMOKER", "BLAST_FURNACE", "CARTOGRAPHY_TABLE", "FLETCHING_TABLE", "GRINDSTONE", "SMITHING_TABLE",
                "STONECUTTER", "BELL", "CAMPFIRE", "SOUL_CAMPFIRE", "BEE_NEST", "BEEHIVE", "RESPAWN_ANCHOR", "CANDLE",
                "WHITE_CANDLE", "ORANGE_CANDLE", "MAGENTA_CANDLE", "LIGHT_BLUE_CANDLE", "YELLOW_CANDLE", "LIME_CANDLE",
                "PINK_CANDLE", "GRAY_CANDLE", "LIGHT_GRAY_CANDLE", "CYAN_CANDLE", "PURPLE_CANDLE", "BLUE_CANDLE", "BROWN_CANDLE",
                "GREEN_CANDLE", "RED_CANDLE", "BLACK_CANDLE", "VAULT", "MOVING_PISTON", "REDSTONE_WIRE", "OAK_WALL_SIGN",
                "SPRUCE_WALL_SIGN", "BIRCH_WALL_SIGN", "ACACIA_WALL_SIGN", "CHERRY_WALL_SIGN", "JUNGLE_WALL_SIGN",
                "DARK_OAK_WALL_SIGN", "MANGROVE_WALL_SIGN", "BAMBOO_WALL_SIGN", "OAK_WALL_HANGING_SIGN",
                "SPRUCE_WALL_HANGING_SIGN", "BIRCH_WALL_HANGING_SIGN", "ACACIA_WALL_HANGING_SIGN",
                "CHERRY_WALL_HANGING_SIGN", "JUNGLE_WALL_HANGING_SIGN", "DARK_OAK_WALL_HANGING_SIGN",
                "MANGROVE_WALL_HANGING_SIGN", "CRIMSON_WALL_HANGING_SIGN", "WARPED_WALL_HANGING_SIGN", "BAMBOO_WALL_HANGING_SIGN",
                "WATER_CAULDRON", "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON", "POTTED_TORCHFLOWER", "POTTED_OAK_SAPLING",
                "POTTED_SPRUCE_SAPLING", "POTTED_BIRCH_SAPLING", "POTTED_JUNGLE_SAPLING", "POTTED_ACACIA_SAPLING",
                "POTTED_CHERRY_SAPLING", "POTTED_DARK_OAK_SAPLING", "POTTED_MANGROVE_PROPAGULE", "POTTED_FERN",
                "POTTED_DANDELION", "POTTED_POPPY", "POTTED_BLUE_ORCHID", "POTTED_ALLIUM", "POTTED_AZURE_BLUET",
                "POTTED_RED_TULIP", "POTTED_ORANGE_TULIP", "POTTED_WHITE_TULIP", "POTTED_PINK_TULIP", "POTTED_OXEYE_DAISY",
                "POTTED_CORNFLOWER", "POTTED_LILY_OF_THE_VALLEY", "POTTED_WITHER_ROSE", "POTTED_RED_MUSHROOM",
                "POTTED_BROWN_MUSHROOM", "POTTED_DEAD_BUSH", "POTTED_CACTUS", "POTTED_BAMBOO", "SWEET_BERRY_BUSH",
                "CRIMSON_WALL_SIGN", "WARPED_WALL_SIGN", "POTTED_CRIMSON_FUNGUS", "POTTED_WARPED_FUNGUS", "POTTED_CRIMSON_ROOTS",
                "POTTED_WARPED_ROOTS", "CANDLE_CAKE", "WHITE_CANDLE_CAKE", "ORANGE_CANDLE_CAKE", "MAGENTA_CANDLE_CAKE",
                "LIGHT_BLUE_CANDLE_CAKE", "YELLOW_CANDLE_CAKE", "LIME_CANDLE_CAKE", "PINK_CANDLE_CAKE", "GRAY_CANDLE_CAKE",
                "LIGHT_GRAY_CANDLE_CAKE", "CYAN_CANDLE_CAKE", "PURPLE_CANDLE_CAKE", "BLUE_CANDLE_CAKE", "BROWN_CANDLE_CAKE",
                "GREEN_CANDLE_CAKE", "RED_CANDLE_CAKE", "BLACK_CANDLE_CAKE", "CAVE_VINES", "CAVE_VINES_PLANT",
                "POTTED_AZALEA_BUSH", "POTTED_FLOWERING_AZALEA_BUSH"
        );
        // @formatter:on
    }

    private final EventDispatcher eventDispatcher;
    private final WorldEdit worldEdit;
    private final PlotAreaManager plotAreaManager;
    private final PlotListener plotListener;
    // To prevent recursion
    private boolean tmpTeleport = true;
    private Field fieldPlayer;
    private PlayerMoveEvent moveTmp;
    private String internalVersion;

    {
        try {
            fieldPlayer = PlayerEvent.class.getDeclaredField("player");
            fieldPlayer.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Inject
    public PlayerEventListener(
            final @NonNull PlotAreaManager plotAreaManager,
            final @NonNull EventDispatcher eventDispatcher,
            final @NonNull WorldEdit worldEdit,
            final @NonNull PlotListener plotListener
    ) {
        this.eventDispatcher = eventDispatcher;
        this.worldEdit = worldEdit;
        this.plotAreaManager = plotAreaManager;
        this.plotListener = plotListener;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        Location location = BukkitUtil.adapt(event.getBlock().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        if (plot != null) {
            event.setDropItems(plot.getFlag(TileDropFlag.class));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDyeSign(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block != null && block.getState() instanceof Sign) {
            if (DYES.contains(itemStack.getType().toString())) {
                Location location = BukkitUtil.adapt(block.getLocation());
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    return;
                }
                Plot plot = location.getOwnedPlot();
                if (plot == null) {
                    if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, EditSignFlag.class, false)
                            && !event.getPlayer().hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD.toString())) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if (plot.isAdded(event.getPlayer().getUniqueId())) {
                    return; // allow for added players
                }
                if (!plot.getFlag(EditSignFlag.class)
                        && !event.getPlayer().hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER.toString())) {
                    plot.debug(event.getPlayer().getName() + " could not color the sign because of edit-sign = false");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEffect(@NonNull EntityPotionEffectEvent event) {
        if (Settings.Enabled_Components.DISABLE_BEACON_EFFECT_OVERFLOW ||
                event.getCause() != EntityPotionEffectEvent.Cause.BEACON ||
                !(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PotionEffect effect = event.getNewEffect();
        if (effect == null) {
            PotionEffect oldEffect = event.getOldEffect();
            if (oldEffect != null) {
                String name = oldEffect.getType().getName();
                plotListener.addEffect(uuid, name, -1);
            }
        } else {
            long expiresAt = System.currentTimeMillis() + effect.getDuration() * 50L; //Convert ticks to milliseconds
            String name = effect.getType().getName();
            plotListener.addEffect(uuid, name, expiresAt);
        }
    }

    @EventHandler
    public void onVehicleEntityCollision(VehicleEntityCollisionEvent e) {
        if (e.getVehicle() instanceof Boat) {
            Location location = BukkitUtil.adapt(e.getEntity().getLocation());
            if (location.isPlotArea()) {
                if (e.getEntity() instanceof Player) {
                    PlotPlayer<Player> player = BukkitUtil.adapt((Player) e.getEntity());
                    Plot plot = player.getCurrentPlot();
                    if (plot != null) {
                        if (!plot.isAdded(player.getUUID())) {
                            //Here the event is only canceled if the player is not the owner
                            //of the property on which he is located.
                            e.setCancelled(true);
                        }
                    } else {
                        e.setCancelled(true);
                    }
                } else {
                    //Here the event is cancelled too, otherwise you can move the
                    //boat with EchoPets or other mobs running around on the plot.
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void playerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().replace("/", "").toLowerCase(Locale.ROOT).trim();
        if (msg.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        PlotPlayer<Player> plotPlayer = BukkitUtil.adapt(player);
        Location location = plotPlayer.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        String[] parts = msg.split(" ");
        Plot plot = plotPlayer.getCurrentPlot();
        // Check WorldEdit
        switch (parts[0]) {
            case "up", "worldedit:up" -> {
                if (plot == null || (!plot.isAdded(plotPlayer.getUUID()) && !plotPlayer.hasPermission(
                        Permission.PERMISSION_ADMIN_BUILD_OTHER,
                        true
                ))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (plot == null && !area.isRoadFlags()) {
            return;
        }

        List<String> blockedCommands = plot != null ?
                plot.getFlag(BlockedCmdsFlag.class) :
                area.getFlag(BlockedCmdsFlag.class);
        if (blockedCommands.isEmpty()) {
            return;
        }
        if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_BLOCKED_CMDS)) {
            return;
        }
        // When using namespaced commands, we're not interested in the namespace
        String part = parts[0];
        if (part.contains(":")) {
            String[] namespaced = part.split(":");
            part = namespaced[1];
            msg = msg.substring(namespaced[0].length() + 1);
        }
        msg = replaceAliases(msg, part);
        for (String blocked : blockedCommands) {
            if (blocked.equalsIgnoreCase(msg)) {
                String perm;
                if (plot != null && plot.isAdded(plotPlayer.getUUID())) {
                    perm = "plots.admin.command.blocked-cmds.shared";
                } else {
                    perm = "plots.admin.command.blocked-cmds.road";
                }
                if (!plotPlayer.hasPermission(perm)) {
                    plotPlayer.sendMessage(TranslatableCaption.of("blockedcmds.command_blocked"));
                    event.setCancelled(true);
                }
                return;
            }
        }
    }

    private String replaceAliases(String msg, String part) {
        String s1 = part;
        Set<String> aliases = new HashSet<>();
        for (HelpTopic cmdLabel : Bukkit.getServer().getHelpMap().getHelpTopics()) {
            if (part.equals(cmdLabel.getName())) {
                break;
            }
            String label = cmdLabel.getName().replaceFirst("/", "");
            if (aliases.contains(label)) {
                continue;
            }
            PluginCommand p = Bukkit.getPluginCommand(label);
            if (p != null) {
                for (String a : p.getAliases()) {
                    if (aliases.contains(a)) {
                        continue;
                    }
                    aliases.add(a);
                    a = a.replaceFirst("/", "");
                    if (!a.equals(label) && a.equals(part)) {
                        part = label;
                        break;
                    }
                }
            }
        }
        if (!s1.equals(part)) {
            msg = msg.replace(s1, part);
        }
        return msg;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreLogin(final AsyncPlayerPreLoginEvent event) {
        final UUID uuid;
        if (Settings.UUID.OFFLINE) {
            if (Settings.UUID.FORCE_LOWERCASE) {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + event.getName().toLowerCase()).getBytes(Charsets.UTF_8));
            } else {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + event.getName()).getBytes(Charsets.UTF_8));
            }
        } else {
            uuid = event.getUniqueId();
        }
        PlotSquared.get().getImpromptuUUIDPipeline().storeImmediately(event.getName(), uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation") // Paper deprecation
    public void onConnect(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        PlotSquared.platform().playerManager().removePlayer(player.getUniqueId());
        final PlotPlayer<Player> pp = BukkitUtil.adapt(player);

        // we're stripping the country code as we don't want to differ between countries
        pp.setLocale(Locale.forLanguageTag(player.getLocale().substring(0, 2)));

        Location location = pp.getLocation();
        PlotArea area = location.getPlotArea();
        if (area != null) {
            Plot plot = area.getPlot(location);
            if (plot != null) {
                plotListener.plotEntry(pp, plot);
            }
        }
        // Delayed

        // Async
        TaskManager.runTaskLaterAsync(() -> {
            if (!player.hasPlayedBefore() && player.isOnline()) {
                player.saveData();
            }
            this.eventDispatcher.doJoinTask(pp);
        }, TaskTime.seconds(1L));

        if (pp.hasPermission(Permission.PERMISSION_ADMIN_UPDATE_NOTIFICATION.toString()) && Settings.Enabled_Components.UPDATE_NOTIFICATIONS
                && PremiumVerification.isPremium() && UpdateUtility.hasUpdate) {
            Caption boundary = TranslatableCaption.of("update.update_boundary");
            Caption updateNotification = TranslatableCaption.of("update.update_notification");
            pp.sendMessage(boundary);
            pp.sendMessage(
                    updateNotification,
                    TagResolver.builder()
                            .tag("p2version", Tag.inserting(Component.text(UpdateUtility.internalVersion.versionString())))
                            .tag("spigotversion", Tag.inserting(Component.text(UpdateUtility.spigotVersion)))
                            .tag("downloadurl", Tag.preProcessParsed("https://www.spigotmc.org/resources/77506/updates"))
                            .build()
            );
            pp.sendMessage(boundary);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlotPlayer<Player> pp = BukkitUtil.adapt(player);
        this.eventDispatcher.doRespawnTask(pp);
    }

    @SuppressWarnings("deprecation") // We explicitly want #getHomeSynchronous here
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        //We need to account for bad plugins like NoCheatPlus that teleports player on/before login -_-
        if (!player.isOnline()) {
            return;
        }
        BukkitPlayer pp = BukkitUtil.adapt(player);
        try (final MetaDataAccess<Plot> lastPlotAccess =
                     pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
            Plot lastPlot = lastPlotAccess.get().orElse(null);
            org.bukkit.Location to = event.getTo();
            //noinspection ConstantConditions
            if (to != null) {
                Location location = BukkitUtil.adapt(to);
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    if (lastPlot != null) {
                        plotListener.plotExit(pp, lastPlot);
                        lastPlotAccess.remove();
                    }
                    try (final MetaDataAccess<Location> lastLocationAccess =
                                 pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                        lastLocationAccess.remove();
                    }
                    return;
                }
                Plot plot = area.getPlot(location);
                if (plot != null && !plot.equals(lastPlot)) {
                    final boolean result = DenyTeleportFlag.allowsTeleport(pp, plot);
                    // there is one possibility to still allow teleportation:
                    // to is identical to the plot's home location, and untrusted-visit is true
                    // i.e. untrusted-visit can override deny-teleport
                    // this is acceptable, because otherwise it wouldn't make sense to have both flags set
                    if (result || (plot.getFlag(UntrustedVisitFlag.class) && plot.getHomeSynchronous().equals(BukkitUtil.adaptComplete(to)))) {
                        plotListener.plotEntry(pp, plot);
                    } else {
                        pp.sendMessage(
                                TranslatableCaption.of("deny.no_enter"),
                                TagResolver.resolver("plot", Tag.inserting(Component.text(plot.toString())))
                        );
                        event.setCancelled(true);
                    }
                }
            }
        }
        playerMove(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldChanged(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        BukkitPlayer pp = BukkitUtil.adapt(player);
        if (this.worldEdit != null) {
            if (!pp.hasPermission(Permission.PERMISSION_WORLDEDIT_BYPASS)) {
                if (pp.getAttribute("worldedit")) {
                    pp.removeAttribute("worldedit");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void vehicleMove(VehicleMoveEvent event)
            throws IllegalAccessException {
        final org.bukkit.Location from = event.getFrom();
        final org.bukkit.Location to = event.getTo();

        int toX, toZ;
        if ((toX = MathMan.roundInt(to.getX())) != MathMan.roundInt(from.getX()) | (toZ = MathMan.roundInt(to.getZ())) != MathMan
                .roundInt(from.getZ())) {
            Vehicle vehicle = event.getVehicle();

            // Check allowed
            if (!vehicle.getPassengers().isEmpty()) {
                Entity passenger = vehicle.getPassengers().get(0);

                if (passenger instanceof final Player player) {
                    // reset
                    if (moveTmp == null) {
                        moveTmp = new PlayerMoveEvent(null, from, to);
                    }
                    moveTmp.setFrom(from);
                    moveTmp.setTo(to);
                    moveTmp.setCancelled(false);
                    fieldPlayer.set(moveTmp, player);

                    List<Entity> passengers = vehicle.getPassengers();

                    this.playerMove(moveTmp);
                    org.bukkit.Location dest;
                    if (moveTmp.isCancelled()) {
                        dest = from;
                    } else if (MathMan.roundInt(moveTmp.getTo().getX()) != toX || MathMan.roundInt(moveTmp
                            .getTo()
                            .getZ()) != toZ) {
                        dest = to;
                    } else {
                        dest = null;
                    }
                    if (dest != null) {
                        vehicle.eject();
                        vehicle.setVelocity(new Vector(0d, 0d, 0d));
                        PaperLib.teleportAsync(vehicle, dest);
                        passengers.forEach(vehicle::addPassenger);
                        return;
                    }
                }
                if (Settings.Enabled_Components.KILL_ROAD_VEHICLES) {
                    final com.sk89q.worldedit.world.entity.EntityType entityType = BukkitAdapter.adapt(vehicle.getType());
                    // Horses etc are vehicles, but they're also animals
                    // so this filters out all living entities
                    if (EntityCategories.VEHICLE.contains(entityType) && !EntityCategories.ANIMAL.contains(entityType)) {
                        List<MetadataValue> meta = vehicle.getMetadata("plot");
                        Plot toPlot = BukkitUtil.adapt(to).getPlot();
                        if (!meta.isEmpty()) {
                            Plot origin = (Plot) meta.get(0).value();
                            if (origin != null && !origin.getBasePlot(false).equals(toPlot)) {
                                vehicle.remove();
                            }
                        } else if (toPlot != null) {
                            vehicle.setMetadata("plot", new FixedMetadataValue((Plugin) PlotSquared.platform(), toPlot));
                        }
                    }
                }
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerMove(PlayerMoveEvent event) {
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        int x2;
        if (MathMan.roundInt(from.getX()) != (x2 = MathMan.roundInt(to.getX()))) {
            Player player = event.getPlayer();
            BukkitPlayer pp = BukkitUtil.adapt(player);
            // Cancel teleport
            if (TaskManager.removeFromTeleportQueue(pp.getName())) {
                pp.sendMessage(TranslatableCaption.of("teleport.teleport_failed"));
            }
            // Set last location
            Location location = BukkitUtil.adapt(to);
            try (final MetaDataAccess<Location> lastLocationAccess =
                         pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                lastLocationAccess.remove();
            }
            PlotArea area = location.getPlotArea();
            if (area == null) {
                try (final MetaDataAccess<Plot> lastPlotAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                    lastPlotAccess.remove();
                }
                return;
            }
            Plot now = area.getPlot(location);
            Plot lastPlot;
            try (final MetaDataAccess<Plot> lastPlotAccess =
                         pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                lastPlot = lastPlotAccess.get().orElse(null);
            }
            if (now == null) {
                try (final MetaDataAccess<Boolean> kickAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_KICK)) {
                    if (lastPlot != null && !plotListener.plotExit(pp, lastPlot) && this.tmpTeleport && !kickAccess.get().orElse(
                            false)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_EXIT_DENIED)
                                )
                        );
                        this.tmpTeleport = false;
                        if (lastPlot.equals(BukkitUtil.adapt(from).getPlot())) {
                            player.teleport(from);
                        } else {
                            player.teleport(player.getWorld().getSpawnLocation());
                        }
                        this.tmpTeleport = true;
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (now.equals(lastPlot)) {
                ForceFieldListener.handleForcefield(player, pp, now);
            } else if (!plotListener.plotEntry(pp, now) && this.tmpTeleport) {
                pp.sendMessage(
                        TranslatableCaption.of("deny.no_enter"),
                        TagResolver.resolver("plot", Tag.inserting(Component.text(now.toString())))
                );
                this.tmpTeleport = false;
                to.setX(from.getBlockX());
                to.setY(from.getBlockY());
                to.setZ(from.getBlockZ());
                player.teleport(event.getTo());
                this.tmpTeleport = true;
                return;
            }
            int border = area.getBorder(true);
            int x1;
            if (x2 > border && this.tmpTeleport) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                    to.setX(border - 1);
                    this.tmpTeleport = false;
                    player.teleport(event.getTo());
                    this.tmpTeleport = true;
                    pp.sendMessage(TranslatableCaption.of("border.denied"));
                } else if (MathMan.roundInt(from.getX()) <= border) { // Only send if they just moved out of the border
                    pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                }
            } else if (x2 < -border && this.tmpTeleport) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                    to.setX(-border + 1);
                    this.tmpTeleport = false;
                    player.teleport(event.getTo());
                    this.tmpTeleport = true;
                    pp.sendMessage(TranslatableCaption.of("border.denied"));
                } else if (MathMan.roundInt(from.getX()) >= -border) { // Only send if they just moved out of the border
                    pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                }
            } else if (((x1 = MathMan.roundInt(from.getX())) >= border && x2 <= border) || (x1 <= -border && x2 >= -border)) {
                if (pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                    pp.sendMessage(TranslatableCaption.of("border.bypass.entered"));
                }
            }
        }
        int z2;
        if (MathMan.roundInt(from.getZ()) != (z2 = MathMan.roundInt(to.getZ()))) {
            Player player = event.getPlayer();
            BukkitPlayer pp = BukkitUtil.adapt(player);
            // Cancel teleport
            if (TaskManager.removeFromTeleportQueue(pp.getName())) {
                pp.sendMessage(TranslatableCaption.of("teleport.teleport_failed"));
            }
            // Set last location
            Location location = BukkitUtil.adapt(to);
            try (final MetaDataAccess<Location> lastLocationAccess =
                         pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LOCATION)) {
                lastLocationAccess.set(location);
            }
            PlotArea area = location.getPlotArea();
            if (area == null) {
                try (final MetaDataAccess<Plot> lastPlotAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                    lastPlotAccess.remove();
                }
                return;
            }
            Plot plot = area.getPlot(location);
            Plot lastPlot;
            try (final MetaDataAccess<Plot> lastPlotAccess =
                         pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_LAST_PLOT)) {
                lastPlot = lastPlotAccess.get().orElse(null);
            }
            if (plot == null) {
                try (final MetaDataAccess<Boolean> kickAccess =
                             pp.accessTemporaryMetaData(PlayerMetaDataKeys.TEMPORARY_KICK)) {
                    if (lastPlot != null && !plotListener.plotExit(pp, lastPlot) && this.tmpTeleport && !kickAccess.get().orElse(
                            false)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_EXIT_DENIED)
                                )
                        );
                        this.tmpTeleport = false;
                        if (lastPlot.equals(BukkitUtil.adapt(from).getPlot())) {
                            player.teleport(from);
                        } else {
                            player.teleport(player.getWorld().getSpawnLocation());
                        }
                        this.tmpTeleport = true;
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (plot.equals(lastPlot)) {
                ForceFieldListener.handleForcefield(player, pp, plot);
            } else if (!plotListener.plotEntry(pp, plot) && this.tmpTeleport) {
                pp.sendMessage(
                        TranslatableCaption.of("deny.no_enter"),
                        TagResolver.resolver("plot", Tag.inserting(Component.text(plot.toString())))
                );
                this.tmpTeleport = false;
                player.teleport(from);
                to.setX(from.getBlockX());
                to.setY(from.getBlockY());
                to.setZ(from.getBlockZ());
                player.teleport(event.getTo());
                this.tmpTeleport = true;
                return;
            }
            int border = area.getBorder(true);
            int z1;
            if (z2 > border && this.tmpTeleport) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                    to.setZ(border - 1);
                    this.tmpTeleport = false;
                    player.teleport(event.getTo());
                    this.tmpTeleport = true;
                    pp.sendMessage(TranslatableCaption.of("border.denied"));
                } else if (MathMan.roundInt(from.getZ()) <= border) { // Only send if they just moved out of the border
                    pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                }
            } else if (z2 < -border && this.tmpTeleport) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                    to.setZ(-border + 1);
                    this.tmpTeleport = false;
                    player.teleport(event.getTo());
                    this.tmpTeleport = true;
                    pp.sendMessage(TranslatableCaption.of("border.denied"));
                } else if (MathMan.roundInt(from.getZ()) >= -border) { // Only send if they just moved out of the border
                    pp.sendMessage(TranslatableCaption.of("border.bypass.exited"));
                }
            } else if (((z1 = MathMan.roundInt(from.getZ())) >= border && z2 <= border) || (z1 <= -border && z2 >= -border)) {
                if (pp.hasPermission(Permission.PERMISSION_ADMIN_BYPASS_BORDER)) {
                    pp.sendMessage(TranslatableCaption.of("border.bypass.entered"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    @SuppressWarnings("deprecation") // Paper deprecation
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        BukkitPlayer plotPlayer = BukkitUtil.adapt(event.getPlayer());
        Location location = plotPlayer.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlot(location);
        if (plot == null) {
            return;
        }
        if (!((plot.getFlag(ChatFlag.class) && area.isPlotChat() && plotPlayer.getAttribute("chat"))
                || area.isForcingPlotChat())) {
            return;
        }
        if (plot.isDenied(plotPlayer.getUUID()) && !plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_CHAT_BYPASS)) {
            return;
        }
        event.setCancelled(true);
        Set<Player> recipients = event.getRecipients();
        recipients.clear();
        Set<PlotPlayer<?>> spies = new HashSet<>();
        Set<PlotPlayer<?>> plotRecipients = new HashSet<>();
        for (final PlotPlayer<?> pp : PlotSquared.platform().playerManager().getPlayers()) {
            if (pp.getAttribute("chatspy")) {
                spies.add(pp);
            } else {
                Plot current = pp.getCurrentPlot();
                if (current != null && current.getBasePlot(false).equals(plot)) {
                    plotRecipients.add(pp);
                }
            }
        }
        String message = event.getMessage();
        String sender = event.getPlayer().getDisplayName();
        PlotId id = plot.getId();
        String worldName = plot.getWorldName();
        Caption msg = TranslatableCaption.of("chat.plot_chat_format");
        TagResolver.Builder builder = TagResolver.builder();
        builder.tag("world", Tag.inserting(Component.text(worldName)));
        builder.tag("plot_id", Tag.inserting(Component.text(id.toString())));
        builder.tag("sender", Tag.inserting(Component.text(sender)));
        if (plotPlayer.hasPermission("plots.chat.color")) {
            builder.tag("msg", Tag.inserting(MiniMessage.miniMessage().deserialize(
                    message,
                    TagResolver.resolver(StandardTags.color(), StandardTags.gradient(),
                            StandardTags.rainbow(), StandardTags.decorations()
                    )
            )));
        } else {
            builder.tag("msg", Tag.inserting(Component.text(message)));
        }
        for (PlotPlayer<?> receiver : plotRecipients) {
            receiver.sendMessage(msg, builder.build());
        }
        if (!spies.isEmpty()) {
            Caption spymsg = TranslatableCaption.of("chat.plot_chat_spy_format");
            for (PlotPlayer<?> player : spies) {
                player.sendMessage(spymsg, builder.tag("message", Tag.inserting(Component.text(message))).build());
            }
        }
        if (Settings.Chat.LOG_PLOTCHAT_TO_CONSOLE) {
            Caption spymsg = TranslatableCaption.of("chat.plot_chat_spy_format");
            ConsolePlayer.getConsole().sendMessage(
                    spymsg,
                    builder.tag("message", Tag.inserting(Component.text(message))).build()
            );
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        /*if (!event.isLeftClick() || (event.getAction() != InventoryAction.PLACE_ALL) || event
            .isShiftClick()) {
            return;
        }*/
        HumanEntity entity = event.getWhoClicked();
        if (!(entity instanceof Player) || !this.plotAreaManager
                .hasPlotArea(entity.getWorld().getName())) {
            return;
        }

        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) {
            return;
        }
        BukkitPlayer pp = BukkitUtil.adapt(player);
        final PlotInventory inventory = PlotInventory.getOpenPlotInventory(pp);
        if (inventory != null && event.getRawSlot() == event.getSlot()) {
            if (!inventory.onClick(event.getSlot())) {
                event.setResult(Event.Result.DENY);
                event.setCancelled(true);
                inventory.close();
            }
        }
        PlayerInventory inv = player.getInventory();
        int slot = inv.getHeldItemSlot();
        if ((slot > 8) || !event.getEventName().equals("InventoryCreativeEvent")) {
            return;
        }
        ItemStack oldItem = inv.getItemInHand();
        ItemMeta oldMeta = oldItem.getItemMeta();
        ItemStack newItem = event.getCursor();
        ItemMeta newMeta = newItem.getItemMeta();

        if (event.getClick() == ClickType.CREATIVE) {
            final Plot plot = pp.getCurrentPlot();
            if (plot != null) {
                if (plot.getFlag(PreventCreativeCopyFlag.class) && !plot
                        .isAdded(player.getUniqueId()) && !pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                    final ItemStack newStack =
                            new ItemStack(newItem.getType(), newItem.getAmount());
                    event.setCursor(newStack);
                    plot.debug(player.getName()
                            + " could not creative-copy an item because prevent-creative-copy = true");
                }
            } else {
                PlotArea area = pp.getPlotAreaAbs();
                if (area != null && PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, PreventCreativeCopyFlag.class, true)) {
                    final ItemStack newStack =
                            new ItemStack(newItem.getType(), newItem.getAmount());
                    event.setCursor(newStack);
                }
            }
            return;
        }

        String newLore = "";
        if (newMeta != null) {
            List<String> lore = newMeta.getLore();
            if (lore != null) {
                newLore = lore.toString();
            }
        }
        String oldLore = "";
        if (oldMeta != null) {
            List<String> lore = oldMeta.getLore();
            if (lore != null) {
                oldLore = lore.toString();
            }
        }
        Material itemType = newItem.getType();
        if (!"[(+NBT)]".equals(newLore) || (oldItem.equals(newItem) && newLore.equals(oldLore))) {
            if (newMeta == null || (itemType != Material.LEGACY_BANNER && itemType != Material.PLAYER_HEAD)) {
                return;
            }
        }
        Block block = player.getTargetBlock(null, 7);
        org.bukkit.block.BlockState state = block.getState();
        Material stateType = state.getType();
        if (stateType != itemType) {
            if (stateType == Material.LEGACY_WALL_BANNER || stateType == Material.LEGACY_STANDING_BANNER) {
                if (itemType != Material.LEGACY_BANNER) {
                    return;
                }
            } else if (stateType == Material.LEGACY_SKULL) {
                if (itemType != Material.LEGACY_SKULL_ITEM) {
                    return;
                }
            } else {
                return;
            }
        }
        Location location = BukkitUtil.adapt(state.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = area.getPlotAbs(location);
        boolean cancelled = false;
        if (plot == null) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_ROAD)
                        )
                );
                cancelled = true;
            }
        } else if (!plot.hasOwner()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)
                        )
                );
                cancelled = true;
            }
        } else {
            UUID uuid = pp.getUUID();
            if (!plot.isAdded(uuid)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_OTHER)
                            )
                    );
                    cancelled = true;
                }
            }
        }
        if (cancelled) {
            if ((oldItem.getType() == newItem.getType()) && (oldItem.getDurability() == newItem
                    .getDurability())) {
                event.setCursor(
                        new ItemStack(newItem.getType(), newItem.getAmount(), newItem.getDurability()));
                event.setCancelled(true);
                return;
            }
            event.setCursor(
                    new ItemStack(newItem.getType(), newItem.getAmount(), newItem.getDurability()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractAtEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (!(entity instanceof ArmorStand) && !(entity instanceof ItemFrame)) {
            return;
        }
        Location location = BukkitUtil.adapt(e.getRightClicked().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        EntitySpawnListener.testNether(entity);
        Plot plot = location.getPlotAbs();
        BukkitPlayer pp = BukkitUtil.adapt(e.getPlayer());
        if (plot == null) {
            if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, MiscInteractFlag.class, true) && !pp.hasPermission(
                    Permission.PERMISSION_ADMIN_INTERACT_ROAD
            )) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_ROAD)
                        )
                );
                e.setCancelled(true);
            }
        } else {
            if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    pp.sendMessage(TranslatableCaption.of("done.building_restricted"));
                    e.setCancelled(true);
                    return;
                }
            }
            if (!plot.hasOwner()) {
                if (!pp.hasPermission("plots.admin.interact.unowned")) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)
                            )
                    );
                    e.setCancelled(true);
                }
            } else {
                UUID uuid = pp.getUUID();
                if (plot.isAdded(uuid)) {
                    return;
                }
                if (plot.getFlag(MiscInteractFlag.class)) {
                    return;
                }
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_OTHER)
                            )
                    );
                    e.setCancelled(true);
                    plot.debug(pp.getName() + " could not interact with " + entity.getType()
                            + " because misc-interact = false");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    @SuppressWarnings("deprecation") // Paper deprecation
    public void onCancelledInteract(PlayerInteractEvent event) {
        if (event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_AIR) {
            Player player = event.getPlayer();
            BukkitPlayer pp = BukkitUtil.adapt(player);
            PlotArea area = pp.getPlotAreaAbs();
            if (area == null) {
                return;
            }
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                Material item = event.getMaterial();
                if (item.toString().toLowerCase().endsWith("_egg")) {
                    event.setCancelled(true);
                    event.setUseItemInHand(Event.Result.DENY);
                }
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            Material type = hand.getType();
            Material offType = offHand.getType();
            if (type == Material.AIR) {
                type = offType;
            }
            if (type.toString().toLowerCase().endsWith("_egg")) {
                Block block = player.getTargetBlockExact(5, FluidCollisionMode.SOURCE_ONLY);
                if (block != null && block.getType() != Material.AIR) {
                    Location location = BukkitUtil.adapt(block.getLocation());
                    if (!this.eventDispatcher.checkPlayerBlockEvent(pp, PlayerBlockEventType.SPAWN_MOB, location, null, true)) {
                        event.setCancelled(true);
                        event.setUseItemInHand(Event.Result.DENY);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BukkitPlayer pp = BukkitUtil.adapt(player);
        PlotArea area = pp.getPlotAreaAbs();
        if (area == null) {
            return;
        }
        PlayerBlockEventType eventType;
        BlockType blocktype1;
        Block block = event.getClickedBlock();
        if (block == null) {
            // We do not care in this case, the player is likely interacting with air ("nothing").
            return;
        }
        Location location = BukkitUtil.adapt(block.getLocation());
        Action action = event.getAction();
        switch (action) {
            case PHYSICAL -> {
                eventType = PlayerBlockEventType.TRIGGER_PHYSICAL;
                blocktype1 = BukkitAdapter.asBlockType(block.getType());
            }

            //todo rearrange the right click code. it is all over the place.
            case RIGHT_CLICK_BLOCK -> {
                Material blockType = block.getType();
                eventType = PlayerBlockEventType.INTERACT_BLOCK;
                blocktype1 = BukkitAdapter.asBlockType(block.getType());

                if (INTERACTABLE_MATERIALS != null ? INTERACTABLE_MATERIALS.contains(blockType.name()) : blockType.isInteractable()) {
                    if (!player.isSneaking()) {
                        break;
                    }
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    ItemStack offHand = player.getInventory().getItemInOffHand();

                    // sneaking players interact with blocks if both hands are empty
                    if (hand.getType() == Material.AIR && offHand.getType() == Material.AIR) {
                        break;
                    }
                }

                Material type = event.getMaterial();

                // in the following, lb needs to have the material of the item in hand i.e. type
                switch (type.toString()) {
                    case "REDSTONE", "STRING", "PUMPKIN_SEEDS", "MELON_SEEDS", "COCOA_BEANS", "WHEAT_SEEDS", "BEETROOT_SEEDS",
                            "SWEET_BERRIES", "GLOW_BERRIES" -> {
                        return;
                    }
                    default -> {
                        //eventType = PlayerBlockEventType.PLACE_BLOCK;
                        if (type.isBlock()) {
                            return;
                        }
                    }
                }
                if (PaperLib.isPaper()) {
                    if (MaterialTags.SPAWN_EGGS.isTagged(type) || Material.EGG.equals(type)) {
                        eventType = PlayerBlockEventType.SPAWN_MOB;
                        break;
                    }
                } else {
                    if (type.toString().toLowerCase().endsWith("egg")) {
                        eventType = PlayerBlockEventType.SPAWN_MOB;
                        break;
                    }
                }
                if (type.isEdible()) {
                    //Allow all players to eat while also allowing the block place event to be fired
                    return;
                }
                if (type == Material.ARMOR_STAND) {
                    location = BukkitUtil.adapt(block.getRelative(event.getBlockFace()).getLocation());
                    eventType = PlayerBlockEventType.PLACE_MISC;
                }
                if (org.bukkit.Tag.ITEMS_BOATS.isTagged(type) || MINECARTS.contains(type)) {
                    eventType = PlayerBlockEventType.PLACE_VEHICLE;
                    break;
                }
                if (type == Material.FIREWORK_ROCKET || type == Material.FIREWORK_STAR) {
                    eventType = PlayerBlockEventType.SPAWN_MOB;
                    break;
                }
                if (BOOKS.contains(type)) {
                    eventType = PlayerBlockEventType.READ;
                    break;
                }
            }
            case LEFT_CLICK_BLOCK -> {
                Material blockType = block.getType();

                // todo: when the code above is rearranged, it would be great to beautify this as well.
                // will code this as a temporary, specific bug fix (for dragon eggs)
                if (blockType != Material.DRAGON_EGG) {
                    return;
                }

                eventType = PlayerBlockEventType.INTERACT_BLOCK;
                blocktype1 = BukkitAdapter.asBlockType(block.getType());
            }
            default -> {
                return;
            }
        }
        if (this.worldEdit != null && pp.getAttribute("worldedit")) {
            if (event.getMaterial() == Material.getMaterial(this.worldEdit.getConfiguration().wandItem)) {
                return;
            }
        }
        if (!this.eventDispatcher.checkPlayerBlockEvent(pp, eventType, location, blocktype1, true)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    // Boats can sometimes be placed on interactable blocks such as levers,
    // see PS-175. Armor stands, minecarts and end crystals (the other entities
    // supported by this event) don't have this issue.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBoatPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Entity placed = event.getEntity();
        if (!(placed instanceof Boat)) {
            return;
        }
        BukkitPlayer pp = BukkitUtil.adapt(event.getPlayer());
        PlotArea area = pp.getPlotAreaAbs();
        if (area == null) {
            return;
        }
        PlayerBlockEventType eventType = PlayerBlockEventType.PLACE_VEHICLE;
        Block block = event.getBlock();
        BlockType blockType = BukkitAdapter.asBlockType(block.getType());
        Location location = BukkitUtil.adapt(block.getLocation());
        if (!PlotSquared.get().getEventDispatcher()
                .checkPlayerBlockEvent(pp, eventType, location, blockType, true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        BlockFace bf = event.getBlockFace();
        // Note: a month after Bukkit 1.14.4 released, they added the API method
        // PlayerBucketEmptyEvent#getBlock(), which returns the block the
        // bucket contents is going to be placed at. Currently we determine this
        // block ourselves to retain compatibility with 1.13.
        final Block block;
        // if the block can be waterlogged, the event might waterlog the block
        // sometimes
        if (event.getBlockClicked().getBlockData() instanceof Waterlogged waterlogged
                && !waterlogged.isWaterlogged() && event.getBucket() != Material.LAVA_BUCKET) {
            block = event.getBlockClicked();
        } else {
            block = event.getBlockClicked().getLocation()
                    .add(bf.getModX(), bf.getModY(), bf.getModZ())
                    .getBlock();
        }
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        BukkitPlayer pp = BukkitUtil.adapt(event.getPlayer());
        Plot plot = area.getPlot(location);
        if (plot == null) {
            if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                return;
            }
            pp.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD))
            );
            event.setCancelled(true);
        } else if (!plot.hasOwner()) {
            if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                return;
            }
            pp.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                    )
            );
            event.setCancelled(true);
        } else if (!plot.isAdded(pp.getUUID())) {
            if (pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                return;
            }
            pp.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                    )
            );
            event.setCancelled(true);
        } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                pp.sendMessage(
                        TranslatableCaption.of("done.building_restricted")
                );
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity closer = event.getPlayer();
        if (!(closer instanceof Player player)) {
            return;
        }
        PlotInventory.removePlotInventoryOpen(BukkitUtil.adapt(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        TaskManager.removeFromTeleportQueue(event.getPlayer().getName());
        BukkitPlayer pp = BukkitUtil.adapt(event.getPlayer());
        pp.unregister();
        plotListener.logout(pp.getUUID());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block blockClicked = event.getBlockClicked();
        Location location = BukkitUtil.adapt(blockClicked.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Player player = event.getPlayer();
        BukkitPlayer plotPlayer = BukkitUtil.adapt(player);
        Plot plot = area.getPlot(location);
        if (plot == null) {
            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                return;
            }
            plotPlayer.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD))
            );
            event.setCancelled(true);
        } else if (!plot.hasOwner()) {
            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                return;
            }
            plotPlayer.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                    )
            );
            event.setCancelled(true);
        } else if (!plot.isAdded(plotPlayer.getUUID())) {
            if (plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                return;
            }
            plotPlayer.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    TagResolver.resolver(
                            "node",
                            Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                    )
            );
            event.setCancelled(true);
        } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
            if (!plotPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                plotPlayer.sendMessage(
                        TranslatableCaption.of("done.building_restricted")
                );
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Block block = event.getBlock().getRelative(event.getBlockFace());
        Location location = BukkitUtil.adapt(block.getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Player p = event.getPlayer();
        if (p == null) {
            event.setCancelled(true);
            return;
        }
        BukkitPlayer pp = BukkitUtil.adapt(p);
        Plot plot = area.getPlot(location);
        if (plot == null) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_ROAD)
                        )
                );
                event.setCancelled(true);
            }
        } else {
            if (!plot.hasOwner()) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)
                            )
                    );
                    event.setCancelled(true);
                }
                return;
            }
            if (!plot.isAdded(pp.getUUID())) {
                if (!plot.getFlag(HangingPlaceFlag.class)) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER)
                                )
                        );
                        event.setCancelled(true);
                    }
                    return;
                }
            }
            if (BukkitEntityUtil.checkEntity(event.getEntity(), plot)) {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        if (remover instanceof Player p) {
            Location location = BukkitUtil.adapt(event.getEntity().getLocation());
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return;
            }
            BukkitPlayer pp = BukkitUtil.adapt(p);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_ROAD)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_ROAD)
                            )
                    );
                    event.setCancelled(true);
                }
            } else if (!plot.hasOwner()) {
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)
                            )
                    );
                    event.setCancelled(true);
                }
            } else if (!plot.isAdded(pp.getUUID())) {
                if (plot.getFlag(HangingBreakFlag.class)) {
                    return;
                }
                if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_OTHER)
                            )
                    );
                    event.setCancelled(true);
                    plot.debug(p.getName()
                            + " could not break hanging entity because hanging-break = false");
                }
            }
        } else if (remover instanceof Projectile p) {
            if (p.getShooter() instanceof Player shooter) {
                Location location = BukkitUtil.adapt(event.getEntity().getLocation());
                PlotArea area = location.getPlotArea();
                if (area == null) {
                    return;
                }
                BukkitPlayer player = BukkitUtil.adapt(shooter);
                Plot plot = area.getPlot(BukkitUtil.adapt(event.getEntity().getLocation()));
                if (plot != null) {
                    if (!plot.hasOwner()) {
                        if (!player.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission_event"),
                                    TagResolver.resolver(
                                            "node",
                                            Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_UNOWNED)
                                    )
                            );
                            event.setCancelled(true);
                        }
                    } else if (!plot.isAdded(player.getUUID())) {
                        if (!plot.getFlag(HangingBreakFlag.class)) {
                            if (!player.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_OTHER)) {
                                player.sendMessage(
                                        TranslatableCaption.of("permission.no_permission_event"),
                                        TagResolver.resolver(
                                                "node",
                                                Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_OTHER)
                                        )
                                );
                                event.setCancelled(true);
                                plot.debug(player.getName()
                                        + " could not break hanging entity because hanging-break = false");
                            }
                        }
                    }
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.UNKNOWN) {
            return;
        }
        Location location = BukkitUtil.adapt(event.getRightClicked().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Player p = event.getPlayer();
        BukkitPlayer pp = BukkitUtil.adapt(p);
        Plot plot = area.getPlot(location);
        if (plot == null && !area.isRoadFlags()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_ROAD)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_ROAD)
                        )
                );
                event.setCancelled(true);
            }
        } else if (plot != null && !plot.hasOwner()) {
            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_UNOWNED)
                        )
                );
                event.setCancelled(true);
            }
        } else if ((plot != null && !plot.isAdded(pp.getUUID())) || (plot == null && area
                .isRoadFlags())) {
            final Entity entity = event.getRightClicked();
            final com.sk89q.worldedit.world.entity.EntityType entityType =
                    BukkitAdapter.adapt(entity.getType());

            FlagContainer flagContainer;
            if (plot == null) {
                flagContainer = area.getRoadFlagContainer();
            } else {
                flagContainer = plot.getFlagContainer();
            }

            if (EntityCategories.HOSTILE.contains(entityType) && flagContainer
                    .getFlag(HostileInteractFlag.class).getValue()) {
                return;
            }

            if (EntityCategories.ANIMAL.contains(entityType) && flagContainer
                    .getFlag(AnimalInteractFlag.class).getValue()) {
                return;
            }

            // This actually makes use of the interface, so we don't use the
            // category
            if (entity instanceof Tameable && ((Tameable) entity).isTamed() && flagContainer
                    .getFlag(TamedInteractFlag.class).getValue()) {
                return;
            }

            if (EntityCategories.VEHICLE.contains(entityType) && flagContainer
                    .getFlag(VehicleUseFlag.class).getValue()) {
                return;
            }

            if (EntityCategories.PLAYER.contains(entityType) && flagContainer
                    .getFlag(PlayerInteractFlag.class).getValue()) {
                return;
            }

            if (EntityCategories.INTERACTION.contains(entityType) && flagContainer
                    .getFlag(InteractionInteractFlag.class).getValue()) {
                return;
            }

            if (EntityCategories.VILLAGER.contains(entityType) && flagContainer
                    .getFlag(VillagerInteractFlag.class).getValue()) {
                return;
            }

            if ((EntityCategories.HANGING.contains(entityType) || EntityCategories.OTHER
                    .contains(entityType)) && flagContainer.getFlag(MiscInteractFlag.class)
                    .getValue()) {
                return;
            }

            if (!pp.hasPermission(Permission.PERMISSION_ADMIN_INTERACT_OTHER)) {
                pp.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_INTERACT_OTHER)
                        )
                );
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Location location = BukkitUtil.adapt(event.getVehicle().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Entity attacker = event.getAttacker();
        if (attacker instanceof Player p) {
            BukkitPlayer pp = BukkitUtil.adapt(p);
            Plot plot = area.getPlot(location);
            if (plot == null) {
                if (!PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, VehicleBreakFlag.class, true) && !pp.hasPermission(
                        Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_ROAD
                )) {
                    pp.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver(
                                    "node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_ROAD)
                            )
                    );
                    event.setCancelled(true);
                }
            } else {
                if (!plot.hasOwner()) {
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_UNOWNED)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_UNOWNED)
                                )
                        );
                        event.setCancelled(true);
                        return;
                    }
                    return;
                }
                if (!plot.isAdded(pp.getUUID())) {
                    if (plot.getFlag(VehicleBreakFlag.class)) {
                        return;
                    }
                    if (!pp.hasPermission(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_OTHER)) {
                        pp.sendMessage(
                                TranslatableCaption.of("permission.no_permission_event"),
                                TagResolver.resolver(
                                        "node",
                                        Tag.inserting(Permission.PERMISSION_ADMIN_DESTROY_VEHICLE_OTHER)
                                )
                        );
                        event.setCancelled(true);
                        plot.debug(pp.getName()
                                + " could not break vehicle because vehicle-break = false");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        BukkitPlayer pp = BukkitUtil.adapt(player);
        Location location = pp.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, ItemDropFlag.class, false)) {
                event.setCancelled(true);
            }
            return;
        }
        UUID uuid = pp.getUUID();
        if (!plot.isAdded(uuid)) {
            if (!plot.getFlag(ItemDropFlag.class)) {
                plot.debug(player.getName() + " could not drop item because of item-drop = false");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        LivingEntity ent = event.getEntity();
        if (ent instanceof Player player) {
            BukkitPlayer pp = BukkitUtil.adapt(player);
            Location location = pp.getLocation();
            PlotArea area = location.getPlotArea();
            if (area == null) {
                return;
            }
            Plot plot = location.getOwnedPlot();
            if (plot == null) {
                if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DropProtectionFlag.class, true)) {
                    event.setCancelled(true);
                }
                return;
            }
            UUID uuid = pp.getUUID();
            if (!plot.isAdded(uuid) && plot.getFlag(DropProtectionFlag.class)) {
                plot.debug(player.getName() + " could not pick up item because of drop-protection = true");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent event) {
        Location location = BukkitUtil.adapt(event.getEntity().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, KeepInventoryFlag.class, true)) {
                event.setCancelled(true);
            }
            return;
        }
        if (plot.getFlag(KeepInventoryFlag.class)) {
            plot.debug(event.getEntity().getName() + " kept their inventory because of keep-inventory = true");
            event.getDrops().clear();
            event.setKeepInventory(true);
        }
    }

    @SuppressWarnings("deprecation") // #getLocate is needed for Spigot compatibility
    @EventHandler
    public void onLocaleChange(final PlayerLocaleChangeEvent event) {
        // The event is fired before the player is deemed online upon login
        if (!event.getPlayer().isOnline()) {
            return;
        }
        BukkitPlayer player = BukkitUtil.adapt(event.getPlayer());
        // we're stripping the country code as we don't want to differ between countries
        player.setLocale(Locale.forLanguageTag(event.getLocale().substring(0, 2)));
    }

    @EventHandler
    public void onPortalEnter(PlayerPortalEvent event) {
        Location location = BukkitUtil.adapt(event.getPlayer().getLocation());
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DenyPortalTravelFlag.class, true)) {
                event.setCancelled(true);
            }
            return;
        }
        if (plot.getFlag(DenyPortalTravelFlag.class)) {
            plot.debug(event.getPlayer().getName() + " did not travel thru a portal because of deny-portal-travel = true");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortalCreation(PortalCreateEvent event) {
        String world = event.getWorld().getName();
        if (PlotSquared.get().getPlotAreaManager().getPlotAreasSet(world).size() == 0) {
            return;
        }
        BukkitPlayer pp = (event.getEntity() instanceof Player player) ? BukkitUtil.adapt(player) : null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockState state : event.getBlocks()) {
            minX = Math.min(state.getX(), minX);
            maxX = Math.max(state.getX(), maxX);
            minZ = Math.min(state.getZ(), minZ);
            maxZ = Math.max(state.getZ(), maxZ);
        }
        int y = event.getBlocks().get(0).getY(); // Don't need to worry about this too much
        for (Location location : List.of( // We don't care about duplicate locations
                Location.at(world, minX, y, minZ),
                Location.at(world, minX, y, maxZ),
                Location.at(world, maxX, y, minZ),
                Location.at(world, maxX, y, maxZ)
        )) {
            PlotArea area = location.getPlotArea();
            if (area == null) {
                continue;
            }
            if (area.notifyIfOutsideBuildArea(pp, location.getY())) {
                event.setCancelled(true);
                return;
            }
            Plot plot = location.getOwnedPlot();
            if (plot == null) {
                if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, DenyPortalsFlag.class, true)) {
                    event.setCancelled(true);
                    return;
                }
                continue;
            }
            if (plot.getFlag(DenyPortalsFlag.class)) {
                StringBuilder builder = new StringBuilder();
                if (event.getEntity() != null) {
                    builder.append(event.getEntity().getName()).append(" did not create a portal");
                } else {
                    builder.append("Portal creation cancelled");
                }
                plot.debug(builder.append(" because of deny-portals = true").toString());
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerTakeLecternBook(PlayerTakeLecternBookEvent event) {
        Player player = event.getPlayer();
        BukkitPlayer pp = BukkitUtil.adapt(player);
        Location location = pp.getLocation();
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return;
        }
        Plot plot = location.getOwnedPlot();
        if (plot == null) {
            if (PlotFlagUtil.isAreaRoadFlagsAndFlagEquals(area, LecternReadBookFlag.class, true)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!plot.isAdded(pp.getUUID())) {
            if (plot.getFlag(LecternReadBookFlag.class)) {
                plot.debug(event.getPlayer().getName() + " could not take the book because of lectern-read-book = true");
                event.setCancelled(true);
            }
        }
    }

}
