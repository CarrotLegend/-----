package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.recipe.ChimeraRecipe;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.*;

public final class ModGeneRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, AnomalousContainmentChronicles.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<RecipeType<ChimeraRecipe>> CHIMERA_TYPE = TYPES.register("chimera_modification", () -> new RecipeType<ChimeraRecipe>() {
        @Override public String toString() { return "yixiangshouronglu:chimera_modification"; }
    });
    public static final RegistryObject<RecipeSerializer<ChimeraRecipe>> CHIMERA_SERIALIZER = SERIALIZERS.register("chimera_modification", ChimeraRecipe.Serializer::new);
    private ModGeneRecipes() {}
}
