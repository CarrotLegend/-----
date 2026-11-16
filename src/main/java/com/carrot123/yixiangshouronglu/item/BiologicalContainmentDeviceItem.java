package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.menu.BiologicalContainmentDeviceMenu;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
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

public final class BiologicalContainmentDeviceItem extends Item {
    private static final String RESULT_KEY = "StoredResearchResult";

    public BiologicalContainmentDeviceItem() {
        super(new Properties().stacksTo(1));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack device = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || player.isSpectator()
                || player.pick(player.getBlockReach(), 1.0F, false).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(device);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = player.getInventory().selected;
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, owner) -> new BiologicalContainmentDeviceMenu(id, inventory, slot, device),
                    device.getHoverName()), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(device, level.isClientSide);
    }

    public static ItemStack storedResult(ItemStack device) {
        CompoundTag tag = device.getTag();
        if (!device.is(ModItems.BIOLOGICAL_CONTAINMENT_DEVICE.get())
                || tag == null || !tag.contains(RESULT_KEY, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.of(tag.getCompound(RESULT_KEY));
        return ResearchResultItem.sourceType(result) == null ? ItemStack.EMPTY : result;
    }

    public static void storeResult(ItemStack device, ItemStack result) {
        if (result.isEmpty()) {
            device.removeTagKey(RESULT_KEY);
        } else {
            device.addTagElement(RESULT_KEY, result.save(new CompoundTag()));
        }
    }
}
