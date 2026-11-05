package com.carrot123.yixiangshouronglu.item;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ListTag;
import com.carrot123.yixiangshouronglu.gene.*;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class GeneModuleItem extends Item {
    public static final int VERSION = 1;
    public record Data(ResourceLocation id, GeneTypes.Strength strength, GeneTypes.Quality quality) {}
    public GeneModuleItem(Properties properties) { super(properties); }
    @Nullable
    public static Data read(ItemStack stack) {
        if (!(stack.getItem() instanceof GeneModuleItem)) { return null; }
        CompoundTag tag = stack.getTagElement("Gene");
        if (tag == null || tag.getInt("Version") != VERSION) { return null; }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
        if (id == null) { return null; }
        try {
            return new Data(id, GeneTypes.Strength.valueOf(tag.getString("Strength")), GeneTypes.Quality.valueOf(tag.getString("Quality")));
        } catch (IllegalArgumentException exception) { return null; }
    }
    @Nullable
    public static GeneDefinition definition(ItemStack stack) {
        Data data = read(stack);
        GeneDefinition definition = data == null ? null : GeneDefinitions.get(data.id());
        return definition != null && definition.strength() == data.strength() ? definition : null;
    }
    public static ItemStack create(ResourceLocation id, GeneTypes.Quality quality) {
        GeneDefinition definition = GeneDefinitions.get(id);
        if (definition == null) { return ItemStack.EMPTY; }
        ItemStack stack = new ItemStack(ModItems.GENE_MODULE.get());
        CompoundTag tag = stack.getOrCreateTagElement("Gene");
        tag.putInt("Version", VERSION);
        tag.putString("Id", id.toString());
        tag.putString("Strength", definition.strength().name());
        tag.putString("Quality", quality.name());
        tag.put("Extension", new CompoundTag());
        writeCategories(stack, definition);
        return stack;
    }
    public static ItemStack upgrade(ItemStack original, GeneTypes.Quality target) {
        Data data = read(original);
        if (data == null || definition(original) == null || data.quality().next() != target) { return ItemStack.EMPTY; }
        ItemStack result = original.copyWithCount(1);
        result.getOrCreateTagElement("Gene").putString("Quality", target.name());
        return result;
    }
    private static void writeCategories(ItemStack module, GeneDefinition definition) {
        ListTag categories = new ListTag();
        definition.categories().stream().sorted().forEach(id -> categories.add(StringTag.valueOf(id.toString())));
        module.getOrCreateTagElement("Gene").put("Categories", categories);
    }
    @Override
    public Component getName(ItemStack stack) {
        GeneDefinition definition = definition(stack);
        return definition == null ? super.getName(stack) : Component.translatable(definition.nameKey());
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Data data = read(stack);
        GeneDefinition definition = definition(stack);
        if (data != null) {
            tooltip.add(Component.translatable(data.strength().key()).append(" / ").append(Component.translatable(data.quality().key())));
        }
        if (definition != null) {
            tooltip.add(Component.translatable(definition.region().key()));
            tooltip.add(Component.translatable("gene.yixiangshouronglu.cost", definition.cost()));
            if (!definition.categories().isEmpty()) { tooltip.add(GeneDescriptions.categories(definition)); }
            tooltip.addAll(GeneDescriptions.effects(stack));
        } else { tooltip.add(Component.translatable("gene.yixiangshouronglu.invalid")); }
    }
}
