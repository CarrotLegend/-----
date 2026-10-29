package com.carrot123.yixiangshouronglu.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class EnderDragonWingsItem extends Item implements ICurioItem {
    private static final String FLIGHT_OWNER_KEY = "yixiangshouronglu_ender_dragon_wings_flight";

    public EnderDragonWingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return "back".equals(context.identifier());
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!"back".equals(context.identifier())
                || !(context.entity() instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getAbilities().mayfly) {
            player.getPersistentData().putBoolean(FLIGHT_OWNER_KEY, true);
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (!(context.entity() instanceof ServerPlayer player)) {
            return;
        }

        boolean grantedByWings = player.getPersistentData().getBoolean(FLIGHT_OWNER_KEY);
        player.getPersistentData().remove(FLIGHT_OWNER_KEY);

        if (!grantedByWings || player.isCreative() || player.isSpectator()) {
            return;
        }

        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable("tooltip.yixiangshouronglu.ender_dragon_wings")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
        );
    }
}
