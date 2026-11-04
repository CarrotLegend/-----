package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public final class GeneCapacityUpgrades {
    public record Material(ResourceLocation item, int count, boolean available) {}
    public record Preview(GeneTypes.Capacity capacity, List<Material> materials) {
        public Preview { materials = List.copyOf(materials); }
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeEnum(capacity);
            buffer.writeVarInt(materials.size());
            for (Material material : materials) {
                buffer.writeResourceLocation(material.item());
                buffer.writeVarInt(material.count());
                buffer.writeBoolean(material.available());
            }
        }
        public static Preview decode(FriendlyByteBuf buffer) {
            GeneTypes.Capacity capacity = buffer.readEnum(GeneTypes.Capacity.class);
            int size = buffer.readVarInt();
            if (size != (capacity == GeneTypes.Capacity.V ? 0 : 3)) { throw new IllegalArgumentException("Invalid capacity upgrade materials"); }
            List<Material> materials = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                ResourceLocation item = buffer.readResourceLocation();
                int count = buffer.readVarInt();
                if (count != 1) { throw new IllegalArgumentException("Invalid capacity upgrade count"); }
                materials.add(new Material(item, count, buffer.readBoolean()));
            }
            return new Preview(capacity, materials);
        }
    }
    public record Slot(int index, ResourceLocation item, int count) {}
    public record Deduction(int slot, int count) {}
    public record Plan(String status, List<Deduction> deductions) {
        public Plan { deductions = List.copyOf(deductions); }
    }

    private static final List<List<ResourceLocation>> REQUIREMENTS = List.of(
            ids("cataclysm:amethyst_crab_shell", "yixiangshouronglu:wendigo_eye", "mutantmonsters:hulk_hammer"),
            ids("minecraft:nether_star", "minecraft:dragon_egg", "mutantmore:mutant_wither_skeleton_skull"),
            ids("cataclysm:abyssal_egg", "mythsandlegends:tenebrite_shard", "theinkarena:soul_core"),
            ids("deeperdarker:warden_carapace", "cataclysm:witherite_ingot", "iceandfire:fire_dragon_heart"));

    private GeneCapacityUpgrades() {}
    private static List<ResourceLocation> ids(String... values) { return java.util.Arrays.stream(values).map(ResourceLocation::new).toList(); }
    public static List<ResourceLocation> requirements(GeneTypes.Capacity capacity) {
        return capacity == GeneTypes.Capacity.V ? List.of() : REQUIREMENTS.get(capacity.ordinal());
    }
    private static boolean registered(ResourceLocation item) {
        return ForgeRegistries.ITEMS.containsKey(item) && ForgeRegistries.ITEMS.getValue(item) != Items.AIR;
    }
    public static Preview preview(GeneTypes.Capacity capacity) {
        return new Preview(capacity, requirements(capacity).stream().map(item -> new Material(item, 1, registered(item))).toList());
    }
    public static List<Slot> slots(Inventory inventory) {
        List<Slot> slots = new ArrayList<>();
        for (int slot = 0; slot < Math.min(36, inventory.items.size()); slot++) {
            var stack = inventory.items.get(slot);
            if (!stack.isEmpty()) { slots.add(new Slot(slot, ForgeRegistries.ITEMS.getKey(stack.getItem()), stack.getCount())); }
        }
        return slots;
    }
    public static int count(List<Slot> slots, ResourceLocation item) {
        return slots.stream().filter(slot -> slot.index() >= 0 && slot.index() < 36 && item.equals(slot.item()))
                .mapToInt(slot -> Math.max(0, slot.count())).sum();
    }
    public static Plan plan(GeneTypes.Capacity capacity, long revision, long currentRevision, List<Slot> slots) {
        return plan(capacity, revision, currentRevision, slots, GeneCapacityUpgrades::registered);
    }
    static Plan plan(GeneTypes.Capacity capacity, long revision, long currentRevision, List<Slot> slots, Predicate<ResourceLocation> registered) {
        if (revision != currentRevision) { return new Plan("capacity_upgrade_stale", List.of()); }
        if (capacity == GeneTypes.Capacity.V) { return new Plan("capacity_upgrade_max", List.of()); }
        List<ResourceLocation> requirements = requirements(capacity);
        if (requirements.stream().anyMatch(item -> !registered.test(item))) { return new Plan("capacity_upgrade_missing_item", List.of()); }
        if (requirements.stream().anyMatch(item -> count(slots, item) < 1)) { return new Plan("capacity_upgrade_materials", List.of()); }
        Map<Integer, Integer> remaining = new HashMap<>();
        List<Deduction> deductions = new ArrayList<>();
        for (ResourceLocation item : requirements) {
            for (Slot slot : slots) {
                if (slot.index() < 0 || slot.index() >= 36 || !item.equals(slot.item())) { continue; }
                int count = remaining.getOrDefault(slot.index(), slot.count());
                if (count > 0) {
                    deductions.add(new Deduction(slot.index(), 1));
                    remaining.put(slot.index(), count - 1);
                    break;
                }
            }
        }
        if (deductions.size() != requirements.size()) { return new Plan("capacity_upgrade_materials", List.of()); }
        return new Plan("ok", deductions);
    }
    static boolean apply(Plan plan, Inventory inventory, BooleanSupplier upgrade) {
        if (!plan.status().equals("ok") || plan.deductions().size() != 3) { return false; }
        for (Deduction deduction : plan.deductions()) {
            if (deduction.slot() < 0 || deduction.slot() >= 36 || inventory.getItem(deduction.slot()).getCount() < deduction.count()) { return false; }
        }
        return apply(plan, slot -> inventory.getItem(slot).copy(), deduction -> inventory.removeItem(deduction.slot(), deduction.count()), inventory::setItem, upgrade);
    }
    static <T> boolean apply(Plan plan, IntFunction<T> snapshot, Consumer<Deduction> consume, BiConsumer<Integer, T> restore, BooleanSupplier upgrade) {
        if (!plan.status().equals("ok") || plan.deductions().size() != 3) { return false; }
        Map<Integer, T> originals = new LinkedHashMap<>();
        for (Deduction deduction : plan.deductions()) { originals.computeIfAbsent(deduction.slot(), snapshot::apply); }
        boolean upgraded = false;
        try {
            plan.deductions().forEach(consume);
            upgraded = upgrade.getAsBoolean();
            return upgraded;
        } finally {
            if (!upgraded) { originals.forEach(restore); }
        }
    }
}
