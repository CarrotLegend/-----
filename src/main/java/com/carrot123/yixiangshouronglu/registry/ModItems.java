package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.ResonanceCoreItem;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<Item> CONTAINMENT_CHAMBER = ITEMS.register("containment_chamber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOUL = ITEMS.register("soul", () -> new SoulItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RESONANCE_CORE = ITEMS.register("resonance_core", () -> new ResonanceCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {
    }
}
