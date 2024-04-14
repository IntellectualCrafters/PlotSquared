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
package com.plotsquared.core.commands.parser;

import com.plotsquared.core.configuration.caption.LocaleHolder;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.FlagContainer;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import com.plotsquared.core.plot.flag.InternalFlag;
import com.plotsquared.core.plot.flag.PlotFlag;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.Collection;
import java.util.function.Function;

/**
 * Parser that parses and suggests {@link PlotFlag plot flags}.
 */
public final class PlotFlagParser implements ArgumentParser<PlotPlayer<?>, PlotFlag<?, ?>>,
        BlockingSuggestionProvider<PlotPlayer<?>> {

    /**
     * Returns a new parser that parses {@link PlotFlag plot flags}.
     *
     * @param source the source of available flag values
     * @return the parser
     */
    public static @NonNull ParserDescriptor<PlotPlayer<?>, PlotFlag<?, ?>> plotFlagParser(final @NonNull FlagSource source) {
        return ParserDescriptor.of(new PlotFlagParser(source), new TypeToken<PlotFlag<?, ?>>() {
        });
    }

    private final FlagSource flagSource;

    private PlotFlagParser(final @NonNull FlagSource flagSource) {
        this.flagSource = flagSource;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull PlotFlag<?, ?>> parse(
            final @NonNull CommandContext<@NonNull PlotPlayer<?>> commandContext,
            final @NonNull CommandInput commandInput
    ) {
        final String flagName = commandInput.readString();
        final PlotFlag<?, ?> flag = GlobalFlagContainer.getInstance().getFlagFromString(flagName);
        if (flag == null) {
            return ArgumentParseResult.failure(new PlotFlagParseException(commandContext));
        }
        return ArgumentParseResult.success(flag);
    }

    @Override
    public @NonNull Iterable<@NonNull Suggestion> suggestions(
            final @NonNull CommandContext<PlotPlayer<?>> context,
            final @NonNull CommandInput input
    ) {
        return this.flagSource.flags(context.sender())
                .stream()
                .filter(flag -> (!(flag instanceof InternalFlag)))
                .map(PlotFlag::getName)
                .map(Suggestion::simple)
                .toList();
    }

    public enum FlagSource {
        /**
         * All recognized flags.
         */
        GLOBAL(player -> GlobalFlagContainer.getInstance(), false),
        /**
         * All flags that have been configured in the current plot.
         */
        PLOT(player -> {
            final Plot plot = player.getCurrentPlot();
            if (plot == null) {
                return GlobalFlagContainer.getInstance();
            }
            return plot.getFlagContainer();
        }, true);

        private final Function<PlotPlayer<?>, FlagContainer> containerFunction;
        private final boolean storedOnly;

        FlagSource(final @NonNull Function<PlotPlayer<?>, FlagContainer> containerFunction, final boolean storedOnly) {
            this.containerFunction = containerFunction;
            this.storedOnly = storedOnly;
        }

        /**
         * Returns the flag container.
         *
         * @param player the player to get the container for
         * @return the container
         */
        public @NonNull FlagContainer flagContainer(final @NonNull PlotPlayer<?> player) {
            return this.containerFunction.apply(player);
        }

        /**
         * Returns the flags from this source.
         *
         * @param player the player to get the flags for
         * @return the flags
         */
        public @NonNull Collection<@NonNull PlotFlag<?, ?>> flags(final @NonNull PlotPlayer<?> player) {
            final FlagContainer container = this.flagContainer(player);
            if (this.storedOnly) {
                return container.getFlagMap().values();
            }
            return container.getRecognizedPlotFlags();
        }
    }

    /**
     * Exception thrown when an invalid flag name is supplied.
     */
    public static final class PlotFlagParseException extends ParserException implements ComponentMessageThrowable {

        private PlotFlagParseException(final @NonNull CommandContext<?> context) {
            super(PlotFlagParser.class, context, TranslatableCaption.of("flag.not_valid_flag"));
        }

        @Override
        public @NonNull Component componentMessage() {
            // TODO(City): This sucks...
            return ((TranslatableCaption) this.errorCaption()).toComponent(LocaleHolder.console());
        }
    }
}
