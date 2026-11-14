package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.gene.recipe.ChimeraRecipe;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import com.carrot123.yixiangshouronglu.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

import java.util.Comparator;

public final class GeneSynthesizerBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final ItemStackHandler chimeraInventory = new ItemStackHandler(5) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private int chimeraProgress;
    private boolean chimeraMatched;
    private CompoundTag chimeraContext = new CompoundTag();

    public int chimeraProgress() { return chimeraProgress; }
    public boolean chimeraMatched() { return chimeraMatched; }

    private LazyOptional<IItemHandler> automation = LazyOptional.of(this::automationHandler);
    public GeneSynthesizerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.GENE_SYNTHESIZER.get(), pos, state); }
    private IItemHandler automationHandler() {
        return new IItemHandler() {
            public int getSlots() { return 5; }
            public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return inventory.extractItem(slot, amount, simulate); }
            public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
            public boolean isItemValid(int slot, ItemStack stack) { return false; }
        };
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) { return capability == ForgeCapabilities.ITEM_HANDLER ? automation.cast() : super.getCapability(capability, side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); automation = LazyOptional.of(this::automationHandler); }
    public static void tick(Level level, BlockPos pos, BlockState state, GeneSynthesizerBlockEntity machine) {
        if (level.isClientSide) { return; }
        machine.tickChimera(level);
    }
    private SimpleContainer chimeraInputs() {
        SimpleContainer inputs = new SimpleContainer(4);
        for (int slot = 0; slot < 4; slot++) {
            inputs.setItem(slot, chimeraInventory.getStackInSlot(slot).copy());
        }
        return inputs;
    }

    private void tickChimera(Level level) {
        var recipes = level.getRecipeManager().getAllRecipesFor(ModGeneRecipes.CHIMERA_TYPE.get());
        ChimeraRecipe recipe = null;
        SimpleContainer inputs = recipes.isEmpty() ? null : chimeraInputs();
        if (inputs != null) {
            String previous = chimeraContext.getString("Recipe");
            recipe = recipes.stream()
                    .filter(value -> value.matches(inputs, level))
                    .min(Comparator.<ChimeraRecipe, Boolean>comparing(value -> !value.getId().toString().equals(previous))
                            .thenComparing(ChimeraRecipe::getId)).orElse(null);
        }
        chimeraMatched = recipe != null;
        if (recipe == null) {
            if (chimeraProgress != 0 || !chimeraContext.isEmpty()) {
                chimeraProgress = 0;
                chimeraContext = new CompoundTag();
                setChanged();
            }
            return;
        }

        CompoundTag current = new CompoundTag();
        current.putString("Recipe", recipe.getId().toString());
        current.putString("Definition", recipe.signature());
        ListTag identities = new ListTag();
        for (int slot = 0; slot < 4; slot++) {
            identities.add(inputs.getItem(slot).copyWithCount(1).save(new CompoundTag()));
        }
        current.put("Inputs", identities);
        if (!current.equals(chimeraContext)) {
            chimeraProgress = 0;
            chimeraContext = current;
            setChanged();
        }

        ItemStack output = recipe.assemble(inputs, level.registryAccess());
        if (output.isEmpty() || !chimeraInventory.insertItem(4, output, true).isEmpty()) {
            return;
        }
        if (++chimeraProgress >= ChimeraRecipe.DURATION) {
            int[] assignment = recipe.matchSlots(chimeraInputs());
            if (assignment != null && chimeraInventory.insertItem(4, output, true).isEmpty()) {
                for (int index = 0; index < assignment.length; index++) {
                    chimeraInventory.extractItem(assignment[index], recipe.inputs().get(index).count(), false);
                }
                chimeraInventory.insertItem(4, output, false);
            }
            chimeraProgress = 0;
        }
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Items", inventory.serializeNBT());
        tag.put("ChimeraItems", chimeraInventory.serializeNBT());
        tag.putInt("ChimeraProgress", chimeraProgress);
        tag.put("ChimeraContext", chimeraContext.copy());
    }
    @Override public void load(CompoundTag tag) { super.load(tag); inventory.deserializeNBT(tag.getCompound("Items"));
        CompoundTag items = tag.getCompound("ChimeraItems").copy();
        items.putInt("Size", 5);
        chimeraInventory.deserializeNBT(items);
        chimeraProgress = Math.max(0, Math.min(ChimeraRecipe.DURATION - 1, tag.getInt("ChimeraProgress")));
        chimeraContext = tag.getCompound("ChimeraContext").copy();
        chimeraMatched = false;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.yixiangshouronglu.gene_synthesizer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new GeneSynthesizerMenu(id, playerInventory, this); }
}
