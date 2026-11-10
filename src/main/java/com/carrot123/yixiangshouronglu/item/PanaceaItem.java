package com.carrot123.yixiangshouronglu.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public final class PanaceaItem extends Item implements ICurioItem {
    public static final String SLOT = "charm";

    public PanaceaItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        return SLOT.equals(context.identifier()) && !context.cosmetic();
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        if (!canEquip(context, stack) || previous.is(this) || !(context.entity() instanceof ServerPlayer player)) {
            return;
        }
        List<MobEffect> harmful = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(effect.getEffect());
            }
        }
        for (MobEffect effect : harmful) {
            player.removeEffect(effect);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.panacea").withStyle(ChatFormatting.GOLD));
    }
}
