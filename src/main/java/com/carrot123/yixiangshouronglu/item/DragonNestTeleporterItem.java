package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.teleport.DragonNestTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DragonNestTeleporterItem extends Item {
    public DragonNestTeleporterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer && !DragonNestTravel.canStart(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            DragonNestTravel.begin(player, player.getUsedItemHand(), stack);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.dragon_nest_teleporter.enter").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.dragon_nest_teleporter.return").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.dragon_nest_teleporter.cooldown").withStyle(ChatFormatting.GRAY));
    }
}
