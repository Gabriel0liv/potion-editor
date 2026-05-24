package com.gabri.potioneditor.potion;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum PotionForm {
    DRINK("drink", Items.POTION),
    SPLASH("splash", Items.SPLASH_POTION),
    LINGERING("lingering", Items.LINGERING_POTION);

    private final String id;
    private final net.minecraft.world.item.Item item;

    PotionForm(String id, net.minecraft.world.item.Item item) {
        this.id = id;
        this.item = item;
    }

    public String id() {
        return id;
    }

    public net.minecraft.world.item.Item item() {
        return item;
    }

    public Component label() {
        return Component.translatable("potioneditor.form." + id);
    }

    public static PotionForm fromItemStack(ItemStack stack) {
        if (stack.is(Items.SPLASH_POTION)) {
            return SPLASH;
        }

        if (stack.is(Items.LINGERING_POTION)) {
            return LINGERING;
        }

        return DRINK;
    }

    public static PotionForm fromId(String id) {
        for (PotionForm form : values()) {
            if (form.id.equalsIgnoreCase(id)) {
                return form;
            }
        }
        return DRINK;
    }
}

