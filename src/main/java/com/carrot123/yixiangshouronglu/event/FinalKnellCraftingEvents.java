package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class FinalKnellCraftingEvents {
    private static final ResourceLocation MATERIAL = new ResourceLocation("mythsandlegends", "refined_tenebrite");

    private FinalKnellCraftingEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockState bell = event.getLevel().getBlockState(pos);
        if (event.isCanceled() || player.isSpectator() || !bell.is(Blocks.BELL)) {
            return;
        }
        InteractionHand hand = event.getHand();
        ItemStack material = player.getItemInHand(hand);
        if (!isMaterial(material) && hand == InteractionHand.MAIN_HAND) {
            hand = InteractionHand.OFF_HAND;
            material = player.getOffhandItem();
        }
        if (!isMaterial(material)) {
            return;
        }

        event.setCanceled(true);
        if (event.getLevel().isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        event.setCancellationResult(InteractionResult.FAIL);
        if (!(player instanceof ServerPlayer serverPlayer) || !player.isAlive() || !player.mayBuild()) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        if (!level.mayInteract(player, pos) || !level.getWorldBorder().isWithinBounds(pos)
                || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, bell, player))
                || level.getBlockState(pos) != bell || player.getItemInHand(hand) != material || !isMaterial(material)) {
            return;
        }

        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.removeBlock(pos, false)) {
            return;
        }
        ItemEntity reward = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                new ItemStack(ModItems.FINAL_KNELL.get()));
        reward.setDefaultPickUpDelay();
        if (!level.addFreshEntity(reward)) {
            snapshot.restore(true);
            return;
        }
        if (!player.getAbilities().instabuild) {
            material.shrink(1);
            player.getInventory().setChanged();
        }
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    private static boolean isMaterial(ItemStack stack) {
        return !stack.isEmpty() && MATERIAL.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
