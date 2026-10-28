package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.block.ContainmentChamberBlock;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContainmentChamberFormationHandler {
    private ContainmentChamberFormationHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown() || !event.getItemStack().is(Items.REDSTONE)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.IRON_BLOCK) || !level.getBlockState(pos.above()).is(Blocks.GLASS)) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        BlockState lower = ModBlocks.CONTAINMENT_CHAMBER.get().defaultBlockState().setValue(ContainmentChamberBlock.FACING, event.getEntity().getDirection().getOpposite()).setValue(ContainmentChamberBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(ContainmentChamberBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlock(pos, lower, 3);
        level.setBlock(pos.above(), upper, 3);
        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
