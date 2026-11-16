package com.carrot123.yixiangshouronglu.item;

import com.mojang.logging.LogUtils;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class GeneExtractorItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TAG_DEPTH = 64;
    private static final int MAX_TAG_BYTES = 16 * 1024;
    private static final String MESSAGE = "message.yixiangshouronglu.gene_extractor.";

    public GeneExtractorItem() {
        super(new Properties());
    }

    public void extract(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player || target.level() != player.level() || !target.isAlive() || target.isRemoved()) {
            return;
        }
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null || !Float.isFinite(health) || !Float.isFinite(maxHealth) || health <= 0 || maxHealth <= 0) {
            feedback(player, "invalid_data");
            return;
        }
        if (health >= maxHealth * 0.2F) {
            feedback(player, "health_too_high");
            return;
        }

        ItemStack fragment = new ItemStack(ModItems.GENE_FRAGMENT.get());
        try {
            CompoundTag data = GeneFragmentItem.sourceData(id, target.saveWithoutId(new CompoundTag()));
            if (!isSafeSnapshot(data)) {
                feedback(player, "invalid_data");
                return;
            }
            fragment.setTag(data);
            if (GeneFragmentItem.sourceType(fragment) == null) {
                feedback(player, "invalid_data");
                return;
            }
        } catch (RuntimeException | StackOverflowError exception) {
            LOGGER.warn("Could not serialize a gene fragment for {}", id, exception);
            feedback(player, "invalid_data");
            return;
        }

        var inventory = player.getInventory();
        int slot = inventory.getSlotWithRemainingSpace(fragment);
        if (slot < 0) {
            slot = inventory.getFreeSlot();
        }
        if (slot < 0) {
            feedback(player, "inventory_full");
            return;
        }
        int before = inventory.getItem(slot).getCount();
        inventory.add(slot, fragment);
        feedback(player, inventory.getItem(slot).getCount() == before + 1 ? "success" : "inventory_full");
    }

    static boolean isSafeSnapshot(CompoundTag data) {
        return hasSafeDepth(data, 0) && fitsNetworkBudget(data);
    }

    private static boolean hasSafeDepth(Tag tag, int depth) {
        if (depth > MAX_TAG_DEPTH) {
            return false;
        }
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) {
                if (!hasSafeDepth(compound.get(key), depth + 1)) {
                    return false;
                }
            }
        } else if (tag instanceof ListTag list) {
            for (Tag element : list) {
                if (!hasSafeDepth(element, depth + 1)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean fitsNetworkBudget(CompoundTag data) {
        try (DataOutputStream output = new DataOutputStream(new OutputStream() {
            private int remaining = MAX_TAG_BYTES;

            @Override public void write(int value) throws IOException {
                consume(1);
            }

            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                consume(length);
            }

            private void consume(int length) throws IOException {
                remaining -= length;
                if (remaining < 0) {
                    throw new IOException("Gene fragment NBT exceeds the size limit");
                }
            }
        })) {
            NbtIo.write(data, output);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static void feedback(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(MESSAGE + key), true);
    }
}
