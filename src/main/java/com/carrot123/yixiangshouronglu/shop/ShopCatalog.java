package com.carrot123.yixiangshouronglu.shop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class ShopCatalog {
    public record Product(String key, ResourceLocation item, ResourceLocation potion, long price) {}
    public record Offer(String key, long price, ItemStack stack) {
        public Offer { stack = stack.copy(); }
        @Override public ItemStack stack() { return stack.copy(); }
    }
    public static final List<Product> PRODUCTS = List.of(
            new Product("chemical_x", new ResourceLocation("minecraft:potion"), new ResourceLocation("mutantmonsters:chemical_x"), 2),
            new Product("compound_z", new ResourceLocation("mutantmore:compound_z"), null, 20),
            new Product("medkit_enchanted", new ResourceLocation("roughtweaks:medkit_enchanted"), null, 10));
    public static final List<Product> SELL_PRODUCTS = List.of(
            new Product("iron_ingot", new ResourceLocation("minecraft:iron_ingot"), null, 1),
            new Product("gold_ingot", new ResourceLocation("minecraft:gold_ingot"), null, 2),
            new Product("diamond", new ResourceLocation("minecraft:diamond"), null, 5),
            new Product("netherite_ingot", new ResourceLocation("minecraft:netherite_ingot"), null, 20));

    private ShopCatalog() {}
    public static long sellPrice(ResourceLocation item) { return SELL_PRODUCTS.stream().filter(product -> product.item().equals(item)).mapToLong(Product::price).findFirst().orElse(0); }
    public static long saleValue(ResourceLocation item, int count) {
        if (count <= 0) { return 0; }
        return Math.multiplyExact(sellPrice(item), count);
    }
    public static List<Offer> offers() {
        return PRODUCTS.stream().map(product -> new Offer(product.key(), product.price(), create(product))).toList();
    }
    public static List<Offer> sellOffers() {
        return SELL_PRODUCTS.stream().map(product -> new Offer(product.key(), product.price(), create(product))).toList();
    }
    private static ItemStack create(Product product) {
        if (!ForgeRegistries.ITEMS.containsKey(product.item())) { return ItemStack.EMPTY; }
        var item = ForgeRegistries.ITEMS.getValue(product.item());
        if (item == null || item == Items.AIR) { return ItemStack.EMPTY; }
        ItemStack result = new ItemStack(item);
        if (product.potion() != null) {
            if (!ForgeRegistries.POTIONS.containsKey(product.potion())) { return ItemStack.EMPTY; }
            var potion = ForgeRegistries.POTIONS.getValue(product.potion());
            if (potion == null || potion == Potions.EMPTY) { return ItemStack.EMPTY; }
            PotionUtils.setPotion(result, potion);
        }
        return result;
    }
}
