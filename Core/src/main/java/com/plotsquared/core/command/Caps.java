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

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.AnimalCapFlag;
import com.plotsquared.core.plot.flag.implementations.EntityCapFlag;
import com.plotsquared.core.plot.flag.implementations.HostileCapFlag;
import com.plotsquared.core.plot.flag.implementations.MiscCapFlag;
import com.plotsquared.core.plot.flag.implementations.MobCapFlag;
import com.plotsquared.core.plot.flag.implementations.VehicleCapFlag;
import com.plotsquared.core.plot.flag.implementations.VillagerCapFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import static com.plotsquared.core.util.entity.EntityCategories.CAP_ANIMAL;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_ENTITY;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MISC;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MOB;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_MONSTER;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_VEHICLE;
import static com.plotsquared.core.util.entity.EntityCategories.CAP_VILLAGER;

@CommandDeclaration(command = "caps",
        category = CommandCategory.INFO,
        usage = "/plot caps")
public class Caps extends SubCommand {

    @Override
    public boolean onCommand(final PlotPlayer<?> player, final String[] args) {
        final Plot plot = player.getCurrentPlot();
        if (plot == null) {
            player.sendMessage(TranslatableCaption.of("errors.not_in_plot"));
            return false;
        }
        if (!plot.isAdded(player.getUUID()) && !player.hasPermission(Permission.PERMISSION_ADMIN_CAPS_OTHER)) {
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    TagResolver.resolver("node", Tag.inserting(Permission.PERMISSION_ADMIN_CAPS_OTHER))
            );
            return false;
        }
        if (plot.getVolume() > Integer.MAX_VALUE) {
            player.sendMessage(TranslatableCaption.of("schematics.schematic_too_large"));
            return false;
        }
        player.sendMessage(TranslatableCaption.of("info.plot_caps_header"));
        final int[] countedEntities = plot.countEntities();
        sendFormatted(plot, player, MobCapFlag.class, countedEntities, "mobs", CAP_MOB);
        sendFormatted(plot, player, HostileCapFlag.class, countedEntities, "hostile", CAP_MONSTER);
        sendFormatted(plot, player, AnimalCapFlag.class, countedEntities, "animals", CAP_ANIMAL);
        sendFormatted(plot, player, VehicleCapFlag.class, countedEntities, "vehicle", CAP_VEHICLE);
        sendFormatted(plot, player, MiscCapFlag.class, countedEntities, "misc", CAP_MISC);
        sendFormatted(plot, player, VillagerCapFlag.class, countedEntities, "villagers", CAP_VILLAGER);
        sendFormatted(plot, player, EntityCapFlag.class, countedEntities, "entities", CAP_ENTITY);
        return true;
    }

    private <T extends PlotFlag<Integer, T>> void sendFormatted(
            final Plot plot,
            final PlotPlayer<?> player, final Class<T> capFlag, final int[] countedEntities,
            final String name, final int type
    ) {
        final int current = countedEntities[type];
        final int max = plot.getFlag(capFlag);
        final String percentage = String.format("%.1f", 100 * ((float) current / max));
        ComponentLike maxBeautified = max >= Integer.MAX_VALUE
                ? TranslatableCaption.of("info.infinite").toComponent(player)
                : Component.text(max);
        player.sendMessage(
                TranslatableCaption.of("info.plot_caps_format"),
                TagResolver.builder()
                        .tag("cap", Tag.inserting(Component.text(name)))
                        .tag("current", Tag.inserting(Component.text(current)))
                        .tag("limit", Tag.inserting(maxBeautified))
                        .tag("percentage", Tag.inserting(Component.text(percentage)))
                        .build()
        );
    }

}
