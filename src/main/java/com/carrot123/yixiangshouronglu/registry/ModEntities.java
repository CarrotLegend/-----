package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.entity.TenaciousShieldEntity;
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
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<EntityType<TenaciousShieldEntity>> TENACIOUS_SHIELD = ENTITIES.register("tenacious_shield",
            () -> EntityType.Builder.of(TenaciousShieldEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.0F)
                    .clientTrackingRange(10)
                    .build(AnomalousContainmentChronicles.MOD_ID + ":tenacious_shield"));

    private ModEntities() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TENACIOUS_SHIELD.get(), TenaciousShieldEntity.createAttributes().build());
    }
}
