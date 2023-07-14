package lych.summoningex;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    public static final ForgeConfigSpec CONFIG;
    public static final ForgeConfigSpec.ConfigValue<String> SELECTOR_TOKEN;

    private CommonConfig() {}

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Command Settings");
        builder.push("entitySelectorToken");
        SELECTOR_TOKEN = builder
                .comment("The token of mod's new entity selector, must not clash with vanilla")
                .define("entitySelectorToken", "b");
        builder.pop();

        CONFIG = builder.build();
    }
}
