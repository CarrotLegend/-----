package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    private ModTags() {
    }

    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> MOB_CONTAINER_WHITELIST =
                create(AnomalousContainmentChronicles.MOD_ID, "mob_container_whitelist");
        public static final TagKey<EntityType<?>> BOSS =
                create("c", "boss");

        private EntityTypes() {
        }

        private static TagKey<EntityType<?>> create(
                String namespace,
                String path) {
            return TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation(namespace, path));
        }
    }
}
