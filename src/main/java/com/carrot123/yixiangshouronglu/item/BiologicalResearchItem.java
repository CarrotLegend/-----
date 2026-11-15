package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class BiologicalResearchItem extends Item {
    private static final int MAX_BYTES = 32 * 1024;
    private final boolean result;

    public BiologicalResearchItem(boolean result) {
        super(new Properties().stacksTo(1));
        this.result = result;
    }

    public static ItemStack sample(ResourceLocation id, CompoundTag snapshot) {
        ItemStack stack = new ItemStack(ModItems.BIOLOGICAL_RESEARCH_SAMPLE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("EntityId", id.toString());
        tag.put("EntityData", snapshot);
        tag.putBoolean("ResearchCompleted", false);
        return stack;
    }

    public static ItemStack finish(ItemStack sample) {
        ItemStack result = new ItemStack(ModItems.BIOLOGICAL_RESEARCH_RESULT.get());
        result.setTag(sample.getTag().copy());
        result.getOrCreateTag().putBoolean("ResearchCompleted", true);
        return result;
    }

    public static ResourceLocation entityId(ItemStack stack, boolean result) {
        if (!stack.is((result ? ModItems.BIOLOGICAL_RESEARCH_RESULT : ModItems.BIOLOGICAL_RESEARCH_SAMPLE).get())
                || stack.getCount() != 1 || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("EntityId"));
        return id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id)
                && ForgeRegistries.ENTITY_TYPES.getValue(id) != EntityType.PLAYER
                && tag.contains("ResearchCompleted", Tag.TAG_BYTE)
                && tag.getBoolean("ResearchCompleted") == result
                && tag.getCompound("EntityData").hasUUID("UUID")
                && tag.getCompound("EntityData").contains("Health", Tag.TAG_ANY_NUMERIC)
                && id.toString().equals(tag.getCompound("EntityData").getString("id"))
                && isBounded(tag) ? id : null;
    }

    public static boolean isBounded(CompoundTag tag) {
        if (!withinDepth(tag, 0)) {
            return false;
        }
        try {
            NbtIo.write(tag, new DataOutputStream(new OutputStream() {
                private int remaining = MAX_BYTES;
                @Override public void write(int value) throws IOException { reserve(1); }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException { reserve(length); }
                private void reserve(int length) throws IOException {
                    remaining -= length;
                    if (remaining < 0) {
                        throw new IOException("Biological research NBT exceeds size limit");
                    }
                }
            }));
            return true;
        } catch (IOException | RuntimeException | StackOverflowError exception) {
            return false;
        }
    }

    private static boolean withinDepth(Tag tag, int depth) {
        if (depth > 64) {
            return false;
        }
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                if (!withinDepth(compound.get(key), depth + 1)) {
                    return false;
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag entry : list) {
                if (!withinDepth(entry, depth + 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override public Component getName(ItemStack stack) {
        ResourceLocation id = entityId(stack, result);
        return id == null ? super.getName(stack) : Component.translatable(getDescriptionId() + ".named",
                ForgeRegistries.ENTITY_TYPES.getValue(id).getDescription());
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flags) {
        ResourceLocation id = entityId(stack, result);
        if (id != null) {
            tooltip.add(Component.translatable("tooltip.yixiangshouronglu.sample_source",
                    ForgeRegistries.ENTITY_TYPES.getValue(id).getDescription()));
            tooltip.add(Component.translatable("tooltip.yixiangshouronglu.sample_" + (result ? "completed" : "unresearched")));
        }
    }
}
