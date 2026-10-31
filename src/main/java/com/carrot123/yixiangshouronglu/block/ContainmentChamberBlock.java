package com.carrot123.yixiangshouronglu.block;

import com.carrot123.yixiangshouronglu.block.entity.ContainmentChamberBlockEntity;
import com.carrot123.yixiangshouronglu.item.ResonanceCoreItem;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ContainmentChamberBlock extends BaseEntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ContainmentChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(HALF, DoubleBlockHalf.LOWER)
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER
                ? RenderShape.MODEL
                : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();

        if (above.getY() >= context.getLevel().getMaxBuildHeight()
                || !context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {
        level.setBlock(
                pos.above(),
                state.setValue(HALF, DoubleBlockHalf.UPPER),
                3
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new ContainmentChamberBlockEntity(pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.CONTAINMENT_CHAMBER.get(),
                ContainmentChamberBlockEntity::serverTick
        );
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? pos
                : pos.below();

        if (!(level.getBlockEntity(lowerPos) instanceof ContainmentChamberBlockEntity chamber)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.RESONANCE_CORE.get())) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ResonanceCoreItem.bindOrUnbind(
                        held,
                        serverPlayer,
                        lowerPos
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ItemStack returned = chamber.extractSoul();

                if (!returned.isEmpty() && !player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (held.is(ModItems.SOUL.get())) {
            if (!level.isClientSide && chamber.insertSoul(held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, chamber, lowerPos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide) {
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? pos
                    : pos.below();

            if (level.getBlockEntity(lowerPos) instanceof ContainmentChamberBlockEntity chamber) {
                ItemStack extracted = chamber.extractSoul();

                if (!extracted.isEmpty()) {
                    Block.popResource(level, lowerPos, extracted);
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? pos
                    : pos.below();

            if (level.getBlockEntity(lowerPos) instanceof ContainmentChamberBlockEntity chamber) {
                ItemStack extracted = chamber.extractSoul();

                if (!extracted.isEmpty()) {
                    Block.popResource(level, lowerPos, extracted);
                }

                Containers.dropContents(level, lowerPos, chamber);
                chamber.clearContent();
            }

            BlockPos upperPos = lowerPos.above();

            restoreBlock(level, lowerPos, Blocks.IRON_BLOCK.defaultBlockState());
            restoreBlock(level, upperPos, Blocks.GLASS.defaultBlockState());
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void restoreBlock(Level level, BlockPos pos, BlockState restoredState) {
        BlockState currentState = level.getBlockState(pos);

        if (currentState.is(this) || currentState.isAir()) {
            level.setBlock(pos, restoredState, 3);
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
}
