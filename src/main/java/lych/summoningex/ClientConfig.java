package lych.summoningex;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.IntValue AREA_RENDER_DISTANCE;

    private ClientConfig() {}

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Render Settings");
        builder.push("renderDistance");
        AREA_RENDER_DISTANCE = builder
                .comment("Max summoning area render distance. If the distance between you and a " +
                        "summoning area exceeds this value, the area will not be rendered")
                .defineInRange("maxSummoningAreaRenderDistance", 100, 30, 300);
        builder.pop();

        CONFIG = builder.build();
    }
}
