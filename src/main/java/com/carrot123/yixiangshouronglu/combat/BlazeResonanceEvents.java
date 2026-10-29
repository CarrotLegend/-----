package com.carrot123.yixiangshouronglu.combat;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.resonance.ResonanceLinks;
import com.carrot123.yixiangshouronglu.resonance.ResonanceRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlazeResonanceEvents {
    private BlazeResonanceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFireDamage(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }

        if (hasBlazeResonance(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageBurningEnemy(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();

        if (target.level().isClientSide
                || !target.isOnFire()
                || event.getAmount() <= 0.0F) {
            return;
        }

        ServerPlayer attacker = PlayerDamageSourceResolver.resolve(event.getSource());

        if (attacker == null || attacker == target || !hasBlazeResonance(attacker)) {
            return;
        }

        event.setAmount(event.getAmount() * 1.10F);
    }

    private static boolean hasBlazeResonance(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> {
                    ICurioStacksHandler handler =
                            inventory.getCurios().get("resonance_core");

                    if (handler == null) {
                        return false;
                    }

                    for (int slot = 0; slot < handler.getStacks().getSlots(); slot++) {
                        ItemStack core = handler.getStacks().getStackInSlot(slot);

                        if (!core.is(ModItems.RESONANCE_CORE.get())) {
                            continue;
                        }

                        for (ResonanceLinks.Link link : ResonanceLinks.list(core)) {
                            String entityId = ResonanceLinks.containedEntity(player.server, link);

                            if (ResonanceRules.grantsBlazeProtection(entityId)) {
                                return true;
                            }
                        }
                    }

                    return false;
                })
                .orElse(false);
    }
}
