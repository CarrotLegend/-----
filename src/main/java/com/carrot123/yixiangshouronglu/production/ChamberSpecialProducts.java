package com.carrot123.yixiangshouronglu.production;

import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ChamberSpecialProducts {
    public record Recipe(int inputCount, ItemStack result) {
    }

    public record Rule(String chamber, String input, int inputCount, String itemId, int outputCount, boolean itemInput) {
        private Rule(String chamber, String input, int inputCount, String itemId, int outputCount) {
            this(chamber, input, inputCount, itemId, outputCount, false);
        }

        public ItemStack inputStack() {
            if (!itemInput) {
                return ContainmentExtractItem.create(input).copyWithCount(inputCount);
            }
            Item item = registeredItem(input);
            return item == null ? ItemStack.EMPTY : new ItemStack(item, inputCount);
        }

        @Nullable
        public Recipe product() {
            Item item = registeredItem(itemId);
            return item == null ? null : new Recipe(inputCount, new ItemStack(item, outputCount));
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("cataclysm:amethyst_crab", "cataclysm:amethyst_crab", 1, "cataclysm:blessed_amethyst_crab_meat", 1),
            new Rule("cataclysm:amethyst_crab", "minecraft:warden", 2, "yixiangshouronglu:sedative_meat", 1),
            new Rule("iceandfire:fire_dragon", "iceandfire:fire_dragon", 1, "iceandfire:fire_dragon_blood", 1),
            new Rule("iceandfire:ice_dragon", "iceandfire:ice_dragon", 1, "iceandfire:ice_dragon_blood", 1),
            new Rule("iceandfire:lightning_dragon", "iceandfire:lightning_dragon", 1, "iceandfire:lightning_dragon_blood", 1),
            new Rule("yixiangshouronglu:young_ender_dragon", "yixiangshouronglu:young_ender_dragon", 1, "minecraft:dragon_breath", 1),
            new Rule("minecraft:wither", "minecraft:wither", 4, "minecraft:nether_star", 1),
            new Rule("mutantmore:mutant_hoglin", "mutantmore:mutant_jungle_zombie", 1, "minecraft:porkchop", 64),
            new Rule("mutantmonsters:mutant_skeleton", "mutantmonsters:mutant_skeleton", 1, "minecraft:bone", 2),
            new Rule("iceandfire:fire_dragon", "iceandfire:fire_dragon_blood", 1, "iceandfire:dragonsteel_fire_ingot", 1, true),
            new Rule("iceandfire:ice_dragon", "iceandfire:ice_dragon_blood", 1, "iceandfire:dragonsteel_ice_ingot", 1, true),
            new Rule("iceandfire:lightning_dragon", "iceandfire:lightning_dragon_blood", 1, "iceandfire:dragonsteel_lightning_ingot", 1, true)
    );

    public static List<Rule> availableRules() {
        return RULES.stream()
                .filter(rule -> ForgeRegistries.ENTITY_TYPES.containsKey(new ResourceLocation(rule.chamber())))
                .filter(rule -> !rule.inputStack().isEmpty())
                .filter(rule -> rule.product() != null)
                .toList();
    }

    private ChamberSpecialProducts() {
    }

    @Nullable
    public static Recipe forInput(String chamberEntityId, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String extractEntityId = ContainmentExtractItem.getEntityId(stack);
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        for (Rule rule : RULES) {
            String inputId = rule.itemInput() && itemId != null ? itemId.toString() : extractEntityId;
            if (rule.chamber().equals(chamberEntityId) && rule.input().equals(inputId)) {
                return rule.product();
            }
        }
        return null;
    }

    @Nullable
    private static Item registeredItem(String itemId) {
        ResourceLocation id = new ResourceLocation(itemId);
        return ForgeRegistries.ITEMS.containsKey(id) ? ForgeRegistries.ITEMS.getValue(id) : null;
    }
}
