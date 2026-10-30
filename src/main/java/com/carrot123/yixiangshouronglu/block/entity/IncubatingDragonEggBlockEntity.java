package com.carrot123.yixiangshouronglu.block.entity;

import com.carrot123.yixiangshouronglu.block.IncubatingDragonEggBlock;
import com.carrot123.yixiangshouronglu.entity.YoungEnderDragonEntity;
import com.carrot123.yixiangshouronglu.registry.ModBlockEntities;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class IncubatingDragonEggBlockEntity extends BlockEntity {
    private static final ResourceLocation VOID_STONE =
            new ResourceLocation("cataclysm", "void_stone");

    private static final int STAGE_TICKS = 400;

    private int ticksInStage;

    @Nullable
    private UUID ownerUUID;

    public IncubatingDragonEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATING_DRAGON_EGG.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        ownerUUID = owner;
        setChanged();
    }

    public static void serverTick(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState state,
            IncubatingDragonEggBlockEntity egg
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || egg.ownerUUID == null
                || !VOID_STONE.equals(
                        ForgeRegistries.BLOCKS.getKey(
                                level.getBlockState(pos.below()).getBlock()
                        )
                )) {
            return;
        }

        egg.ticksInStage++;

        if (egg.ticksInStage < STAGE_TICKS) {
            if (egg.ticksInStage % 20 == 0) {
                egg.setChanged();
            }
            return;
        }

        int stage = state.getValue(IncubatingDragonEggBlock.STAGE);

        if (stage < 3) {
            egg.ticksInStage = 0;

            serverLevel.setBlock(
                    pos,
                    state.setValue(IncubatingDragonEggBlock.STAGE, stage + 1),
                    3
            );

            serverLevel.playSound(
                    null,
                    pos,
                    SoundEvents.SNIFFER_EGG_CRACK,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.75F + stage * 0.09F
            );

            serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.7D,
                    pos.getZ() + 0.5D,
                    24,
                    0.35D,
                    0.3D,
                    0.35D,
                    0.07D
            );

            egg.setChanged();
            return;
        }

        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }

        YoungEnderDragonEntity dragon =
                ModEntities.YOUNG_ENDER_DRAGON.get().create(serverLevel);

        if (dragon == null) {
            return;
        }

        dragon.moveTo(
                pos.getX() + 0.5D,
                pos.getY() + 0.15D,
                pos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );

        dragon.setTame(true);
        dragon.setOwnerUUID(egg.ownerUUID);
        dragon.setMode(YoungEnderDragonEntity.Mode.FOLLOW);
        dragon.setHealth(dragon.getMaxHealth());
        dragon.setPersistenceRequired();

        if (!serverLevel.addFreshEntity(dragon)) {
            return;
        }

        serverLevel.playSound(
                null,
                pos,
                SoundEvents.SNIFFER_EGG_HATCH,
                SoundSource.BLOCKS,
                1.4F,
                0.95F
        );

        serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D,
                pos.getY() + 0.75D,
                pos.getZ() + 0.5D,
                65,
                0.45D,
                0.45D,
                0.45D,
                0.13D
        );

        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("StageTicks", ticksInStage);

        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ticksInStage = Math.max(
                0,
                Math.min(STAGE_TICKS, tag.getInt("StageTicks"))
        );

        ownerUUID = tag.hasUUID("Owner")
                ? tag.getUUID("Owner")
                : null;
    }
}
