package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PanaceaRitualEvents {

    private static final ResourceLocation SOUL_CORE =
            new ResourceLocation("theinkarena", "soul_core");

    private static final ResourceLocation SOUL_LAMP =
            new ResourceLocation("theinkarena", "soul_lamp");

    private static final ResourceLocation SOUL_BRUSH =
            new ResourceLocation("theinkarena", "soul_brush");

    private static final ResourceLocation PANACEA =
            new ResourceLocation("yixiangshouronglu", "panacea");

    private PanaceaRitualEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (event.isCanceled()
                || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!SOUL_BRUSH.equals(
                ForgeRegistries.ITEMS.getKey(
                        event.getItemStack().getItem()
                ))) {
            return;
        }

        Level level = event.getLevel();
        BlockPos center = event.getPos();

        if (!isBlock(level, center, SOUL_LAMP)) {
            return;
        }

        Direction axisDirection;

        if (matchesStructure(level, center, Direction.EAST)) {
            axisDirection = Direction.EAST;
        } else if (matchesStructure(
                level, center, Direction.SOUTH)) {
            axisDirection = Direction.SOUTH;
        } else {
            return;
        }

        Item panacea = ForgeRegistries.ITEMS.getValue(PANACEA);

        if (panacea == null || panacea == Items.AIR) {
            return;
        }

        event.setCancellationResult(
                InteractionResult.sidedSuccess(
                        level.isClientSide
                )
        );
        event.setCanceled(true);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        List<BlockPos> parts = List.of(
                center,
                center.above(),
                center.relative(axisDirection),
                center.relative(axisDirection.getOpposite()),
                center.below()
        );

        for (BlockPos part : parts) {
            serverLevel.setBlock(
                    part,
                    Blocks.AIR.defaultBlockState(),
                    2
            );
        }

        for (BlockPos part : parts) {
            serverLevel.updateNeighborsAt(part, Blocks.AIR);
        }

        ItemEntity reward = new ItemEntity(
                serverLevel,
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D,
                new ItemStack(panacea)
        );

        reward.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(reward);
    }

    private static boolean matchesStructure(
            Level level,
            BlockPos center,
            Direction side
    ) {
        Direction opposite = side.getOpposite();

        if (!isBlock(level, center, SOUL_LAMP)) {
            return false;
        }

        if (!isBlock(level, center.above(), SOUL_CORE)) {
            return false;
        }

        if (!level.getBlockState(center.below())
                .is(Blocks.SOUL_SAND)) {
            return false;
        }

        if (!level.getBlockState(center.relative(side))
                .is(Blocks.SOUL_SAND)) {
            return false;
        }

        if (!level.getBlockState(center.relative(opposite))
                .is(Blocks.SOUL_SAND)) {
            return false;
        }

        if (!level.getBlockState(
                center.above().relative(side)).isAir()) {
            return false;
        }

        if (!level.getBlockState(
                center.above().relative(opposite)).isAir()) {
            return false;
        }

        if (!level.getBlockState(
                center.below().relative(side)).isAir()) {
            return false;
        }

        return level.getBlockState(
                center.below().relative(opposite)).isAir();
    }

    private static boolean isBlock(
            Level level,
            BlockPos pos,
            ResourceLocation id
    ) {
        return id.equals(
                ForgeRegistries.BLOCKS.getKey(
                        level.getBlockState(pos).getBlock()
                )
        );
    }
}
