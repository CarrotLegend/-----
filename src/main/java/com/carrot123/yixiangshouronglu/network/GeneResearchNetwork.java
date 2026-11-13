package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.client.GeneResearchClient;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchCapability;
import com.carrot123.yixiangshouronglu.menu.GeneResearchTableMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "yixiangshouronglu", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneResearchNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("yixiangshouronglu", "gene_research"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private GeneResearchNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, Action.class, Action::encode, Action::decode, Action::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, MenuState.class, MenuState::encode, MenuState::decode, MenuState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, PlayerState.class, PlayerState::encode, PlayerState::decode, PlayerState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    @SubscribeEvent public static void opened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getContainer() instanceof GeneResearchTableMenu menu) {
            syncPlayer(player);
            menu.respond(player, null);
        }
    }

    public static void send(Action action) {
        CHANNEL.sendToServer(action);
    }

    public static void syncMenu(ServerPlayer player, MenuState state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
    }

    public static void syncPlayer(ServerPlayer player) {
        var data = GeneResearchCapability.get(player);
        if (data != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerState(data.snapshot()));
        }
    }

    public record Action(int container, UUID session, long revision, int action, int page, String target, String extract) {
        static void encode(Action packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.container);
            buf.writeUUID(packet.session);
            buf.writeLong(packet.revision);
            buf.writeByte(packet.action);
            buf.writeByte(packet.page);
            buf.writeUtf(packet.target, 256);
            buf.writeUtf(packet.extract, 256);
        }

        static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readVarInt(), buf.readUUID(), buf.readLong(), buf.readUnsignedByte(),
                    buf.readUnsignedByte(), buf.readUtf(256), buf.readUtf(256));
        }

        static void handle(Action packet, Supplier<NetworkEvent.Context> context) {
            var ctx = context.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null && player.containerMenu instanceof GeneResearchTableMenu menu && menu.containerId == packet.container) {
                    menu.act(player, packet);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public record MenuState(int container, UUID session, long revision, int page, String selection, String status, int gain) {
        static void encode(MenuState packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.container);
            buf.writeUUID(packet.session);
            buf.writeLong(packet.revision);
            buf.writeByte(packet.page);
            buf.writeUtf(packet.selection, 256);
            buf.writeUtf(packet.status, 64);
            buf.writeVarInt(packet.gain);
        }

        static MenuState decode(FriendlyByteBuf buf) {
            return new MenuState(buf.readVarInt(), buf.readUUID(), buf.readLong(), buf.readUnsignedByte(),
                    buf.readUtf(256), buf.readUtf(64), buf.readVarInt());
        }

        static void handle(MenuState packet, Supplier<NetworkEvent.Context> context) {
            var ctx = context.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GeneResearchClient.apply(packet)));
            ctx.setPacketHandled(true);
        }
    }

    public record PlayerState(CompoundTag data) {
        static void encode(PlayerState packet, FriendlyByteBuf buf) {
            buf.writeNbt(packet.data);
        }

        static PlayerState decode(FriendlyByteBuf buf) {
            CompoundTag data = buf.readNbt();
            return new PlayerState(data == null ? new CompoundTag() : data);
        }

        static void handle(PlayerState packet, Supplier<NetworkEvent.Context> context) {
            var ctx = context.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GeneResearchClient.apply(packet)));
            ctx.setPacketHandled(true);
        }
    }
}
