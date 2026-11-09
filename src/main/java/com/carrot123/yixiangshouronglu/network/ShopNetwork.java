package com.carrot123.yixiangshouronglu.network;

import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.shop.ShopCapability;
import com.carrot123.yixiangshouronglu.shop.ShopCatalog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class ShopNetwork {
    private static final String VERSION = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("yixiangshouronglu", "shop"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ShopNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, State.class, State::encode, State::decode, State::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Action.class, Action::encode, Action::decode, Action::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sync(ServerPlayer player) {
        if (player.containerMenu instanceof CarrotKingShopMenu menu) {
            menu.syncState("ok");
        } else {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new State(-1, 0, 0, ShopCapability.get(player).save(), "ok", List.of(), List.of()));
        }
    }

    public static void sendState(ServerPlayer player, CarrotKingShopMenu menu) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new State(menu.containerId, menu.revision, menu.page,
                ShopCapability.get(player).save(), menu.status, menu.offers, menu.sellOffers));
    }

    public record State(int window, long revision, int page, CompoundTag wallet, String status, List<ShopCatalog.Offer> offers, List<ShopCatalog.Offer> sellOffers) {
        static void encode(State message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.window);
            buffer.writeLong(message.revision);
            buffer.writeVarInt(message.page);
            buffer.writeNbt(message.wallet);
            buffer.writeUtf(message.status, 64);
            buffer.writeVarInt(message.offers.size());

            for (var offer : message.offers) {
                buffer.writeUtf(offer.key(), 32);
                buffer.writeLong(offer.price());
                buffer.writeItem(offer.stack());
            }

            buffer.writeVarInt(message.sellOffers.size());

            for (var offer : message.sellOffers) {
                buffer.writeUtf(offer.key(), 32);
                buffer.writeLong(offer.price());
                buffer.writeItem(offer.stack());
            }
        }

        static State decode(FriendlyByteBuf buffer) {
            int window = buffer.readVarInt();
            long revision = buffer.readLong();
            int page = buffer.readVarInt();
            CompoundTag wallet = buffer.readNbt();
            String status = buffer.readUtf(64);
            int size = buffer.readVarInt();

            if (size < 0 || size > ShopCatalog.PRODUCTS.size() || page < 0 || page > 2) {
                throw new IllegalArgumentException("Invalid shop state");
            }

            List<ShopCatalog.Offer> offers = new ArrayList<>();

            for (int index = 0; index < size; index++) {
                offers.add(new ShopCatalog.Offer(buffer.readUtf(32), buffer.readLong(), buffer.readItem()));
            }

            int sellSize = buffer.readVarInt();

            if (sellSize < 0 || sellSize > 4) {
                throw new IllegalArgumentException("Invalid sell catalog");
            }

            List<ShopCatalog.Offer> sales = new ArrayList<>();

            for (int index = 0; index < sellSize; index++) {
                sales.add(new ShopCatalog.Offer(buffer.readUtf(32), buffer.readLong(), buffer.readItem()));
            }

            return new State(window, revision, page, wallet == null ? new CompoundTag() : wallet, status, List.copyOf(offers), List.copyOf(sales));
        }

        static void handle(State message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.carrot123.yixiangshouronglu.client.ShopClientPackets.apply(message)));
            context.get().setPacketHandled(true);
        }
    }

    public record Action(int window, long revision, long walletRevision, int action, int product) {
        static void encode(Action message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.window);
            buffer.writeLong(message.revision);
            buffer.writeLong(message.walletRevision);
            buffer.writeVarInt(message.action);
            buffer.writeVarInt(message.product);
        }

        static Action decode(FriendlyByteBuf buffer) {
            return new Action(buffer.readVarInt(), buffer.readLong(), buffer.readLong(), buffer.readVarInt(), buffer.readVarInt());
        }

        static void handle(Action message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                ServerPlayer player = context.get().getSender();

                if (player != null && player.containerMenu instanceof CarrotKingShopMenu menu) {
                    if (menu.containerId == message.window && menu.stillValid(player)) {
                        menu.act(player, message);
                    } else {
                        menu.syncState("invalid_session");
                    }
                }
            });

            context.get().setPacketHandled(true);
        }
    }
}
