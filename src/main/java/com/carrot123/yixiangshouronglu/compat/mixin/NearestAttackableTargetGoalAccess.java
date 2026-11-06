package com.carrot123.yixiangshouronglu.compat.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NearestAttackableTargetGoal.class)
public interface NearestAttackableTargetGoalAccess {

    @Accessor("targetType")
    Class<?> yixiangshouronglu$getTargetType();

    @Accessor("targetConditions")
    TargetingConditions yixiangshouronglu$getTargetConditions();
}
