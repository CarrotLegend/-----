package com.carrot123.yixiangshouronglu.gene.research;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.GeneResearchNetwork;
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

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneResearchCapability {
    public static final Capability<PlayerGeneResearch> TYPE = CapabilityManager.get(new CapabilityToken<>() {});

    private GeneResearchCapability() {}

    @Nullable public static PlayerGeneResearch get(Player player) {
        return player.getCapability(TYPE).orElse(null);
    }

    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            Provider provider = new Provider();
            event.addCapability(new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "gene_research"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            PlayerGeneResearch original = get(event.getOriginal());
            PlayerGeneResearch replacement = get(event.getEntity());
            if (original != null && replacement != null) {
                replacement.load(original.save());
            }
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        restore(event.getEntity());
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        restore(event.getEntity());
    }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        restore(event.getEntity());
    }

    private static void restore(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            GeneResearchService.restore(serverPlayer);
            GeneResearchNetwork.syncPlayer(serverPlayer);
        }
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerGeneResearch data = new PlayerGeneResearch();
        private LazyOptional<PlayerGeneResearch> optional = LazyOptional.of(() -> data);

        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability != TYPE) {
                return LazyOptional.empty();
            }
            if (!optional.isPresent()) {
                optional = LazyOptional.of(() -> data);
            }
            return optional.cast();
        }

        void invalidate() {
            optional.invalidate();
        }

        @Override public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override public void deserializeNBT(CompoundTag tag) {
            data.load(tag);
        }
    }
}
