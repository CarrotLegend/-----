package com.carrot123.yixiangshouronglu.compat.mixin;

import com.carrot123.yixiangshouronglu.gene.PhantomState;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.attributeslib.impl.AttributeEvents", remap = false)
public abstract class AttributesLibPhantomMixin {
    @Inject(method = "apothCriticalStrike(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void acc$excludePhantomCritical(LivingHurtEvent event, CallbackInfo callback) {
        if (PhantomState.isFollowUp(event.getSource())) { callback.cancel(); }
    }
}
