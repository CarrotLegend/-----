package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.registry.ModFluids;
import com.carrot123.yixiangshouronglu.item.BottledEternalSpringItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EternalSpringEvents {
    private EternalSpringEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 20 != 0) {
            return;
        }

        if (entity.getFluidTypeHeight(ModFluids.ETERNAL_SPRING_TYPE.get()) <= 0.0D) {
            return;
        }

        MobEffectInstance current = entity.getEffect(MobEffects.REGENERATION);
        if (current == null || current.getAmplifier() == 0 && current.getDuration() <= 40) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    @SubscribeEvent
    public static void onFillBottleFromBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            return;
        }
        fillBottle(event, event.getPos());
    }

    @SubscribeEvent
    public static void onFillBottleFromItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled() || !event.getItemStack().is(Items.GLASS_BOTTLE)) {
            return;
        }

        BlockHitResult hit = BottledEternalSpringItem.getFluidHit(event.getLevel(), event.getEntity());
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        fillBottle(event, hit.getBlockPos());
    }

    private static void fillBottle(PlayerInteractEvent event, BlockPos pos) {
        ItemStack held = event.getItemStack();
        if (!held.is(Items.GLASS_BOTTLE)) {
            return;
        }

        Level level = event.getLevel();
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource() || !fluid.is(ModFluids.ETERNAL_SPRING.get())) {
            return;
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);

        if (level.isClientSide) {
            return;
        }

        Player player = event.getEntity();
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
        ItemStack filled = new ItemStack(ModItems.BOTTLED_ETERNAL_SPRING.get());

        if (player.getAbilities().instabuild) {
            if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
            return;
        }

        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(event.getHand(), filled);
        } else if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
    }
}
