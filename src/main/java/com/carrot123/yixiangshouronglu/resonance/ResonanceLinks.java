package com.carrot123.yixiangshouronglu.resonance;

import com.carrot123.yixiangshouronglu.block.ContainmentChamberBlock;
import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

public final class ResonanceLinks {
    public static final int MAX_LINKS = 64;
    private static final String LINKS_KEY = "BoundChambers";

    private ResonanceLinks() {
    }

    public record Link(String dimension, BlockPos pos) {
    }

    public static List<Link> list(ItemStack stack) {
        List<Link> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LINKS_KEY, 9)) {
            return result;
        }
        ListTag list = tag.getList(LINKS_KEY, 10);
        for (int i = 0; i < list.size() && i < MAX_LINKS; i++) {
            CompoundTag entry = list.getCompound(i);
            if (ResourceLocation.tryParse(entry.getString("Dimension")) != null) {
                result.add(new Link(entry.getString("Dimension"), BlockPos.of(entry.getLong("Pos"))));
            }
        }
        return result;
    }

    private static void save(ItemStack stack, List<Link> links) {
        ListTag list = new ListTag();
        for (Link link : links) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", link.dimension());
            entry.putLong("Pos", link.pos().asLong());
            list.add(entry);
        }
        stack.getOrCreateTag().put(LINKS_KEY, list);
    }

    public static boolean add(ItemStack stack, Link link) {
        List<Link> links = list(stack);
        if (links.contains(link) || links.size() >= MAX_LINKS) {
            return false;
        }
        links.add(link);
        save(stack, links);
        return true;
    }

    public static boolean remove(ItemStack stack, Link link) {
        List<Link> links = list(stack);
        if (!links.remove(link)) {
            return false;
        }
        save(stack, links);
        return true;
    }

    public static boolean contains(ItemStack stack, Link link) {
        return list(stack).contains(link);
    }

    public static String containedEntity(MinecraftServer server, Link link) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(link.dimension());
        if (dimensionId == null) {
            return "";
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = server.getLevel(key);
        if (level == null || !level.hasChunkAt(link.pos())) {
            return "";
        }
        if (!level.getBlockState(link.pos()).is(ModBlocks.CONTAINMENT_CHAMBER.get()) || level.getBlockState(link.pos()).getValue(ContainmentChamberBlock.HALF) != DoubleBlockHalf.LOWER) {
            return "";
        }
        return level.getBlockEntity(link.pos()) instanceof ContainmentChamberBlockEntity chamber ? chamber.getEntityId() : "";
    }
}
