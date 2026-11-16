package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class BiologicalGeneModuleItem extends Item {
    private static final String TEXT = "tooltip.yixiangshouronglu.biological_gene_module.";

    public BiologicalGeneModuleItem() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack fromFragment(ItemStack fragment) {
        if (GeneFragmentItem.sourceType(fragment) == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag data = fragment.getTag();
        return create(new ResourceLocation(data.getString("EntityId")), data.getCompound("EntityData"));
    }

    public static ItemStack displayStack(BiologicalGene gene) {
        var source = gene.displaySource();
        var data = new CompoundTag();
        data.putString("id", source.toString());
        data.putUUID("UUID", new UUID(0, 0));
        return create(source, data);
    }

    private static ItemStack create(ResourceLocation source, CompoundTag snapshot) {
        ItemStack stack = new ItemStack(ModItems.BIOLOGICAL_GENE_MODULE.get());
        CompoundTag data = stack.getOrCreateTag();
        data.putString("GeneType", BiologicalGene.fromSource(source).id);
        data.putString("SourceEntityId", source.toString());
        data.put("EntityData", snapshot.copy());
        return stack;
    }

    @Nullable
    public static BiologicalGene gene(ItemStack stack) {
        if (!stack.is(ModItems.BIOLOGICAL_GENE_MODULE.get()) || stack.getCount() != 1 || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        ResourceLocation source = ResourceLocation.tryParse(tag.getString("SourceEntityId"));
        if (source == null) {
            return null;
        }
        BiologicalGene gene = BiologicalGene.fromSource(source);
        if (!gene.id.equals(tag.getString("GeneType"))) {
            return null;
        }
        var fragmentData = new CompoundTag();
        fragmentData.putString("EntityId", source.toString());
        fragmentData.put("EntityData", tag.getCompound("EntityData"));
        return GeneFragmentItem.sourceType(fragmentData) == null ? null : gene;
    }

    @Override
    public Component getName(ItemStack stack) {
        BiologicalGene gene = gene(stack);
        return gene == null ? super.getName(stack) : Component.translatable(getDescriptionId() + "." + gene.id);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BiologicalGene gene = gene(stack);
        if (gene == null) {
            return;
        }
        if (gene.armor > 0) {
            line(tooltip, "armor", gene.armor);
        }
        if (gene.health > 0) {
            line(tooltip, "health", gene.health);
        }
        if (gene.attack > 0) {
            line(tooltip, "attack", gene.attack);
        }
        if (gene == BiologicalGene.SPINAL_VOIDIZATION) {
            line(tooltip, "void", BiologicalGene.VOID_CHANCE_PERCENT, BiologicalGene.VOID_REDUCTION_PERCENT);
        }
        if (gene == BiologicalGene.MOLTEN_SKELETON) {
            line(tooltip, "shock");
            line(tooltip, "shock_damage", BiologicalGene.SHOCK_DAMAGE);
            line(tooltip, "shock_fire", BiologicalGene.SHOCK_FIRE_SECONDS);
            line(tooltip, "shock_cooldown", BiologicalGene.SHOCK_COOLDOWN / 20);
        }
        switch (gene) {
            case SOULFIRE_REACTOR_CORE -> {
                line(tooltip, "all_damage", BiologicalGene.SOULFIRE_DAMAGE_PERCENT);
                line(tooltip, "blazing_brand", BiologicalGene.BRAND_CHANCE_PERCENT, BiologicalGene.BRAND_TICKS / 20);
                line(tooltip, "burning_damage", BiologicalGene.BURNING_DAMAGE_PERCENT);
            }
            case WAR_INSTINCT -> {
                line(tooltip, "ranged_damage", BiologicalGene.RANGED_DAMAGE_PERCENT);
                line(tooltip, "crit_chance", BiologicalGene.CRIT_CHANCE_POINTS);
            }
            case ABYSSAL_CIRCULATION -> {
                line(tooltip, "movement_speed", BiologicalGene.MOVE_SPEED_PERCENT);
                line(tooltip, "swim_speed", BiologicalGene.SWIM_SPEED_PERCENT);
                line(tooltip, "water_healing", BiologicalGene.WATER_HEAL);
            }
            case DESERT_MUSCLE_REINFORCEMENT ->
                    line(tooltip, "stun", BiologicalGene.STUN_CHANCE_PERCENT, BiologicalGene.STUN_TICKS / 20);
            case CURSED_MUSCLE_FIBERS -> {
                line(tooltip, "attack_speed", BiologicalGene.ATTACK_SPEED_PERCENT);
                line(tooltip, "rage");
                line(tooltip, "rage_damage", BiologicalGene.RAGE_DAMAGE_PERCENT);
                line(tooltip, "rage_max", BiologicalGene.MAX_RAGE);
                line(tooltip, "rage_expiration", BiologicalGene.RAGE_TICKS / 20);
            }
            case THUNDER_CORE -> {
                line(tooltip, "cold_damage", BiologicalGene.COLD_DAMAGE);
                line(tooltip, "bolt", BiologicalGene.BOLT_CHANCE_PERCENT);
                line(tooltip, "bolt_damage", BiologicalGene.BOLT_DAMAGE);
            }
            default -> {}
        }
    }

    private static void line(List<Component> tooltip, String effect, Object... args) {
        tooltip.add(Component.translatable(TEXT + effect, args).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
