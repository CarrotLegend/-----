package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.gene.recipe.GeneRecipe;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.*;

public final class ModGeneRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, AnomalousContainmentChronicles.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<RecipeType<GeneRecipe>> TYPE = TYPES.register("gene_synthesis", () -> new RecipeType<GeneRecipe>() {@Override public String toString() {return "yixiangshouronglu:gene_synthesis";}});
    public static final RegistryObject<RecipeSerializer<GeneRecipe>> SERIALIZER = SERIALIZERS.register("gene_synthesis", GeneRecipe.Serializer::new);
    private ModGeneRecipes() {}
}
