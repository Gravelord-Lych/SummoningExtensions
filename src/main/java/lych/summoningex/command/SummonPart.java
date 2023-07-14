package lych.summoningex.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lych.summoningex.SummoningExtensions;
import lych.summoningex.command.argument.AreaNameArgument;
import lych.summoningex.world.Area;
import lych.summoningex.world.AreaManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SummonPart {
    public static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable(SummoningExtensions.prefixCommandMsg("summon.failed")));

    private SummonPart() {}

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandBuildContext cbc) {
        return literal("summon")
                .then(argument("entity", ResourceArgument.resource(cbc, Registries.ENTITY_TYPE))
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(literal("inside")
                                .then(argument("areaName", AreaNameArgument.area())
                                        .executes(context -> summonEntities(context, 1, null))
                                        .then(literal("count")
                                                .then(argument("entityCnt", IntegerArgumentType.integer(1, 2000))
                                                        .executes(context -> summonEntities(context,  IntegerArgumentType.getInteger(context, "entityCnt"), null))
                                                        .then(literal("nbt")
                                                                .then(argument("entityNbt", CompoundTagArgument.compoundTag())
                                                                        .executes(context -> summonEntities(context,
                                                                                IntegerArgumentType.getInteger(context, "entityCnt"),
                                                                                CompoundTagArgument.getCompoundTag(context, "entityNbt"))))))))));
    }

    private static int summonEntities(CommandContext<CommandSourceStack> context, int maxCount, @Nullable CompoundTag tag) throws CommandSyntaxException {
        CommandSourceStack src = context.getSource();
        ServerLevel level = src.getLevel();
        String areaName = AreaNameArgument.getAreaName(context, "areaName");
        Area area = AreaManager.get(level).byName(areaName);
        if (area == null) {
            throw AreaPart.ERROR_NO_AREA_WITH_NAME_FOUND.create(areaName);
        }
        Entity entity = null;
        int count;
        int successCount = 0;
        for (count = 0; count < maxCount; count++) {
            try {
                entity = SummonCommand.createEntity(src,
                        ResourceArgument.getSummonableEntityType(context, "entity"),
                        area.getRandomBottomPos(level.getRandom()),
                        Objects.requireNonNullElseGet(tag, CompoundTag::new).copy(),
                        tag == null);
                successCount++;
            } catch (CommandSyntaxException e) {
                SummoningExtensions.LOGGER.warn("Failed to summon entity", e);
            }
        }
        if (successCount == 0) {
            throw ERROR_FAILED.create();
        }
        if (count == 1 && successCount == 1) {
            Entity finalEntity = Objects.requireNonNull(entity);
            src.sendSuccess(() -> Component.translatable("commands.summon.success", finalEntity.getDisplayName()), true);
            return 1;
        }
//      Used in lambda expressions
        int finalCount = count;
        int finalSuccessCount = successCount;
        if (count == successCount) {
            src.sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("summon.success"), finalSuccessCount), true);
        } else {
            src.sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("summon.success_partially"), finalCount, finalSuccessCount), true);
        }
        return successCount;
    }
}
