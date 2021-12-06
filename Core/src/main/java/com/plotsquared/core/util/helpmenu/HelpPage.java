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
 *                  Copyright (C) 2021 IntellectualSites
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
package com.plotsquared.core.util.helpmenu;

import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.util.StringMan;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;

import java.util.ArrayList;
import java.util.List;

public class HelpPage {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
    private final List<HelpObject> helpObjects;
    private final Template catTemplate;
    private final Template curTemplate;
    private final Template maxTemplate;

    public HelpPage(CommandCategory category, int currentPage, int maxPages) {
        this.helpObjects = new ArrayList<>();
        this.catTemplate = Template.template("category", category == null ? "ALL" : category.name());
        this.curTemplate = Template.template("current", String.valueOf(currentPage + 1));
        this.maxTemplate = Template.template("max", String.valueOf(maxPages + 1));
    }

    public void render(PlotPlayer<?> player) {
        if (this.helpObjects.size() < 1) {
            player.sendMessage(TranslatableCaption.of("help.no_permission"));
        } else {
            Template header = Template.template("header", TranslatableCaption.of("help.help_header").getComponent(player));
            Template page_header = Template.template(
                    "page_header",
                    MINI_MESSAGE.deserialize(
                            TranslatableCaption.of("help.help_page_header").getComponent(player),
                            TemplateResolver.templates(
                                    catTemplate,
                                    curTemplate,
                                    maxTemplate)
                    )
            );
            Template help_objects = Template.template("help_objects", StringMan.join(this.helpObjects, "\n"));
            Template footer = Template.template("footer", TranslatableCaption.of("help.help_footer").getComponent(player));
            player.sendMessage(
                    StaticCaption.of("<header>\n<page_header>\n<help_objects>\n<footer>"),
                    header,
                    page_header,
                    help_objects,
                    footer
            );
        }
    }

    public void addHelpItem(HelpObject object) {
        this.helpObjects.add(object);
    }

}
