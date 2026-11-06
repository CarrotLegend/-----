package com.carrot123.yixiangshouronglu.genevalidation;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.gene.*;
import com.carrot123.yixiangshouronglu.gene.recipe.GeneRecipe;
import com.carrot123.yixiangshouronglu.item.*;
import com.carrot123.yixiangshouronglu.registry.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder("yixiangshouronglu")
@PrefixGameTestTemplate(false)
public final class GeneUpgradeRuntimeTests {
    private static List<GeneDefinition> definitions() {
        return GeneDefinitions.values().stream().filter(value -> value.id().getNamespace().equals("yixiangshouronglu")).toList();
    }
    private static List<GeneRecipe> upgrades(GameTestHelper helper) {
        return helper.getLevel().getRecipeManager().getAllRecipesFor(ModGeneRecipes.TYPE.get()).stream()
                .filter(recipe -> recipe.mode() == GeneTypes.Mode.UPGRADE).toList();
    }
    private static SimpleContainer inputs(GeneRecipe recipe) {
        var inputs = new SimpleContainer(4);
        for (var input : recipe.inputs()) {
            ItemStack stack = input.geneId() != null ? GeneModuleItem.create(input.geneId(), input.quality()) : input.ingredient().getItems()[0].copy();
            inputs.setItem(input.slot(), stack.copyWithCount(input.count()));
        }
        return inputs;
    }
    private static GeneSynthesizerBlockEntity machine(GameTestHelper helper, SimpleContainer inputs) {
        var state = ModBlocks.GENE_SYNTHESIZER.get().defaultBlockState();
        var machine = new GeneSynthesizerBlockEntity(helper.absolutePos(BlockPos.ZERO), state);
        machine.setLevel(helper.getLevel()); machine.mode(GeneTypes.Mode.UPGRADE);
        for (int slot = 0; slot < 4; slot++) { machine.inventory.setStackInSlot(slot, inputs.getItem(slot).copy()); }
        return machine;
    }
    private static void tick(GameTestHelper helper, GeneSynthesizerBlockEntity machine, int ticks) {
        for (int index = 0; index < ticks; index++) {
            GeneSynthesizerBlockEntity.tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
        }
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void everyGeneUpgradesWithExactCountsAndNbt(GameTestHelper helper) {
        var recipes = upgrades(helper);
        helper.assertTrue(definitions().size() == 14 && recipes.size() == 28, "Fourteen genes and twenty-eight upgrades required");
        for (var definition : definitions()) {
            for (var quality : new GeneTypes.Quality[]{GeneTypes.Quality.ALPHA, GeneTypes.Quality.BETA}) {
                var matches = recipes.stream().filter(recipe -> recipe.inputs().get(0).geneId().equals(definition.id())
                        && recipe.inputs().get(0).quality() == quality).toList();
                helper.assertTrue(matches.size() == 1, "Exactly one upgrade per gene and quality");
                GeneRecipe recipe = matches.get(0); var input = inputs(recipe);
                input.getItem(0).setHoverName(Component.literal("preserved name"));
                input.getItem(0).getOrCreateTagElement("Gene").getCompound("Extension").putString("custom", "preserved");
                input.getItem(0).getOrCreateTag().putString("external", "preserved");
                var legacyCategories = new net.minecraft.nbt.ListTag();
                legacyCategories.add(net.minecraft.nbt.StringTag.valueOf("legacy:preserved"));
                input.getItem(0).getOrCreateTagElement("Gene").put("Categories", legacyCategories);
                var expected = input.getItem(0).copy();
                expected.getOrCreateTagElement("Gene").putString("Quality", quality.next().name());
                var machine = machine(helper, input);
                tick(helper, machine, 199);
                helper.assertTrue(machine.inventory.getStackInSlot(4).isEmpty() && machine.inventory.getStackInSlot(1).getCount() == recipe.inputs().get(1).count(), "No early output or consumption");
                tick(helper, machine, 1);
                helper.assertTrue(ItemStack.matches(expected, machine.inventory.getStackInSlot(4)), "Only quality changes in actual output NBT");
                for (int slot = 0; slot < 4; slot++) { helper.assertTrue(machine.inventory.getStackInSlot(slot).isEmpty(), "All required counts consumed"); }
                tick(helper, machine, 400);
                helper.assertTrue(ItemStack.matches(expected, machine.inventory.getStackInSlot(4)), "Exhausted ingredients cannot duplicate output");
            }
        }
        helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void invalidInputsAndBlockedOutputsDoNotConsume(GameTestHelper helper) {
        for (var recipe : upgrades(helper)) {
            var original = inputs(recipe);
            if (recipe.inputs().get(0).quality() == GeneTypes.Quality.ALPHA) {
                var meats = recipe.inputs().get(3).ingredient().getItems();
                helper.assertTrue(meats.length > 1, "Meat tag retains alternatives");
                for (var meat : meats) {
                    var alternative = inputs(recipe); alternative.setItem(3, meat.copyWithCount(16));
                    helper.assertTrue(recipe.matchesInputs(alternative, GeneTypes.Mode.UPGRADE), "Every meat tag alternative matches");
                }
            }
            for (int slot = 0; slot < 4; slot++) {
                var invalid = inputs(recipe); invalid.getItem(slot).shrink(1);
                helper.assertTrue(!recipe.matchesInputs(invalid, GeneTypes.Mode.UPGRADE), "Insufficient quantity rejected");
            }
            for (var quality : GeneTypes.Quality.values()) {
                if (quality == recipe.inputs().get(0).quality()) { continue; }
                var invalid = inputs(recipe); invalid.getItem(0).getOrCreateTagElement("Gene").putString("Quality", quality.name());
                helper.assertTrue(!recipe.matchesInputs(invalid, GeneTypes.Mode.UPGRADE), "Wrong quality and skipping rejected");
            }
            var invalid = inputs(recipe);
            var other = definitions().stream().filter(value -> !value.id().equals(recipe.inputs().get(0).geneId())).findFirst().orElseThrow();
            invalid.setItem(0, GeneModuleItem.create(other.id(), recipe.inputs().get(0).quality()));
            helper.assertTrue(!recipe.matchesInputs(invalid, GeneTypes.Mode.UPGRADE), "Wrong gene rejected");
            invalid = inputs(recipe); invalid.getItem(0).getOrCreateTagElement("Gene").putInt("Version", -1);
            helper.assertTrue(!recipe.matchesInputs(invalid, GeneTypes.Mode.UPGRADE), "Corrupted NBT rejected");
            var machine = machine(helper, original); machine.inventory.setStackInSlot(4, new ItemStack(Items.COBBLESTONE, 64));
            tick(helper, machine, 250);
            for (int slot = 0; slot < 4; slot++) { helper.assertTrue(ItemStack.matches(original.getItem(slot), machine.inventory.getStackInSlot(slot)), "Blocked output keeps all materials"); }
            machine.inventory.setStackInSlot(4, ItemStack.EMPTY); tick(helper, machine, 200);
            helper.assertTrue(GeneModuleItem.read(machine.inventory.getStackInSlot(4)).quality() == recipe.inputs().get(0).quality().next(), "Unblocking resumes normal craft");
        }
        helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void tooltipsHideOnlyGeneIds(GameTestHelper helper) {
        for (var definition : definitions()) {
            for (var quality : GeneTypes.Quality.values()) {
                var stack = GeneModuleItem.create(definition.id(), quality);
                List<Component> tooltip = new ArrayList<>();
                stack.getItem().appendHoverText(stack, helper.getLevel(), tooltip, TooltipFlag.Default.NORMAL);
                helper.assertTrue(tooltip.stream().noneMatch(line -> line.getString().contains(definition.id().toString())), "Tooltip hides gene registry ID");
                helper.assertTrue(tooltip.get(0).equals(Component.translatable(definition.strength().key()).append(" / ").append(Component.translatable(quality.key()))), "Strength and quality retained");
                helper.assertTrue(tooltip.contains(Component.translatable("gene.yixiangshouronglu.cost", definition.cost())), "Capacity retained");
                helper.assertTrue(tooltip.contains(Component.translatable(definition.region().key())), "Region retained");
                helper.assertTrue(stack.getHoverName().equals(Component.translatable(definition.nameKey())), "Name retained");
                if (!definition.categories().isEmpty()) { helper.assertTrue(tooltip.contains(GeneDescriptions.categories(definition)), "Categories retained"); }
                for (var effect : GeneDescriptions.effects(stack)) {
                    helper.assertTrue(tooltip.contains(effect) && effect.getStyle().isItalic() && ChatFormatting.GOLD.getColor().equals(effect.getStyle().getColor().getValue()), "Gold italic effects retained");
                }
                var copy = ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
                helper.assertTrue(GeneModuleItem.definition(copy) == definition && GeneModuleItem.read(copy).quality() == quality, "Existing NBT roundtrip readable");
            }
        }
        helper.succeed();
    }
    private static ContainmentChamberBlockEntity chamber(GameTestHelper helper, String entity) {
        var chamber = new ContainmentChamberBlockEntity(helper.absolutePos(BlockPos.ZERO), ModBlocks.CONTAINMENT_CHAMBER.get().defaultBlockState());
        chamber.setLevel(helper.getLevel());
        chamber.insertSoul(SoulItem.create(ModItems.SOUL.get(), new ResourceLocation(entity)));
        return chamber;
    }
    private static void tick(GameTestHelper helper, ContainmentChamberBlockEntity chamber, int ticks) {
        for (int index = 0; index < ticks; index++) { ContainmentChamberBlockEntity.serverTick(helper.getLevel(), chamber.getBlockPos(), chamber.getBlockState(), chamber); }
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void dragonBreathConsumesOnceAndRejectsOtherEntities(GameTestHelper helper) {
        String dragon = "yixiangshouronglu:young_ender_dragon";
        var chamber = chamber(helper, dragon);
        var extract = ContainmentExtractItem.create(dragon); extract.setCount(2);
        helper.assertTrue(chamber.canPlaceItem(2, extract), "Matching extract accepted");
        chamber.setItem(2, extract);
        tick(helper, chamber, 19);
        helper.assertTrue(chamber.getItem(2).getCount() == 2 && chamber.getItem(3).isEmpty(), "Original twenty tick timing");
        tick(helper, chamber, 1);
        helper.assertTrue(chamber.getItem(2).getCount() == 1 && chamber.getItem(3).is(Items.DRAGON_BREATH) && chamber.getItem(3).getCount() == 1, "One extract for one breath");
        tick(helper, chamber, 100);
        helper.assertTrue(chamber.getItem(2).isEmpty() && chamber.getItem(3).getCount() == 2, "Exhausted input stops output");
        chamber.setItem(2, ContainmentExtractItem.create(dragon)); chamber.setItem(3, new ItemStack(Items.DRAGON_BREATH, 64));
        tick(helper, chamber, 40);
        helper.assertTrue(chamber.getItem(2).getCount() == 1 && chamber.getItem(3).getCount() == 64, "Full output consumes nothing");
        for (String wrong : new String[]{"minecraft:ender_dragon", "minecraft:wither"}) {
            var bad = ContainmentExtractItem.create(wrong);
            helper.assertTrue(!chamber.canPlaceItem(2, bad), "Foreign extract rejected");
            chamber.setItem(2, bad); chamber.setItem(3, ItemStack.EMPTY); tick(helper, chamber, 40);
            helper.assertTrue(chamber.getItem(2).getCount() == 1 && chamber.getItem(3).isEmpty(), "Forced foreign input cannot craft");
            var wrongChamber = chamber(helper, wrong); wrongChamber.setItem(2, ContainmentExtractItem.create(dragon)); tick(helper, wrongChamber, 40);
            helper.assertTrue(wrongChamber.getItem(3).isEmpty(), "Wrong chamber cannot use young dragon extract");
        }
        chamber.setItem(2, new ItemStack(ModItems.CONTAINMENT_EXTRACT.get())); tick(helper, chamber, 40);
        helper.assertTrue(chamber.getItem(3).isEmpty(), "Missing EntityId rejected");
        chamber.clearContent(); tick(helper, chamber, 4800);
        helper.assertTrue(chamber.getItem(0).is(Items.DRAGON_EGG) || chamber.getItem(1).is(Items.DRAGON_EGG), "Original egg production unchanged");
        helper.succeed();
    }
}
