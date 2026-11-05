package com.carrot123.yixiangshouronglu.shop;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class ShopCapability {
    public static final Capability<PlayerShopData> TYPE = CapabilityManager.get(new CapabilityToken<>() {});
    private ShopCapability() {}
    public static PlayerShopData get(Player player) {
        return player.getCapability(TYPE).orElseThrow(() -> new IllegalStateException("Missing player shop data"));
    }
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider provider = new Provider();
            event.addCapability(new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "shop"), provider);
            event.addListener(provider::invalidate);
        }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try { get(event.getEntity()).load(get(event.getOriginal()).save()); }
        finally { event.getOriginal().invalidateCaps(); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { ShopNetwork.sync(player); }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { ShopNetwork.sync(player); }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { ShopNetwork.sync(player); }
    }
    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerShopData data = new PlayerShopData();
        private LazyOptional<PlayerShopData> value = LazyOptional.of(() -> data);
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
