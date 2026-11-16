package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.GeneExtractorItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class GeneExtractorEvents {
    private GeneExtractorEvents() {
    }

    @SubscribeEvent
    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getItemStack().getItem() instanceof GeneExtractorItem extractor)) {
            return;
        }
        Entity clicked = event.getTarget();
        Entity target = clicked instanceof PartEntity<?> part ? part.getParent() : clicked;
        if (!(target instanceof LivingEntity living) || target instanceof Player) {
            return;
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            extractor.extract(player, living);
        }
    }
}
