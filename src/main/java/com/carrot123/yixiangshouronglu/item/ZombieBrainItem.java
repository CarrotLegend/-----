package com.carrot123.yixiangshouronglu.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;

public final class ZombieBrainItem extends Item {
    public ZombieBrainItem() {
        super(new Properties().food(new FoodProperties.Builder()
                .nutrition(Foods.PORKCHOP.getNutrition())
                .saturationMod(Foods.PORKCHOP.getSaturationModifier())
                .alwaysEat()
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600), 1.0F)
                .effect(() -> new MobEffectInstance(MobEffects.POISON, 200), 1.0F)
                .build()));
    }
}
