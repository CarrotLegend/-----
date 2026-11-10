package com.carrot123.yixiangshouronglu.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;

public final class BottledEternalSpringItem extends Item {
    private static final int DRINK_DURATION = 32;
    private static final int REGENERATION_DURATION = 100;
    private static final int COOLDOWN_TICKS = 1200;

    public BottledEternalSpringItem(Properties properties) {
        super(properties);
    }

    public static BlockHitResult getFluidHit(Level level, Player player) {
        return getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof Player player) || level.isClientSide) {
            return stack;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_DURATION, 4));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));

        if (player.getAbilities().instabuild) {
            return stack;
        }

        stack.shrink(1);
        ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            return emptyBottle;
        }

        if (!player.getInventory().add(emptyBottle)) {
            player.drop(emptyBottle, false);
        }
        return stack;
    }
}
