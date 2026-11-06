package com.carrot123.yixiangshouronglu.compat.mixin;

import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "net.firefoxsalesman.dungeonslibs.utils.AbilityHelper",
        remap = false
)
public abstract class DungeonsLibsAbilityHelperMixin {

    @Inject(
            method = "checkTargetGoals(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/LivingEntity;)Ljava/lang/Boolean;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void yixiangshouronglu$checkTargetGoals(
            Mob mob,
            LivingEntity target,
            CallbackInfoReturnable<Boolean> callback
    ) {
        boolean matches = mob.targetSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(goal -> goal instanceof NearestAttackableTargetGoal<?>)
                .map(goal -> {
                    NearestAttackableTargetGoalAccess access =
                            (NearestAttackableTargetGoalAccess) goal;

                    Class<?> targetType =
                            access.yixiangshouronglu$getTargetType();

                    TargetingConditions conditions =
                            access.yixiangshouronglu$getTargetConditions();

                    Predicate<LivingEntity> selector =
                            ((TargetingConditionsAccess) conditions)
                                    .yixiangshouronglu$getSelector();

                    return (targetType == null || targetType.isInstance(target))
                            && (selector == null || selector.test(target));
                })
                .reduce(false, (left, right) -> left || right);

        callback.setReturnValue(matches);
    }
}
