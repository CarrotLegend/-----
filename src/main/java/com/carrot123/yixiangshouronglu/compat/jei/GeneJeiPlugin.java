package com.carrot123.yixiangshouronglu.compat.jei;

import com.carrot123.yixiangshouronglu.client.ContainmentChamberScreen;
import com.carrot123.yixiangshouronglu.client.ChimeraDiagram;
import com.carrot123.yixiangshouronglu.gene.recipe.ChimeraRecipe;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import com.carrot123.yixiangshouronglu.production.ChamberSpecialProducts;
import com.carrot123.yixiangshouronglu.client.GeneSynthesizerScreen;
import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
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
    private static final RecipeType<ChimeraRecipe> CHIMERA = RecipeType.create("yixiangshouronglu", "chimera_modification", ChimeraRecipe.class);
    private static final RecipeType<ChamberSpecialProducts.Rule> EXTRACTION = RecipeType.create("yixiangshouronglu", "special_extraction", ChamberSpecialProducts.Rule.class);
    private List<ChimeraRecipe> chimeraRecipes = List.of();
    private final java.util.Set<ChimeraRecipe> indexedChimera = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private IJeiRuntime runtime;

    @Override public ResourceLocation getPluginUid() { return new ResourceLocation("yixiangshouronglu", "genes"); }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.CONTAINMENT_EXTRACT.get(), (stack, context) -> {
            String entityId = ContainmentExtractItem.getEntityId(stack);
            return entityId.isEmpty() ? IIngredientSubtypeInterpreter.NONE : entityId;
        });
        registration.registerSubtypeInterpreter(ModItems.SOUL.get(), (stack, context) -> {
            String entityId = SoulItem.getEntityId(stack);
            return entityId.isEmpty() ? IIngredientSubtypeInterpreter.NONE : entityId;
        });
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ChimeraCategory(helper), new ExtractionCategory(helper));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        var connection = Minecraft.getInstance().getConnection();
        chimeraRecipes = connection == null ? List.of() : currentChimera(connection.getRecipeManager());
        indexedChimera.clear();
        indexedChimera.addAll(chimeraRecipes);
        registration.addRecipes(CHIMERA, chimeraRecipes);
        registration.addRecipes(EXTRACTION, ChamberSpecialProducts.availableRules());
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.GENE_SYNTHESIZER.get()), CHIMERA);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()), EXTRACTION);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(GeneSynthesizerScreen.class, new IGuiContainerHandler<>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(GeneSynthesizerScreen screen, double mouseX, double mouseY) {
                boolean chimera = screen.getMenu().page() == GeneSynthesizerMenu.PAGE_CHIMERA;
                if (!chimera || chimeraRecipes.isEmpty()) {
                    return List.of();
                }
                if (chimera) {
                    int x = (int) Math.floor(mouseX / screen.guiScale() + screen.getGuiLeft() / screen.guiScale() - screen.getGuiLeft());
                    int y = (int) Math.floor(mouseY / screen.guiScale() + screen.getGuiTop() / screen.guiScale() - screen.getGuiTop());
                    if (!ChimeraDiagram.onProgressLines(x, y)) {
                        return List.of();
                    }
                }
                return List.of(scaledArea(screen, 61, 39, 144, 48, CHIMERA));
            }

            @Override public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(GeneSynthesizerScreen screen) {
                return List.of(screen.scaledTab(0), screen.scaledTab(1));
            }
        });
        registration.addGuiContainerHandler(ContainmentChamberScreen.class, new IGuiContainerHandler<>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(ContainmentChamberScreen screen, double mouseX, double mouseY) {
                return screen.getMenu().getPage() == ContainmentChamberMenu.PAGE_EXTRACTION
                        ? List.of(IGuiClickableArea.createBasic(75, 49, 31, 8, EXTRACTION)) : List.of();
            }
        });
    }

    private static IGuiClickableArea scaledArea(GeneSynthesizerScreen screen, int x, int y, int width, int height, RecipeType<?> type) {
        float scale = screen.guiScale();
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int startX = (int) Math.floor((left + x) * scale);
        int startY = (int) Math.floor((top + y) * scale);
        return IGuiClickableArea.createBasic(startX - left, startY - top,
                (int) Math.ceil((left + x + width) * scale) - startX,
                (int) Math.ceil((top + y + height) * scale) - startY, type);
    }

    @Override public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(ModItems.GENE_MODULE.get())));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override public void onRuntimeUnavailable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        runtime = null;
        chimeraRecipes = List.of();
        indexedChimera.clear();
    }

    @SubscribeEvent public void recipesUpdated(RecipesUpdatedEvent event) {
        if (runtime == null) { return; }
        var manager = runtime.getRecipeManager();
        List<ChimeraRecipe> next = currentChimera(event.getRecipeManager());
        if (!chimeraRecipes.equals(next)) {
            manager.hideRecipes(CHIMERA, chimeraRecipes);
            manager.addRecipes(CHIMERA, next.stream().filter(indexedChimera::add).toList());
            manager.unhideRecipes(CHIMERA, next);
            chimeraRecipes = next;
        }
    }

    private static List<ChimeraRecipe> currentChimera(RecipeManager manager) {
        return manager.getAllRecipesFor(ModGeneRecipes.CHIMERA_TYPE.get()).stream()
                .filter(recipe -> recipe.inputs().stream().allMatch(input -> java.util.Arrays.stream(input.ingredient().getItems()).anyMatch(stack -> !stack.is(ModItems.GENE_MODULE.get()))))
                .toList();
    }

    private static final class ChimeraCategory implements IRecipeCategory<ChimeraRecipe> {
        private final IDrawable background;
        private final IDrawable icon;

        private ChimeraCategory(IGuiHelper helper) {
            background = helper.createBlankDrawable(150, 78);
            icon = helper.createDrawableItemStack(new ItemStack(ModItems.GENE_SYNTHESIZER.get()));
        }

        @Override public RecipeType<ChimeraRecipe> getRecipeType() { return CHIMERA; }
        @Override public Component getTitle() { return Component.translatable("gui.yixiangshouronglu.chimera_tab"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public ResourceLocation getRegistryName(ChimeraRecipe recipe) { return recipe.getId(); }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, ChimeraRecipe recipe, IFocusGroup focuses) {
            builder.setShapeless();
            for (int index = 0; index < 4; index++) {
                var input = recipe.inputs().get(index);
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack stack : input.ingredient().getItems()) {
                    if (!stack.is(ModItems.GENE_MODULE.get())) { stacks.add(stack.copyWithCount(input.count())); }
                }
                builder.addSlot(RecipeIngredientRole.INPUT, 9 + index % 2 * 113, 8 + index / 2 * 39)
                        .setStandardSlotBackground().addItemStacks(stacks);
            }
            var level = Minecraft.getInstance().level;
            if (level != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 66, 28).addItemStack(recipe.getResultItem(level.registryAccess()));
            }
        }

        @Override public void draw(ChimeraRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            for (int index = 0; index < 4; index++) {
                ChimeraDiagram.line(graphics, 17 + index % 2 * 113, 16 + index / 2 * 39, 74, 36, 60, 60);
            }
            ChimeraDiagram.diamond(graphics, 74, 36);
            drawTime(graphics, ChimeraRecipe.DURATION, 67);
        }
    }

    private static final class ExtractionCategory implements IRecipeCategory<ChamberSpecialProducts.Rule> {
        private final IDrawable background;
        private final IDrawable icon;

        private ExtractionCategory(IGuiHelper helper) {
            background = helper.createBlankDrawable(150, 61);
            icon = helper.createDrawableItemStack(new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()));
        }

        @Override public RecipeType<ChamberSpecialProducts.Rule> getRecipeType() { return EXTRACTION; }
        @Override public Component getTitle() { return Component.translatable("jei.yixiangshouronglu.special_extraction"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, ChamberSpecialProducts.Rule rule, IFocusGroup focuses) {
            var recipe = rule.product();
            if (recipe == null) {
                return;
            }
            builder.addSlot(RecipeIngredientRole.CATALYST, 13, 25).setStandardSlotBackground()
                    .addItemStack(SoulItem.create(ModItems.SOUL.get(), new ResourceLocation(rule.chamber())));
            builder.addSlot(RecipeIngredientRole.INPUT, 64, 25).setStandardSlotBackground()
                    .addItemStack(ContainmentExtractItem.create(rule.input()).copyWithCount(recipe.inputCount()));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 25).setOutputSlotBackground().addItemStack(recipe.result());
        }

        @Override public void draw(ChamberSpecialProducts.Rule rule, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.yixiangshouronglu.chamber_soul"), 5, 8, 0x777777, false);
            graphics.fill(85, 30, 112, 35, 0xFFE1B65A);
            drawTime(graphics, 20, 49);
        }
    }

    private static void drawTime(GuiGraphics graphics, int ticks, int y) {
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("gene.yixiangshouronglu.time",
                GeneTime.totalSeconds(ticks)), 3, y, 0x777777, false);
    }
}
