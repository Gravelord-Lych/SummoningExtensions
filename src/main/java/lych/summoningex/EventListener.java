package lych.summoningex;

import lych.summoningex.world.AreaManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SummoningExtensions.MOD_ID)
public class EventListener {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Level level = event.getEntity().level();
        if (level instanceof ServerLevel sl) {
//          Synchronizes the data of AreaManager to the client, this will call the load method of SavedData
            sl.getServer().getAllLevels().forEach(AreaManager::get);
        }
    }
}
