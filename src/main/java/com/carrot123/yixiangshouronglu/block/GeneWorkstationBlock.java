package com.carrot123.yixiangshouronglu.block;

import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.gene.GeneCapability;
import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class GeneWorkstationBlock extends Block implements EntityBlock {
    private static final VoxelShape SYNTHESIZER_SHAPE = Shapes.block();

    private final boolean synthesizer;
    public GeneWorkstationBlock(Properties properties, boolean synthesizer) { super(properties); this.synthesizer = synthesizer; registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return synthesizer ? SYNTHESIZER_SHAPE : super.getShape(state, level, pos, context); }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return synthesizer ? new GeneSynthesizerBlockEntity(pos, state) : null; }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && synthesizer && type == ModBlockEntities.GENE_SYNTHESIZER.get() ? (world, pos, blockState, entity) -> GeneSynthesizerBlockEntity.tick(world, pos, blockState, (GeneSynthesizerBlockEntity) entity) : null;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (synthesizer && level.getBlockEntity(pos) instanceof GeneSynthesizerBlockEntity machine) { NetworkHooks.openScreen(serverPlayer, machine, pos); }
            else if (!synthesizer) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, owner) -> new GeneModificationMenu(id, inventory, pos), Component.translatable("block.yixiangshouronglu.gene_modification_chamber")), pos);
                GeneNetwork.sync(serverPlayer, "ok");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof GeneSynthesizerBlockEntity machine) {
            for (int slot = 0; slot < 5; slot++) { Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), machine.inventory.extractItem(slot, Integer.MAX_VALUE, false)); }
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
