package com.carrot123.yixiangshouronglu.gene;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class GeneCapacityUpgradesTest {
    private List<GeneCapacityUpgrades.Slot> supplied(GeneTypes.Capacity capacity) {
        var items = GeneCapacityUpgrades.requirements(capacity);
        List<GeneCapacityUpgrades.Slot> slots = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) { slots.add(new GeneCapacityUpgrades.Slot(index * 9, items.get(index), 2)); }
        return slots;
    }

    @Test void allFourStagesPlanExactlyOneOfEachCurrentMaterial() {
        String[][] expected = {
                {"cataclysm:amethyst_crab_shell", "yixiangshouronglu:wendigo_eye", "mutantmonsters:hulk_hammer"},
                {"minecraft:nether_star", "minecraft:dragon_egg", "mutantmore:mutant_wither_skeleton_skull"},
                {"cataclysm:abyssal_egg", "mythsandlegends:tenebrite_shard", "theinkarena:soul_core"},
                {"deeperdarker:warden_carapace", "cataclysm:witherite_ingot", "iceandfire:fire_dragon_heart"}};
        for (int stage = 0; stage < 4; stage++) {
            var capacity = GeneTypes.Capacity.values()[stage];
            assertEquals(List.of(expected[stage]), GeneCapacityUpgrades.requirements(capacity).stream().map(ResourceLocation::toString).toList());
            var plan = GeneCapacityUpgrades.plan(capacity, 4, 4, supplied(capacity), item -> true);
            assertEquals("ok", plan.status());
            assertEquals(List.of(new GeneCapacityUpgrades.Deduction(0, 1), new GeneCapacityUpgrades.Deduction(9, 1), new GeneCapacityUpgrades.Deduction(18, 1)), plan.deductions());
        }
    }

    @Test void shortagesMissingRegistrationsAndFutureStageItemsNeverProducePartialDeductions() {
        var slots = supplied(GeneTypes.Capacity.I);
        slots.remove(2);
        var shortage = GeneCapacityUpgrades.plan(GeneTypes.Capacity.I, 0, 0, slots, item -> true);
        assertEquals("capacity_upgrade_materials", shortage.status());
        assertTrue(shortage.deductions().isEmpty());
        var missing = GeneCapacityUpgrades.plan(GeneTypes.Capacity.I, 0, 0, supplied(GeneTypes.Capacity.I), item -> !item.getPath().equals("hulk_hammer"));
        assertEquals("capacity_upgrade_missing_item", missing.status());
        assertTrue(missing.deductions().isEmpty());
        assertEquals("capacity_upgrade_materials", GeneCapacityUpgrades.plan(GeneTypes.Capacity.I, 0, 0, supplied(GeneTypes.Capacity.II), item -> true).status());
    }

    @Test void onlyMainInventoryAndHotbarContributeAndSplitStacksAreCounted() {
        ResourceLocation id = GeneCapacityUpgrades.requirements(GeneTypes.Capacity.I).get(0);
        var slots = List.of(new GeneCapacityUpgrades.Slot(0, id, 2), new GeneCapacityUpgrades.Slot(35, id, 3),
                new GeneCapacityUpgrades.Slot(36, id, 64), new GeneCapacityUpgrades.Slot(40, id, 64), new GeneCapacityUpgrades.Slot(-1, id, 64));
        assertEquals(5, GeneCapacityUpgrades.count(slots, id));
        var excluded = new ArrayList<>(supplied(GeneTypes.Capacity.I));
        excluded.set(0, new GeneCapacityUpgrades.Slot(36, id, 64));
        assertEquals("capacity_upgrade_materials", GeneCapacityUpgrades.plan(GeneTypes.Capacity.I, 0, 0, excluded, item -> true).status());
    }

    @Test void oldRevisionAndMaximumCapacityAreRejected() {
        assertEquals("capacity_upgrade_stale", GeneCapacityUpgrades.plan(GeneTypes.Capacity.II, 0, 1, supplied(GeneTypes.Capacity.II), item -> true).status());
        var plan = GeneCapacityUpgrades.plan(GeneTypes.Capacity.V, 4, 4, List.of(), item -> true);
        assertEquals("capacity_upgrade_max", plan.status());
        assertTrue(plan.deductions().isEmpty());
    }

    private GeneCapacityUpgrades.Plan commitPlan() {
        return new GeneCapacityUpgrades.Plan("ok", List.of(new GeneCapacityUpgrades.Deduction(0, 1), new GeneCapacityUpgrades.Deduction(9, 1), new GeneCapacityUpgrades.Deduction(35, 1)));
    }
    private record Stack(int count, String metadata) {}
    private Map<Integer, Stack> materials() {
        Map<Integer, Stack> inventory = new HashMap<>();
        inventory.put(0, new Stack(2, "first"));
        inventory.put(9, new Stack(1, "second"));
        inventory.put(35, new Stack(1, "Damage:17,CustomData:keep"));
        return inventory;
    }
    private boolean commit(Map<Integer, Stack> inventory, BooleanSupplier advance) {
        return GeneCapacityUpgrades.apply(commitPlan(), inventory::get,
                deduction -> inventory.compute(deduction.slot(), (slot, stack) -> new Stack(stack.count() - deduction.count(), stack.metadata())),
                inventory::put, advance);
    }

    @Test void successfulCommitConsumesExactlyThreeItemsAndAdvancesOnce() {
        var inventory = materials();
        int[] commits = {0};
        assertTrue(commit(inventory, () -> { commits[0]++; return true; }));
        assertEquals(1, inventory.get(0).count());
        assertEquals(0, inventory.get(9).count());
        assertEquals(0, inventory.get(35).count());
        assertEquals(1, commits[0]);
    }

    @Test void failedOrThrowingCapacityCommitRestoresExactSnapshots() {
        for (boolean throwing : new boolean[]{false, true}) {
            var inventory = materials();
            var original = Map.copyOf(inventory);
            if (throwing) {
                assertThrows(IllegalStateException.class, () -> commit(inventory, () -> { throw new IllegalStateException("test"); }));
            } else { assertFalse(commit(inventory, () -> false)); }
            assertEquals(original, inventory);
        }
    }

    @Test void materialShortageCannotCallTheCommitOrTouchInventory() {
        var inventory = materials();
        var original = Map.copyOf(inventory);
        var failed = new GeneCapacityUpgrades.Plan("capacity_upgrade_materials", List.of());
        assertFalse(GeneCapacityUpgrades.apply(failed, inventory::get, deduction -> fail("must not consume"), inventory::put, () -> { fail("must not upgrade"); return true; }));
        assertEquals(original, inventory);
    }

    @Test void previewWireRoundTripRetainsServerAvailabilityAndTier() {
        var preview = new GeneCapacityUpgrades.Preview(GeneTypes.Capacity.I, GeneCapacityUpgrades.requirements(GeneTypes.Capacity.I).stream()
                .map(item -> new GeneCapacityUpgrades.Material(item, 1, !item.getPath().equals("hulk_hammer"))).toList());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            preview.encode(buffer);
            assertEquals(preview, GeneCapacityUpgrades.Preview.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
