package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class GeneService {
    private GeneService() {}
    public static GeneTypes.Region region(int index) {
        for (var region : GeneTypes.Region.values()) { if (index < region.slots) { return index < 0 ? null : region; } index -= region.slots; }
        return null;
    }
    public static int localSlot(int index) {
        for (var region : GeneTypes.Region.values()) { if (index < region.slots) { return index; } index -= region.slots; }
        return -1;
    }
    public static String check(PlayerGenes genes, ItemStack module, int bodySlot, boolean replace) {
        var region = region(bodySlot); int slot = localSlot(bodySlot);
        if (region == null || slot < 0) { return "invalid"; }
        ItemStack old = genes.get(region, slot);
        if (!replace && !old.isEmpty()) { return "occupied"; }
        if (replace && old.isEmpty()) { return "empty"; }
        if (genes.unknown()) { return "unknown_capacity"; }
        var definition = GeneModuleItem.definition(module);
        if (module.getCount() != 1 || definition == null) { return "invalid"; }
        var candidate = new GeneRules.Entry(definition.id(), definition.region(), definition.cost(), definition.conflicts(), true);
        return GeneRules.validate(candidate, region, genes.entries(replace ? region : null, replace ? slot : -1), genes.limit());
    }
    public static String move(ServerPlayer player, int action, int inventorySlot, int bodySlot, long revision, ItemStack selected) {
        PlayerGenes genes = GeneCapability.get(player);
        if (revision != genes.revision()) { return "stale"; }
        var region = region(bodySlot); int slot = localSlot(bodySlot);
        if (action == 4) {
            if (!genes.toggleExtreme()) { return "extreme_locked"; }
            refresh(player); return "ok";
        }
        if (region == null || slot < 0) { return "invalid"; }
        ItemStack old = genes.get(region, slot);
        if (action == 2) {
            if (old.isEmpty()) { return "empty"; }
            int free = player.getInventory().getFreeSlot();
            if (free < 0 || old.getCount() > old.getMaxStackSize()) { return "inventory_full"; }
            genes.set(region, slot, ItemStack.EMPTY); player.getInventory().setItem(free, old);
            removed(player, old); refresh(player); return "ok";
        }
        if (action != 1 && action != 3) { return "invalid"; }
        if (inventorySlot < 0 || inventorySlot >= 36) { return "invalid"; }
        ItemStack input = player.getInventory().getItem(inventorySlot);
        if (!ItemStack.matches(input, selected) || input.isEmpty()) { return "stale"; }
        String result = check(genes, input, bodySlot, action == 3);
        if (!result.equals("ok")) { return result; }
        ItemStack installed = input.copyWithCount(1);
        genes.set(region, slot, installed);
        player.getInventory().setItem(inventorySlot, action == 3 ? old : ItemStack.EMPTY);
        if (!old.isEmpty()) { removed(player, old); }
        var definition = GeneModuleItem.definition(installed);
        if (definition.effect() != null) { definition.effect().installed(player, installed.copy()); }
        refresh(player); return "ok";
    }
    private static void removed(ServerPlayer player, ItemStack module) {
        var definition = GeneModuleItem.definition(module);
        if (definition != null && definition.effect() != null) { definition.effect().removed(player, module.copy()); }
    }
    public static void refresh(ServerPlayer player) {
        visit(player, false); GeneEffects.refresh(player); player.getInventory().setChanged(); player.containerMenu.broadcastChanges(); GeneNetwork.sync(player, "ok");
    }
    public static void restore(ServerPlayer player) { visit(player, true); GeneEffects.refresh(player); }
    private static void visit(ServerPlayer player, boolean restore) {
        PlayerGenes genes = GeneCapability.get(player);
        for (var region : GeneTypes.Region.values()) {
            for (int slot = 0; slot < region.slots; slot++) {
                ItemStack stack = genes.get(region, slot); var definition = GeneModuleItem.definition(stack);
                if (definition != null && definition.region() == region && definition.effect() != null) {
                    if (restore) { definition.effect().restored(player, stack.copy()); } else { definition.effect().refreshed(player, stack.copy()); }
                }
            }
        }
    }
    public static boolean upgradeCapacity(ServerPlayer player) {
        if (!GeneCapability.get(player).upgradeCapacity()) { return false; }
        refresh(player); return true;
    }
    public static void unlockExtreme(ServerPlayer player) { GeneCapability.get(player).unlockExtreme(); refresh(player); }
}
