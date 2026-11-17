package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BiologicalCleaverItem;
import com.carrot123.yixiangshouronglu.item.GeneFragmentItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BiologicalCleaverEvents {
    private static final String HARVESTED = AnomalousContainmentChronicles.MOD_ID + ":CleaverHarvested";
    private static final double HARVEST_RANGE = 8.0;
    private static final Map<LivingEntity, ItemStack> PENDING_DROPS = Collections.synchronizedMap(new WeakHashMap<>());

    private BiologicalCleaverEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void harvest(LivingDeathEvent event) {
        var target = event.getEntity();
        if (target instanceof Player || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        PENDING_DROPS.remove(target);
        if (event.isCanceled() || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || target.getPersistentData().getBoolean(HARVESTED)) {
            return;
        }
        ServerPlayer player;
        if (event.getSource().getEntity() instanceof ServerPlayer sourcePlayer) {
            player = sourcePlayer;
        } else if (target.getKillCredit() instanceof ServerPlayer creditedPlayer) {
            player = creditedPlayer;
        } else {
            player = nearestCleaverHolder(level, target);
        }
        if (player == null || !holdsCleaver(player, level)) {
            return;
        }
        ItemStack fragment = GeneFragmentItem.capture(target);
        if (!fragment.isEmpty()) {
            PENDING_DROPS.put(target, fragment);
        }
    }

    private static boolean holdsCleaver(ServerPlayer player, ServerLevel level) {
        return player.serverLevel() == level && player.isAlive() && !player.isSpectator()
                && player.getMainHandItem().is(ModItems.BIOLOGICAL_CLEAVER.get());
    }

    private static ServerPlayer nearestCleaverHolder(ServerLevel level, LivingEntity target) {
        ServerPlayer nearest = null;
        double nearestDistance = HARVEST_RANGE * HARVEST_RANGE;
        for (ServerPlayer player : level.players()) {
            if (!holdsCleaver(player, level)) {
                continue;
            }
            double distance = player.distanceToSqr(target);
            if (distance <= nearestDistance && (nearest == null || distance < nearestDistance)) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void addFragmentDrop(LivingDropsEvent event) {
        var target = event.getEntity();
        ItemStack fragment = PENDING_DROPS.remove(target);
        if (event.isCanceled() || fragment == null || target.getPersistentData().getBoolean(HARVESTED)) {
            return;
        }
        var drop = new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(), fragment);
        drop.setDefaultPickUpDelay();
        if (event.getDrops().add(drop)) {
            target.getPersistentData().putBoolean(HARVESTED, true);
        }
    }

    @SubscribeEvent
    public static void upgrade(ItemStackedOnOtherEvent event) {
        var player = event.getPlayer();
        var menu = player.containerMenu;
        var slot = event.getSlot();
        ItemStack sword = event.getStackedOnItem();
        ItemStack cleaver = event.getCarriedItem();
        if (event.getClickAction() != ClickAction.SECONDARY || player.isSpectator()
                || (player.isCreative() && menu == player.inventoryMenu)
                || slot.container != player.getInventory() || !slot.isActive() || !slot.mayPickup(player)
                || !slot.mayPlace(cleaver) || !menu.slots.contains(slot) || !ItemStack.matches(slot.getItem(), cleaver)
                || !cleaver.is(ModItems.BIOLOGICAL_CLEAVER.get()) || cleaver.getCount() != 1
                || sword.isEmpty() || !(sword.getItem() instanceof SwordItem)
                || sword.is(ModItems.BIOLOGICAL_CLEAVER.get())
                || BiologicalCleaverItem.upgrades(cleaver) == Integer.MAX_VALUE) {
            return;
        }
        if (!player.level().isClientSide) {
            var cursor = event.getCarriedSlotAccess();
            ItemStack actualSword = cursor.get();
            if (!menu.stillValid(player) || !ItemStack.matches(actualSword, sword)) {
                return;
            }
            ItemStack upgraded = slot.getItem().copy();
            if (!BiologicalCleaverItem.upgrade(upgraded)
                    || !cursor.set(actualSword.getCount() == 1 ? ItemStack.EMPTY
                    : actualSword.copyWithCount(actualSword.getCount() - 1))) {
                return;
            }
            slot.set(upgraded);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        event.setCanceled(true);
    }
}
