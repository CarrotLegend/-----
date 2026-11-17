package com.carrot123.yixiangshouronglu.compat.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityEffectAccess {
    @Invoker("onEffectAdded")
    void acc$onEffectAdded(MobEffectInstance effect, Entity source);

    @Invoker("onEffectUpdated")
    void acc$onEffectUpdated(MobEffectInstance effect, boolean reapplyAttributes, Entity source);
}
