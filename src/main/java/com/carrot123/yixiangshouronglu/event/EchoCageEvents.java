package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
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

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class EchoCageEvents {
    private static final ResourceLocation WARDEN =
            new ResourceLocation("minecraft", "warden");

    private EchoCageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handle(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handle(event, event.getTarget());
    }

    private static void handle(PlayerInteractEvent event, Entity target) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack held = event.getItemStack();

        if (!held.is(ModItems.ECHO_CAGE.get()) || held.isEmpty()) {
            return;
        }

        if (target.getType() != EntityType.WARDEN || target.isRemoved() || !target.isAlive()) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), WARDEN);

        ItemEntity droppedSoul = new ItemEntity(
                level,
                target.getX(),
                target.getY() + 0.3D,
                target.getZ(),
                soul
        );

        if (!level.addFreshEntity(droppedSoul)) {
            return;
        }

        target.ejectPassengers();
        target.discard();

        if (!event.getEntity().getAbilities().instabuild) {
            held.shrink(1);
        }

        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.0F,
                0.7F
        );
    }
}
