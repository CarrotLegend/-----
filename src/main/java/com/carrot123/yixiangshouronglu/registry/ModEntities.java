
package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.TenaciousShieldEntity;
import com.carrot123.yixiangshouronglu.entity.ShadowDemonEntity;
import com.carrot123.yixiangshouronglu.entity.YoungEnderDragonEntity;
import com.carrot123.yixiangshouronglu.entity.LegendaryCarrotKingEntity;
import com.carrot123.yixiangshouronglu.entity.GeneDragonFireball;
import com.carrot123.yixiangshouronglu.entity.GeneDragonBreathCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.ENTITY_TYPES,
                    AnomalousContainmentChronicles.MOD_ID
            );

    public static final RegistryObject<EntityType<TenaciousShieldEntity>> TENACIOUS_SHIELD =
            ENTITIES.register("tenacious_shield",
                    () -> EntityType.Builder.of(TenaciousShieldEntity::new, MobCategory.MONSTER)
                            .sized(1.2F, 2.0F)
                            .clientTrackingRange(10)
                            .build(AnomalousContainmentChronicles.MOD_ID + ":tenacious_shield"));

    public static final RegistryObject<EntityType<YoungEnderDragonEntity>> YOUNG_ENDER_DRAGON =
            ENTITIES.register("young_ender_dragon",
                    () -> EntityType.Builder.of(YoungEnderDragonEntity::new, MobCategory.CREATURE)
                            .sized(0.95F, 0.95F)
                            .clientTrackingRange(10)
                            .build(AnomalousContainmentChronicles.MOD_ID + ":young_ender_dragon"));

    public static final RegistryObject<EntityType<ShadowDemonEntity>> SHADOW_DEMON =
            ENTITIES.register("shadow_demon",
                    () -> EntityType.Builder.of(ShadowDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(AnomalousContainmentChronicles.MOD_ID + ":shadow_demon"));

    public static final RegistryObject<EntityType<LegendaryCarrotKingEntity>> LEGENDARY_CARROT_KING = ENTITIES.register("legendary_carrot_king",
            () -> EntityType.Builder.of(LegendaryCarrotKingEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F)
                    .clientTrackingRange(10).build(AnomalousContainmentChronicles.MOD_ID + ":legendary_carrot_king"));

    public static final RegistryObject<EntityType<GeneDragonFireball>> GENE_DRAGON_FIREBALL =
            ENTITIES.register("gene_dragon_fireball", () -> EntityType.Builder.of(GeneDragonFireball::new, MobCategory.MISC)
                    .sized(1, 1).fireImmune().noSave().clientTrackingRange(4).updateInterval(10)
                    .build(AnomalousContainmentChronicles.MOD_ID + ":gene_dragon_fireball"));

    public static final RegistryObject<EntityType<GeneDragonBreathCloud>> GENE_DRAGON_BREATH_CLOUD =
            ENTITIES.register("gene_dragon_breath_cloud", () -> EntityType.Builder.of(GeneDragonBreathCloud::new, MobCategory.MISC)
                    .sized(6, 0.5F).fireImmune().noSave().clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
                    .build(AnomalousContainmentChronicles.MOD_ID + ":gene_dragon_breath_cloud"));

    private ModEntities() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LEGENDARY_CARROT_KING.get(), LegendaryCarrotKingEntity.createAttributes().build());
        event.put(TENACIOUS_SHIELD.get(), TenaciousShieldEntity.createAttributes().build());
        event.put(YOUNG_ENDER_DRAGON.get(), YoungEnderDragonEntity.createAttributes().build());
        event.put(SHADOW_DEMON.get(), ShadowDemonEntity.createAttributes().build());
    }
}
