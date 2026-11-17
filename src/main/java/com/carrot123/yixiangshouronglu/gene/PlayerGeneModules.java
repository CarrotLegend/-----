package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.BiologicalGeneModuleItem;
import com.carrot123.yixiangshouronglu.item.GeneFragmentItem;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class PlayerGeneModules {
    public static final int SIZE = 21;
    private static final Capability<PlayerGeneModules> TYPE = CapabilityManager.get(new CapabilityToken<>() {});
    private final SimpleContainer slots = new SimpleContainer(SIZE);
    private final EnumSet<BiologicalGene> active = EnumSet.noneOf(BiologicalGene.class);
    private long shockReadyAt;
    private int rageStacks;
    private long rageExpiresAt;
    private int waterTicks;
    private UUID skyTarget;
    private int skyHits;
    private long skyReadyAt;
    private long fallenEndsAt;
    private long fallenReadyAt;
    private long annihilationReadyAt;

    public static PlayerGeneModules get(Player player) {
        return player.getCapability(TYPE).orElseThrow(() -> new IllegalStateException("Missing player gene modules"));
    }

    public SimpleContainer slots() {
        return slots;
    }

    public static boolean accepts(SimpleContainer contents, int slot, ItemStack stack) {
        BiologicalGene gene = BiologicalGeneModuleItem.gene(stack);
        if (gene == null) {
            return GeneFragmentItem.sourceType(stack) != null;
        }
        if (!gene.allowsSlot(slot)) {
            return false;
        }
        for (int index = 0; index < SIZE; index++) {
            if (index != slot && BiologicalGeneModuleItem.gene(contents.getItem(index)) == gene) {
                return false;
            }
        }
        return true;
    }

    public void recount() {
        active.clear();
        for (int slot = 0; slot < SIZE; slot++) {
            BiologicalGene gene = BiologicalGeneModuleItem.gene(slots.getItem(slot));
            if (gene != null && gene.allowsSlot(slot)) {
                active.add(gene);
            }
        }
        if (count(BiologicalGene.CURSED_MUSCLE_FIBERS) == 0) {
            clearRage();
        }
        if (count(BiologicalGene.ABYSSAL_CIRCULATION) == 0) {
            waterTicks = 0;
        }
        if (count(BiologicalGene.SKY_STORM_CORE) == 0) {
            clearSkyCombo();
        }
        if (count(BiologicalGene.FALLEN_COMBAT_NERVES) == 0) {
            endFallenForm();
        }
    }

    public int count(BiologicalGene gene) {
        return active.contains(gene) ? 1 : 0;
    }

    public boolean skyHit(UUID target, long now) {
        if (now < skyReadyAt) {
            return false;
        }
        if (!target.equals(skyTarget)) {
            skyTarget = target;
            skyHits = 0;
        }
        if (++skyHits < BiologicalGene.SKY_HITS) {
            return false;
        }
        clearSkyCombo();
        skyReadyAt = now + BiologicalGene.SKY_COOLDOWN;
        return true;
    }

    public void clearSkyCombo() {
        skyTarget = null;
        skyHits = 0;
    }

    public boolean startFallenForm(long now) {
        if (now < fallenReadyAt || fallenForm(now)) {
            return false;
        }
        fallenEndsAt = now + BiologicalGene.FALLEN_TICKS;
        fallenReadyAt = now + BiologicalGene.FALLEN_COOLDOWN;
        return true;
    }

    public boolean fallenForm(long now) {
        return now < fallenEndsAt;
    }

    public boolean expireFallenForm(long now) {
        if (fallenEndsAt == 0 || fallenForm(now)) {
            return false;
        }
        endFallenForm();
        return true;
    }

    public void endFallenForm() {
        fallenEndsAt = 0;
    }

    public boolean startAnnihilationCooldown(long now) {
        if (now < annihilationReadyAt) {
            return false;
        }
        annihilationReadyAt = now + BiologicalGene.ANNIHILATION_COOLDOWN;
        return true;
    }

    public int rageStacks(long gameTime) {
        if (rageStacks > 0 && gameTime >= rageExpiresAt) {
            clearRage();
        }
        return rageStacks;
    }

    public void addRage(long gameTime) {
        rageStacks = Math.min(BiologicalGene.MAX_RAGE, rageStacks(gameTime) + 1);
        rageExpiresAt = gameTime + BiologicalGene.RAGE_TICKS;
    }

    private void clearRage() {
        rageStacks = 0;
        rageExpiresAt = 0;
    }

    public boolean tickWaterHealing(boolean inWater) {
        if (!inWater || count(BiologicalGene.ABYSSAL_CIRCULATION) == 0) {
            waterTicks = 0;
            return false;
        }
        if (++waterTicks < BiologicalGene.WATER_HEAL_TICKS) {
            return false;
        }
        waterTicks = 0;
        return true;
    }

    public boolean startShockCooldown(long gameTime) {
        if (gameTime < shockReadyAt) {
            return false;
        }
        shockReadyAt = gameTime + BiologicalGene.SHOCK_COOLDOWN;
        return true;
    }

    private CompoundTag save() {
        var tag = new CompoundTag();
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < SIZE; slot++) {
            items.set(slot, slots.getItem(slot));
        }
        ContainerHelper.saveAllItems(tag, items);
        tag.putLong("ShockReadyAt", shockReadyAt);
        tag.putLong("SkyReadyAt", skyReadyAt);
        tag.putLong("FallenReadyAt", fallenReadyAt);
        tag.putLong("AnnihilationReadyAt", annihilationReadyAt);
        return tag;
    }

    private void load(CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int slot = 0; slot < SIZE; slot++) {
            slots.setItem(slot, items.get(slot));
        }
        shockReadyAt = Math.max(0, tag.getLong("ShockReadyAt"));
        skyReadyAt = Math.max(0, tag.getLong("SkyReadyAt"));
        fallenReadyAt = Math.max(0, tag.getLong("FallenReadyAt"));
        annihilationReadyAt = Math.max(0, tag.getLong("AnnihilationReadyAt"));
        clearSkyCombo();
        endFallenForm();
        clearRage();
        waterTicks = 0;
        recount();
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            var provider = new Provider();
            event.addCapability(new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "gene_modules"), provider);
            event.addListener(() -> provider.optional.invalidate());
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            get(event.getEntity()).load(get(event.getOriginal()).save());
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerGeneModules modules = new PlayerGeneModules();
        private LazyOptional<PlayerGeneModules> optional = LazyOptional.of(() -> modules);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> type, @Nullable Direction side) {
            if (type != TYPE) {
                return LazyOptional.empty();
            }
            if (!optional.isPresent()) {
                optional = LazyOptional.of(() -> modules);
            }
            return optional.cast();
        }

        @Override
        public CompoundTag serializeNBT() {
            return modules.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            modules.load(tag);
        }
    }
}
