package com.carrot123.yixiangshouronglu.block;

import com.carrot123.yixiangshouronglu.block.entity.AmethystCrabLureBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class AmethystCrabLureBlock extends BaseEntityBlock {
    private static final int SUMMON_DELAY = 40;
    private static final ResourceLocation CRAB_ID =
            new ResourceLocation("cataclysm", "amethyst_crab");

    public AmethystCrabLureBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmethystCrabLureBlockEntity(pos, state);
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean isMoving
    ) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        if (!level.getBiome(pos).is(Biomes.LUSH_CAVES)) {
            return;
        }

        level.scheduleTick(pos, this, SUMMON_DELAY);
    }

    @Override
    public void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }

        if (!level.getBiome(pos).is(Biomes.LUSH_CAVES)) {
            return;
        }

        if (!ForgeRegistries.ENTITY_TYPES.containsKey(CRAB_ID)) {
            return;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(CRAB_ID);

        if (type == null) {
            return;
        }

        Entity entity = type.create(level);

        if (!(entity instanceof Mob crab)) {
            return;
        }

        crab.moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );

        crab.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(pos),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );

        if (level.addFreshEntity(crab)) {
            level.removeBlock(pos, false);
        }
    }
}