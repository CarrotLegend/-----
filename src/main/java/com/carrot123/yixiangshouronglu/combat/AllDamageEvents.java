package com.carrot123.yixiangshouronglu.combat;

import com.carrot123.yixiangshouronglu.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AllDamageEvents {
    private AllDamageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target == null || target.level().isClientSide) {
            return;
        }
        ServerPlayer attacker = PlayerDamageSourceResolver.resolve(event.getSource());
        if (attacker == null || attacker == target) {
            return;
        }
        AttributeInstance attribute = attacker.getAttribute(ModAttributes.ALL_DAMAGE.get());
        if (attribute == null) {
            return;
        }
        float amount = event.getAmount();
        float modified = AllDamageMath.apply(amount, attribute.getValue());
        if (Float.compare(amount, modified) != 0) {
            event.setAmount(modified);
        }
    }
}
