package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.client.LegendaryCarrotKingSummonerRenderer;
import com.carrot123.yixiangshouronglu.entity.LegendaryCarrotKingEntity;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class LegendaryCarrotKingSummonerItem extends Item {
    public LegendaryCarrotKingSummonerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face);
        return summon(level, player, context.getItemInHand(), pos, face)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(2.0D);
        BlockPos pos = BlockPos.containing(player.position().add(forward));
        return summon(serverLevel, player, stack, pos, Direction.UP)
                ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private static boolean summon(ServerLevel level, Player player, ItemStack stack, BlockPos pos, Direction face) {
        EntityType<LegendaryCarrotKingEntity> type = ModEntities.LEGENDARY_CARROT_KING.get();
        AABB box = type.getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (!player.isAlive() || stack.isEmpty() || !canSpawnAt(level, player, stack, pos, face, box)) {
            return summonFailed(player);
        }

        LegendaryCarrotKingEntity king = type.create(level, null, null, pos, MobSpawnType.SPAWN_EGG, false, false);
        if (king == null) {
            return summonFailed(player);
        }
        if (stack.hasCustomHoverName()) {
            king.setCustomName(stack.getHoverName());
        }
        if (!level.addFreshEntity(king)) {
            return summonFailed(player);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    private static boolean canSpawnAt(ServerLevel level, Player player, ItemStack stack, BlockPos pos,
                                      Direction face, AABB box) {
        BlockPos support = pos.below();
        WorldBorder border = level.getWorldBorder();
        if (support.getY() < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                || !border.isWithinBounds(box.minX, box.minZ) || !border.isWithinBounds(box.maxX, box.maxZ)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return false;
        }
        if (!level.hasChunksAt(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
            return false;
        }
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                && !level.containsAnyLiquid(box) && level.noCollision(box)
                && level.getEntities((Entity) null, box, entity -> entity.isPickable() && !entity.isSpectator()).isEmpty();
    }

    private static boolean summonFailed(Player player) {
        player.displayClientMessage(Component.translatable("message.yixiangshouronglu.legendary_carrot_king_summoner.spawn_failed"), true);
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        LegendaryCarrotKingSummonerRenderer.initialize(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.legendary_carrot_king_summoner")
                .withStyle(ChatFormatting.GOLD));
    }
}
