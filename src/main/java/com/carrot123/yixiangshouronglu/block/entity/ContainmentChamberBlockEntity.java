package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.item.BiologicalGeneModuleItem;
import com.carrot123.yixiangshouronglu.item.GeneFragmentItem;

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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ContainmentChamberBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int MAX_EXTRACT_MB = 4000;
    public static final int EXTRACT_COST_MB = 100;
    public static final int EXTRACT_TIME = 20;

    public static final int BASE_OUTPUTS = 10;
    public static final int OUTPUTS_PER_UPGRADE = 5;
    public static final int UPGRADE_COUNT = 8;
    public static final int MAX_OUTPUTS = BASE_OUTPUTS + OUTPUTS_PER_UPGRADE * UPGRADE_COUNT;
    public static final int EXTRACTION_INPUT = MAX_OUTPUTS;
    public static final int EXTRACTION_OUTPUT = EXTRACTION_INPUT + 1;
    public static final int FIRST_UPGRADE = EXTRACTION_OUTPUT + 1;
    public static final int SLOT_COUNT = FIRST_UPGRADE + UPGRADE_COUNT;
    public static final int DATA_COUNT = 7;

    private static final int ACCUMULATION_INTERVAL = 24;

    private ItemStack soul = ItemStack.EMPTY;
    private final NonNullList<ItemStack> outputs = NonNullList.withSize(MAX_OUTPUTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_COUNT, ItemStack.EMPTY);

    private final Deque<ItemStack> pendingProduction = new ArrayDeque<>();

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
                case 4 -> getUnlockedOutputs();
                case 5 -> getExtractionInterval();
                case 6 -> pendingProduction.isEmpty() ? 0 : 1;
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
            return DATA_COUNT;
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
        return getEntityId().isEmpty() ? 0 : productionInterval(ChamberProduction.INTERVAL);
    }

    private int productionInterval(int base) {
        int multiplier = 1;
        for (ItemStack upgrade : upgrades) {
            if (upgrade.is(ModItems.SPEED_UPGRADE.get())) {
                multiplier++;
            }
        }
        return Math.max(1, (int) ((base + (long) multiplier - 1) / multiplier));
    }

    public int getUnlockedOutputs() {
        return unlockedOutputs(-1, ItemStack.EMPTY);
    }

    private int unlockedOutputs(int changedSlot, ItemStack replacement) {
        int count = BASE_OUTPUTS;
        for (int slot = 0; slot < upgrades.size(); slot++) {
            ItemStack upgrade = slot == changedSlot ? replacement : upgrades.get(slot);
            if (upgrade.is(ModItems.CAPACITY_UPGRADE.get())) {
                count += OUTPUTS_PER_UPGRADE;
            }
        }
        return Math.min(MAX_OUTPUTS, count);
    }

    public static boolean isUpgrade(ItemStack stack) {
        return stack.is(ModItems.CAPACITY_UPGRADE.get()) || stack.is(ModItems.SPEED_UPGRADE.get())
                || stack.is(ModItems.EXTRACTION_UPGRADE.get());
    }

    public boolean canChangeUpgrade(int slot, ItemStack replacement) {
        if (!isUpgradeSlot(slot) || (!replacement.isEmpty() && (!isUpgrade(replacement) || replacement.getCount() != 1))) {
            return false;
        }
        if (!getItem(slot).is(ModItems.CAPACITY_UPGRADE.get()) || replacement.is(ModItems.CAPACITY_UPGRADE.get())) {
            return true;
        }
        int unlocked = getUnlockedOutputs();
        int after = unlockedOutputs(slot - FIRST_UPGRADE, replacement);
        for (int index = after; index < unlocked; index++) {
            if (!outputs.get(index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUpgradeSlot(int slot) {
        return slot >= FIRST_UPGRADE && slot < SLOT_COUNT;
    }

    private int getExtractionMultiplier() {
        int multiplier = 1;
        for (ItemStack upgrade : upgrades) {
            if (upgrade.is(ModItems.EXTRACTION_UPGRADE.get())) {
                multiplier++;
            }
        }
        return multiplier;
    }

    public int getExtractionInterval() {
        if (!extractionInput.is(ModItems.EXTRACTOR.get())) {
            return EXTRACT_TIME;
        }
        int multiplier = getExtractionMultiplier();
        return Math.max(1, (EXTRACT_TIME + multiplier - 1) / multiplier);
    }

    private void changeUpgrade(int slot, ItemStack replacement) {
        int oldExtractionInterval = getExtractionInterval();
        int oldInterval = getProductionInterval();
        upgrades.set(slot - FIRST_UPGRADE, replacement);
        int newInterval = getProductionInterval();
        int newExtractionInterval = getExtractionInterval();
        if (oldExtractionInterval != newExtractionInterval) {
            extractionProgress = (int) Math.max(0L, Math.min(newExtractionInterval - 1L,
                    (long) extractionProgress * newExtractionInterval / oldExtractionInterval));
        }
        if (oldInterval > 0 && newInterval > 0 && oldInterval != newInterval) {
            productionProgress = (int) Math.min(newInterval - 1L,
                    (long) productionProgress * newInterval / oldInterval);
        }
        setChanged();
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

        boolean normalizedInput = ContainmentExtractItem.normalize(chamber.extractionInput);
        boolean normalizedOutput = ContainmentExtractItem.normalize(chamber.extractionOutput);
        if (normalizedInput || normalizedOutput) { chamber.setChanged(); }

        String entityId = chamber.getEntityId();

        if (!entityId.isEmpty()) {
            chamber.tickExtraction(entityId);
        }
        if (level instanceof ServerLevel server) {
            chamber.tickProduction(server, pos, entityId);
        }
        chamber.setChanged();
    }

    private void tickProduction(ServerLevel level, BlockPos pos, String entityId) {
        if (!pendingProduction.isEmpty()) {
            flushProduction();
            return;
        }
        if (entityId.isEmpty()) {
            return;
        }

        int interval = getProductionInterval();
        productionProgress = Math.min(productionProgress, interval - 1) + 1;
        if (productionProgress >= interval) {
            productionProgress = 0;
            pendingProduction.addAll(ChamberProduction.roll(level, pos, entityId));
            flushProduction();
        }
    }

    private void flushProduction() {
        int unlocked = getUnlockedOutputs();
        for (var iterator = pendingProduction.iterator(); iterator.hasNext();) {
            ItemStack pending = iterator.next();
            for (int slot = 0; slot < unlocked && !pending.isEmpty(); slot++) {
                ItemStack stored = outputs.get(slot);
                if (!stored.isEmpty() && ItemStack.isSameItemSameTags(stored, pending)) {
                    int limit = Math.min(getMaxStackSize(), stored.getMaxStackSize());
                    int moved = Math.min(pending.getCount(), limit - stored.getCount());
                    if (moved > 0) {
                        stored.grow(moved);
                        pending.shrink(moved);
                    }
                }
            }
            for (int slot = 0; slot < unlocked && !pending.isEmpty(); slot++) {
                if (outputs.get(slot).isEmpty()) {
                    outputs.set(slot, pending.split(Math.min(getMaxStackSize(), pending.getMaxStackSize())));
                }
            }
            if (pending.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void tickExtraction(String entityId) {
        if (extractStoredMb < MAX_EXTRACT_MB) {
            extractionAccumulationTicks += getExtractionMultiplier();

            if (extractionAccumulationTicks >= ACCUMULATION_INTERVAL) {
                extractionAccumulationTicks -= ACCUMULATION_INTERVAL;
                extractStoredMb = Math.min(MAX_EXTRACT_MB, extractStoredMb + 1);
            }
        } else {
            extractionAccumulationTicks = 0;
        }

        if (extractionInput.is(ModItems.GENE_FRAGMENT.get())) {
            synthesizeModule();
            return;
        }

        boolean usingExtractor = extractionInput.is(ModItems.EXTRACTOR.get());
        ItemStack result = ItemStack.EMPTY;
        int inputCount = 1;

        if (usingExtractor) {
            if (extractStoredMb >= EXTRACT_COST_MB) {
                result = ContainmentExtractItem.create(entityId);
            }
        } else {
            ChamberSpecialProducts.Recipe recipe = ChamberSpecialProducts.forInput(entityId, extractionInput);

            if (recipe != null && extractionInput.getCount() >= recipe.inputCount()) {
                result = recipe.result();
                inputCount = recipe.inputCount();
            }
        }

        if (result.isEmpty()) {
            extractionProgress = 0;
            return;
        }
        if (!canStoreExtraction(result)) {
            if (!usingExtractor) {
                extractionProgress = 0;
            }
            return;
        }

        extractionProgress++;

        if (extractionProgress < getExtractionInterval()) {
            return;
        }

        if (usingExtractor) {
            if (extractStoredMb < EXTRACT_COST_MB) {
                extractionProgress = 0;
                return;
            }

            extractStoredMb -= EXTRACT_COST_MB;
        }
        extractionInput.shrink(inputCount);

        if (extractionInput.isEmpty()) {
            extractionInput = ItemStack.EMPTY;
        }

        storeExtraction(result);
        extractionProgress = 0;
    }

    private void synthesizeModule() {
        if (extractStoredMb < EXTRACT_COST_MB || !extractionOutput.isEmpty()) {
            return;
        }
        if (extractionProgress == 0 && GeneFragmentItem.sourceType(extractionInput) == null) {
            return;
        }
        if (++extractionProgress < EXTRACT_TIME) {
            return;
        }
        ItemStack result = BiologicalGeneModuleItem.fromFragment(extractionInput);
        extractionProgress = 0;
        if (result.isEmpty()) {
            return;
        }
        extractStoredMb -= EXTRACT_COST_MB;
        extractionInput.shrink(1);
        if (extractionInput.isEmpty()) {
            extractionInput = ItemStack.EMPTY;
        }
        storeExtraction(result);
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
        tag.put("Upgrades", ContainerHelper.saveAllItems(new CompoundTag(), upgrades));

        if (!pendingProduction.isEmpty()) {
            ListTag pending = new ListTag();
            for (ItemStack stack : pendingProduction) {
                CompoundTag item = stack.copyWithCount(1).save(new CompoundTag());
                item.putInt("PendingCount", stack.getCount());
                pending.add(item);
            }
            tag.put("PendingProduction", pending);
        }

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
        upgrades.clear();
        ContainerHelper.loadAllItems(tag.getCompound("Upgrades"), upgrades);

        pendingProduction.clear();
        ListTag pending = tag.getList("PendingProduction", Tag.TAG_COMPOUND);
        for (int index = 0; index < pending.size(); index++) {
            CompoundTag item = pending.getCompound(index);
            ItemStack stack = ItemStack.of(item);
            if (!stack.isEmpty()) {
                stack.setCount(item.getInt("PendingCount"));
                if (!stack.isEmpty()) {
                    pendingProduction.add(stack);
                }
            }
        }
        productionProgress = Math.max(0, tag.getInt("ProductionProgress"));
        extractStoredMb = Math.max(0, Math.min(MAX_EXTRACT_MB, tag.getInt("ExtractStoredMb")));
        extractionAccumulationTicks = Math.max(0, Math.min(ACCUMULATION_INTERVAL - 1, tag.getInt("ExtractionAccumulationTicks")));

        extractionInput = tag.contains("ExtractionInput", 10)
                ? ItemStack.of(tag.getCompound("ExtractionInput"))
                : ItemStack.EMPTY;

        extractionOutput = tag.contains("ExtractionOutput", 10)
                ? ItemStack.of(tag.getCompound("ExtractionOutput"))
                : ItemStack.EMPTY;
        extractionProgress = Math.max(0, Math.min(getExtractionInterval() - 1, tag.getInt("ExtractionProgress")));
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
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return pendingProduction.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < MAX_OUTPUTS) {
            return outputs.get(slot);
        }
        if (isUpgradeSlot(slot)) {
            return upgrades.get(slot - FIRST_UPGRADE);
        }
        return switch (slot) {
            case EXTRACTION_INPUT -> extractionInput;
            case EXTRACTION_OUTPUT -> extractionOutput;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0 || !canRemove(slot)) {
            return ItemStack.EMPTY;
        }
        if (isUpgradeSlot(slot)) {
            ItemStack result = getItem(slot);
            if (!result.isEmpty()) {
                changeUpgrade(slot, ItemStack.EMPTY);
            }
            return result;
        }
        ItemStack result = getItem(slot).split(amount);
        if (!result.isEmpty()) {
            if (slot == EXTRACTION_INPUT) {
                extractionProgress = 0;
            }
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, getItem(slot).getCount());
    }

    private boolean canRemove(int slot) {
        if (slot >= 0 && slot < MAX_OUTPUTS) {
            return slot < getUnlockedOutputs();
        }
        if (isUpgradeSlot(slot)) {
            return canChangeUpgrade(slot, ItemStack.EMPTY);
        }
        return slot == EXTRACTION_INPUT || slot == EXTRACTION_OUTPUT;
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return canRemove(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            if (canChangeUpgrade(slot, stack)) {
                changeUpgrade(slot, stack);
            }
            return;
        }
        if (!canRemove(slot)) {
            return;
        }
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (slot < MAX_OUTPUTS) {
            outputs.set(slot, stack);
        } else if (slot == EXTRACTION_INPUT) {
            extractionInput = stack;
            extractionProgress = 0;
        } else {
            extractionOutput = stack;
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isUpgradeSlot(slot)) {
            return isUpgrade(stack) && canChangeUpgrade(slot, stack);
        }
        return slot == EXTRACTION_INPUT && acceptsExtraction(getEntityId(), stack);
    }

    public static boolean acceptsExtraction(String entityId, ItemStack stack) {
        return !entityId.isEmpty() && (stack.is(ModItems.EXTRACTOR.get()) || GeneFragmentItem.sourceType(stack) != null
                || ChamberSpecialProducts.forInput(entityId, stack) != null);
    }

    public NonNullList<ItemStack> drainContents() {
        NonNullList<ItemStack> contents = NonNullList.create();
        contents.add(soul);
        soul = ItemStack.EMPTY;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            contents.add(getItem(slot));
        }
        for (ItemStack pending : pendingProduction) {
            while (!pending.isEmpty()) {
                contents.add(pending.split(pending.getMaxStackSize()));
            }
        }
        clearContent();
        return contents;
    }

    @Override
    public void clearContent() {
        outputs.clear();
        upgrades.clear();
        pendingProduction.clear();
        extractionInput = ItemStack.EMPTY;
        extractionOutput = ItemStack.EMPTY;
        extractionProgress = 0;
        productionProgress = 0;
        setChanged();
    }
}
