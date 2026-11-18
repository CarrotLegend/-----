package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class GeneJumpEvents {
    private static final double VERTICAL_DRAG = 0.9800000190734863D;
    private static final double MIN_SPEED = 0.003D;
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<Attribute> JUMP_HEIGHT = ATTRIBUTES.register("gene_jump_height",
            () -> new RangedAttribute("attribute.yixiangshouronglu.gene_jump_height", 1.0D, 1.0D, 4.0D).setSyncable(true));

    public static void addPlayerAttribute(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, JUMP_HEIGHT.get());
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.onGround() || player.isPassenger()
                || player.getAbilities().flying || player.isFallFlying() || player.isInWater()
                || player.isInLava() || player.onClimbable() || player.isNoGravity()) {
            return;
        }
        double multiplier = player.getAttributeValue(JUMP_HEIGHT.get());
        var movement = player.getDeltaMovement();
        double gravity = player.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
        if (multiplier <= 1 || movement.y <= MIN_SPEED || gravity <= 0) {
            return;
        }
        double desired = height(movement.y, gravity) * multiplier;
        double low = movement.y;
        double high = low * multiplier;
        for (int iteration = 0; iteration < 20; iteration++) {
            double middle = (low + high) * 0.5;
            if (height(middle, gravity) < desired) {
                low = middle;
            } else {
                high = middle;
            }
        }
        player.setDeltaMovement(movement.x, high, movement.z);
    }

    private static double height(double velocity, double gravity) {
        double offset = gravity * VERTICAL_DRAG / (1.0 - VERTICAL_DRAG);
        double ticks = Math.ceil(Math.log1p((MIN_SPEED - velocity) / (velocity + offset)) / Math.log(VERTICAL_DRAG));
        double sum = -Math.expm1(ticks * Math.log(VERTICAL_DRAG)) / (1.0 - VERTICAL_DRAG);
        return (velocity + offset) * sum - offset * ticks;
    }
}
