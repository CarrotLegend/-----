package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.compat.mixin.LivingEntityDeathAccess;
import com.carrot123.yixiangshouronglu.gene.ChimeraGrowth;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class ChimeraGrowthEvents {
    private record PendingKill(LivingDeathEvent event, ServerPlayer player, ItemStack core) {}

    private static final Map<LivingEntity, PendingKill> PENDING = new IdentityHashMap<>();
    private static final Set<LivingEntity> COUNTED = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim instanceof Player || COUNTED.contains(victim) || PENDING.containsKey(victim)) {
            return;
        }
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer player ? player
                : victim.getKillCredit() instanceof ServerPlayer player ? player : null;
        if (killer == null || !killer.isAlive() || killer.isRemoved() || killer.hasDisconnected()) {
            return;
        }
        ItemStack core = ChimeraGrowth.equipped(killer, ModItems.BLOODTHIRSTY_CHIMERA.get());
        if (!core.isEmpty()) {
            PENDING.put(victim, new PendingKill(event, killer, core));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void drops(LivingDropsEvent event) {
        PendingKill kill = PENDING.remove(event.getEntity());
        if (kill != null) {
            finish(event.getEntity(), kill);
        }
    }

    private static void finish(LivingEntity victim, PendingKill kill) {
        if (!kill.event().isCanceled() && ((LivingEntityDeathAccess) victim).acc$isDead()
                && COUNTED.add(victim) && kill.core().is(ModItems.BLOODTHIRSTY_CHIMERA.get())) {
            ChimeraGrowth.addKill(kill.core());
            ChimeraGrowth.refresh(kill.player());
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !PENDING.isEmpty()) {
            var pending = new ArrayList<>(PENDING.entrySet());
            PENDING.clear();
            for (var entry : pending) {
                finish(entry.getKey(), entry.getValue());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void consumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()) {
            ItemStack core = ChimeraGrowth.equipped(player, ModItems.GLUTTONOUS_CHIMERA.get());
            if (!core.isEmpty()) {
                ChimeraGrowth.recordFood(core, event.getItem(), player);
                ChimeraGrowth.refresh(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            ChimeraGrowth.refresh(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        restore(event);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        restore(event);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        restore(event);
    }

    private static void restore(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChimeraGrowth.refresh(player);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PENDING.clear();
        COUNTED.clear();
    }
}
