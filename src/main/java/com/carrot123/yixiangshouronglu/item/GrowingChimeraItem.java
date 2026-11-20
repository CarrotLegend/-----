package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.gene.ChimeraGrowth;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class GrowingChimeraItem extends ChimeraCurioItem {
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        if (!isChimeraSlot(context) || context.entity() != null && !context.entity().level().isClientSide) {
            return ImmutableMultimap.of();
        }
        return ChimeraGrowth.displayAttributes(stack);
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips, ItemStack stack) {
        if (stack.hasTag() && (stack.getTag().getInt("HideFlags") & 2) != 0) {
            return List.of();
        }
        List<Component> result = new ArrayList<>();
        result.add(Component.empty());
        result.add(Component.translatable("curios.modifiers.chimera_modification").withStyle(ChatFormatting.GOLD));
        for (var entry : ChimeraGrowth.displayAttributes(stack).entries()) {
            double value = entry.getValue().getAmount();
            String amount = entry.getValue().getId().equals(ChimeraGrowth.BLOOD_ATTACK) ? ChimeraGrowth.format(value, 100)
                    : entry.getValue().getId().equals(ChimeraGrowth.GLUTTONY_HEALTH) ? ChimeraGrowth.format(value, 151)
                    : String.format(Locale.ROOT, "%.2f", value);
            result.add(Component.translatable("attribute.modifier.plus.0", amount,
                    Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.BLUE));
        }
        return result;
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        refresh(context);
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack replacement, ItemStack stack) {
        refresh(context);
    }

    private static void refresh(SlotContext context) {
        if (isChimeraSlot(context) && context.entity() instanceof ServerPlayer player) {
            ChimeraGrowth.refresh(player);
        }
    }
}
