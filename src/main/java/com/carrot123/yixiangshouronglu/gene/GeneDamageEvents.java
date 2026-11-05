package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class GeneDamageEvents {
    public static final TagKey<DamageType> MAGIC = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("yixiangshouronglu", "magic"));
    private GeneDamageEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
        double remaining = 1;
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            for (var module : GeneEffects.installed(player).values()) {
                var definition = GeneModuleItem.definition(module);
                if (definition.effect() != null) { remaining *= 1 - Math.max(0, Math.min(1, definition.effect().explosionReduction(GeneModuleItem.read(module).quality()))); }
            }
        }
        if (event.getSource().is(MAGIC) && GeneEffects.categories(GeneEffects.installed(player)).contains(GeneCategories.ELEMENTAL)) { remaining *= 0.15; }
        event.setAmount((float) (event.getAmount() * remaining));
    }
}
