package com.carrot123.yixiangshouronglu.compat.jei;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.client.ContainmentChamberScreen;
import com.carrot123.yixiangshouronglu.client.ChimeraDiagram;
import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.gene.recipe.ChimeraRecipe;
import com.carrot123.yixiangshouronglu.item.BiologicalGeneModuleItem;
import com.carrot123.yixiangshouronglu.item.GeneFragmentItem;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import com.carrot123.yixiangshouronglu.production.ChamberSpecialProducts;
import com.carrot123.yixiangshouronglu.gene.GeneTime;
import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@JeiPlugin
public final class GeneJeiPlugin implements IModPlugin {
    private static final RecipeType<ChimeraRecipe> CHIMERA = RecipeType.create("yixiangshouronglu", "chimera_modification", ChimeraRecipe.class);
    private static final RecipeType<ChamberSpecialProducts.Rule> EXTRACTION = RecipeType.create("yixiangshouronglu", "special_extraction", ChamberSpecialProducts.Rule.class);
    private static final RecipeType<ModuleExtraction> MODULE_EXTRACTION =
            RecipeType.create("yixiangshouronglu", "gene_module_extraction", ModuleExtraction.class);
    private List<ChimeraRecipe> chimeraRecipes = List.of();
    private final java.util.Set<ChimeraRecipe> indexedChimera = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final Set<ResourceLocation> indexedExtracts = new HashSet<>();
    private final Set<String> indexedGeneSources = new HashSet<>();
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
        registration.registerSubtypeInterpreter(ModItems.GENE_FRAGMENT.get(), (stack, context) -> {
            var entityType = GeneFragmentItem.sourceType(stack);
            var source = entityType == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            if (source == null) {
                return IIngredientSubtypeInterpreter.NONE;
            }
            return source.toString();
        });
        registration.registerSubtypeInterpreter(ModItems.BIOLOGICAL_GENE_MODULE.get(), (stack, context) -> {
            var source = BiologicalGeneModuleItem.validatedSource(stack);
            var gene = BiologicalGeneModuleItem.gene(stack);
            return source == null || gene == null ? IIngredientSubtypeInterpreter.NONE : gene.id + ":" + source;
        });
    }

    @Override public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(MODULE_EXTRACTION, new ISimpleRecipeManagerPlugin<ModuleExtraction>() {
            @Override public boolean isHandledInput(ITypedIngredient<?> input) {
                var type = input.getItemStack().map(GeneFragmentItem::sourceType).orElse(null);
                var source = type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
                return isAdditionalCommonSource(source);
            }

            @Override public boolean isHandledOutput(ITypedIngredient<?> output) {
                var source = output.getItemStack().map(BiologicalGeneModuleItem::validatedSource).orElse(null);
                return isAdditionalCommonSource(source)
                        && output.getItemStack().map(BiologicalGeneModuleItem::gene).orElse(null) == BiologicalGene.COMMON;
            }

            @Override public List<ModuleExtraction> getRecipesForInput(ITypedIngredient<?> input) {
                if (!isHandledInput(input)) {
                    return List.of();
                }
                var type = GeneFragmentItem.sourceType(input.getItemStack().orElse(ItemStack.EMPTY));
                return List.of(moduleExtraction(ForgeRegistries.ENTITY_TYPES.getKey(type)));
            }

            @Override public List<ModuleExtraction> getRecipesForOutput(ITypedIngredient<?> output) {
                return isHandledOutput(output) ? List.of(moduleExtraction(BiologicalGeneModuleItem.validatedSource(
                        output.getItemStack().orElse(ItemStack.EMPTY)))) : List.of();
            }

            @Override public List<ModuleExtraction> getAllRecipes() {
                return List.of();
            }
        });
    }

    @Override public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(moduleExtractions().stream().map(ModuleExtraction::fragment).toList());
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ChimeraCategory(helper), new ExtractionCategory(helper),
                new ModuleExtractionCategory(helper));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        var connection = Minecraft.getInstance().getConnection();
        chimeraRecipes = connection == null ? List.of() : currentChimera(connection.getRecipeManager());
        indexedChimera.clear();
        indexedChimera.addAll(chimeraRecipes);
        registration.addRecipes(CHIMERA, chimeraRecipes);
        registration.addRecipes(EXTRACTION, ChamberSpecialProducts.availableRules());
        var extractions = moduleExtractions();
        registration.addRecipes(MODULE_EXTRACTION, extractions);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.GENE_SYNTHESIZER.get()), CHIMERA);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()), EXTRACTION, MODULE_EXTRACTION);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(ContainmentChamberScreen.class, new IGuiContainerHandler<>() {
            @Override public Collection<IGuiClickableArea> getGuiClickableAreas(ContainmentChamberScreen screen, double mouseX, double mouseY) {
                return screen.getMenu().getPage() == ContainmentChamberMenu.PAGE_EXTRACTION
                        ? List.of(IGuiClickableArea.createBasic(75, 49, 31, 8, EXTRACTION, MODULE_EXTRACTION)) : List.of();
            }
        });
    }

    @Override public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        indexChimeraIngredients(chimeraRecipes);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override public void onRuntimeUnavailable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        runtime = null;
        chimeraRecipes = List.of();
        indexedChimera.clear();
        indexedExtracts.clear();
        indexedGeneSources.clear();
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
            indexChimeraIngredients(next);
        }
    }

    private void indexChimeraIngredients(List<ChimeraRecipe> recipes) {
        List<ItemStack> stacks = new ArrayList<>();
        recipes.stream()
                .flatMap(recipe -> recipe.inputs().stream())
                .map(ChimeraRecipe.Input::extractEntity)
                .filter(Objects::nonNull)
                .filter(indexedExtracts::add)
                .map(entity -> ContainmentExtractItem.create(entity.toString()))
                .filter(stack -> !stack.isEmpty())
                .forEach(stacks::add);
        recipes.stream().flatMap(recipe -> recipe.inputs().stream())
                .filter(input -> input.geneSource() != null)
                .flatMap(input -> input.displayStacks().stream())
                .filter(stack -> indexedGeneSources.add(stack.getTag().getString("GeneType") + ":"
                        + stack.getTag().getString("SourceEntityId")))
                .forEach(stacks::add);
        if (!stacks.isEmpty()) {
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, stacks);
        }
    }

    private static List<ChimeraRecipe> currentChimera(RecipeManager manager) {
        return manager.getAllRecipesFor(ModGeneRecipes.CHIMERA_TYPE.get()).stream()
                .filter(recipe -> recipe.inputs().stream().allMatch(input -> !input.displayStacks().isEmpty()))
                .toList();
    }

    private static List<ModuleExtraction> moduleExtractions() {
        List<ModuleExtraction> recipes = new ArrayList<>();
        for (BiologicalGene gene : BiologicalGene.values()) {
            for (var source : gene.displaySources()) {
                if (ForgeRegistries.ENTITY_TYPES.containsKey(source)) {
                    var extraction = moduleExtraction(source);
                    if (!extraction.module().isEmpty() && BiologicalGeneModuleItem.gene(extraction.module()) == gene) {
                        recipes.add(extraction);
                    }
                }
            }
        }
        return recipes;
    }

    private static boolean isAdditionalCommonSource(ResourceLocation source) {
        return source != null && !source.equals(BiologicalGene.COMMON.displaySource())
                && BiologicalGene.fromSource(source) == BiologicalGene.COMMON;
    }

    private static ModuleExtraction moduleExtraction(ResourceLocation source) {
        var snapshot = new CompoundTag();
        snapshot.putUUID("UUID", new UUID(0, 0));
        var fragment = new ItemStack(ModItems.GENE_FRAGMENT.get());
        fragment.setTag(GeneFragmentItem.sourceData(source, snapshot));
        return new ModuleExtraction(BiologicalGene.fromSource(source), fragment, BiologicalGeneModuleItem.fromFragment(fragment));
    }

    private record ModuleExtraction(BiologicalGene gene, ItemStack fragment, ItemStack module) {}

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
            for (int index = 0; index < recipe.inputs().size(); index++) {
                var input = recipe.inputs().get(index);
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, 9 + index % 2 * 113, 8 + index / 2 * 39)
                        .setStandardSlotBackground().addItemStacks(input.displayStacks());
                if (input.geneSource() != null) {
                    var entity = ForgeRegistries.ENTITY_TYPES.getValue(input.geneSource());
                    if (entity != null) {
                        slot.addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                                "jei.yixiangshouronglu.gene_source", entity.getDescription())));
                    }
                }
            }
            var level = Minecraft.getInstance().level;
            if (level != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 66, 28).addItemStack(recipe.getResultItem(level.registryAccess()));
            }
        }

        @Override public void draw(ChimeraRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            for (int index = 0; index < recipe.inputs().size(); index++) {
                ChimeraDiagram.line(graphics, 17 + index % 2 * 113, 16 + index / 2 * 39, 74, 36, 60, 60);
            }
            ChimeraDiagram.diamond(graphics, 74, 36);
            drawTime(graphics, ChimeraRecipe.DURATION, 67);
        }
    }

    private static final class ExtractionCategory implements IRecipeCategory<ChamberSpecialProducts.Rule> {
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable arrow;

        private ExtractionCategory(IGuiHelper helper) {
            background = helper.createBlankDrawable(150, 61);
            icon = helper.createDrawableItemStack(new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()));
            arrow = helper.getRecipeArrow();
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
                    .addItemStack(rule.inputStack());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 25).setOutputSlotBackground().addItemStack(recipe.result());
        }

        @Override public void draw(ChamberSpecialProducts.Rule rule, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.yixiangshouronglu.chamber_soul"), 5, 8, 0x777777, false);
            arrow.draw(graphics, 88, 25);
            drawTime(graphics, 20, 49);
        }
    }

    private static void drawTime(GuiGraphics graphics, int ticks, int y) {
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("gene.yixiangshouronglu.time",
                GeneTime.totalSeconds(ticks)), 3, y, 0x777777, false);
    }

    private final class ModuleExtractionCategory implements IRecipeCategory<ModuleExtraction> {
        private static final int WIDTH = 180;
        private static final int CHAMBER_X = (WIDTH - 16) / 2;
        private static final int SLOT_Y = 7;
        private static final int TEXT_COLOR = 0x777777;
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawable arrow;

        private ModuleExtractionCategory(IGuiHelper helper) {
            background = helper.createBlankDrawable(WIDTH, 112);
            icon = helper.createDrawableItemStack(new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()));
            arrow = helper.getRecipeArrow();
        }

        @Override public RecipeType<ModuleExtraction> getRecipeType() { return MODULE_EXTRACTION; }
        @Override public Component getTitle() { return Component.translatable("jei.yixiangshouronglu.gene_module_extraction"); }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }

        @Override public void setRecipe(IRecipeLayoutBuilder builder, ModuleExtraction recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 21, SLOT_Y).setStandardSlotBackground()
                    .addItemStack(recipe.fragment());
            builder.addSlot(RecipeIngredientRole.CATALYST, CHAMBER_X, SLOT_Y).setStandardSlotBackground()
                    .addItemStack(new ItemStack(ModItems.CONTAINMENT_CHAMBER.get()));
            builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 37, SLOT_Y).setOutputSlotBackground()
                    .addItemStack(recipe.module());
        }

        @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, ModuleExtraction recipe, IFocusGroup focuses) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override public ScreenRectangle getArea() {
                    return new ScreenRectangle(CHAMBER_X, SLOT_Y, 16, 16);
                }

                @Override public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    if (runtime == null || !input.getKey().equals(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT))) {
                        return false;
                    }
                    if (!input.isSimulate()) {
                        runtime.getRecipesGui().showTypes(List.of(EXTRACTION, MODULE_EXTRACTION));
                    }
                    return true;
                }
            });
        }

        @Override public void draw(ModuleExtraction recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            arrow.draw(graphics, 111, SLOT_Y);
            var font = Minecraft.getInstance().font;
            var chamberName = ModItems.CONTAINMENT_CHAMBER.get().getDescription();
            graphics.drawString(font, chamberName, (WIDTH - font.width(chamberName)) / 2, 29, TEXT_COLOR, false);
            graphics.drawString(font, Component.translatable("jei.yixiangshouronglu.gene_fluid",
                    ContainmentChamberBlockEntity.EXTRACT_COST_MB), 3, 42, TEXT_COLOR, false);
            drawTime(graphics, ContainmentChamberBlockEntity.EXTRACT_TIME, 54);
            graphics.drawString(font, Component.translatable("jei.yixiangshouronglu.contained_soul_required"),
                    3, 66, TEXT_COLOR, false);
            if (recipe.gene() == BiologicalGene.COMMON) {
                graphics.drawWordWrap(font, Component.translatable("jei.yixiangshouronglu.common_module_sources"),
                        3, 78, WIDTH - 6, TEXT_COLOR);
            }
        }
    }
}
