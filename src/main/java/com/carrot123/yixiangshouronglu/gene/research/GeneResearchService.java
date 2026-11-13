package com.carrot123.yixiangshouronglu.gene.research;

import com.carrot123.yixiangshouronglu.block.entity.GeneResearchTableBlockEntity;
import com.carrot123.yixiangshouronglu.network.GeneResearchNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GeneResearchService {
    public record Outcome(String key, int gain) {
        public Component message() {
            return Component.translatable("gene_research.yixiangshouronglu." + key, gain);
        }
    }

    private GeneResearchService() {}

    public static Outcome research(ServerPlayer player, GeneResearchTableBlockEntity table,
                                   GeneResearchDefinition definition, String expectedExtract) {
        PlayerGeneResearch data = GeneResearchCapability.get(player);
        if (data == null || definition == null) {
            return new Outcome("no_selection", 0);
        }
        if (!definition.available()) {
            return new Outcome("missing_material", 0);
        }
        int progress = data.progress(definition.id());
        if (progress >= 100) {
            return new Outcome("research_complete", 0);
        }
        ItemStack input = table.items().getItem(0);
        String extract = GeneResearchDefinition.extractId(input);
        if (extract.isEmpty()) {
            return new Outcome("invalid_extract", 0);
        }
        if (!extract.equals(expectedExtract)) {
            return new Outcome("stale", 0);
        }
        if (data.used(definition.id()).contains(extract)) {
            return new Outcome("extract_used", 0);
        }
        int roll = definition.minProgress() + player.getRandom().nextInt(definition.maxProgress() - definition.minProgress() + 1);
        int gain = Math.min(100 - progress, roll);
        table.items().removeItem(0, 1);
        data.research(definition.id(), extract, gain);
        return new Outcome("research_success", gain);
    }

    public static Outcome synthesize(ServerPlayer player, GeneResearchTableBlockEntity table, GeneResearchDefinition definition) {
        PlayerGeneResearch data = GeneResearchCapability.get(player);
        if (data == null || definition == null) {
            return new Outcome("no_selection", 0);
        }
        if (!definition.available()) {
            return new Outcome("missing_material", 0);
        }
        if (data.progress(definition.id()) < 100) {
            return new Outcome("locked", 0);
        }
        if (!table.items().getItem(1).isEmpty()) {
            return new Outcome("output_occupied", 0);
        }
        ItemStack result = definition.result();
        Inventory inventory = player.getInventory();
        int[] deductions = plan(inventory, definition);
        if (result.isEmpty() || deductions == null) {
            return new Outcome("insufficient_materials", 0);
        }
        for (int slot = 0; slot < deductions.length; slot++) {
            if (deductions[slot] > 0) {
                inventory.removeItem(slot, deductions[slot]);
            }
        }
        inventory.setChanged();
        table.items().setItem(1, result);
        return new Outcome("synthesis_success", 0);
    }

    private static int[] plan(Inventory inventory, GeneResearchDefinition definition) {
        int[] deductions = new int[inventory.items.size()];
        List<GeneResearchDefinition.Material> materials = new ArrayList<>(definition.materials());
        materials.sort(Comparator.comparing(material -> material.exactTag() == null));
        for (GeneResearchDefinition.Material material : materials) {
            int remaining = material.count();
            for (int slot = 0; slot < deductions.length && remaining > 0; slot++) {
                ItemStack stack = inventory.items.get(slot);
                if (material.matches(stack)) {
                    int take = Math.min(remaining, stack.getCount() - deductions[slot]);
                    deductions[slot] += take;
                    remaining -= take;
                }
            }
            if (remaining > 0) {
                return null;
            }
        }
        return deductions;
    }

    public static int count(Inventory inventory, GeneResearchDefinition.Material material) {
        return inventory.items.stream().filter(material::matches).mapToInt(ItemStack::getCount).sum();
    }

    public static boolean canSynthesize(Inventory inventory, GeneResearchDefinition definition) {
        return definition.available() && plan(inventory, definition) != null;
    }

    public static boolean use(ServerPlayer player, GeneResearchDefinition definition, ItemStack held) {
        PlayerGeneResearch data = GeneResearchCapability.get(player);
        AttributeInstance attribute = attribute(player, definition);
        if (data == null || attribute == null) {
            player.displayClientMessage(Component.translatable("message.yixiangshouronglu.gene_modifier.missing_attribute"), true);
            return false;
        }
        if (data.uses(definition) >= definition.limit()) {
            player.displayClientMessage(Component.translatable("message.yixiangshouronglu.gene_modifier.max_load"), true);
            return false;
        }
        if (!player.isAlive() || player.isSpectator() || held.isEmpty()
                || !definition.id().equals(ForgeRegistries.ITEMS.getKey(held.getItem()))) {
            return false;
        }
        float oldMax = player.getMaxHealth();
        data.setUses(definition, data.uses(definition) + 1);
        reconcile(attribute, definition, data.uses(definition));
        if (definition.equals(GeneResearchRegistry.HEALTH)) {
            float gained = Math.min(5, Math.max(0, player.getMaxHealth() - oldMax));
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + gained));
        }
        held.shrink(1);
        player.getInventory().setChanged();
        player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1, 1);
        player.displayClientMessage(Component.translatable("message.yixiangshouronglu.gene_modifier.success"), true);
        GeneResearchNetwork.syncPlayer(player);
        return true;
    }

    public static void restore(ServerPlayer player) {
        PlayerGeneResearch data = GeneResearchCapability.get(player);
        if (data == null) {
            return;
        }
        for (GeneResearchDefinition definition : GeneResearchRegistry.values()) {
            AttributeInstance attribute = attribute(player, definition);
            if (attribute != null) {
                reconcile(attribute, definition, data.uses(definition));
            }
        }
    }

    private static AttributeInstance attribute(ServerPlayer player, GeneResearchDefinition definition) {
        return ForgeRegistries.ATTRIBUTES.containsKey(definition.attribute())
                ? player.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(definition.attribute())) : null;
    }

    private static void reconcile(AttributeInstance attribute, GeneResearchDefinition definition, int uses) {
        var id = definition.modifierId();
        double amount = uses * definition.amount();
        AttributeModifier current = attribute.getModifier(id);
        if (current != null && current.getOperation() == AttributeModifier.Operation.ADDITION && current.getAmount() == amount && uses > 0) {
            return;
        }
        attribute.removeModifier(id);
        if (uses > 0) {
            attribute.addTransientModifier(new AttributeModifier(id, "ACC Gene Modifier", amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
