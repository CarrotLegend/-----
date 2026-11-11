package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.client.LanternActivation;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class LanternNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "lantern"),
            () -> VERSION, VERSION::equals, VERSION::equals
    );

    private LanternNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, Activation.class, (message, buffer) -> {}, buffer -> new Activation(),
                (message, context) -> {
                    var packet = context.get();
                    packet.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> LanternActivation::play));
                    packet.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void activate(ServerPlayer player) {
        player.serverLevel().getChunkSource().broadcast(player, new ClientboundEntityEventPacket(player, (byte) 35));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Activation());
    }

    private record Activation() {
    }
}
