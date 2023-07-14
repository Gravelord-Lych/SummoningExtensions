package lych.summoningex.command;

import lych.summoningex.SummoningExtensions;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SummoningExtensions.MOD_ID)
public class SummoningExCommand {
    public static final int SUCCESS = 1;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(SummoningExtensions.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(SummonPart.register(event.getBuildContext()))
                .then(AreaPart.register()));
    }
}
