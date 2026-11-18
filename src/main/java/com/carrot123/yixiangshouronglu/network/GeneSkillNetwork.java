package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.client.ChainLightningClient;
import com.carrot123.yixiangshouronglu.event.BiologicalGeneModuleEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class GeneSkillNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "gene_skill"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private GeneSkillNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, ActivateFallenForm.class, (message, buffer) -> {}, buffer -> new ActivateFallenForm(),
                (message, context) -> {
                    var packet = context.get();
                    var player = packet.getSender();
                    if (player != null) {
                        packet.enqueueWork(() -> BiologicalGeneModuleEffects.activateSkill(player));
                    }
                    packet.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, ChainArc.class, ChainArc::encode, ChainArc::decode, (arc, context) -> {
            var packet = context.get();
            packet.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ChainLightningClient.show(arc.start(), arc.end())));
            packet.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendChainArc(ServerLevel level, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        double lengthSquared = delta.lengthSqr();
        if (lengthSquared <= 0) {
            return;
        }
        var arc = new ChainArc(start, end);
        for (var player : level.players()) {
            var viewer = player.getEyePosition();
            double fraction = viewer.subtract(start).dot(delta) / lengthSquared;
            var closest = start.add(delta.scale(Math.max(0, Math.min(1, fraction))));
            if (viewer.distanceToSqr(closest) <= 64 * 64) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), arc);
            }
        }
    }

    public static void requestFallenForm() {
        CHANNEL.sendToServer(new ActivateFallenForm());
    }

    private record ActivateFallenForm() {}

    private record ChainArc(Vec3 start, Vec3 end) {
        private void encode(FriendlyByteBuf buffer) {
            writePosition(buffer, start);
            writePosition(buffer, end);
        }

        private static ChainArc decode(FriendlyByteBuf buffer) {
            return new ChainArc(readPosition(buffer), readPosition(buffer));
        }

        private static void writePosition(FriendlyByteBuf buffer, Vec3 position) {
            buffer.writeDouble(position.x);
            buffer.writeDouble(position.y);
            buffer.writeDouble(position.z);
        }

        private static Vec3 readPosition(FriendlyByteBuf buffer) {
            return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }
    }
}
