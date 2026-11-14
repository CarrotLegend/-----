package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneCapability {
    public static final Capability<PlayerGenes> TYPE = CapabilityManager.get(new CapabilityToken<>() {});
    private GeneCapability() {}
    public static PlayerGenes get(Player player) { return player.getCapability(TYPE).orElseThrow(() -> new IllegalStateException("Missing player genes")); }
    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider provider = new Provider();
            event.addCapability(new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "genes"), provider);
            event.addListener(provider::invalidate);
        }
    }
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            get(player).returnItems(player);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.isAlive()
                && player.tickCount % 20 == 0) {
            get(player).returnItems(player);
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            get(event.getEntity()).load(get(original).save());
        } finally {
            original.invalidateCaps();
        }
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerGenes data = new PlayerGenes();
        private LazyOptional<PlayerGenes> value = LazyOptional.of(() -> data);
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability != TYPE) { return LazyOptional.empty(); }
            if (!value.isPresent()) { value = LazyOptional.of(() -> data); }
            return value.cast();
        }
        private void invalidate() { value.invalidate(); }
        @Override public CompoundTag serializeNBT() { return data.save(); }
        @Override public void deserializeNBT(CompoundTag tag) { data.load(tag); }
    }
}
