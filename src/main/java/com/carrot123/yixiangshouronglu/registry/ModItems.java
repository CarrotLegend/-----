package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BlazeCageItem;
import com.carrot123.yixiangshouronglu.item.EnderDragonWingsItem;
import com.carrot123.yixiangshouronglu.item.ExemptionShieldItem;
import com.carrot123.yixiangshouronglu.item.ResonanceCoreItem;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.item.WendigoEyeItem;
import com.carrot123.yixiangshouronglu.item.WendigoSightItem;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<Item> CONTAINMENT_CHAMBER = ITEMS.register("containment_chamber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOUL = ITEMS.register("soul", () -> new SoulItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RESONANCE_CORE = ITEMS.register("resonance_core", () -> new ResonanceCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> WENDIGO_EYE = ITEMS.register("wendigo_eye", () -> new WendigoEyeItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(2).saturationMod(0.2F).alwaysEat().build())));
    public static final RegistryObject<Item> TENACIOUS_SHIELD_FRAGMENT = ITEMS.register("tenacious_shield_fragment", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> TENACIOUS_SHIELD_SPAWN_EGG = ITEMS.register("tenacious_shield_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.TENACIOUS_SHIELD, 0x534B93, 0xE0C977, new Item.Properties()));
    public static final RegistryObject<Item> EXEMPTION_SHIELD = ITEMS.register("exemption_shield", () -> new ExemptionShieldItem(new Item.Properties().durability(2).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WENDIGO_SIGHT = ITEMS.register("wendigo_sight", () -> new WendigoSightItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BLAZE_CAGE = ITEMS.register("blaze_cage", () -> new BlazeCageItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ENDER_DRAGON_WINGS = ITEMS.register("ender_dragon_wings", () -> new EnderDragonWingsItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {
    }
}
