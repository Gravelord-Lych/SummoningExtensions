package lych.summoningex.command;

import com.google.common.collect.ImmutableCollection;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lych.summoningex.SummoningExtensions;
import lych.summoningex.command.argument.AreaNameArgument;
import lych.summoningex.command.argument.DyeColorArgument;
import lych.summoningex.world.Area;
import lych.summoningex.world.AreaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.function.ToIntBiFunction;

import static lych.summoningex.command.SummoningExCommand.SUCCESS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class AreaPart {
    private static final String DIM = "dim";
    private static final String NAME = "name";
    public static final DynamicCommandExceptionType ERROR_NO_AREA_WITH_NAME_FOUND = new DynamicCommandExceptionType(o -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.area_not_found"), o));
    public static final DynamicCommandExceptionType ERROR_NO_AREA_FOUND_IN_LEVEL = new DynamicCommandExceptionType(o -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.area_not_found_in_level"), o));
    public static final SimpleCommandExceptionType ERROR_NO_AREAS = new SimpleCommandExceptionType(Component.translatable(SummoningExtensions.prefixCommandMsg("area.no_areas")));
    public static final SimpleCommandExceptionType ERROR_ALREADY_SHOWN = new SimpleCommandExceptionType(Component.translatable(SummoningExtensions.prefixCommandMsg("area.already_shown_areas")));
    public static final SimpleCommandExceptionType ERROR_ALREADY_HID = new SimpleCommandExceptionType(Component.translatable(SummoningExtensions.prefixCommandMsg("area.already_hidden_areas")));

    private AreaPart() {}

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("area")
                .then(literal("define")
                        .then(argument("from", BlockPosArgument.blockPos())
                                .then(argument("to", BlockPosArgument.blockPos())
                                        .then(literal("as")
                                                .then(argument(NAME, AreaNameArgument.areaNoSuggestion())
                                                        .executes(context -> addArea(context,
                                                                AreaNameArgument.getAreaName(context, NAME),
                                                                BlockPosArgument.getBlockPos(context, "from"),
                                                                BlockPosArgument.getBlockPos(context, "to")))
                                                        .then(literal("colored")
                                                                .then(literal("preset")
                                                                        .then(argument("color", DyeColorArgument.dyeColor())
                                                                                .executes(addAreaUsingColor(DyeColorArgument::getIntDyeColor, false))
                                                                                .then(defineAreaInCustomDimension(DyeColorArgument::getIntDyeColor))))
                                                                .then(literal("custom")
                                                                        .then(argument("color", IntegerArgumentType.integer(0, 16777215))
                                                                                .executes(addAreaUsingColor(IntegerArgumentType::getInteger, false))
                                                                                .then(defineAreaInCustomDimension(IntegerArgumentType::getInteger)))))
                                                        .then(defineAreaInCustomDimension()))))))
                .then(literal("remove")
                        .then(literal("all")
                                .executes(AreaPart::removeAllAreas)
                                .then(literal("in")
                                        .then(argument(DIM, DimensionArgument.dimension())
                                                .executes(context -> removeAllAreas(context, DimensionArgument.getDimension(context, DIM)))))
                                .then(literal("completely")
                                        .executes(AreaPart::removeAllAreasInAllLevels)))
                        .then(literal("local")
                                .then(argument(NAME, AreaNameArgument.area())
                                        .executes(context -> removeArea(context, AreaNameArgument.getAreaName(context, NAME)))))
                        .then(literal("global")
                                .then(argument(DIM, DimensionArgument.dimension())
                                        .then(argument(NAME, AreaNameArgument.areaNoSuggestion())
                                                .executes(context -> removeArea(context,
                                                        AreaNameArgument.getAreaName(context, NAME),
                                                        DimensionArgument.getDimension(context, DIM)))))))
                .then(literal("query")
                        .then(literal("local")
                                .then(argument(NAME, AreaNameArgument.area())
                                        .executes(context -> queryArea(context, AreaNameArgument.getAreaName(context, NAME)))))
                        .then(literal("global")
                                .then(argument(DIM, DimensionArgument.dimension())
                                        .then(argument(NAME, AreaNameArgument.areaNoSuggestion())
                                                .executes(context -> queryArea(context,
                                                        AreaNameArgument.getAreaName(context, NAME),
                                                        DimensionArgument.getDimension(context, DIM)))))))
                .then(literal("list")
                        .executes(AreaPart::listArea)
                        .then(literal("in")
                                .then(argument(DIM, DimensionArgument.dimension())
                                        .executes(context -> listArea(context, DimensionArgument.getDimension(context, DIM))))))
                .then(literal("show")
                        .executes(context -> showAreas(context, true)))
                .then(literal("hide")
                        .executes(context -> showAreas(context, false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> defineAreaInCustomDimension() {
        return defineAreaInCustomDimension((context, st) -> -1);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> defineAreaInCustomDimension(ToIntBiFunction<? super CommandContext<CommandSourceStack>, ? super String> colorFunction) {
        return literal("in")
                .then(argument(DIM, DimensionArgument.dimension())
                        .executes(addAreaUsingColor(colorFunction, true)));
    }

    private static Command<CommandSourceStack> addAreaUsingColor(ToIntBiFunction<? super CommandContext<CommandSourceStack>, ? super String> colorFunction, boolean hasDimension) {
        return context -> addArea(context,
                AreaNameArgument.getAreaName(context, NAME),
                BlockPosArgument.getBlockPos(context, "from"),
                BlockPosArgument.getBlockPos(context, "to"),
                colorFunction.applyAsInt(context, "color"),
                hasDimension ? DimensionArgument.getDimension(context, DIM) : context.getSource().getLevel());
    }

    private static int addArea(CommandContext<CommandSourceStack> context, String name, BlockPos from, BlockPos to) {
        return addArea(context, name, from, to, -1, context.getSource().getLevel());
    }

    private static int addArea(CommandContext<CommandSourceStack> context, String name, BlockPos from, BlockPos to, int color, ServerLevel level) {
//      Negative color means that random color will be used
        Area area = color < 0 ? Area.create(name, from, to, level.random) : Area.create(name, from, to, color);
        Area oldArea = AreaManager.get(level).add(area);
        if (oldArea == null) {
            context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.add_area"),
                    area.getFormattedName(),
                    level.dimension().location()), true);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.replace_area"),
                    area.getFormattedName(),
                    level.dimension().location()), true);
        }
        return SUCCESS;
    }

    private static int removeAllAreas(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return removeAllAreas(context, context.getSource().getLevel());
    }

    private static int removeAllAreasInAllLevels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int count = 0;
        for (ServerLevel level : context.getSource().getServer().getAllLevels()) {
            if (AreaManager.get(level).clear()) {
                count++;
            }
        }
        if (count == 0) {
            throw ERROR_NO_AREAS.create();
        }
        context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.remove_all_areas")), true);
        return count;
    }

    private static int removeAllAreas(CommandContext<CommandSourceStack> context, ServerLevel level) throws CommandSyntaxException {
        boolean cleared = AreaManager.get(level).clear();
        if (cleared) {
            context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.remove_all_areas_in_level"),
                    level.dimension().location()), true);
            return SUCCESS;
        }
        throw ERROR_NO_AREA_FOUND_IN_LEVEL.create(level.dimension().location());
    }

    private static int removeArea(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return removeArea(context, name, context.getSource().getLevel());
    }

    private static int removeArea(CommandContext<CommandSourceStack> context, String name, ServerLevel level) throws CommandSyntaxException {
        Area area = AreaManager.get(level).remove(name);
        if (area == null) {
            throw ERROR_NO_AREA_WITH_NAME_FOUND.create(name);
        }
        context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.remove_area"),
                area.getFormattedName(),
                level.dimension().location()), true);
        return SUCCESS;
    }

    private static int queryArea(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return queryArea(context, name, context.getSource().getLevel());
    }

    private static int queryArea(CommandContext<CommandSourceStack> context, String name, ServerLevel level) throws CommandSyntaxException {
        Area area = AreaManager.get(level).byName(name);
        if (area == null) {
            throw ERROR_NO_AREA_WITH_NAME_FOUND.create(name);
        }
        context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.query_area"),
                area.getFormattedName(),
                area.getDescription(level)), false);
        return SUCCESS;
    }

    private static int listArea(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return listArea(context, context.getSource().getLevel());
    }

    private static int listArea(CommandContext<CommandSourceStack> context, ServerLevel level) throws CommandSyntaxException {
        ImmutableCollection<Area> areas = AreaManager.get(level).getAreas().values();
        if (areas.isEmpty()) {
            throw ERROR_NO_AREA_FOUND_IN_LEVEL.create(level.dimension().location());
        }
        context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg("area.list_area"), level.dimension().location()), false);

        for (Area area : areas) {
            context.getSource().sendSuccess(() -> area.getFormattedName()
                    .append(Component.translatable(SummoningExtensions.prefixMsg("message", "colon")))
                    .append(area.getDescription(level)), false);
        }
        return SUCCESS;
    }

    private static int showAreas(CommandContext<CommandSourceStack> context, boolean show) throws CommandSyntaxException {
        int count = 0;
        for (ServerLevel level : context.getSource().getServer().getAllLevels()) {
            count += AreaManager.get(level).setShowsAreas(show);
        }
        if (count == 0) {
            throw show ? ERROR_ALREADY_SHOWN.create() : ERROR_ALREADY_HID.create();
        }
        context.getSource().sendSuccess(() -> Component.translatable(SummoningExtensions.prefixCommandMsg(show ? "area.show_areas" : "area.hide_areas")), true);
        return count;
    }
}
