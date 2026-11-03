package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GenePlayerEvents {
    private GenePlayerEvents() {}
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try { GeneCapability.get(event.getEntity()).load(GeneCapability.get(event.getOriginal()).save()); }
        finally { event.getOriginal().invalidateCaps(); }
    }
    private static void restore(ServerPlayer player) {
        GeneService.restore(player); GeneNetwork.sync(player, "ok");
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) { restore(player); } }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) { restore(player); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) { restore(player); } }
}
