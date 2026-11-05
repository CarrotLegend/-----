package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.gene.GeneCapability;
import com.carrot123.yixiangshouronglu.gene.GeneSkills;
import com.carrot123.yixiangshouronglu.gene.GeneCapacityUpgrades;
import net.minecraft.network.chat.Component;
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
import java.util.UUID;
import java.util.function.Supplier;

public final class GeneNetwork {
    private static final String VERSION = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("yixiangshouronglu", "genes"), () -> VERSION, VERSION::equals, VERSION::equals);
    private GeneNetwork() {}
    public static void register() {
        CHANNEL.registerMessage(4, DomainState.class, DomainState::encode, DomainState::decode, DomainState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Action.class, Action::encode, Action::decode, Action::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, SkillRequest.class, SkillRequest::encode, SkillRequest::decode, SkillRequest::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, SkillResult.class, SkillResult::encode, SkillResult::decode, SkillResult::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public record DomainState(UUID owner, int entity, ResourceLocation dimension, long session, int remaining) {
        public static void encode(DomainState message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.owner); buffer.writeVarInt(message.entity); buffer.writeResourceLocation(message.dimension); buffer.writeLong(message.session); buffer.writeVarInt(message.remaining);
        }
        public static DomainState decode(FriendlyByteBuf buffer) {
            var result = new DomainState(buffer.readUUID(), buffer.readVarInt(), buffer.readResourceLocation(), buffer.readLong(), buffer.readVarInt());
            if (result.remaining < 0 || result.remaining > 600) { throw new IllegalArgumentException("Invalid domain duration"); }
            return result;
        }
        static void handle(DomainState message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.carrot123.yixiangshouronglu.client.BlazingDomainVisuals.apply(message)));
            context.get().setPacketHandled(true);
        }
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
    public static void sync(ServerPlayer player, String status) {
        var genes = player.getCapability(GeneCapability.TYPE).orElse(null);
        if (genes == null) { player.displayClientMessage(Component.translatable("gene.yixiangshouronglu.capacity_upgrade_failed"), true); return; }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(genes.save(), status, GeneCapacityUpgrades.preview(genes.capacity())));
    }
    public record Snapshot(CompoundTag data, String status, GeneCapacityUpgrades.Preview capacityUpgrade) {
        static void encode(Snapshot message, FriendlyByteBuf buffer) { buffer.writeNbt(message.data); buffer.writeUtf(message.status, 64); message.capacityUpgrade.encode(buffer); }
        static Snapshot decode(FriendlyByteBuf buffer) { CompoundTag tag = buffer.readNbt(); return new Snapshot(tag == null ? new CompoundTag() : tag, buffer.readUtf(64), GeneCapacityUpgrades.Preview.decode(buffer)); }
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
                if (player != null) {
                    if (player.containerMenu instanceof GeneModificationMenu menu && menu.containerId == message.window && menu.stillValid(player)) { menu.act(player, message); }
                    else if (message.action == 5) { sync(player, "capacity_upgrade_invalid_menu"); }
                }
            }); context.get().setPacketHandled(true);
        }
    }
}
