package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerGenes {
    private final List<ItemStack> pending = new ArrayList<>();

    public CompoundTag save() {
        CompoundTag data = new CompoundTag();
        ListTag items = new ListTag();
        for (ItemStack stack : pending) {
            items.add(stack.save(new CompoundTag()));
        }
        data.put("PendingReturns", items);
        return data;
    }

    public void load(CompoundTag data) {
        pending.clear();
        readItems(data.getList("PendingReturns", Tag.TAG_COMPOUND));
        CompoundTag regions = data.getCompound("Regions");
        for (String region : regions.getAllKeys()) {
            readItems(regions.getList(region, Tag.TAG_COMPOUND));
        }
    }

    private void readItems(ListTag items) {
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = ItemStack.of(items.getCompound(index));
            if (!stack.isEmpty()) {
                pending.add(stack);
            }
        }
    }

    public void returnItems(ServerPlayer player) {
        if (pending.isEmpty()) {
            return;
        }
        var inventory = player.getInventory();
        for (var iterator = pending.iterator(); iterator.hasNext();) {
            ItemStack stack = iterator.next();
            for (int slot = 0; slot < inventory.items.size() && !stack.isEmpty(); slot++) {
                ItemStack existing = inventory.items.get(slot);
                if (existing.isEmpty()) {
                    inventory.setItem(slot, stack.split(stack.getMaxStackSize()));
                } else if (ItemStack.isSameItemSameTags(existing, stack)) {
                    int amount = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                    if (amount > 0) {
                        existing.grow(amount);
                        stack.shrink(amount);
                    }
                }
            }
            if (stack.isEmpty()) {
                iterator.remove();
            }
        }
        inventory.setChanged();
    }
}
