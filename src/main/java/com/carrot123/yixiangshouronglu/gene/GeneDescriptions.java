package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.nbt.CompoundTag;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class GeneDescriptions {
    private GeneDescriptions() {}
    public static List<Component> effects(ItemStack module) { return effects(module, new CompoundTag()); }
    public static List<Component> effects(ItemStack module, CompoundTag runtime) {
        GeneDefinition definition = GeneModuleItem.definition(module); var data = GeneModuleItem.read(module);
        if (definition == null || data == null) { return List.of(); }
        List<Component> result = new ArrayList<>();
        if (definition.effect() != null) {
            result.addAll(definition.effect().descriptions(data.quality()));
            var display = definition.effect().display(data.quality(), runtime);
            display.attributes().forEach(line -> result.add(Component.translatable(line.translationKey()).append(": " + line.value())));
            display.activeSkills().forEach(key -> result.add(Component.translatable(key)));
            display.passiveSkills().forEach(key -> result.add(Component.translatable(key)));
            display.conditions().forEach(key -> result.add(Component.translatable(key)));
            if (display.cooldownTicks() > 0) { result.add(Component.translatable("gene.yixiangshouronglu.cooldown", display.cooldownTicks())); }
        }
        if (result.isEmpty()) { result.add(Component.translatable(definition.descriptionKey())); }
        definition.categories().stream().sorted().map(GeneCategories::get).filter(java.util.Objects::nonNull).forEach(category -> result.addAll(category.descriptions()));
        return result.stream().map(line -> (Component) line.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)).toList();
    }
    public static Component categories(GeneDefinition definition) {
        var names = Component.empty();
        for (var id : definition.categories().stream().sorted().toList()) {
            if (!names.getString().isEmpty()) { names.append(Component.translatable("gene.yixiangshouronglu.category.separator")); }
            var category = GeneCategories.get(id); names.append(category == null ? Component.literal(id.toString()) : Component.translatable(category.nameKey()));
        }
        return Component.translatable("gene.yixiangshouronglu.categories", names);
    }
}
