package com.gabri.potioneditor.potion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;

public record PotionVariantKey(ResourceLocation potionId, PotionForm form) {
    public static PotionVariantKey of(ResourceLocation potionId, PotionForm form) {
        return new PotionVariantKey(potionId, form == null ? PotionForm.DRINK : form);
    }

    public static PotionVariantKey from(ItemStack stack) {
        Potion potion = PotionUtils.getPotion(stack);
        ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
        if (potionId == null) {
            potionId = ResourceLocation.withDefaultNamespace("water");
        }
        return new PotionVariantKey(potionId, PotionForm.fromItemStack(stack));
    }

    public static PotionVariantKey from(Potion potion, PotionForm form) {
        ResourceLocation potionId = net.minecraft.core.registries.BuiltInRegistries.POTION.getKey(potion);
        if (potionId == null) {
            potionId = ResourceLocation.withDefaultNamespace("water");
        }
        return new PotionVariantKey(potionId, form == null ? PotionForm.DRINK : form);
    }

    public static PotionVariantKey fromTag(CompoundTag tag) {
        Potion potion = PotionUtils.getPotion(tag);
        ResourceLocation potionId = net.minecraft.core.registries.BuiltInRegistries.POTION.getKey(potion);
        if (potionId == null) {
            potionId = ResourceLocation.withDefaultNamespace("water");
        }
        PotionForm form = PotionForm.fromId(tag.getString("ItemForm"));
        return new PotionVariantKey(potionId, form);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Potion", potionId.toString());
        tag.putString("Form", form.id());
        return tag;
    }

    public static PotionVariantKey load(CompoundTag tag) {
        ResourceLocation potionId = ResourceLocation.tryParse(tag.getString("Potion"));
        if (potionId == null) {
            potionId = ResourceLocation.withDefaultNamespace("water");
        }
        return new PotionVariantKey(potionId, PotionForm.fromId(tag.getString("Form")));
    }

    public String asStringKey() {
        return potionId + "|" + form.id();
    }
}

