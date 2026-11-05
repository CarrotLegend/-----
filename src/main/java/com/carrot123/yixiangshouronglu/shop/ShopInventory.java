package com.carrot123.yixiangshouronglu.shop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShopInventory {
    public record Space(int slot, int free) {}
    public record Placement(int slot, int count) {}
    private ShopInventory() {}
    public static List<ItemStack> snapshot(Inventory inventory) {
        return inventory.items.stream().map(ItemStack::copy).toList();
    }
    public static void restore(Inventory inventory, List<ItemStack> stacks) {
        for (int slot = 0; slot < 36; slot++) { inventory.setItem(slot, stacks.get(slot).copy()); }
    }
    public record SalePlan(List<ItemStack> stacks, int count) {}
    public static int count(List<ItemStack> stacks, ResourceLocation item) {
        int count = 0;
        for (int slot = 0; slot < Math.min(36, stacks.size()); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty() && item.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) { count = Math.addExact(count, stack.getCount()); }
        }
        return count;
    }
    public static Optional<SalePlan> remove(List<ItemStack> stacks, ResourceLocation item, boolean all) {
        if (stacks.size() != 36 || ShopCatalog.sellPrice(item) <= 0) { return Optional.empty(); }
        int owned = count(stacks, item);
        if (owned <= 0) { return Optional.empty(); }
        int quantity = all ? owned : 1;
        int remaining = quantity;
        List<ItemStack> result = new ArrayList<>(stacks.stream().map(ItemStack::copy).toList());
        for (ItemStack stack : result) {
            if (!stack.isEmpty() && item.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken); remaining -= taken;
                if (remaining == 0) { break; }
            }
        }
        return Optional.of(new SalePlan(List.copyOf(result), quantity));
    }
    public static boolean matches(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) { return false; }
        for (int slot = 0; slot < first.size(); slot++) {
            if (!ItemStack.matches(first.get(slot), second.get(slot))) { return false; }
        }
        return true;
    }
    public static Optional<List<ItemStack>> insert(List<ItemStack> inventory, ItemStack product) {
        if (product.isEmpty() || inventory.size() != 36) { return Optional.empty(); }
        List<Space> spaces = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, product)) {
                spaces.add(new Space(slot, Math.max(0, Math.min(64, stack.getMaxStackSize()) - stack.getCount())));
            }
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.get(slot).isEmpty()) { spaces.add(new Space(slot, Math.min(64, product.getMaxStackSize()))); }
        }
        var plan = allocation(product.getCount(), spaces);
        if (plan.isEmpty()) { return Optional.empty(); }
        List<ItemStack> result = new ArrayList<>(inventory.stream().map(ItemStack::copy).toList());
        for (Placement placement : plan.get()) {
            if (result.get(placement.slot()).isEmpty()) { result.set(placement.slot(), product.copyWithCount(placement.count())); }
            else { result.get(placement.slot()).grow(placement.count()); }
        }
        return Optional.of(result);
    }
    static Optional<List<Placement>> allocation(int count, List<Space> spaces) {
        if (count <= 0) { return Optional.empty(); }
        List<Placement> placements = new ArrayList<>();
        for (Space space : spaces) {
            if (space.slot() >= 0 && space.slot() < 36 && space.free() > 0) {
                int moved = Math.min(count, space.free());
                placements.add(new Placement(space.slot(), moved));
                count -= moved;
                if (count == 0) { return Optional.of(List.copyOf(placements)); }
            }
        }
        return Optional.empty();
    }
}
