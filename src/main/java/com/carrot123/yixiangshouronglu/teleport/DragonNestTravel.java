package com.carrot123.yixiangshouronglu.teleport;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DragonNestTravel {
    private static final ResourceKey<Level> DRAGON_NEST = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "dragon_nest"));
    private static final String DATA_KEY = AnomalousContainmentChronicles.MOD_ID + ":dragon_nest_teleporter";
    private static final TicketType<UUID> TICKET = TicketType.create(DATA_KEY, UUID::compareTo, 220);
    private static final Map<UUID, Request> REQUESTS = new LinkedHashMap<>();
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_WIDTH = SEARCH_RADIUS * 2 + 1;
    private static final int SEARCH_COUNT = SEARCH_WIDTH * SEARCH_WIDTH;
    private static int nextRequest;

    private DragonNestTravel() {
    }

    public static boolean canStart(ServerPlayer player) {
        if (REQUESTS.containsKey(player.getUUID())) {
            message(player, "busy");
            return false;
        }
        if (!allowed(player)) {
            message(player, "unavailable");
            return false;
        }
        return true;
    }

    public static void begin(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (!canStart(player) || !stack.is(ModItems.DRAGON_NEST_TELEPORTER.get())
                || player.getItemInHand(hand) != stack || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        boolean returning = player.level().dimension().equals(DRAGON_NEST);
        ServerLevel target = player.server.getLevel(returning ? Level.OVERWORLD : DRAGON_NEST);
        if (target == null) {
            message(player, "missing_dimension");
            return;
        }
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(DATA_KEY);
        Position preferred = Position.read(data.getCompound(returning ? "OverworldReturn" : "DragonNestReturn"));
        if (preferred != null && !withinBounds(player, target, preferred.feet())) {
            preferred = null;
        }
        Request request = new Request(player, hand, stack, target, preferred, returning);
        REQUESTS.put(player.getUUID(), request);
        request.load(preferred == null ? request.spawn : BlockPos.containing(preferred.feet()));
        message(player, "searching");
    }

    private static boolean allowed(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && !player.hasDisconnected() && !player.isSleeping()
                && player.canChangeDimensions() && !player.isChangingDimension();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || REQUESTS.isEmpty()) {
            return;
        }
        List<Request> requests = new ArrayList<>(REQUESTS.values());
        int start = Math.floorMod(nextRequest++, requests.size());
        int budget = 64;
        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get((start + i) % requests.size());
            ServerPlayer player = request.player;
            if (player.server != event.getServer() || REQUESTS.get(player.getUUID()) != request) {
                continue;
            }
            if (!allowed(player) || player.serverLevel() != request.origin || player.getItemInHand(request.hand) != request.stack
                    || player.getCooldowns().isOnCooldown(request.stack.getItem())
                    || player.server.getLevel(request.target.dimension()) != request.target) {
                finish(request, "cancelled");
                continue;
            }
            if (--request.remaining <= 0) {
                finish(request, "no_safe_position");
                continue;
            }
            if (budget <= 0 || !request.ready()) {
                continue;
            }
            if (request.preferred != null) {
                budget--;
                Position preferred = request.preferred;
                request.preferred = null;
                if (safe(player, request.target, preferred.feet())) {
                    teleport(request, preferred);
                    continue;
                }
                request.load(request.spawn);
                continue;
            }
            int allowance = Math.min(16, budget);
            while (allowance-- > 0 && request.cursor < SEARCH_COUNT) {
                budget--;
                int index = (request.offset + request.cursor++ * 541) % SEARCH_COUNT;
                int x = request.spawn.getX() + index % SEARCH_WIDTH - SEARCH_RADIUS;
                int z = request.spawn.getZ() + index / SEARCH_WIDTH - SEARCH_RADIUS;
                int y = request.target.getChunkSource().getChunkNow(x >> 4, z >> 4)
                        .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
                BlockPos support = new BlockPos(x, y, z);
                if (request.target.isOutsideBuildHeight(support)) {
                    continue;
                }
                var shape = request.target.getBlockState(support).getCollisionShape(request.target, support, CollisionContext.of(player));
                if (shape.isEmpty()) {
                    continue;
                }
                Vec3 feet = new Vec3(x + 0.5, y + shape.max(Direction.Axis.Y), z + 0.5);
                if (safe(player, request.target, feet)) {
                    teleport(request, new Position(feet, player.getYRot(), player.getXRot()));
                    break;
                }
            }
            if (REQUESTS.get(player.getUUID()) == request && request.cursor >= SEARCH_COUNT) {
                finish(request, "no_safe_position");
            }
        }
    }

    private static boolean withinBounds(ServerPlayer player, ServerLevel level, Vec3 feet) {
        if (!Double.isFinite(feet.x) || !Double.isFinite(feet.y) || !Double.isFinite(feet.z)
                || Math.abs(feet.x) > 30000000 || Math.abs(feet.z) > 30000000) {
            return false;
        }
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(feet);
        return box.minY >= level.getMinBuildHeight() + 1 && box.maxY <= level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(box);
    }

    private static boolean safe(ServerPlayer player, ServerLevel level, Vec3 feet) {
        if (!withinBounds(player, level, feet)) {
            return false;
        }
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(feet);
        BlockPos min = BlockPos.containing(box.minX - 1, box.minY - 0.05, box.minZ - 1);
        BlockPos max = BlockPos.containing(box.maxX + 1, box.maxY, box.maxZ + 1);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
                if (!loaded(level, x, z)) {
                    return false;
                }
            }
        }
        BlockPos bodyMin = BlockPos.containing(box.minX, box.minY - 0.05, box.minZ);
        BlockPos bodyMax = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(bodyMin, bodyMax)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || hazardous(state)) {
                return false;
            }
        }
        if (!level.noCollision(player, box)
                || !level.getEntities(player, box, entity -> !entity.isRemoved() && !entity.isSpectator() && entity.isPickable()).isEmpty()) {
            return false;
        }
        for (double x : new double[]{box.minX + 0.001, box.maxX - 0.001}) {
            for (double z : new double[]{box.minZ + 0.001, box.maxZ - 0.001}) {
                AABB foot = new AABB(x - 0.001, feet.y - 0.05, z - 0.001, x + 0.001, feet.y, z + 0.001);
                boolean supported = false;
                for (var shape : level.getBlockCollisions(player, foot)) {
                    if (!shape.isEmpty()) {
                        supported = true;
                        break;
                    }
                }
                if (!supported) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hazardous(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE)
                || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)
                || state.is(BlockTags.LEAVES);
    }

    private static boolean loaded(ServerLevel level, int x, int z) {
        var chunk = level.getChunkSource().getChunkNow(x, z);
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)
                && level.areEntitiesLoaded(ChunkPos.asLong(x, z));
    }

    private static void teleport(Request request, Position destination) {
        ServerPlayer player = request.player;
        Position from = new Position(player.position(), player.getYRot(), player.getXRot());
        REQUESTS.remove(player.getUUID());
        try {
            player.teleportTo(request.target, destination.feet.x, destination.feet.y, destination.feet.z, destination.yaw, destination.pitch);
            if (player.serverLevel() != request.target || player.position().distanceToSqr(destination.feet) > 0.01) {
                message(player, "failed");
                return;
            }
            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            CompoundTag data = persisted.getCompound(DATA_KEY);
            if (request.returning) {
                data.put("DragonNestReturn", from.write());
            } else if (request.origin.dimension().equals(Level.OVERWORLD)) {
                data.put("OverworldReturn", from.write());
            } else {
                data.remove("OverworldReturn");
            }
            persisted.put(DATA_KEY, data);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            player.fallDistance = 0;
            player.setDeltaMovement(Vec3.ZERO);
            player.getCooldowns().addCooldown(ModItems.DRAGON_NEST_TELEPORTER.get(), 40);
        } finally {
            request.release();
        }
    }

    private static void finish(Request request, String reason) {
        REQUESTS.remove(request.player.getUUID(), request);
        request.release();
        if (reason != null && !request.player.hasDisconnected()) {
            message(request.player, reason);
        }
    }

    private static void message(ServerPlayer player, String reason) {
        player.displayClientMessage(Component.translatable("message.yixiangshouronglu.dragon_nest_teleporter." + reason), true);
    }

    private static void cancel(Player player) {
        Request request = REQUESTS.get(player.getUUID());
        if (request != null && request.player == player) {
            finish(request, null);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        cancel(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Request request : new ArrayList<>(REQUESTS.values())) {
            if (request.player.server == event.getServer()) {
                finish(request, null);
            }
        }
        nextRequest = 0;
    }

    private record Position(Vec3 feet, float yaw, float pitch) {
        private CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("X", feet.x);
            tag.putDouble("Y", feet.y);
            tag.putDouble("Z", feet.z);
            tag.putFloat("Yaw", yaw);
            tag.putFloat("Pitch", pitch);
            return tag;
        }

        private static Position read(CompoundTag tag) {
            for (String key : List.of("X", "Y", "Z", "Yaw", "Pitch")) {
                if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    return null;
                }
            }
            Vec3 feet = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            float yaw = tag.getFloat("Yaw"), pitch = tag.getFloat("Pitch");
            return Float.isFinite(yaw) && Float.isFinite(pitch) ? new Position(feet, yaw, pitch) : null;
        }
    }

    private static final class Request {
        private final ServerPlayer player;
        private final InteractionHand hand;
        private final ItemStack stack;
        private final ServerLevel origin;
        private final ServerLevel target;
        private final BlockPos spawn;
        private final boolean returning;
        private final int offset;
        private final List<ChunkPos> tickets = new ArrayList<>();
        private Position preferred;
        private int remaining = 200;
        private int cursor;

        private Request(ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel target, Position preferred, boolean returning) {
            this.player = player;
            this.hand = hand;
            this.stack = stack;
            this.origin = player.serverLevel();
            this.target = target;
            this.preferred = preferred;
            this.returning = returning;
            var data = target.getLevelData();
            BlockPos spawn = new BlockPos(data.getXSpawn(), data.getYSpawn(), data.getZSpawn());
            this.spawn = target.getWorldBorder().clampToBounds(spawn.getX(), spawn.getY(), spawn.getZ());
            this.offset = Math.floorMod(player.getUUID().hashCode(), SEARCH_COUNT);
        }

        private void load(BlockPos anchor) {
            release();
            ChunkPos center = new ChunkPos(anchor);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    ChunkPos chunk = new ChunkPos(center.x + x, center.z + z);
                    target.getChunkSource().addRegionTicket(TICKET, chunk, 0, player.getUUID());
                    tickets.add(chunk);
                }
            }
        }

        private boolean ready() {
            for (ChunkPos chunk : tickets) {
                if (!loaded(target, chunk.x, chunk.z)) {
                    return false;
                }
            }
            return true;
        }

        private void release() {
            for (ChunkPos chunk : tickets) {
                target.getChunkSource().removeRegionTicket(TICKET, chunk, 0, player.getUUID());
            }
            tickets.clear();
        }
    }
}
