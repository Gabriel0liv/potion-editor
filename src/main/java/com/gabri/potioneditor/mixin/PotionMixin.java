package com.gabri.potioneditor.mixin;

import com.gabri.potioneditor.potion.PotionEditorRuntime;
import com.gabri.potioneditor.potion.PotionForm;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Potion.class)
public class PotionMixin {
    @Inject(method = "getEffects", at = @At("RETURN"), cancellable = true)
    private void potioneditor$onGetEffects(CallbackInfoReturnable<List<MobEffectInstance>> cir) {
        Potion potion = (Potion) (Object) this;
        List<MobEffectInstance> resolved = PotionEditorRuntime.resolvePotionEffects(potion, PotionForm.DRINK, cir.getReturnValue());
        if (resolved != cir.getReturnValue()) {
            cir.setReturnValue(List.copyOf(resolved));
        }
    }
}

