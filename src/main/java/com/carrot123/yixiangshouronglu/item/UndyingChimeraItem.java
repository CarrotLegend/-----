package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.event.MutantGeneSkills;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.UUID;

public final class UndyingChimeraItem extends ChimeraCurioItem {
    private static final ResourceLocation LIFE_STEAL = new ResourceLocation("attributeslib", "life_steal");
    private static final ResourceLocation OVERHEAL = new ResourceLocation("attributeslib", "overheal");

    public static boolean isEquipped(ServerPlayer player) {
        return isEquipped(player, ModItems.UNDYING_CHIMERA.get());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        if (!isChimeraSlot(context)) {
            return ImmutableMultimap.of();
        }
        var modifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        var lifeSteal = ForgeRegistries.ATTRIBUTES.getValue(LIFE_STEAL);
        var overheal = ForgeRegistries.ATTRIBUTES.getValue(OVERHEAL);
        if (lifeSteal != null) {
            modifiers.put(lifeSteal, new AttributeModifier(uuid, "Undying Chimera life steal", 0.25,
                    AttributeModifier.Operation.ADDITION));
        }
        if (overheal != null) {
            modifiers.put(overheal, new AttributeModifier(uuid, "Undying Chimera overheal", 0.15,
                    AttributeModifier.Operation.ADDITION));
        }
        return modifiers.build();
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        if (!previous.is(this) && isChimeraSlot(context) && context.entity() instanceof ServerPlayer player) {
            MutantGeneSkills.onUndyingChimeraChanged(player, true);
        }
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack replacement, ItemStack stack) {
        if (!replacement.is(this) && isChimeraSlot(context) && context.entity() instanceof ServerPlayer player) {
            MutantGeneSkills.onUndyingChimeraChanged(player, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.undying_chimera.regeneration").withStyle(ChatFormatting.GOLD));
    }
}
