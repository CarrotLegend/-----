package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, AnomalousContainmentChronicles.MOD_ID);

    public static final RegistryObject<Attribute> ALL_DAMAGE = ATTRIBUTES.register(
            "all_damage",
            () -> new RangedAttribute(
                    "attribute.name.yixiangshouronglu.all_damage",
                    1.0D,
                    0.0D,
                    32767.0D
            ).setSyncable(true)
    );

    private ModAttributes() {
    }

    public static void addPlayerAttribute(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ALL_DAMAGE.get());
    }
}
