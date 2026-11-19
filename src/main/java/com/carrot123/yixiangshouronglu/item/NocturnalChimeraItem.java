package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.event.GeneJumpEvents;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class NocturnalChimeraItem extends ChimeraCurioItem {
    private static final int REFRESH_TICKS = 100;
    private static final int NIGHT_VISION_TICKS = 400;

    private final Map<ServerPlayer, Long> nextRefresh = new WeakHashMap<>();

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        if (!isChimeraSlot(context)) {
            return ImmutableMultimap.of();
        }
        return ImmutableMultimap.of(
                Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid, "Nocturnal Chimera speed", 0.20,
                        AttributeModifier.Operation.MULTIPLY_TOTAL),
                GeneJumpEvents.JUMP_HEIGHT.get(), new AttributeModifier(uuid, "Nocturnal Chimera jump", 0.10,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        if (!previous.is(this) && isChimeraSlot(context) && context.entity() instanceof ServerPlayer player
                && player.isAlive() && !player.isRemoved()) {
            refreshNightVision(player, gameTime(player));
        }
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        if (isChimeraSlot(context) && context.entity() instanceof ServerPlayer player
                && player.isAlive() && !player.isRemoved()) {
            long now = gameTime(player);
            if (now >= nextRefresh.getOrDefault(player, 0L)) {
                refreshNightVision(player, now);
            }
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack replacement, ItemStack stack) {
        if (isChimeraSlot(context) && context.entity() instanceof ServerPlayer player) {
            nextRefresh.remove(player);
        }
    }

    private void refreshNightVision(ServerPlayer player, long now) {
        nextRefresh.put(player, now + REFRESH_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false));
    }

    private static long gameTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    @SubscribeEvent
    public static void preventFallDamage(LivingAttackEvent event) {
        if (event.getSource().is(DamageTypes.FALL) && event.getEntity() instanceof ServerPlayer player
                && player.isAlive() && !player.isRemoved()
                && isEquipped(player, ModItems.NOCTURNAL_CHIMERA.get())) {
            event.setCanceled(true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.nocturnal_chimera.night_vision").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.nocturnal_chimera.fall_immunity").withStyle(ChatFormatting.GOLD));
    }
}
