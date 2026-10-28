package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ContainmentChamberBlockEntity extends BlockEntity {
    private ItemStack soul = ItemStack.EMPTY;

    public ContainmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINMENT_CHAMBER.get(), pos, state);
    }

    public ItemStack getSoul() {
        return soul.copy();
    }

    public String getEntityId() {
        return soul.is(ModItems.SOUL.get()) ? SoulItem.getEntityId(soul) : "";
    }

    public boolean insertSoul(ItemStack stack) {
        if (!soul.isEmpty() || !stack.is(ModItems.SOUL.get()) || SoulItem.getEntityId(stack).isEmpty()) {
            return false;
        }
        soul = stack.copyWithCount(1);
        sync();
        return true;
    }

    public ItemStack extractSoul() {
        ItemStack result = soul;
        soul = ItemStack.EMPTY;
        sync();
        return result;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!soul.isEmpty()) {
            tag.put("Soul", soul.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        soul = tag.contains("Soul", 10) ? ItemStack.of(tag.getCompound("Soul")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
