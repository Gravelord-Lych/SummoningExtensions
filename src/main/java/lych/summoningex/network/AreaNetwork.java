package lych.summoningex.network;

import lych.summoningex.SummoningExtensions;
import lych.summoningex.client.ClientAreaManager;
import lych.summoningex.world.Area;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.SortedMap;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = SummoningExtensions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AreaNetwork {
    public static final String VERSION = "1.0";
    private static SimpleChannel channel;
    private static int id = 0;

    public static int nextID() {
        return id++;
    }

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        event.enqueueWork(AreaNetwork::registerChannel);
    }

    public static void registerChannel() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(SummoningExtensions.MOD_ID, "areas"),
                () -> VERSION,
                version -> version.equals(VERSION),
                version -> version.equals(VERSION)
        );
        channel.messageBuilder(OperationPacket.class, nextID())
                .encoder(OperationPacket::encode)
                .decoder(OperationPacket::decode)
                .consumerMainThread(AreaNetwork::handle)
                .add();
    }

    public static void sync(ServerLevel level,  Operation operation) {
        sync(level, null, operation);
    }

    public static void sync(ServerLevel level, @Nullable Area area, Operation operation) {
        getChannel().send(PacketDistributor.ALL.noArg(), new OperationPacket(level.dimension(), area, operation));
    }

    public static SimpleChannel getChannel() {
        return channel;
    }

    private static void handle(OperationPacket area, Supplier<NetworkEvent.Context> sup) {
        SummoningExtensions.LOGGER.debug("Operated area %s clientside".formatted(area));
        ClientAreaManager.getInstanceIn(area.dim()).accept(area.area(), area.operation());
    }

    public record OperationPacket(ResourceKey<Level> dim, @Nullable Area area, Operation operation) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeResourceKey(dim);
            if (area != null) {
//              The area is present
                buf.writeBoolean(true);
                area.encode(buf);
            } else {
//              The area is absent
                buf.writeBoolean(false);
            }
            buf.writeEnum(operation);
        }

        public static OperationPacket decode(FriendlyByteBuf buf) {
//          Only decodes the area if the area exists
            return new OperationPacket(buf.readResourceKey(Registries.DIMENSION), buf.readBoolean() ? Area.decode(buf) : null, buf.readEnum(Operation.class));
        }
    }

    public enum Operation {
        ADD {
            @Override
            public void operate(ClientAreaManager manager, SortedMap<String, Area> map, @Nullable Area area) {
                Objects.requireNonNull(area);
                map.put(area.getName(), area);
            }
        },
        CLEAR {
            @Override
            public void operate(ClientAreaManager manager, SortedMap<String, Area> map, @Nullable Area area) {
                map.clear();
            }
        },
        REMOVE {
            @Override
            public void operate(ClientAreaManager manager, SortedMap<String, Area> map, @Nullable Area area) {
                Objects.requireNonNull(area);
                map.remove(area.getName());
            }
        },
        SHOW {
            @Override
            public void operate(ClientAreaManager manager, SortedMap<String, Area> map, @Nullable Area area) {
                manager.setShowsAreas(true);
            }
        },
        HIDE {
            @Override
            public void operate(ClientAreaManager manager, SortedMap<String, Area> map, @Nullable Area area) {
                manager.setShowsAreas(false);
            }
        };

        public abstract void operate(ClientAreaManager manager, SortedMap<String, Area> map, @Nullable Area area);
    }
}
