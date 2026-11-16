package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BiologicalCleaverItem;
import com.carrot123.yixiangshouronglu.item.GeneFragmentItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BiologicalCleaverEvents {
    private static final String HARVESTED = AnomalousContainmentChronicles.MOD_ID + ":CleaverHarvested";

    private BiologicalCleaverEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void harvest(LivingDeathEvent event) {
        var target = event.getEntity();
        var source = event.getSource();
        if (target instanceof Player || target.level().isClientSide || !source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !player.getMainHandItem().is(ModItems.BIOLOGICAL_CLEAVER.get())
                || target.getPersistentData().getBoolean(HARVESTED)) {
            return;
        }
        ItemStack fragment = GeneFragmentItem.capture(target);
        if (fragment.isEmpty()) {
            return;
        }
        target.getPersistentData().putBoolean(HARVESTED, true);
        var inventory = player.getInventory();
        int slot = inventory.getSlotWithRemainingSpace(fragment);
        if (slot < 0) {
            slot = inventory.getFreeSlot();
        }
        if (slot < 0) {
            player.drop(fragment, false);
        } else {
            inventory.add(slot, fragment);
            if (!fragment.isEmpty()) {
                player.drop(fragment, false);
            }
        }
    }

    @SubscribeEvent
    public static void upgrade(ItemStackedOnOtherEvent event) {
        var player = event.getPlayer();
        var menu = player.containerMenu;
        var slot = event.getSlot();
        ItemStack sword = event.getCarriedItem();
        ItemStack cleaver = event.getStackedOnItem();
        if (event.getClickAction() != ClickAction.SECONDARY || player.isSpectator()
                || (player.isCreative() && menu == player.inventoryMenu)
                || slot.container != player.getInventory() || !slot.isActive() || !slot.mayPickup(player)
                || !slot.mayPlace(cleaver) || slot.index < 0 || slot.index >= menu.slots.size()
                || menu.getSlot(slot.index) != slot || slot.getItem() != cleaver
                || !cleaver.is(ModItems.BIOLOGICAL_CLEAVER.get()) || cleaver.getCount() != 1
                || sword.isEmpty() || !(sword.getItem() instanceof SwordItem)
                || sword.is(ModItems.BIOLOGICAL_CLEAVER.get())
                || BiologicalCleaverItem.upgrades(cleaver) == Integer.MAX_VALUE) {
            return;
        }
        if (!player.level().isClientSide) {
            if (!menu.stillValid(player) || event.getCarriedSlotAccess().get() != sword
                    || !BiologicalCleaverItem.upgrade(cleaver)) {
                return;
            }
            sword.shrink(1);
            if (sword.isEmpty()) {
                event.getCarriedSlotAccess().set(ItemStack.EMPTY);
            }
            slot.setChanged();
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        event.setCanceled(true);
    }
}
