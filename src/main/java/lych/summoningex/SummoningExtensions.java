package lych.summoningex;

import com.mojang.logging.LogUtils;
import lych.summoningex.command.argument.AreaNameArgument;
import lych.summoningex.command.argument.DyeColorArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(SummoningExtensions.MOD_ID)
public class SummoningExtensions {
    public static final String MOD_ID = "summoningex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SummoningExtensions() {
        ArgumentTypeInfos.registerByClass(AreaNameArgument.class, new AreaNameArgument.Info());
        ArgumentTypeInfos.registerByClass(DyeColorArgument.class, SingletonArgumentInfo.contextFree(DyeColorArgument::dyeColor));
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, CommonConfig.CONFIG);
        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG);
    }

    public static String prefixCommandMsg(String msg) {
        return prefixMsg("commands", msg);
    }

    public static String prefixMsg(String type, String msg) {
        return type + "." + MOD_ID + "." + msg;
    }
}
