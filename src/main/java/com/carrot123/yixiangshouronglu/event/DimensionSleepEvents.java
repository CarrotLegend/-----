package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DimensionSleepEvents {
    private static final ResourceKey<Level> DRAGON_NEST = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(AnomalousContainmentChronicles.MOD_ID, "dragon_nest"));
    private static final ResourceKey<Level> FUNGAL_WORLD = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(AnomalousContainmentChronicles.MOD_ID, "fungal_world"));

    private DimensionSleepEvents() {
    }

    @SubscribeEvent
    public static void onSleepInBed(PlayerSleepInBedEvent event) {
        ResourceKey<Level> dimension = event.getEntity().level().dimension();
        if (dimension.equals(DRAGON_NEST) || dimension.equals(FUNGAL_WORLD)) {
            event.setResult(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
        }
    }
}
