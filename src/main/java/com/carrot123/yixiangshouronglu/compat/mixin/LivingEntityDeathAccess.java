package com.carrot123.yixiangshouronglu.compat.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityDeathAccess {
    @Accessor("dead")
    boolean acc$isDead();
}
