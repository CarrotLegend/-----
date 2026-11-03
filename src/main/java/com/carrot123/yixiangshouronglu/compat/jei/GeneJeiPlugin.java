package com.carrot123.yixiangshouronglu.compat.jei;

import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.gene.GeneTypes;
import com.carrot123.yixiangshouronglu.gene.recipe.GeneRecipe;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.registry.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class GeneJeiPlugin implements IModPlugin {
    private static final RecipeType<GeneRecipe> SYNTHESIS = RecipeType.create("yixiangshouronglu", "gene_synthesis", GeneRecipe.class);
    private static final RecipeType<GeneRecipe> UPGRADE = RecipeType.create("yixiangshouronglu", "gene_upgrade", GeneRecipe.class);
    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("yixiangshouronglu", "genes"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper(), GeneTypes.Mode.SYNTHESIS), new Category(registration.getJeiHelpers().getGuiHelper(), GeneTypes.Mode.UPGRADE));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) { return; }
        List<GeneRecipe> recipes = level.getRecipeManager().getAllRecipesFor(ModGeneRecipes.TYPE.get());
        registration.addRecipes(SYNTHESIS, recipes.stream().filter(recipe -> recipe.mode() == GeneTypes.Mode.SYNTHESIS).toList());
        registration.addRecipes(UPGRADE, recipes.stream().filter(recipe -> recipe.mode() == GeneTypes.Mode.UPGRADE).toList());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) { registration.addRecipeCatalyst(new ItemStack(ModItems.GENE_SYNTHESIZER.get()), SYNTHESIS, UPGRADE); }
    private static final class Category implements IRecipeCategory<GeneRecipe> {
        private final GeneTypes.Mode mode;
        private final IDrawable background;
        private final IDrawable icon;
        private Category(IGuiHelper helper, GeneTypes.Mode mode) { this.mode = mode; background = helper.createBlankDrawable(150, 68); icon = helper.createDrawableItemStack(new ItemStack(ModItems.GENE_SYNTHESIZER.get())); }
        @Override public RecipeType<GeneRecipe> getRecipeType() { return mode == GeneTypes.Mode.SYNTHESIS ? SYNTHESIS : UPGRADE; }
        @Override public Component getTitle() { return Component.translatable("gene.yixiangshouronglu.mode." + mode.name().toLowerCase(java.util.Locale.ROOT)); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, GeneRecipe recipe, IFocusGroup focuses) {
            for (var input : recipe.inputs()) {
                List<ItemStack> stacks = new ArrayList<>();
                if (input.geneId() != null) {
                    ItemStack module = GeneModuleItem.create(input.geneId(), input.quality() == null ? GeneTypes.Quality.ALPHA : input.quality());
                    if (!module.isEmpty()) { module.setCount(input.count()); stacks.add(module); }
                }
                if (stacks.isEmpty()) { for (ItemStack item : input.ingredient().getItems()) { stacks.add(item.copyWithCount(input.count())); } }
                builder.addSlot(RecipeIngredientRole.INPUT, 4 + input.slot() % 2 * 22, 5 + input.slot() / 2 * 22).addItemStacks(stacks);
            }
            var level = Minecraft.getInstance().level;
            if (level != null) { builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 16).addItemStack(recipe.getResultItem(level.registryAccess())); }
        }
        @Override public void draw(GeneRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.fill(56, 20, 103, 27, 0xFF263B47);
            graphics.fill(57, 21, 102, 26, 0xFFE1B65A);
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("gene.yixiangshouronglu.time", GeneTime.seconds(recipe.duration())), 3, 55, 0x777777, false);
        }
    }
}
