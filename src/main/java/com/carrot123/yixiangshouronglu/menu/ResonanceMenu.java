package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.registry.ModMenus;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.resonance.ResonanceLinks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ResonanceMenu extends AbstractContainerMenu {
    public record Entry(String dimension, net.minecraft.core.BlockPos pos, String entityId) {
        public Component entityName() {
            return entityId.isEmpty() ? Component.translatable("screen.yixiangshouronglu.empty") : SoulItem.getEntityName(entityId);
        }
    }

    private final List<Entry> entries;

    public ResonanceMenu(int windowId, Inventory inventory, FriendlyByteBuf buffer) {
        this(windowId, inventory, read(buffer));
    }

    public ResonanceMenu(int windowId, Inventory inventory, List<Entry> entries) {
        super(ModMenus.RESONANCE.get(), windowId);
        this.entries = List.copyOf(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public static List<Entry> snapshots(ServerPlayer player, ItemStack stack) {
        List<Entry> entries = new ArrayList<>();
        for (ResonanceLinks.Link link : ResonanceLinks.list(stack)) {
            entries.add(new Entry(link.dimension(), link.pos(), ResonanceLinks.containedEntity(player.server, link)));
        }
        return entries;
    }

    public static void write(FriendlyByteBuf buffer, List<Entry> entries) {
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeUtf(entry.dimension(), 256);
            buffer.writeBlockPos(entry.pos());
            buffer.writeUtf(entry.entityId(), 256);
        }
    }

    private static List<Entry> read(FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), ResonanceLinks.MAX_LINKS);
        List<Entry> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(buffer.readUtf(256), buffer.readBlockPos(), buffer.readUtf(256)));
        }
        return entries;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
