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
 *               Copyright (C) 2014 - 2022 IntellectualSites
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
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.core.command;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.sk89q.worldedit.command.util.SuggestionHelper;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.biome.BiomeTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandDeclaration(command = "setbiome",
        permission = "plots.set.biome",
        usage = "/plot biome [biome]",
        aliases = {"biome", "sb", "setb", "b"},
        category = CommandCategory.APPEARANCE,
        requiredType = RequiredType.NONE)
public class Biome extends SetCommand {

    @Override
    public boolean set(final PlotPlayer<?> player, final Plot plot, final String value) {
        BiomeType biome = null;
        try {
            biome = BiomeTypes.get(value.toLowerCase());
        } catch (final Exception ignore) {
        }
        if (biome == null) {
            Component separator = MINI_MESSAGE.deserialize(
                    TranslatableCaption.of("blocklist.block_list_separator").getComponent(player)
            );
            TextComponent.Builder biomes = Component.text();
            final Collection<BiomeType> biomeTypes = BiomeType.REGISTRY.values();
            int counter = 0;
            for (final BiomeType biomeType : biomeTypes) {
                if (counter++ > 0) {
                    biomes.append(separator);
                }
                biomes.append(Component.text(biomeType.toString()));
            }
            player.sendMessage(TranslatableCaption.of("biome.need_biome"));
            player.sendMessage(
                    TranslatableCaption.of("commandconfig.subcommand_set_options_header"),
                    TagResolver.resolver("values", Tag.inserting(biomes))
            );
            return false;
        }
        if (plot.getRunning() > 0) {
            player.sendMessage(TranslatableCaption.of("errors.wait_for_timer"));
            return false;
        }
        if (plot.getVolume() > Integer.MAX_VALUE) {
            player.sendMessage(TranslatableCaption.of("schematics.schematic_too_large"));
            return false;
        }
        plot.addRunning();
        plot.getPlotModificationManager().setBiome(biome, () -> {
            plot.removeRunning();
            player.sendMessage(
                    TranslatableCaption.of("biome.biome_set_to"),
                    TagResolver.resolver("value", Tag.inserting(Component.text(value.toLowerCase())))
            );
        });
        return true;
    }

    @Override
    public Collection<Command> tab(final PlotPlayer<?> player, final String[] args, final boolean space) {
        return SuggestionHelper.getNamespacedRegistrySuggestions(BiomeType.REGISTRY, args[0])
                .map(value -> value.toLowerCase(Locale.ENGLISH).replace("minecraft:", ""))
                .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ENGLISH)))
                .map(value -> new Command(null, false, value, "", RequiredType.NONE, null) {
                }).collect(Collectors.toList());
    }

}
