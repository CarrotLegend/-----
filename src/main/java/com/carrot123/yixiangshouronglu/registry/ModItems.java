
package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<Item> CONTAINMENT_CHAMBER = ITEMS.register("containment_chamber", () -> new Item(new Item.Properties()));

    private ModItems() {
    }
}
