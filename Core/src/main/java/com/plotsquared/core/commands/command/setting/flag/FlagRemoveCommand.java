package com.plotsquared.core.commands.command.setting.flag;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.keys.CloudKey;
import com.google.inject.Inject;
import com.plotsquared.core.commands.parser.PlotFlagParser;
import com.plotsquared.core.commands.suggestions.FlagValueSuggestionProvider;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.PlotFlagAddEvent;
import com.plotsquared.core.events.PlotFlagRemoveEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.FlagParseException;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.types.ListFlag;
import com.plotsquared.core.util.EventDispatcher;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static cloud.commandframework.arguments.standard.StringParser.greedyStringParser;
import static com.plotsquared.core.commands.parser.PlotFlagParser.plotFlagParser;

public final class FlagRemoveCommand extends FlagCommandBean {

    private static final CloudKey<PlotFlag<?, ?>> COMPONENT_FLAG = CloudKey.of("flag", new TypeToken<PlotFlag<?, ?>>() {});
    private static final CloudKey<String> COMPONENT_VALUE = CloudKey.of("value", String.class);

    private final EventDispatcher eventDispatcher;

    @Inject
    public FlagRemoveCommand(final @NonNull EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    protected Command.@NonNull Builder<PlotPlayer<?>> configurePlotCommand(
            final Command.@NonNull Builder<PlotPlayer<?>> builder
    ) {
        return builder.literal("remove")
                .required(COMPONENT_FLAG, plotFlagParser(PlotFlagParser.FlagSource.PLOT))
                .optional(COMPONENT_VALUE, greedyStringParser(), new FlagValueSuggestionProvider(COMPONENT_FLAG));
    }

    @Override
    public void execute(final @NonNull CommandContext<PlotPlayer<?>> commandContext) {
        final PlotPlayer<?> player = commandContext.sender();
        final Plot plot = commandContext.inject(Plot.class).orElseThrow();
        final PlotFlag<?, ?> flag = commandContext.get(COMPONENT_FLAG);
        final String flagValue = commandContext.getOrDefault(COMPONENT_VALUE, null);

        final PlotFlagRemoveEvent event = this.eventDispatcher.callFlagRemove(flag, plot);
        if (event.getEventResult() == Result.DENY) {
            player.sendMessage(
                    TranslatableCaption.of("events.event_denied"),
                    TagResolver.resolver("value", Tag.inserting(Component.text("Flag set")))
            );
            return;
        }
        final String flagKey = flag.getName().toLowerCase(Locale.ENGLISH);
        if (event.getEventResult() != Result.FORCE
                && !player.hasPermission(Permission.PERMISSION_SET_FLAG_KEY.format(flagKey))) {
            if (flagValue == null) {
                player.sendMessage(
                        TranslatableCaption.of("permission.no_permission"),
                        TagResolver.resolver(
                                "node",
                                Tag.inserting(Component.text(Permission.PERMISSION_SET_FLAG_KEY.format(flagKey)))
                        )
                );
                return;
            }
        }

        if (flagValue != null && flag instanceof ListFlag<?,?> listFlag) {
            final List<?> list = new ArrayList<>(plot.getFlag(listFlag));
            final PlotFlag parsedFlag;
            try {
                parsedFlag = listFlag.parse(flagValue);
            } catch (final FlagParseException e) {
                player.sendMessage(
                        TranslatableCaption.of("flag.flag_parse_error"),
                        TagResolver.builder()
                                .tag("flag_name", Tag.inserting(Component.text(flag.getName())))
                                .tag("flag_value", Tag.inserting(Component.text(e.getValue())))
                                .tag("error", Tag.inserting(e.getErrorMessage().toComponent(player)))
                                .build()
                );
                return;
            }
            if (((List<?>) parsedFlag.getValue()).isEmpty()) {
                player.sendMessage(TranslatableCaption.of("flag.flag_not_removed"));
                return;
            }
            if (list.removeAll((List) parsedFlag.getValue())) {
                if (list.isEmpty()) {
                    if (plot.removeFlag(flag)) {
                        player.sendMessage(
                                TranslatableCaption.of("flag.flag_removed"),
                                TagResolver.builder()
                                        .tag("flag", Tag.inserting(Component.text(flagKey)))
                                        .tag("value", Tag.inserting(Component.text(flag.toString())))
                                        .build()
                        );
                        return;
                    } else {
                        player.sendMessage(TranslatableCaption.of("flag.flag_not_removed"));
                        return;
                    }
                } else {
                    PlotFlag<?, ?> plotFlag = parsedFlag.createFlagInstance(list);
                    PlotFlagAddEvent addEvent = eventDispatcher.callFlagAdd(plotFlag, plot);
                    if (addEvent.getEventResult() == Result.DENY) {
                        player.sendMessage(
                                TranslatableCaption.of("events.event_denied"),
                                TagResolver.resolver(
                                        "value",
                                        Tag.inserting(Component.text("Re-addition of " + plotFlag.getName()))
                                )
                        );
                        return;
                    }
                    if (plot.setFlag(addEvent.getFlag())) {
                        player.sendMessage(TranslatableCaption.of("flag.flag_partially_removed"));
                        return;
                    } else {
                        player.sendMessage(TranslatableCaption.of("flag.flag_not_removed"));
                        return;
                    }
                }
            }
        } else if (!plot.removeFlag(flag)) {
            player.sendMessage(TranslatableCaption.of("flag.flag_not_removed"));
            return;
        }
        player.sendMessage(
                TranslatableCaption.of("flag.flag_removed"),
                TagResolver.builder()
                        .tag("flag", Tag.inserting(Component.text(flagKey)))
                        .tag("value", Tag.inserting(Component.text(flag.toString())))
                        .build()
        );
    }
}