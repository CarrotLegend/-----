package com.carrot123.yixiangshouronglu.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class FinalKnellItem extends Item implements ICurioItem {
    public static final String SLOT = "hands";

    public FinalKnellItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return SLOT.equals(context.identifier()) && !context.cosmetic();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.final_knell").withStyle(ChatFormatting.GOLD));
    }
}
