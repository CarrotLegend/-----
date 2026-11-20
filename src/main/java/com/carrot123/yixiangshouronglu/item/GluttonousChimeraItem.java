package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.gene.ChimeraGrowth;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class GluttonousChimeraItem extends GrowingChimeraItem {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String text = "tooltip.yixiangshouronglu.gluttonous_chimera.";
        int foods = ChimeraGrowth.foods(stack).size();
        tooltip.add(Component.translatable(text + "growth").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "limit").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "conversion").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "foods", foods).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "health", ChimeraGrowth.format(ChimeraGrowth.gluttonyHealth(foods), 151))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "attack", String.format(Locale.ROOT, "%.2f", ChimeraGrowth.displayedAttack(stack)))
                .withStyle(ChatFormatting.GOLD));
    }
}
