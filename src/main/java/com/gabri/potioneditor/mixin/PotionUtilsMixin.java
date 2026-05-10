package com.gabri.potioneditor.mixin;

import com.gabri.potioneditor.potion.PotionEditorRuntime;
import com.gabri.potioneditor.potion.PotionForm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PotionUtils.class)
public abstract class PotionUtilsMixin {
    @Inject(method = "getColor(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private static void potioneditor$onGetColor(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int resolved = PotionEditorRuntime.resolveItemColor(stack, cir.getReturnValue());
        if (resolved != cir.getReturnValue()) {
            cir.setReturnValue(resolved);
        }
    }

    @Inject(method = "getMobEffects(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void potioneditor$onGetMobEffects(ItemStack stack, CallbackInfoReturnable<List<MobEffectInstance>> cir) {
        List<MobEffectInstance> resolved = PotionEditorRuntime.resolveItemEffects(stack, cir.getReturnValue());
        if (resolved != cir.getReturnValue()) {
            cir.setReturnValue(List.copyOf(resolved));
        }
    }

    @Inject(method = "getAllEffects(Lnet/minecraft/nbt/CompoundTag;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void potioneditor$onGetAllEffects(CompoundTag tag, CallbackInfoReturnable<List<MobEffectInstance>> cir) {
        net.minecraft.world.item.alchemy.Potion potion = PotionUtils.getPotion(tag);
        List<MobEffectInstance> resolved = PotionEditorRuntime.resolvePotionEffects(potion, PotionForm.DRINK, cir.getReturnValue());
        if (resolved != cir.getReturnValue()) {
            cir.setReturnValue(List.copyOf(resolved));
        }
    }
}

