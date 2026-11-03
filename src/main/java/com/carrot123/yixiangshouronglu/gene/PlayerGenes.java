package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.world.effect.MobEffectInstance;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

public final class PlayerGenes {
    private final EnumMap<GeneTypes.Region, NonNullList<ItemStack>> regions = new EnumMap<>(GeneTypes.Region.class);
    private GeneTypes.Capacity capacity = GeneTypes.Capacity.I;
    private boolean unlocked;
    private boolean extreme;
    private long revision;
    private CompoundTag runtime = new CompoundTag();
    MobEffectInstance regenerationInstance;
    public PlayerGenes() {
        for (var region : GeneTypes.Region.values()) { regions.put(region, NonNullList.withSize(region.slots, ItemStack.EMPTY)); }
    }
    public ItemStack get(GeneTypes.Region region, int slot) { return regions.get(region).get(slot).copy(); }
    void set(GeneTypes.Region region, int slot, ItemStack stack) { regions.get(region).set(slot, stack.copy()); revision++; }
    public GeneTypes.Capacity capacity() { return capacity; }
    public long revision() { return revision; }
    public boolean unlocked() { return unlocked; }
    public boolean extreme() { return extreme; }
    public int limit() { return capacity.limit + (extreme && unlocked && capacity == GeneTypes.Capacity.V ? 4 : 0); }
    public boolean upgradeCapacity() {
        if (capacity == GeneTypes.Capacity.V) { return false; }
        capacity = GeneTypes.Capacity.values()[capacity.ordinal() + 1]; revision++; return true;
    }
    public void unlockExtreme() { if (!unlocked) { unlocked = true; revision++; } }
    public boolean toggleExtreme() {
        if (!unlocked || capacity != GeneTypes.Capacity.V || (extreme && (unknown() || used() > capacity.limit))) { return false; }
        extreme = !extreme; revision++; return true;
    }
    public List<GeneRules.Entry> entries(GeneTypes.Region skippedRegion, int skippedSlot) {
        List<GeneRules.Entry> result = new ArrayList<>();
        for (var region : GeneTypes.Region.values()) {
            for (int slot = 0; slot < region.slots; slot++) {
                if (region == skippedRegion && slot == skippedSlot) { continue; }
                ItemStack stack = regions.get(region).get(slot);
                if (stack.isEmpty()) { continue; }
                var definition = GeneModuleItem.definition(stack);
                var data = GeneModuleItem.read(stack);
                result.add(definition == null || definition.region() != region
                        ? new GeneRules.Entry(data == null ? null : data.id(), region, 0, Set.of(), false)
                        : new GeneRules.Entry(definition.id(), region, definition.cost(), definition.conflicts(), true));
            }
        }
        return result;
    }
    public boolean unknown() { return entries(null, -1).stream().anyMatch(entry -> !entry.known()); }
    public long used() { return entries(null, -1).stream().mapToLong(GeneRules.Entry::cost).sum(); }
    public GeneTypes.Rejection rejection() { return unknown() ? GeneTypes.Rejection.UNKNOWN : GeneTypes.rejection(used(), capacity.limit); }
    public CompoundTag runtime() { return runtime.copy(); }
    CompoundTag effectRuntime() { return runtime; }
    public void setRuntime(CompoundTag value) { runtime = value.copy(); revision++; }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("Version", 1); tag.putString("Capacity", capacity.name());
        tag.putBoolean("ExtremeUnlocked", unlocked); tag.putBoolean("ExtremeEnabled", extreme); tag.putLong("Revision", revision); tag.put("Runtime", runtime.copy());
        CompoundTag containers = new CompoundTag();
        for (var region : GeneTypes.Region.values()) {
            ListTag slots = new ListTag();
            for (int slot = 0; slot < region.slots; slot++) {
                ItemStack stack = regions.get(region).get(slot);
                if (!stack.isEmpty()) { CompoundTag item = stack.save(new CompoundTag()); item.putInt("Slot", slot); slots.add(item); }
            }
            containers.put(region.name(), slots);
        }
        tag.put("Regions", containers); return tag;
    }
    public void load(CompoundTag tag) {
        regenerationInstance = null;
        try { capacity = GeneTypes.Capacity.valueOf(tag.getString("Capacity")); } catch (IllegalArgumentException exception) { capacity = GeneTypes.Capacity.I; }
        unlocked = tag.getBoolean("ExtremeUnlocked"); extreme = tag.getBoolean("ExtremeEnabled") && unlocked && capacity == GeneTypes.Capacity.V;
        revision = Math.max(0, tag.getLong("Revision")); runtime = tag.getCompound("Runtime").copy();
        for (var region : GeneTypes.Region.values()) {
            var slots = regions.get(region); java.util.Collections.fill(slots, ItemStack.EMPTY);
            ListTag items = tag.getCompound("Regions").getList(region.name(), 10);
            for (int i = 0; i < items.size(); i++) { CompoundTag item = items.getCompound(i); int slot = item.getInt("Slot"); if (slot >= 0 && slot < region.slots) { slots.set(slot, ItemStack.of(item)); } }
        }
    }
}
