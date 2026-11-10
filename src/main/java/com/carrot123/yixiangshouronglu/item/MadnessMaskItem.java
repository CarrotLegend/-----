package com.carrot123.yixiangshouronglu.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class MadnessMaskItem extends Item implements ICurioItem {
    private static final String START_TICK = "yixiangshouronglu_madness_mask_start_tick";
    private static final String LAST_TICK = "yixiangshouronglu_madness_mask_last_tick";

    public MadnessMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return "head".equals(context.identifier());
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (!"head".equals(context.identifier()) || !(context.entity() instanceof Player player)
                || player.level().isClientSide || !player.isAlive()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        if (!data.contains(START_TICK, Tag.TAG_LONG) || data.getLong(START_TICK) > now) {
            data.putLong(START_TICK, now);
        }

        if (data.contains(LAST_TICK, Tag.TAG_LONG) && data.getLong(LAST_TICK) == now) {
            return;
        }
        data.putLong(LAST_TICK, now);

        long elapsed = now - data.getLong(START_TICK);
        if (elapsed > 0 && elapsed % 20L == 0) {
            float health = player.getHealth() - 2.0F;
            player.setHealth(Math.max(0.0F, health));
            if (health <= 0.0F) {
                player.die(player.damageSources().magic());
                return;
            }
        }

        if (elapsed > 0 && elapsed % 400L == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 3));
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack, ItemStack stack) {
        if (context.entity() instanceof Player player && !player.level().isClientSide) {
            CompoundTag data = player.getPersistentData();
            data.remove(START_TICK);
            data.remove(LAST_TICK);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.madness_mask").withStyle(ChatFormatting.GOLD));
    }
}
