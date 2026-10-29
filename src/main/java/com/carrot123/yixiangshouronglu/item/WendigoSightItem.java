package com.carrot123.yixiangshouronglu.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class WendigoSightItem extends Item implements ICurioItem {
    private static final int EFFECT_DURATION = 260;
    private static final int RENEW_THRESHOLD = 220;

    public WendigoSightItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "head".equals(slotContext.identifier());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!"head".equals(slotContext.identifier())) {
            return;
        }

        LivingEntity wearer = slotContext.entity();

        if (wearer == null || wearer.level().isClientSide) {
            return;
        }

        refreshEffect(wearer, MobEffects.NIGHT_VISION);
        refreshEffect(wearer, MobEffects.DAMAGE_BOOST);
    }

    private static void refreshEffect(LivingEntity wearer, MobEffect effect) {
        MobEffectInstance current = wearer.getEffect(effect);

        if (current == null
                || (current.getAmplifier() == 0
                && current.getDuration() <= RENEW_THRESHOLD)) {
            wearer.addEffect(
                    new MobEffectInstance(
                            effect,
                            EFFECT_DURATION,
                            0,
                            true,
                            false,
                            true
                    )
            );
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable("tooltip.yixiangshouronglu.wendigo_sight")
                        .withStyle(ChatFormatting.GOLD)
        );
    }
}
