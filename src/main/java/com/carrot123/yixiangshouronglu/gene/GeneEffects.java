package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneEffects {
    private GeneEffects() {}
    public static Map<ResourceLocation, ItemStack> installed(ServerPlayer player) {
        return installed(GeneCapability.get(player));
    }
    public static Map<ResourceLocation, ItemStack> installed(PlayerGenes genes) {
        Map<ResourceLocation, ItemStack> result = new LinkedHashMap<>();
        for (var region : GeneTypes.Region.values()) { for (int slot = 0; slot < region.slots; slot++) {
            ItemStack module = genes.get(region, slot); var definition = GeneModuleItem.definition(module);
            if (definition != null && definition.region() == region && module.getCount() == 1) { result.putIfAbsent(definition.id(), module); }
        } }
        return result;
    }
    public static Set<ResourceLocation> categories(Map<ResourceLocation, ItemStack> installed) {
        return GeneCategories.active(GeneCategories.counts(installed.keySet().stream().map(GeneDefinitions::get).toList()));
    }
    public static void refresh(ServerPlayer player) {
        Map<ResourceLocation, ItemStack> installed = installed(player); Set<ResourceLocation> active = categories(installed);
        for (var category : GeneCategories.values()) { for (var bonus : category.bonuses()) {
            reconcile(player, bonus.attribute(), category.modifierId(bonus.attribute()), active.contains(category.id()) ? bonus.amount() : 0);
        } }
        ItemStack cells = installed.get(RegenerativeCells.ID);
        var definition = GeneDefinitions.get(RegenerativeCells.ID);
        reconcile(player, RegenerativeCells.OVERHEAL, definition.effectId("overheal"), cells == null ? 0 : RegenerativeCells.overheal(GeneModuleItem.read(cells).quality()));
        if (cells == null) { GeneCapability.get(player).effectRuntime().remove("RegenerativeCells"); }
        AdditionalGenes.refresh(player, installed);
        ExtendedGenes.refresh(player, installed);
        ElementalGenes.refresh(player, installed);
        DragonGenes.refresh(player, installed);
        if (player.getHealth() > player.getMaxHealth()) { player.setHealth(player.getMaxHealth()); }
        GeneRegeneration.maintain(player, providesRegeneration(active));
    }
    private static boolean providesRegeneration(Set<ResourceLocation> active) { return active.stream().map(GeneCategories::get).filter(Objects::nonNull).anyMatch(GeneCategories.Category::regeneration); }
    private static void reconcile(ServerPlayer player, ResourceLocation attributeId, UUID id, double amount) {
        reconcile(player, attributeId, id, amount, AttributeModifier.Operation.ADDITION);
    }
    static void reconcile(ServerPlayer player, ResourceLocation attributeId, UUID id, double amount, AttributeModifier.Operation operation) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null || !ForgeRegistries.ATTRIBUTES.containsKey(attributeId)) { return; }
        var instance = player.getAttribute(attribute); if (instance == null) { return; }
        var existing = instance.getModifier(id);
        if (existing != null && amount != 0 && existing.getOperation() == operation && Double.compare(existing.getAmount(), amount) == 0) { return; }
        if (existing != null) { instance.removeModifier(id); }
        if (amount != 0) { instance.addTransientModifier(new AttributeModifier(id, "ACC Gene " + id, amount, operation)); }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive()) { return; }
        var installed = installed(player); ItemStack cells = installed.get(RegenerativeCells.ID);
        CompoundTag runtime = GeneCapability.get(player).effectRuntime();
        if (cells == null) { runtime.remove("RegenerativeCells"); }
        else {
            var quality = GeneModuleItem.read(cells).quality(); CompoundTag state = runtime.getCompound("RegenerativeCells");
            int elapsed = state.getString("Quality").equals(quality.name()) ? Math.max(0, state.getInt("Elapsed")) : 0;
            elapsed++;
            if (elapsed >= RegenerativeCells.interval(quality)) { elapsed = 0; if (player.getHealth() < player.getMaxHealth()) { player.heal(1.0F); } }
            state.putString("Quality", quality.name()); state.putInt("Elapsed", elapsed); runtime.put("RegenerativeCells", state);
        }
        GeneRegeneration.maintain(player, providesRegeneration(categories(installed)));
    }
}
