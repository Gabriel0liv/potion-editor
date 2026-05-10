package com.gabri.potioneditor.potion;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.gabri.babel.core.api.BabelItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PotionEditorRuntime {
    private PotionEditorRuntime() {
    }

    public static List<MobEffectInstance> resolvePotionEffects(Potion potion, PotionForm form, List<MobEffectInstance> original) {
        if (potion == null || original == null) {
            return List.of();
        }

        return applyOverride(PotionVariantKey.from(potion, form), original);
    }

    public static List<MobEffectInstance> resolveItemEffects(ItemStack stack, List<MobEffectInstance> original) {
        if (stack == null || stack.isEmpty() || original == null) {
            return List.of();
        }

        Potion potion = PotionUtils.getPotion(stack);
        if (potion == null) {
            return List.of();
        }

        PotionForm form = PotionForm.fromItemStack(stack);
        return applyOverride(PotionVariantKey.from(potion, form), original);
    }

    public static int resolveItemColor(ItemStack stack, int originalColor) {
        return BabelItems.colorResolvers().resolve(stack, originalColor);
    }

    public static PotionVariantData defaultVariantData(PotionVariantKey key) {
        Potion potion = BuiltInRegistries.POTION.get(key.potionId());
        if (potion == null) {
            return new PotionVariantData();
        }
        return PotionVariantData.fromEffects(potion.getEffects());
    }

    public static PotionVariantData currentVariantData(PotionVariantKey key) {
        PotionVariantData override = currentOverride(key);
        if (override != null) {
            return override.copy();
        }
        return defaultVariantData(key);
    }

    public static void applyClientOverrides(Map<String, PotionVariantData> overrides) {
        PotionEditorClientCache.replaceAll(overrides);
    }

    private static PotionVariantData currentOverride(PotionVariantKey key) {
        if (key == null) {
            return null;
        }

        if (FMLEnvironment.dist.isClient()) {
            return PotionEditorClientCache.get(key).orElse(null);
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        return PotionEditorState.get(server).getOverride(key);
    }

    public static List<MobEffectInstance> applyOverride(PotionVariantKey key, List<MobEffectInstance> original) {
        PotionVariantData override = currentOverride(key);
        if (override == null) {
            return original;
        }

        List<MobEffectInstance> instances = toInstances(override);
        return instances.isEmpty() ? original : instances;
    }

    private static List<MobEffectInstance> toInstances(PotionVariantData data) {
        if (data == null) {
            return List.of();
        }

        List<MobEffectInstance> list = new ArrayList<>();
        for (PotionEffectSpec spec : data.effects()) {
            MobEffectInstance instance = spec.toInstance();
            if (instance != null) {
                list.add(instance);
            }
        }
        return list;
    }
}

