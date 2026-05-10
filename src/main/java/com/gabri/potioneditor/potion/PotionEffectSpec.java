package com.gabri.potioneditor.potion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;

public class PotionEffectSpec {
    private String effectId;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean visible = true;
    private boolean showIcon = true;

    public PotionEffectSpec(String effectId, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        this.effectId = effectId == null ? "" : effectId;
        this.duration = Math.max(0, duration);
        this.amplifier = Math.max(0, amplifier);
        this.ambient = ambient;
        this.visible = visible;
        this.showIcon = showIcon;
    }

    public static PotionEffectSpec of(MobEffectInstance instance) {
        MobEffect effect = instance.getEffect();
        ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return new PotionEffectSpec(
                key == null ? "" : key.toString(),
                instance.getDuration(),
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()
        );
    }

    public PotionEffectSpec copy() {
        return new PotionEffectSpec(effectId, duration, amplifier, ambient, visible, showIcon);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Effect", effectId);
        tag.putInt("Duration", duration);
        tag.putInt("Amplifier", amplifier);
        tag.putBoolean("Ambient", ambient);
        tag.putBoolean("Visible", visible);
        tag.putBoolean("ShowIcon", showIcon);
        return tag;
    }

    public static PotionEffectSpec load(CompoundTag tag) {
        return new PotionEffectSpec(
                tag.getString("Effect"),
                tag.getInt("Duration"),
                tag.getInt("Amplifier"),
                tag.getBoolean("Ambient"),
                tag.contains("Visible") ? tag.getBoolean("Visible") : true,
                tag.contains("ShowIcon") ? tag.getBoolean("ShowIcon") : true
        );
    }

    public MobEffectInstance toInstance() {
        ResourceLocation key = parseEffectId(effectId);
        if (key == null) {
            return null;
        }

        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(key);
        if (effect == null) {
            return null;
        }

        return new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon);
    }

    public String effectId() {
        return effectId;
    }

    public void effectId(String effectId) {
        this.effectId = effectId == null ? "" : effectId;
    }

    public int duration() {
        return duration;
    }

    public void duration(int duration) {
        this.duration = Math.max(0, duration);
    }

    public int durationSeconds() {
        return Math.max(0, duration / 20);
    }

    public void durationSeconds(int seconds) {
        this.duration = Math.max(0, seconds) * 20;
    }

    public int amplifier() {
        return amplifier;
    }

    public void amplifier(int amplifier) {
        this.amplifier = Math.max(0, amplifier);
    }

    public boolean ambient() {
        return ambient;
    }

    public void ambient(boolean ambient) {
        this.ambient = ambient;
    }

    public boolean visible() {
        return visible;
    }

    public void visible(boolean visible) {
        this.visible = visible;
    }

    public boolean showIcon() {
        return showIcon;
    }

    public void showIcon(boolean showIcon) {
        this.showIcon = showIcon;
    }

    private static ResourceLocation parseEffectId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.withDefaultNamespace(id);
    }
}

