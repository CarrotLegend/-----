package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ContainmentChamberBlockEntity extends BlockEntity implements Container {
    private ItemStack soul = ItemStack.EMPTY;
    private final NonNullList<ItemStack> outputs = NonNullList.withSize(2, ItemStack.EMPTY);
    private int productionProgress;

    private final ContainerData productionData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> productionProgress;
                case 1 -> getProductionInterval();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                productionProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ContainmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINMENT_CHAMBER.get(), pos, state);
    }

    public ItemStack getSoul() {
        return soul.copy();
    }

    public String getEntityId() {
        return soul.is(ModItems.SOUL.get()) ? SoulItem.getEntityId(soul) : "";
    }

    public ContainerData getProductionData() {
        return productionData;
    }

    public int getProductionInterval() {
        ChamberProduction.Recipe recipe = ChamberProduction.forEntity(getEntityId());
        return recipe == null ? 0 : recipe.intervalTicks();
    }

    public boolean insertSoul(ItemStack stack) {
        if (!soul.isEmpty() || !stack.is(ModItems.SOUL.get()) || SoulItem.getEntityId(stack).isEmpty()) {
            return false;
        }
        soul = stack.copyWithCount(1);
        productionProgress = 0;
        sync();
        return true;
    }

    public ItemStack extractSoul() {
        ItemStack result = soul;
        soul = ItemStack.EMPTY;
        productionProgress = 0;
        sync();
        return result;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ContainmentChamberBlockEntity chamber) {
        ChamberProduction.Recipe recipe = ChamberProduction.forEntity(chamber.getEntityId());
        if (recipe == null || !chamber.canStore(recipe.item())) {
            return;
        }

        chamber.productionProgress++;
        if (chamber.productionProgress >= recipe.intervalTicks()) {
            if (chamber.storeOne(recipe.item())) {
                chamber.productionProgress = 0;
            }
        }
        chamber.setChanged();
    }

    private boolean canStore(Item item) {
        for (ItemStack stack : outputs) {
            if (stack.isEmpty() || (stack.is(item) && stack.getCount() < stack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private boolean storeOne(Item item) {
        for (int index = 0; index < outputs.size(); index++) {
            ItemStack stack = outputs.get(index);
            if (stack.isEmpty()) {
                outputs.set(index, new ItemStack(item));
                return true;
            }
            if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return true;
            }
        }
        return false;
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
        ContainerHelper.saveAllItems(tag, outputs);
        tag.putInt("ProductionProgress", productionProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        soul = tag.contains("Soul", 10) ? ItemStack.of(tag.getCompound("Soul")) : ItemStack.EMPTY;
        for (int index = 0; index < outputs.size(); index++) {
            outputs.set(index, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, outputs);
        productionProgress = Math.max(0, tag.getInt("ProductionProgress"));
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

    @Override
    public int getContainerSize() {
        return outputs.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < outputs.size() ? outputs.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(outputs, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(outputs, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= outputs.size()) {
            return;
        }
        outputs.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < outputs.size(); index++) {
            outputs.set(index, ItemStack.EMPTY);
        }
        setChanged();
    }
}
