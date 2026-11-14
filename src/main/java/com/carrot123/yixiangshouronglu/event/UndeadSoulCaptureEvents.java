package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UndeadSoulCaptureEvents {
    private UndeadSoulCaptureEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        capture(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        capture(event, event.getTarget());
    }

    private static void capture(PlayerInteractEvent event, Entity target) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND
                || target.isRemoved() || !target.isAlive() || event.getEntity().isSpectator()) {
            return;
        }

        ItemStack held = event.getItemStack();
        EntityType<?> type = target.getType();
        boolean zombie = type == EntityType.ZOMBIE && held.is(ModItems.ROTTEN_BRAIN.get());
        boolean skeleton = type == EntityType.SKELETON && held.is(ModItems.BONE_CAGE.get());
        if (!zombie && !skeleton) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel level) {
            ItemStack soul = SoulItem.create(ModItems.SOUL.get(), EntityType.getKey(type));
            ItemEntity drop = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), soul);
            drop.setDefaultPickUpDelay();
            if (!level.addFreshEntity(drop)) {
                return;
            }

            target.discard();
            if (!event.getEntity().getAbilities().instabuild) {
                held.shrink(1);
            }

            level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
