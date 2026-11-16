package com.carrot123.yixiangshouronglu.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BiologicalCleaverItem extends SwordItem {
    private static final String UPGRADES = "SwordUpgradeCount";

    public BiologicalCleaverItem() {
        super(Tiers.DIAMOND, 8, -2.4F, new Properties());
    }

    public static int upgrades(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(UPGRADES, Tag.TAG_INT) ? Math.max(0, tag.getInt(UPGRADES)) : 0;
    }

    public static boolean upgrade(ItemStack stack) {
        int count = upgrades(stack);
        if (count == Integer.MAX_VALUE) {
            return false;
        }
        stack.getOrCreateTag().putInt(UPGRADES, count + 1);
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        int count = upgrades(stack);
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND || count == 0) {
            return base;
        }
        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        base.forEach((attribute, modifier) -> {
            if (attribute != Attributes.ATTACK_DAMAGE || !modifier.getId().equals(BASE_ATTACK_DAMAGE_UUID)) {
                builder.put(attribute, modifier);
            }
        });
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                (double) getDamage() + count, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.biological_cleaver.upgrades", upgrades(stack)));
    }
}
