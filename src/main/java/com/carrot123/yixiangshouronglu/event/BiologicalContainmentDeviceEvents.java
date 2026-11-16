package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BiologicalContainmentDeviceItem;
import com.carrot123.yixiangshouronglu.item.ResearchResultItem;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BiologicalContainmentDeviceEvents {
    private BiologicalContainmentDeviceEvents() {
    }

    @SubscribeEvent
    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.BIOLOGICAL_CONTAINMENT_DEVICE.get())) {
            return;
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        Entity clicked = event.getTarget();
        Entity target = clicked instanceof PartEntity<?> part ? part.getParent() : clicked;
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()
                && target instanceof LivingEntity living && !(target instanceof Player)) {
            capture(player, living, event.getItemStack());
        }
    }

    private static void capture(ServerPlayer player, LivingEntity target, ItemStack device) {
        if (!target.isAlive() || target.isRemoved() || target.level() != player.level()) {
            return;
        }
        var source = ResearchResultItem.sourceType(BiologicalContainmentDeviceItem.storedResult(device));
        ResourceLocation targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (source == null || targetId == null || !targetId.equals(ForgeRegistries.ENTITY_TYPES.getKey(source))) {
            return;
        }

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), targetId);
        var inventory = player.getInventory();
        int slot = inventory.getSlotWithRemainingSpace(soul);
        if (slot < 0) {
            slot = inventory.getFreeSlot();
        }
        if (slot >= 0) {
            if (!inventory.add(slot, soul) || !soul.isEmpty()) {
                return;
            }
        } else {
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), soul);
            drop.setDefaultPickUpDelay();
            if (!player.level().addFreshEntity(drop)) {
                return;
            }
        }
        target.discard();
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 1);
    }
}
