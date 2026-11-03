package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.util.LegacyAttributeCleanup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyDataCleanupEvents {
    private LegacyDataCleanupEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clean(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clean(player);
        }
    }

    private static void clean(ServerPlayer player) {
        boolean removed = false;
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                removed |= LegacyAttributeCleanup.removeFrom(instance);
            }
        }
        if (removed && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, AnomalousContainmentChronicles.MOD_ID)) {
            if (mapping.getKey().getPath().equals("resonance_core")) {
                mapping.ignore();
            }
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.MENU_TYPES, AnomalousContainmentChronicles.MOD_ID)) {
            if (mapping.getKey().getPath().equals("resonance")) {
                mapping.ignore();
            }
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ATTRIBUTES, AnomalousContainmentChronicles.MOD_ID)) {
            if (mapping.getKey().getPath().equals("all_damage")) {
                mapping.ignore();
            }
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ATTRIBUTES, "taa")) {
            mapping.ignore();
        }
    }
}
