package lych.summoningex.command.argument;

import lych.summoningex.SummoningExtensions;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModArguments {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENTS = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, SummoningExtensions.MOD_ID);
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> AREA_NAME = ARGUMENTS.register("area_name", () -> ArgumentTypeInfos.registerByClass(AreaNameArgument.class, new AreaNameArgument.Info()));
    public static final RegistryObject<ArgumentTypeInfo<?, ?>> DYE_COLOR = ARGUMENTS.register("dye_color", () -> ArgumentTypeInfos.registerByClass(DyeColorArgument.class, SingletonArgumentInfo.contextFree(DyeColorArgument::dyeColor)));

    private ModArguments() {}
}
