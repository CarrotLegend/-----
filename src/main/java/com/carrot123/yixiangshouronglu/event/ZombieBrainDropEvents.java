package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ZombieBrainDropEvents {
    private ZombieBrainDropEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var zombie = event.getEntity();
        if (zombie.getType() != EntityType.ZOMBIE || !(zombie.level() instanceof ServerLevel level)) {
            return;
        }

        double chance = Math.min(1.0, 0.0625 + 0.025 * event.getLootingLevel());
        if (zombie.getRandom().nextDouble() < chance) {
            ItemEntity drop = new ItemEntity(level, zombie.getX(), zombie.getY(), zombie.getZ(),
                    new ItemStack(ModItems.ZOMBIE_BRAIN.get()));
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }
}
