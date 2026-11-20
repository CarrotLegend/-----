package com.carrot123.yixiangshouronglu.compat.mixin;

import com.carrot123.yixiangshouronglu.gene.PlayerGeneModules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    @Inject(method = "eat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            shift = At.Shift.AFTER, remap = false))
    private void acc$gluttonyFood(Level level, ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
        if ((Object) this instanceof ServerPlayer player && stack.isEdible()) {
            var modules = PlayerGeneModules.getIfPresent(player);
            var food = stack.getFoodProperties(player);
            if (modules != null && food != null && food.getNutrition() > 0) {
                int bonus = modules.gluttonyBonus(food.getNutrition());
                if (bonus > 0) {
                    player.getFoodData().setFoodLevel(Math.min(20, player.getFoodData().getFoodLevel() + bonus));
                }
            }
        }
    }
}
