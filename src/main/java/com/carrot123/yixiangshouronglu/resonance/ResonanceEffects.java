package com.carrot123.yixiangshouronglu.resonance;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResonanceEffects {
    private static final String[] TAA_ATTRIBUTES = {
            "bullet_gundamage",
            "rounds_per_minute",
            "bullet_gundamage_launcher",
            "headshot_multiplier",
            "move_speed"
    };

    private ResonanceEffects() {
    }

    private record Key(Attribute attribute, AttributeModifier.Operation operation) {
    }

    private static UUID modifierId(Key key) {
        return UUID.nameUUIDFromBytes(
                ("yixiangshouronglu:resonance_core:" + key.attribute().getDescriptionId() + ":" + key.operation().name())
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }

        Map<Key, Double> amounts = new HashMap<>();
        boolean[] regeneration = {false};

        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            ICurioStacksHandler handler = inventory.getCurios().get("resonance_core");

            if (handler == null) {
                return;
            }

            for (int slot = 0; slot < handler.getStacks().getSlots(); slot++) {
                ItemStack core = handler.getStacks().getStackInSlot(slot);

                if (!core.is(ModItems.RESONANCE_CORE.get())) {
                    continue;
                }

                for (ResonanceLinks.Link link : ResonanceLinks.list(core)) {
                    String entityId = ResonanceLinks.containedEntity(player.server, link);

                    if (entityId.isEmpty()) {
                        continue;
                    }

                    if (ResonanceRules.grantsRegeneration(entityId)) {
                        regeneration[0] = true;
                    }

                    for (ResonanceRules.Bonus bonus : ResonanceRules.allBonusesForEntity(entityId)) {
                        Key key = new Key(bonus.attribute(), bonus.operation());
                        amounts.merge(key, bonus.amount(), Double::sum);
                    }
                }
            }
        });

        Set<Attribute> managed = new HashSet<>();
        managed.add(Attributes.MAX_HEALTH);
        managed.add(Attributes.MOVEMENT_SPEED);
        managed.add(Attributes.ATTACK_DAMAGE);
        managed.add(Attributes.KNOCKBACK_RESISTANCE);

        for (String path : TAA_ATTRIBUTES) {
            ResourceLocation id = new ResourceLocation("taa", path);

            if (ForgeRegistries.ATTRIBUTES.containsKey(id)) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);

                if (attribute != null) {
                    managed.add(attribute);
                }
            }
        }

        for (Attribute attribute : managed) {
            AttributeInstance instance = player.getAttribute(attribute);

            if (instance == null) {
                continue;
            }

            for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
                Key key = new Key(attribute, operation);
                UUID id = modifierId(key);
                double amount = amounts.getOrDefault(key, 0.0D);
                AttributeModifier previous = instance.getModifier(id);

                if (previous != null && Math.abs(previous.getAmount() - amount) < 0.000001D) {
                    continue;
                }

                if (previous != null) {
                    instance.removeModifier(id);
                }

                if (Math.abs(amount) > 0.000001D) {
                    instance.addTransientModifier(
                            new AttributeModifier(id, "ACC Resonance Core", amount, operation)
                    );
                }
            }
        }

        if (regeneration[0]) {
            MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);

            if (current == null
                    || current.getAmplifier() < 1
                    || (current.getAmplifier() == 1 && current.getDuration() <= 25)) {
                player.addEffect(
                        new MobEffectInstance(MobEffects.REGENERATION, 45, 1, false, true, true)
                );
            }
        }
    }
}
