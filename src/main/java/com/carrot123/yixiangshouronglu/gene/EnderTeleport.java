package com.carrot123.yixiangshouronglu.gene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.*;

public final class EnderTeleport {
    private EnderTeleport() {}
    public static BlockHitResult trace(ServerPlayer player, double range) {
        ServerLevel level = player.serverLevel(); Vec3 from = player.getEyePosition(); Vec3 to = from.add(player.getLookAngle().scale(range));
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, context, (clip, pos) -> {
            if (!loaded(level, pos) || level.isOutsideBuildHeight(pos)) { return BlockHitResult.miss(Vec3.atCenterOf(pos), Direction.UP, pos.immutable()); }
            BlockState state = level.getBlockState(pos);
            return level.clipWithInteractionOverride(from, to, pos, clip.getBlockShape(state, level, pos), state);
        }, clip -> BlockHitResult.miss(to, Direction.UP, BlockPos.containing(to)));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }
    public static Optional<Vec3> destination(ServerPlayer player, BlockHitResult hit, double range) {
        ServerLevel level = player.serverLevel(); List<Vec3> candidates = new ArrayList<>();
        BlockPos origin = BlockPos.containing(hit.getLocation());
        for (int x = -2; x <= 2; x++) { for (int z = -2; z <= 2; z++) { for (int y = -3; y <= 3; y++) {
            BlockPos support = origin.offset(x, y, z);
            if (!loaded(level, support) || level.isOutsideBuildHeight(support)) { continue; }
            var shape = level.getBlockState(support).getCollisionShape(level, support, CollisionContext.of(player));
            if (shape.isEmpty()) { continue; }
            Vec3 feet = new Vec3(support.getX() + 0.5, support.getY() + shape.max(Direction.Axis.Y), support.getZ() + 0.5);
            if (hit.getDirection().getAxis().isHorizontal()) {
                Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
                double side = feet.subtract(hit.getLocation()).dot(normal);
                if (side < 0.30 && (side < -0.51 || feet.y < hit.getBlockPos().getY() + 1 - 1.0E-6)) { continue; }
            }
            if (safe(player, feet, range)) { candidates.add(feet); }
        } } }
        return candidates.stream().min(Comparator.comparingDouble(feet -> feet.distanceToSqr(hit.getLocation())));
    }
    public static boolean safe(ServerPlayer player, Vec3 feet, double range) {
        ServerLevel level = player.serverLevel();
        if (!Double.isFinite(feet.x) || !Double.isFinite(feet.y) || !Double.isFinite(feet.z) || feet.distanceToSqr(player.position()) > range * range || feet.distanceToSqr(player.position()) < 0.01) { return false; }
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(feet);
        if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(box)) { return false; }
        BlockPos min = BlockPos.containing(box.minX, box.minY - 0.01, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) { if (!loaded(level, pos)) { return false; } }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || hazardous(state)) { return false; }
        }
        if (!level.noCollision(player, box) || !level.getEntities(player, box, entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).isEmpty()) { return false; }
        AABB below = new AABB(box.minX, box.minY - 0.05, box.minZ, box.maxX, box.minY, box.maxZ);
        boolean supported = false;
        for (var shape : level.getBlockCollisions(player, below)) { if (!shape.isEmpty()) { supported = true; break; } }
        return supported;
    }
    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos.offset(-1, 0, -1)) && level.hasChunkAt(pos.offset(-1, 0, 1)) && level.hasChunkAt(pos.offset(1, 0, -1)) && level.hasChunkAt(pos.offset(1, 0, 1));
    }
    private static boolean hazardous(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY);
    }
    public static String execute(ServerPlayer player) {
        int range = GeneSkillConfig.TELEPORT_RANGE.get(); BlockHitResult hit = trace(player, range);
        if (hit == null) { return "no_target"; }
        var target = destination(player, hit, range); if (target.isEmpty()) { return "no_safe_position"; }
        Vec3 from = player.position(), to = target.get(); ServerLevel level = player.serverLevel();
        player.teleportTo(to.x, to.y, to.z); player.fallDistance = 0;
        for (Vec3 pos : List.of(from, to)) {
            level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.9, pos.z, 20, 0.3, 0.6, 0.3, 0.1);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
        }
        return "ok";
    }
}
