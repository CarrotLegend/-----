package com.carrot123.yixiangshouronglu.production;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChamberSpecialProductsTest {
    @Test
    void matchesEachChamberAndInputWithTheRequiredCount() {
        assertRecipe("cataclysm:amethyst_crab", "cataclysm:amethyst_crab",
                1, "cataclysm:blessed_amethyst_crab_meat");
        assertRecipe("cataclysm:amethyst_crab", "minecraft:warden",
                2, "yixiangshouronglu:sedative_meat");
        assertRecipe("iceandfire:fire_dragon", "iceandfire:fire_dragon",
                1, "iceandfire:fire_dragon_blood");
        assertRecipe("iceandfire:ice_dragon", "iceandfire:ice_dragon",
                1, "iceandfire:ice_dragon_blood");
        assertRecipe("iceandfire:lightning_dragon", "iceandfire:lightning_dragon",
                1, "iceandfire:lightning_dragon_blood");
        assertRecipe("minecraft:wither", "minecraft:wither",
                4, "minecraft:nether_star");
    }

    @Test
    void rejectsWrongChamberAndCrossDragonInputs() {
        assertNull(ChamberSpecialProducts.definitionForInput("minecraft:warden", "minecraft:warden"));
        assertNull(ChamberSpecialProducts.definitionForInput("cataclysm:amethyst_crab", "minecraft:wither"));
        assertNull(ChamberSpecialProducts.definitionForInput("iceandfire:fire_dragon", "iceandfire:ice_dragon"));
        assertNull(ChamberSpecialProducts.definitionForInput("iceandfire:fire_dragon", "iceandfire:lightning_dragon"));
        assertNull(ChamberSpecialProducts.definitionForInput("iceandfire:ice_dragon", "iceandfire:fire_dragon"));
        assertNull(ChamberSpecialProducts.definitionForInput("iceandfire:ice_dragon", "iceandfire:lightning_dragon"));
        assertNull(ChamberSpecialProducts.definitionForInput("iceandfire:lightning_dragon", "iceandfire:fire_dragon"));
        assertNull(ChamberSpecialProducts.definitionForInput("iceandfire:lightning_dragon", "iceandfire:ice_dragon"));
    }

    private static void assertRecipe(String chamberEntityId, String inputEntityId,
                                     int inputCount, String itemId) {
        ChamberSpecialProducts.RecipeDefinition definition =
                ChamberSpecialProducts.definitionForInput(chamberEntityId, inputEntityId);
        assertEquals(new ChamberSpecialProducts.RecipeDefinition(inputCount, itemId), definition);
    }
}
