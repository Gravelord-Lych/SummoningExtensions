package lych.summoningex.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lych.summoningex.CommonConfig;
import lych.summoningex.SummoningExtensions;
import lych.summoningex.client.ClientAreaManager;
import lych.summoningex.command.argument.AreaNameArgument;
import lych.summoningex.world.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.command.EntitySelectorManager;
import net.minecraftforge.common.command.IEntitySelectorType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = SummoningExtensions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public enum EntitiesInAreaSelector implements IEntitySelectorType {
    INSTANCE;

    public static final SimpleCommandExceptionType ERROR_MISSING_AREA = new SimpleCommandExceptionType(Component.translatable(SummoningExtensions.prefixMsg("argument", "entity.selector.missing_area")));

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        EntitySelectorManager.register(CommonConfig.SELECTOR_TOKEN.get(), EntitiesInAreaSelector.INSTANCE);
    }

    @Override
    public EntitySelector build(EntitySelectorParser parser) throws CommandSyntaxException {
        StringReader reader = parser.getReader();
        parser.setSuggestions(EntitiesInAreaSelector::suggestAreaStart);

        if (reader.canRead() && reader.peek() == '(') {
            reader.skip();

            int start = reader.getCursor();

            Level level;
            try {
                level = Minecraft.getInstance().level;
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("dist")) {
                    level = null;
                } else {
                    throw e;
                }
            }
            if (level != null) {
                Level finalLevel = level;
                parser.setSuggestions((builder, consumer) -> AreaNameArgument.suggestLocalAreas(builder, (ClientLevel) finalLevel));

                if (reader.canRead() && reader.peek() != '#') {
                    String name = reader.readString();
                    reader.setCursor(start);
                    throw AreaNameArgument.ERROR_INVALID_AREA.create(name);
                }

                reader.skip();

                String name = reader.readString();
                ClientAreaManager manager = ClientAreaManager.getInstanceIn(level.dimension());
                Area area = manager.get(name);

                if (area == null) {
                    reader.setCursor(start);
                    throw AreaPart.ERROR_NO_AREA_WITH_NAME_FOUND.create(name);
                }

//                reader.skip();
                parser.setSuggestions(EntitiesInAreaSelector::suggestAreaEnd);

                if (reader.canRead() && reader.peek() == ')') {
                    reader.skip();
                    parser.addPredicate(entity -> entity.getBoundingBox().intersects(area.asAABB()));

                    parser.setSuggestions(EntitiesInAreaSelector::suggestOpenOptions);
                    parser.setMaxResults(Integer.MAX_VALUE);
                    parser.setIncludesEntities(true);
                    parser.setOrder(EntitySelector.ORDER_ARBITRARY);
                    parser.addPredicate(Entity::isAlive);

                    if (reader.canRead() && reader.peek() == '[') {
                        reader.skip();
                        parser.setSuggestions((builder, consumer) -> suggestOptionsKeyOrClose(parser, builder));
                        parser.parseOptions();
                    }

                    return parser.getSelector();
                }

                throw EntitySelectorParser.ERROR_EXPECTED_END_OF_OPTIONS.create();
            }
        }

        throw ERROR_MISSING_AREA.create();
    }

    private static CompletableFuture<Suggestions> suggestAreaStart(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
        builder.suggest("(");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAreaEnd(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
        builder.suggest(")");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOpenOptions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer) {
        builder.suggest("[");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOptionsKeyOrClose(EntitySelectorParser parser, SuggestionsBuilder builder) {
        builder.suggest("]");
        EntitySelectorOptions.suggestNames(parser, builder);
        return builder.buildFuture();
    }

    @Override
    public Component getSuggestionTooltip() {
        return Component.translatable(SummoningExtensions.prefixMsg("argument", "entity.selector.entities_in_area"));
    }
}
