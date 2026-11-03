package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.gene.GeneCapability;
import com.carrot123.yixiangshouronglu.gene.GeneSkills;
import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Supplier;

public final class GeneNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("yixiangshouronglu", "genes"), () -> VERSION, VERSION::equals, VERSION::equals);
    private GeneNetwork() {}
    public static void register() {
        CHANNEL.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Action.class, Action::encode, Action::decode, Action::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, SkillRequest.class, SkillRequest::encode, SkillRequest::decode, SkillRequest::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, SkillResult.class, SkillResult::encode, SkillResult::decode, SkillResult::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void skillResult(ServerPlayer player, GeneSkills.Result result) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SkillResult(result.skill(), result.status(), result.readyAt())); }
    public record SkillRequest(ResourceLocation skill) {
        static void encode(SkillRequest message, FriendlyByteBuf buffer) { buffer.writeResourceLocation(message.skill); }
        static SkillRequest decode(FriendlyByteBuf buffer) { return new SkillRequest(buffer.readResourceLocation()); }
        static void handle(SkillRequest message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> { ServerPlayer player = context.get().getSender(); if (player != null) { GeneSkills.handle(player, message.skill); } });
            context.get().setPacketHandled(true);
        }
    }
    public record SkillResult(ResourceLocation skill, String status, long readyAt) {
        static void encode(SkillResult message, FriendlyByteBuf buffer) { buffer.writeResourceLocation(message.skill); buffer.writeUtf(message.status, 64); buffer.writeLong(message.readyAt); }
        static SkillResult decode(FriendlyByteBuf buffer) { return new SkillResult(buffer.readResourceLocation(), buffer.readUtf(64), buffer.readLong()); }
        static void handle(SkillResult message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.carrot123.yixiangshouronglu.client.GeneClientPackets.skillResult(message)));
            context.get().setPacketHandled(true);
        }
    }
    public static void sync(ServerPlayer player, String status) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(GeneCapability.get(player).save(), status)); }
    public record Snapshot(CompoundTag data, String status) {
        static void encode(Snapshot message, FriendlyByteBuf buffer) { buffer.writeNbt(message.data); buffer.writeUtf(message.status, 64); }
        static Snapshot decode(FriendlyByteBuf buffer) { CompoundTag tag = buffer.readNbt(); return new Snapshot(tag == null ? new CompoundTag() : tag, buffer.readUtf(64)); }
        static void handle(Snapshot message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.carrot123.yixiangshouronglu.client.GeneClientPackets.apply(message)));
            context.get().setPacketHandled(true);
        }
    }
    public record Action(int window, long revision, int action, int inventorySlot, int bodySlot) {
        static void encode(Action message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.window); buffer.writeLong(message.revision); buffer.writeVarInt(message.action); buffer.writeVarInt(message.inventorySlot); buffer.writeVarInt(message.bodySlot); }
        static Action decode(FriendlyByteBuf buffer) { return new Action(buffer.readVarInt(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()); }
        static void handle(Action message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                ServerPlayer player = context.get().getSender();
                if (player != null && player.containerMenu instanceof GeneModificationMenu menu && menu.containerId == message.window && menu.stillValid(player)) { menu.act(player, message); }
            }); context.get().setPacketHandled(true);
        }
    }
}
