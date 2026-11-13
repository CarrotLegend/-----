package com.carrot123.yixiangshouronglu.gene.research;

import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public record GeneResearchDefinition(ResourceLocation id, int minProgress, int maxProgress,
                                     ResourceLocation attribute, double amount, int limit, List<Material> materials) {
    public GeneResearchDefinition {
        if (minProgress < 1 || maxProgress < minProgress || maxProgress > 100 || limit < 1 || amount <= 0) {
            throw new IllegalArgumentException("Invalid gene research definition: " + id);
        }
        materials = List.copyOf(materials);
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Missing materials: " + id);
        }
    }

    public String nameKey() {
        return "item." + id.getNamespace() + "." + id.getPath();
    }

    public String effectKey() {
        return "tooltip." + id.getNamespace() + "." + id.getPath() + ".effect";
    }

    public UUID modifierId() {
        return UUID.nameUUIDFromBytes(("yixiangshouronglu:gene_modifier:" + id).getBytes(StandardCharsets.UTF_8));
    }

    public boolean available() {
        return ForgeRegistries.ITEMS.containsKey(id) && materials.stream().allMatch(Material::available);
    }

    public ItemStack result() {
        return ForgeRegistries.ITEMS.containsKey(id) ? new ItemStack(ForgeRegistries.ITEMS.getValue(id)) : ItemStack.EMPTY;
    }

    public static String extractId(ItemStack stack) {
        String id = ContainmentExtractItem.getEntityId(stack);
        ResourceLocation key = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        return key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key) ? key.toString() : "";
    }

    public record Material(ResourceLocation item, int count, boolean extract, CompoundTag exactTag) {
        public Material {
            if (count < 1) {
                throw new IllegalArgumentException("Invalid material count");
            }
            exactTag = exactTag == null ? null : exactTag.copy();
        }

        @Override public CompoundTag exactTag() {
            return exactTag == null ? null : exactTag.copy();
        }

        public boolean available() {
            return ForgeRegistries.ITEMS.containsKey(item);
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !item.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
                return false;
            }
            if (exactTag != null && !exactTag.equals(stack.getTag())) {
                return false;
            }
            if (extract) {
                return !extractId(stack).isEmpty();
            }
            CompoundTag tag = stack.getTag();
            return exactTag != null || !stack.hasTag() || stack.isDamageableItem() && !stack.isDamaged()
                    && tag.size() == 1 && tag.contains("Damage", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) && tag.getInt("Damage") == 0;
        }
    }
}
