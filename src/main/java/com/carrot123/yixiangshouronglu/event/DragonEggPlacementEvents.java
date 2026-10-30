package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.block.entity.IncubatingDragonEggBlockEntity;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DragonEggPlacementEvents {
    private static final ResourceLocation VOID_STONE =
            new ResourceLocation("cataclysm", "void_stone");

    private DragonEggPlacementEvents() {
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getPlacedBlock().is(Blocks.DRAGON_EGG)) {
            return;
        }

        BlockPos pos = event.getPos();

        if (!VOID_STONE.equals(
                ForgeRegistries.BLOCKS.getKey(
                        level.getBlockState(pos.below()).getBlock()
                )
        )) {
            return;
        }

        if (level.setBlock(
                pos,
                ModBlocks.INCUBATING_DRAGON_EGG.get().defaultBlockState(),
                3
        ) && level.getBlockEntity(pos) instanceof IncubatingDragonEggBlockEntity egg) {
            egg.setOwner(player.getUUID());
        }
    }
}
