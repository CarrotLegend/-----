package com.carrot123.yixiangshouronglu.production;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class ChamberSpecialProducts {
    public record Recipe(int inputCount, ItemStack result) {
    }

    record RecipeDefinition(int inputCount, String itemId) {
    }

    private ChamberSpecialProducts() {
    }

    @Nullable
    public static Recipe forInput(String chamberEntityId, String inputEntityId) {
        RecipeDefinition definition = definitionForInput(chamberEntityId, inputEntityId);
        if (definition == null) {
            return null;
        }

        ResourceLocation id = new ResourceLocation(definition.itemId());
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            return null;
        }

        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? null : new Recipe(definition.inputCount(), new ItemStack(item));
    }

    @Nullable
    static RecipeDefinition definitionForInput(String chamberEntityId, String inputEntityId) {
        return switch (chamberEntityId) {
            case "cataclysm:amethyst_crab" -> switch (inputEntityId) {
                case "cataclysm:amethyst_crab" -> new RecipeDefinition(
                        1, "cataclysm:blessed_amethyst_crab_meat"
                );
                case "minecraft:warden" -> new RecipeDefinition(
                        2, "yixiangshouronglu:sedative_meat"
                );
                default -> null;
            };
            case "iceandfire:fire_dragon" -> "iceandfire:fire_dragon".equals(inputEntityId)
                    ? new RecipeDefinition(1, "iceandfire:fire_dragon_blood") : null;
            case "iceandfire:ice_dragon" -> "iceandfire:ice_dragon".equals(inputEntityId)
                    ? new RecipeDefinition(1, "iceandfire:ice_dragon_blood") : null;
            case "iceandfire:lightning_dragon" -> "iceandfire:lightning_dragon".equals(inputEntityId)
                    ? new RecipeDefinition(1, "iceandfire:lightning_dragon_blood") : null;
            case "minecraft:wither" -> "minecraft:wither".equals(inputEntityId)
                    ? new RecipeDefinition(4, "minecraft:nether_star") : null;
            default -> null;
        };
    }
}
