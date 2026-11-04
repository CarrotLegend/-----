package com.carrot123.yixiangshouronglu.compat.jei;

import com.carrot123.yixiangshouronglu.client.GeneSynthesizerScreen;
import com.carrot123.yixiangshouronglu.gene.GeneDefinitions;
import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.gene.GeneTypes;
import com.carrot123.yixiangshouronglu.gene.recipe.GeneRecipe;
import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JeiPlugin
public final class GeneJeiPlugin implements IModPlugin {
    private static final RecipeType<GeneRecipe> SYNTHESIS = RecipeType.create("yixiangshouronglu", "gene_synthesis", GeneRecipe.class);
    private static final RecipeType<GeneRecipe> UPGRADE = RecipeType.create("yixiangshouronglu", "gene_upgrade", GeneRecipe.class);
    private IJeiRuntime runtime;
    private List<GeneRecipe> synthesisRecipes = List.of();
    private List<GeneRecipe> upgradeRecipes = List.of();

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("yixiangshouronglu", "genes"); }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.CONTAINMENT_EXTRACT.get(), (stack, context) -> {
            String entityId = ContainmentExtractItem.getEntityId(stack);
            return entityId.isEmpty() ? IIngredientSubtypeInterpreter.NONE : entityId;
        });
        registration.registerSubtypeInterpreter(ModItems.GENE_MODULE.get(), (stack, context) -> {
            var data = GeneModuleItem.read(stack);
            return data == null ? IIngredientSubtypeInterpreter.NONE : data.id() + "|" + data.strength() + "|" + data.quality();
        });
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new Category(helper, GeneTypes.Mode.SYNTHESIS), new Category(helper, GeneTypes.Mode.UPGRADE));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        List<GeneRecipe> recipes = currentRecipes();
        synthesisRecipes = recipes.stream().filter(recipe -> recipe.mode() == GeneTypes.Mode.SYNTHESIS).toList();
        upgradeRecipes = recipes.stream().filter(recipe -> recipe.mode() == GeneTypes.Mode.UPGRADE).toList();
        registration.addRecipes(SYNTHESIS, synthesisRecipes);
        registration.addRecipes(UPGRADE, upgradeRecipes);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.GENE_SYNTHESIZER.get()), SYNTHESIS, UPGRADE);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(GeneSynthesizerScreen.class, new IGuiContainerHandler<>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(GeneSynthesizerScreen screen, double mouseX, double mouseY) {
                RecipeType<GeneRecipe> type = screen.getMenu().mode() == GeneTypes.Mode.SYNTHESIS ? SYNTHESIS : UPGRADE;
                return List.of(IGuiClickableArea.createBasic(77, 63, 51, 10, type));
            }
        });
    }

    @Override public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override public void onRuntimeUnavailable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        runtime = null;
        synthesisRecipes = List.of();
        upgradeRecipes = List.of();
    }

    @SubscribeEvent public void recipesUpdated(RecipesUpdatedEvent event) {
        if (runtime == null) { return; }
        List<GeneRecipe> recipes = currentRecipes(event.getRecipeManager());
        List<GeneRecipe> newSynthesis = recipes.stream().filter(recipe -> recipe.mode() == GeneTypes.Mode.SYNTHESIS).toList();
        List<GeneRecipe> newUpgrade = recipes.stream().filter(recipe -> recipe.mode() == GeneTypes.Mode.UPGRADE).toList();
        if (synthesisRecipes.equals(newSynthesis) && upgradeRecipes.equals(newUpgrade)) { return; }
        var manager = runtime.getRecipeManager();
        manager.hideRecipes(SYNTHESIS, synthesisRecipes);
        manager.hideRecipes(UPGRADE, upgradeRecipes);
        manager.addRecipes(SYNTHESIS, newSynthesis);
        manager.addRecipes(UPGRADE, newUpgrade);
        synthesisRecipes = newSynthesis;
        upgradeRecipes = newUpgrade;
    }

    private static List<GeneRecipe> currentRecipes() {
        var level = Minecraft.getInstance().level;
        if (level == null) { return List.of(); }
        return currentRecipes(level.getRecipeManager());
    }

    private static List<GeneRecipe> currentRecipes(RecipeManager recipeManager) {
        var level = Minecraft.getInstance().level;
        if (level == null) { return List.of(); }
        return recipeManager.getAllRecipesFor(ModGeneRecipes.TYPE.get()).stream()
                .filter(recipe -> !recipe.getResultItem(level.registryAccess()).isEmpty())
                .filter(recipe -> !recipe.inputs().isEmpty())
                .filter(recipe -> recipe.inputs().stream().allMatch(input -> !inputStacks(input).isEmpty()))
                .toList();
    }

    private static List<ItemStack> inputStacks(GeneRecipe.Input input) {
        List<ItemStack> stacks = new ArrayList<>();
        if (input.geneId() != null || input.quality() != null) {
            var definitions = input.geneId() == null ? GeneDefinitions.values() : GeneDefinitions.values().stream().filter(definition -> definition.id().equals(input.geneId())).toList();
            for (var definition : definitions) {
                for (GeneTypes.Quality quality : GeneTypes.Quality.values()) {
                    if (input.quality() != null && quality != input.quality()) { continue; }
                    ItemStack module = GeneModuleItem.create(definition.id(), quality);
                    if (!module.isEmpty() && input.ingredient().test(module)) { stacks.add(module.copyWithCount(input.count())); }
                }
            }
        } else {
            for (ItemStack stack : input.ingredient().getItems()) {
                if (!stack.isEmpty()) { stacks.add(stack.copyWithCount(input.count())); }
            }
        }
        return stacks;
    }

    private static final class Category implements IRecipeCategory<GeneRecipe> {
        private final GeneTypes.Mode mode;
        private final IDrawable background;
        private final IDrawable icon;

        private Category(IGuiHelper helper, GeneTypes.Mode mode) {
            this.mode = mode;
            background = helper.createBlankDrawable(150, 68);
            icon = helper.createDrawableItemStack(new ItemStack(ModItems.GENE_SYNTHESIZER.get()));
        }

        @Override public RecipeType<GeneRecipe> getRecipeType() { return mode == GeneTypes.Mode.SYNTHESIS ? SYNTHESIS : UPGRADE; }
        @Override public Component getTitle() { return Component.translatable("jei.yixiangshouronglu.gene_" + (mode == GeneTypes.Mode.SYNTHESIS ? "synthesis" : "upgrade")); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, GeneRecipe recipe, IFocusGroup focuses) {
            if (recipe.shapeless()) { builder.setShapeless(); }
            for (var input : recipe.inputs()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 4 + input.slot() % 2 * 22, 5 + input.slot() / 2 * 22)
                        .setStandardSlotBackground()
                        .addItemStacks(inputStacks(input));
            }
            var level = Minecraft.getInstance().level;
            if (level != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 16).setOutputSlotBackground().addItemStack(recipe.getResultItem(level.registryAccess()));
            }
        }

        @Override public void draw(GeneRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.fill(56, 20, 103, 27, 0xFF263B47);
            graphics.fill(57, 21, 102, 26, 0xFFE1B65A);
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("gene.yixiangshouronglu.time", GeneTime.totalSeconds(recipe.duration())), 3, 55, 0x777777, false);
        }
    }
}
