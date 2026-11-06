package com.carrot123.yixiangshouronglu.compat.mixin;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TargetingConditions.class)
public interface TargetingConditionsAccess {

    @Accessor("selector")
    Predicate<LivingEntity> yixiangshouronglu$getSelector();
}
