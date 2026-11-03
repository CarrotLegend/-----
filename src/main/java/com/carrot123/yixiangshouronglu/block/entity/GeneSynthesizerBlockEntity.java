package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.gene.GeneTypes;
import com.carrot123.yixiangshouronglu.gene.recipe.GeneRecipe;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import com.carrot123.yixiangshouronglu.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import org.jetbrains.annotations.Nullable;

public final class GeneSynthesizerBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private LazyOptional<IItemHandler> automation = LazyOptional.of(this::automationHandler);
    private GeneTypes.Mode mode = GeneTypes.Mode.SYNTHESIS;
    private int progress;
    private int duration;
    private String context = "";
    private ItemStack preview = ItemStack.EMPTY;
    public GeneSynthesizerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.GENE_SYNTHESIZER.get(), pos, state); }
    public GeneTypes.Mode mode() { return mode; }
    public int progress() { return progress; }
    public int duration() { return duration; }
    public ItemStack preview() { return preview.copy(); }
    public void mode(GeneTypes.Mode selected) { if (mode != selected) { mode = selected; progress = 0; context = ""; preview = ItemStack.EMPTY; setChanged(); } }
    private IItemHandler automationHandler() {
        return new IItemHandler() {
            public int getSlots() { return 5; }
            public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot >= 0 && slot < 4 ? inventory.insertItem(slot, stack, simulate) : stack; }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot == 4 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
            public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
            public boolean isItemValid(int slot, ItemStack stack) { return slot >= 0 && slot < 4; }
        };
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) { return capability == ForgeCapabilities.ITEM_HANDLER ? automation.cast() : super.getCapability(capability, side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation = LazyOptional.of(this::automationHandler); }
    public static void tick(Level level, BlockPos pos, BlockState state, GeneSynthesizerBlockEntity machine) {
        if (level.isClientSide) { return; }
        SimpleContainer inputs = new SimpleContainer(4);
        for (int slot = 0; slot < 4; slot++) { inputs.setItem(slot, machine.inventory.getStackInSlot(slot).copy()); }
        GeneRecipe recipe = level.getRecipeManager().getAllRecipesFor(ModGeneRecipes.TYPE.get()).stream()
                .filter(value -> value.matchesInputs(inputs, machine.mode)).sorted(java.util.Comparator.comparing(value -> value.getId().toString())).findFirst().orElse(null);
        if (recipe == null) {
            if (machine.progress != 0 || machine.duration != 0 || !machine.context.isEmpty() || !machine.preview.isEmpty()) {
                machine.progress = 0; machine.duration = 0; machine.context = ""; machine.preview = ItemStack.EMPTY; machine.setChanged();
            }
            return;
        }
        ItemStack result = recipe.assemble(inputs, level.registryAccess());
        StringBuilder key = new StringBuilder(recipe.getId().toString()).append(recipe.signature());
        for (int slot = 0; slot < 4; slot++) { key.append(inputs.getItem(slot).copyWithCount(1).save(new CompoundTag())); }
        String context = key.toString();
        if (!machine.context.equals(context)) { machine.progress = 0; machine.context = context; machine.setChanged(); }
        machine.duration = recipe.duration(); machine.preview = result.copy();
        if (result.isEmpty() || !machine.inventory.insertItem(4, result, true).isEmpty()) { return; }
        if (++machine.progress >= machine.duration) {
            int[] matched = recipe.matchSlots(inputs, machine.mode);
            if (matched == null) { machine.progress = 0; machine.setChanged(); return; }
            for (int index = 0; index < matched.length; index++) { machine.inventory.extractItem(matched[index], recipe.inputs().get(index).count(), false); }
            machine.inventory.insertItem(4, result, false); machine.progress = 0;
        }
        machine.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Items", inventory.serializeNBT()); tag.putString("Mode", mode.name()); tag.putInt("Progress", progress); tag.putString("Context", context); }
    @Override public void load(CompoundTag tag) { super.load(tag); inventory.deserializeNBT(tag.getCompound("Items")); try { mode = GeneTypes.Mode.valueOf(tag.getString("Mode")); } catch (IllegalArgumentException exception) { mode = GeneTypes.Mode.SYNTHESIS; } progress = Math.max(0, tag.getInt("Progress")); context = tag.getString("Context"); }
    @Override public Component getDisplayName() { return Component.translatable("block.yixiangshouronglu.gene_synthesizer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new GeneSynthesizerMenu(id, playerInventory, this); }
}
