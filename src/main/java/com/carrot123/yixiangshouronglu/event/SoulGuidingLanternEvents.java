package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SoulGuidingLanternEvents {
    private SoulGuidingLanternEvents() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, SoulGuidingLanternEvents::onDeath));
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || player.isRemoved() || player.hasDisconnected() || player.isSpectator()
                || !player.isDeadOrDying() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SOUL_GUIDING_LANTERN.get()) && stack.getDamageValue() < stack.getMaxDamage()) {
                stack.setDamageValue(stack.getDamageValue() + 1);
                event.setCanceled(true);
                player.setHealth(1.0F);
                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                player.level().broadcastEntityEvent(player, (byte) 35);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                return;
            }
        }
    }
}
