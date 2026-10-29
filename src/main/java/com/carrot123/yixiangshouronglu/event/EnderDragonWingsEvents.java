package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class EnderDragonWingsEvents {
    private EnderDragonWingsEvents() {
    }

    private static boolean hasWings(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> {
                    ICurioStacksHandler handler = inventory.getCurios().get("back");

                    if (handler == null) {
                        return false;
                    }

                    for (int index = 0; index < handler.getStacks().getSlots(); index++) {
                        if (handler.getStacks()
                                .getStackInSlot(index)
                                .is(ModItems.ENDER_DRAGON_WINGS.get())) {
                            return true;
                        }
                    }

                    return false;
                })
                .orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();

        if (player.onGround()
                || !player.getAbilities().mayfly
                || !hasWings(player)) {
            return;
        }

        event.setNewSpeed(
                event.getNewSpeed() + event.getOriginalSpeed() * 4.0F
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled()
                || !(event.getEntity() instanceof Player player)
                || !event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }

        if (player.getAbilities().mayfly && hasWings(player)) {
            event.setCanceled(true);
        }
    }
}
