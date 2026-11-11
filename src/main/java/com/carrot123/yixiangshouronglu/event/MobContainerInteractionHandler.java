package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.item.MobContainerItem;
import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobContainerInteractionHandler {
    private MobContainerInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(
            PlayerInteractEvent.EntityInteractSpecific event) {
        handleCaptureInteraction(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event) {
        handleCaptureInteraction(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof MobContainerItem container)
                || !MobContainerItem.hasStoredEntity(stack)) {
            return;
        }

        cancelInteraction(event, player);
        if (player.level().isClientSide) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer
                && event.getLevel() instanceof ServerLevel serverLevel) {
            container.tryRelease(
                    serverPlayer,
                    event.getHand(),
                    serverLevel,
                    event.getPos(),
                    event.getFace());
        }
    }

    private static void handleCaptureInteraction(
            PlayerInteractEvent event,
            Entity target) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!player.isShiftKeyDown()
                || !(stack.getItem() instanceof MobContainerItem container)
                || !(target instanceof LivingEntity living)) {
            return;
        }

        cancelInteraction(event, player);
        if (player.level().isClientSide) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            container.tryCapture(serverPlayer, event.getHand(), living);
        }
    }

    private static void cancelInteraction(
            PlayerInteractEvent event,
            Player player) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(
                player.level().isClientSide));
    }
}
