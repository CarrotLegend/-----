package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.FinalKnellItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FinalKnellEvents {
    private FinalKnellEvents() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, FinalKnellEvents::onDamage));
    }

    private static void onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float health = target.getHealth();
        float damage = event.getAmount();
        if (event.isCanceled() || target.level().isClientSide || !target.isAlive()
                || !Float.isFinite(damage) || damage <= 0.0F || !(health < target.getMaxHealth() * 0.02F)) {
            return;
        }

        ServerPlayer attacker = attacker(event.getSource());
        if (attacker == null || attacker == target || !attacker.isAlive() || attacker.isSpectator()
                || attacker.hasDisconnected() || attacker.level() != target.level() || !isEquipped(attacker)) {
            return;
        }
        if (attacker.getRandom().nextFloat() < 0.25F) {
            event.setAmount(Math.max(damage, health));
        }
    }

    private static ServerPlayer attacker(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        if (source.is(DamageTypes.PLAYER_ATTACK)) {
            return direct == owner && owner instanceof ServerPlayer player ? player : null;
        }
        if (!(direct instanceof Projectile) && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            return null;
        }
        if (owner instanceof ServerPlayer player) {
            return player;
        }
        if (owner == null && direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean isEquipped(ServerPlayer player) {
        var hands = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inventory -> inventory.getStacksHandler(FinalKnellItem.SLOT)).orElse(null);
        if (hands == null) {
            return false;
        }
        var stacks = hands.getStacks();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            if (stacks.getStackInSlot(slot).is(ModItems.FINAL_KNELL.get())) {
                return true;
            }
        }
        return false;
    }
}
