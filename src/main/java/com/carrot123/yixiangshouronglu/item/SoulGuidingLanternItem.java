package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SoulGuidingLanternItem extends Item {
    public SoulGuidingLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack lantern, ItemStack carried, Slot slot,
                                            ClickAction action, Player player, SlotAccess carriedSlot) {
        if (action != ClickAction.SECONDARY || !carried.is(ModItems.SOUL.get())
                || slot.container != player.getInventory() || slot.getContainerSlot() < 0
                || slot.getContainerSlot() >= Inventory.INVENTORY_SIZE || !slot.mayPickup(player)
                || player.isSpectator() || (player.isCreative() && player.containerMenu.containerId == 0)) {
            return false;
        }

        int damage = lantern.getDamageValue();
        if (!player.level().isClientSide && damage > 0) {
            lantern.setDamageValue(damage - 1);
            carried.shrink(1);
            if (carried.isEmpty()) {
                carriedSlot.set(ItemStack.EMPTY);
            }
            slot.setChanged();
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.soul_guiding_lantern.charges",
                        remaining, stack.getMaxDamage())
                .withStyle(remaining == 0 ? ChatFormatting.RED : ChatFormatting.GOLD));
    }
}
