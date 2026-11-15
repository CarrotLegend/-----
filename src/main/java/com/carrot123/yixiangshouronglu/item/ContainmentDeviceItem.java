package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.menu.ContainmentDeviceMenu;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public final class ContainmentDeviceItem extends Item {
    private static final String RESULT = "ResearchResult";

    public ContainmentDeviceItem() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack stored(ItemStack device) {
        return device.hasTag() && device.getTag().contains(RESULT, Tag.TAG_COMPOUND)
                ? ItemStack.of(device.getTag().getCompound(RESULT)) : ItemStack.EMPTY;
    }

    public static void store(ItemStack device, ItemStack stack) {
        if (stack.isEmpty()) {
            device.removeTagKey(RESULT);
        } else {
            device.getOrCreateTag().put(RESULT, stack.save(new net.minecraft.nbt.CompoundTag()));
        }
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack device = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                || player.pick(5.0D, 1.0F, false).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(device);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = player.getInventory().selected;
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, owner) -> new ContainmentDeviceMenu(id, inventory, slot, device),
                    device.getHoverName()), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(device, level.isClientSide);
    }
}
