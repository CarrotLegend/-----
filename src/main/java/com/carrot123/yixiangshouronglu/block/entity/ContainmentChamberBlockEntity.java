package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import com.carrot123.yixiangshouronglu.production.ChamberProduction;
import com.carrot123.yixiangshouronglu.production.ChamberSpecialProducts;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ContainmentChamberBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int MAX_EXTRACT_MB = 2000;
    public static final int EXTRACT_COST_MB = 100;
    public static final int EXTRACT_TIME = 20;

    private static final int ACCUMULATION_INTERVAL = 60;

    private ItemStack soul = ItemStack.EMPTY;
    private final NonNullList<ItemStack> outputs = NonNullList.withSize(2, ItemStack.EMPTY);

    private ItemStack extractionInput = ItemStack.EMPTY;
    private ItemStack extractionOutput = ItemStack.EMPTY;

    private int productionProgress;
    private int extractStoredMb;
    private int extractionAccumulationTicks;
    private int extractionProgress;

    private final ContainerData productionData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> productionProgress;
                case 1 -> getProductionInterval();
                case 2 -> extractStoredMb;
                case 3 -> extractionProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> productionProgress = Math.max(0, value);
                case 2 -> extractStoredMb = Math.max(0, Math.min(MAX_EXTRACT_MB, value));
                case 3 -> extractionProgress = Math.max(0, Math.min(EXTRACT_TIME, value));
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public ContainmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAINMENT_CHAMBER.get(), pos, state);
    }

    public ItemStack getSoul() {
        return soul.copy();
    }

    public String getEntityId() {
        return soul.is(ModItems.SOUL.get())
                ? SoulItem.getEntityId(soul)
                : "";
    }

    public ContainerData getProductionData() {
        return productionData;
    }

    public int getProductionInterval() {
        ChamberProduction.Recipe recipe = ChamberProduction.forEntity(getEntityId());
        return recipe == null ? 0 : recipe.intervalTicks();
    }

    public int getExtractStoredMb() {
        return extractStoredMb;
    }

    public int getExtractionProgress() {
        return extractionProgress;
    }

    public boolean insertSoul(ItemStack stack) {
        if (!soul.isEmpty()
                || !stack.is(ModItems.SOUL.get())
                || SoulItem.getEntityId(stack).isEmpty()) {
            return false;
        }

        soul = stack.copyWithCount(1);
        productionProgress = 0;
        extractStoredMb = 0;
        extractionAccumulationTicks = 0;
        extractionProgress = 0;

        sync();
        return true;
    }

    public ItemStack extractSoul() {
        ItemStack result = soul;
        soul = ItemStack.EMPTY;

        productionProgress = 0;
        extractStoredMb = 0;
        extractionAccumulationTicks = 0;
        extractionProgress = 0;

        sync();
        return result;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ContainmentChamberBlockEntity chamber
    ) {
        if (level.isClientSide) {
            return;
        }

        String entityId = chamber.getEntityId();

        if (entityId.isEmpty()) {
            return;
        }

        chamber.tickExtraction(entityId);

        ChamberProduction.Recipe recipe = ChamberProduction.forEntity(entityId);

        if (recipe != null && chamber.canStore(recipe.item())) {
            chamber.productionProgress++;

            if (chamber.productionProgress >= recipe.intervalTicks()) {
                if (chamber.storeOne(recipe.item())) {
                    chamber.productionProgress = 0;
                }
            }
        }

        chamber.setChanged();
    }

    private void tickExtraction(String entityId) {
        if (extractStoredMb < MAX_EXTRACT_MB) {
            extractionAccumulationTicks++;

            if (extractionAccumulationTicks >= ACCUMULATION_INTERVAL) {
                extractionAccumulationTicks = 0;
                extractStoredMb = Math.min(MAX_EXTRACT_MB, extractStoredMb + 1);
            }
        } else {
            extractionAccumulationTicks = 0;
        }

        boolean usingExtractor = extractionInput.is(ModItems.EXTRACTOR.get());
        ItemStack result = ItemStack.EMPTY;

        if (usingExtractor) {
            if (extractStoredMb >= EXTRACT_COST_MB) {
                result = ContainmentExtractItem.create(entityId);
            }
        } else if (extractionInput.is(ModItems.CONTAINMENT_EXTRACT.get())
                && entityId.equals(ContainmentExtractItem.getEntityId(extractionInput))) {
            result = ChamberSpecialProducts.forEntity(entityId);
        }

        if (result.isEmpty() || !canStoreExtraction(result)) {
            extractionProgress = 0;
            return;
        }

        extractionProgress++;

        if (extractionProgress < EXTRACT_TIME) {
            return;
        }

        if (usingExtractor) {
            if (extractStoredMb < EXTRACT_COST_MB) {
                extractionProgress = 0;
                return;
            }

            extractStoredMb -= EXTRACT_COST_MB;
        } else {
            extractionInput.shrink(1);

            if (extractionInput.isEmpty()) {
                extractionInput = ItemStack.EMPTY;
            }
        }

        storeExtraction(result);
        extractionProgress = 0;
    }

    private boolean canStoreExtraction(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        if (extractionOutput.isEmpty()) {
            return true;
        }

        return ItemStack.isSameItemSameTags(extractionOutput, result)
                && extractionOutput.getCount() + result.getCount()
                <= extractionOutput.getMaxStackSize();
    }

    private void storeExtraction(ItemStack result) {
        if (extractionOutput.isEmpty()) {
            extractionOutput = result.copy();
        } else {
            extractionOutput.grow(result.getCount());
        }
    }

    private boolean canStore(Item item) {
        for (ItemStack stack : outputs) {
            if (stack.isEmpty()
                    || (stack.is(item) && stack.getCount() < stack.getMaxStackSize())) {
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
        tag.putInt("ExtractStoredMb", extractStoredMb);
        tag.putInt("ExtractionAccumulationTicks", extractionAccumulationTicks);
        tag.putInt("ExtractionProgress", extractionProgress);

        if (!extractionInput.isEmpty()) {
            tag.put("ExtractionInput", extractionInput.save(new CompoundTag()));
        }

        if (!extractionOutput.isEmpty()) {
            tag.put("ExtractionOutput", extractionOutput.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        soul = tag.contains("Soul", 10)
                ? ItemStack.of(tag.getCompound("Soul"))
                : ItemStack.EMPTY;

        for (int index = 0; index < outputs.size(); index++) {
            outputs.set(index, ItemStack.EMPTY);
        }

        ContainerHelper.loadAllItems(tag, outputs);

        productionProgress = Math.max(0, tag.getInt("ProductionProgress"));
        extractStoredMb = Math.max(0, Math.min(MAX_EXTRACT_MB, tag.getInt("ExtractStoredMb")));
        extractionAccumulationTicks = Math.max(0, Math.min(59, tag.getInt("ExtractionAccumulationTicks")));
        extractionProgress = Math.max(0, Math.min(EXTRACT_TIME, tag.getInt("ExtractionProgress")));

        extractionInput = tag.contains("ExtractionInput", 10)
                ? ItemStack.of(tag.getCompound("ExtractionInput"))
                : ItemStack.EMPTY;

        extractionOutput = tag.contains("ExtractionOutput", 10)
                ? ItemStack.of(tag.getCompound("ExtractionOutput"))
                : ItemStack.EMPTY;
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
    public Component getDisplayName() {
        return Component.translatable("screen.yixiangshouronglu.chamber.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainmentChamberMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return extractionInput.isEmpty() && extractionOutput.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case 0, 1 -> outputs.get(slot);
            case 2 -> extractionInput;
            case 3 -> extractionOutput;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result;

        if (slot >= 0 && slot < outputs.size()) {
            result = ContainerHelper.removeItem(outputs, slot, amount);
        } else if (slot == 2) {
            result = extractionInput.split(amount);

            if (extractionInput.isEmpty()) {
                extractionInput = ItemStack.EMPTY;
            }

            extractionProgress = 0;
        } else if (slot == 3) {
            result = extractionOutput.split(amount);

            if (extractionOutput.isEmpty()) {
                extractionOutput = ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result;

        if (slot >= 0 && slot < outputs.size()) {
            result = ContainerHelper.takeItem(outputs, slot);
        } else if (slot == 2) {
            result = extractionInput;
            extractionInput = ItemStack.EMPTY;
            extractionProgress = 0;
        } else if (slot == 3) {
            result = extractionOutput;
            extractionOutput = ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) {
            return;
        }

        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }

        switch (slot) {
            case 0, 1 -> outputs.set(slot, stack);
            case 2 -> {
                extractionInput = stack;
                extractionProgress = 0;
            }
            case 3 -> extractionOutput = stack;
            default -> {
            }
        }

        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 2 || getEntityId().isEmpty()) {
            return false;
        }

        if (stack.is(ModItems.EXTRACTOR.get())) {
            return true;
        }

        return stack.is(ModItems.CONTAINMENT_EXTRACT.get())
                && getEntityId().equals(ContainmentExtractItem.getEntityId(stack));
    }

    @Override
    public void clearContent() {
        for (int index = 0; index < outputs.size(); index++) {
            outputs.set(index, ItemStack.EMPTY);
        }

        extractionInput = ItemStack.EMPTY;
        extractionOutput = ItemStack.EMPTY;
        extractionProgress = 0;

        setChanged();
    }
}