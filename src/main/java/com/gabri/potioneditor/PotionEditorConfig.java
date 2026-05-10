package com.gabri.potioneditor;

import net.minecraftforge.common.ForgeConfigSpec;

public class PotionEditorConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Potion Editor Settings
    public static final ForgeConfigSpec.BooleanValue EDIT_POTION_EDITOR;
    public static final ForgeConfigSpec.IntValue POTION_RESISTANCE;
    public static final ForgeConfigSpec.IntValue POTION_SLOWNESS;
    public static final ForgeConfigSpec.IntValue STRONG_POTION_RESISTANCE;
    public static final ForgeConfigSpec.IntValue STRONG_POTION_SLOWNESS;

    static {
        BUILDER.push("Potion Editor Settings");

        EDIT_POTION_EDITOR = BUILDER
                .comment("If true, the potion effects will be modified to the levels below.")
                .define("editPotionEditor", true);
        
        POTION_RESISTANCE = BUILDER
                .comment("Resistance level for Standard and Long potions.")
                .defineInRange("potionResistance", 3, 1, 10);
        
        POTION_SLOWNESS = BUILDER
                .comment("Slowness level for Standard and Long potions.")
                .defineInRange("potionSlowness", 4, 1, 10);
        
        STRONG_POTION_RESISTANCE = BUILDER
                .comment("Resistance level for Strong potions.")
                .defineInRange("strongPotionResistance", 3, 1, 10);
        
        STRONG_POTION_SLOWNESS = BUILDER
                .comment("Slowness level for Strong potions.")
                .defineInRange("strongPotionSlowness", 4, 1, 10);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
