package lych.summoningex.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lych.summoningex.SummoningExtensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class DyeColorArgument implements ArgumentType<DyeColor> {
    private static final Collection<String> EXAMPLES = Arrays.asList("white", "light_blue", "cyan", "red");
    private static final DynamicCommandExceptionType INVALID_DYE_COLOR = new DynamicCommandExceptionType(
            o -> Component.translatable(SummoningExtensions.prefixMsg("argument", "dye_color.invalid_dye_color"), o));

    public static DyeColorArgument dyeColor() {
        return new DyeColorArgument();
    }

    public static int getIntDyeColor(CommandContext<CommandSourceStack> context, String name) {
        int textColor = getDyeColor(context, name).getTextColor();
        float[] hsbvals = new float[3];
        Color.RGBtoHSB(textColor >> 16 & 0xFF, textColor >> 8 & 0xFF, textColor & 0xFF, hsbvals);
//      Restricts the saturation
        hsbvals[1] = Math.min(0.88f, hsbvals[1]);
        int color = Color.HSBtoRGB(hsbvals[0], hsbvals[1], hsbvals[2]);
//      Make sure the color value is positive
        color &= 0xffffff;
        return color;
    }

    public static DyeColor getDyeColor(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, DyeColor.class);
    }

    @Override
    public DyeColor parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString();
        DyeColor color = DyeColor.byName(name, null);
        if (color == null) {
            throw INVALID_DYE_COLOR.createWithContext(reader, name);
        }
        return color;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(DyeColor.values()).map(DyeColor::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
