package lych.summoningex.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lych.summoningex.SummoningExtensions;
import lych.summoningex.client.ClientAreaManager;
import lych.summoningex.world.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class AreaNameArgument implements ArgumentType<String> {
    public static final DynamicCommandExceptionType ERROR_INVALID_AREA = new DynamicCommandExceptionType(o -> Component.translatable(SummoningExtensions.prefixMsg("argument", "area.invalid_start"), o));
    private static final Collection<String> EXAMPLES = Arrays.asList("#Pig", "#1", "#F-777");
    private final boolean showsSuggestions;

    public AreaNameArgument(boolean showsSuggestions) {
        this.showsSuggestions = showsSuggestions;
    }

    public static AreaNameArgument area() {
        return new AreaNameArgument(true);
    }

    public static AreaNameArgument areaNoSuggestion() {
        return new AreaNameArgument(false);
    }

    public static String getAreaName(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        int pre = reader.read();
        if (pre == '#') {
            return reader.readString();
        }
        reader.setCursor(start);
        String name = reader.readString();
        reader.setCursor(start);
        throw ERROR_INVALID_AREA.create(name);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!showsSuggestions) {
            return Suggestions.empty();
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            return suggestLocalAreas(builder, level);
        }
        return Suggestions.empty();
    }

    public static CompletableFuture<Suggestions> suggestLocalAreas(SuggestionsBuilder builder, ClientLevel level) {
        return SharedSuggestionProvider.suggest(ClientAreaManager.getInstanceIn(level.dimension()).getAreas().keySet().stream().map(Area::format), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info implements ArgumentTypeInfo<AreaNameArgument, Info.Template> {
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            buf.writeBoolean(template.showsSuggestions);
        }

        public Template deserializeFromNetwork(FriendlyByteBuf buf) {
            return new Template(buf.readBoolean());
        }

        public void serializeToJson(Template template, JsonObject object) {
            object.addProperty("shows_suggestions", template.showsSuggestions);
        }

        public Template unpack(AreaNameArgument arg) {
            return new Template(arg.showsSuggestions);
        }

        public final class Template implements ArgumentTypeInfo.Template<AreaNameArgument> {
            final boolean showsSuggestions;

            Template(boolean showsSuggestions) {
                this.showsSuggestions = showsSuggestions;
            }

            public AreaNameArgument instantiate(CommandBuildContext context) {
                return new AreaNameArgument(showsSuggestions);
            }

            public ArgumentTypeInfo<AreaNameArgument, ?> type() {
                return Info.this;
            }
        }
    }
}
