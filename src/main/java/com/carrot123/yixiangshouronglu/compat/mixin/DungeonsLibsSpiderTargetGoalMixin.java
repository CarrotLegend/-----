package com.carrot123.yixiangshouronglu.compat.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.world.entity.monster.Spider$SpiderTargetGoal")
public abstract class DungeonsLibsSpiderTargetGoalMixin<T extends LivingEntity>
        extends NearestAttackableTargetGoal<T> {

    protected DungeonsLibsSpiderTargetGoalMixin(Mob mob, Class<T> targetType, boolean mustSee) {
        super(mob, targetType, mustSee);
    }

    public TargetingConditions getTargetConditions() {
        return targetConditions;
    }
}