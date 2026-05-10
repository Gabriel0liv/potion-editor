package com.gabri.potioneditor.potion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class PotionVariantData {
    private final List<PotionEffectSpec> effects = new ArrayList<>();
    private Integer customColor;

    public static PotionVariantData fromEffects(List<MobEffectInstance> instances) {
        PotionVariantData data = new PotionVariantData();
        if (instances != null) {
            for (MobEffectInstance instance : instances) {
                data.effects.add(PotionEffectSpec.of(instance));
            }
        }
        return data;
    }

    public PotionVariantData copy() {
        PotionVariantData data = new PotionVariantData();
        for (PotionEffectSpec effect : effects) {
            data.effects.add(effect.copy());
        }
        data.customColor = customColor;
        return data;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag effectsTag = new ListTag();
        for (PotionEffectSpec effect : effects) {
            effectsTag.add(effect.save());
        }
        tag.put("Effects", effectsTag);
        if (customColor != null) {
            tag.putInt("Color", customColor);
        }
        return tag;
    }

    public static PotionVariantData load(CompoundTag tag) {
        PotionVariantData data = new PotionVariantData();
        ListTag list = tag.getList("Effects", 10);
        for (int i = 0; i < list.size(); i++) {
            data.effects.add(PotionEffectSpec.load(list.getCompound(i)));
        }
        if (tag.contains("Color")) {
            data.customColor = tag.getInt("Color");
        }
        return data;
    }

    public List<PotionEffectSpec> effects() {
        return effects;
    }

    public Integer customColor() {
        return customColor;
    }

    public void customColor(Integer customColor) {
        this.customColor = customColor;
    }

    public boolean isEmpty() {
        return effects.isEmpty() && customColor == null;
    }
}

