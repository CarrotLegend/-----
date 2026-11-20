package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.gene.ChimeraGrowth;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BloodthirstyChimeraItem extends GrowingChimeraItem {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String text = "tooltip.yixiangshouronglu.bloodthirsty_chimera.";
        tooltip.add(Component.translatable(text + "growth").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "limit").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "kills", ChimeraGrowth.kills(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(text + "attack",
                ChimeraGrowth.format(ChimeraGrowth.bloodDamage(ChimeraGrowth.kills(stack)), 100)).withStyle(ChatFormatting.GOLD));
    }
}
