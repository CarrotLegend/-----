package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ExemptionShieldEvents {
    private ExemptionShieldEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()
                || event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.getAbilities().invulnerable) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.is(ModItems.EXEMPTION_SHIELD.get())) {
            protect(player, mainHand, InteractionHand.MAIN_HAND);
            event.setCanceled(true);
            return;
        }

        ItemStack offHand = player.getOffhandItem();

        if (offHand.is(ModItems.EXEMPTION_SHIELD.get())) {
            protect(player, offHand, InteractionHand.OFF_HAND);
            event.setCanceled(true);
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.EXEMPTION_SHIELD.get())) {
                protect(player, stack, null);
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void protect(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand
    ) {
        boolean breaking = stack.getDamageValue() + 1 >= stack.getMaxDamage();

        stack.hurtAndBreak(1, player, entity -> {
            if (hand != null) {
                entity.broadcastBreakEvent(hand);
            }
        });

        player.level().playSound(
                null,
                player.blockPosition(),
                breaking ? SoundEvents.SHIELD_BREAK : SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                1.0F,
                breaking ? 0.9F : 1.0F
        );

        player.getInventory().setChanged();
    }
}
