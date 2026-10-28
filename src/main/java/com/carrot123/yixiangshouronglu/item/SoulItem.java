package com.carrot123.yixiangshouronglu.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class SoulItem extends Item {
    public static final String ENTITY_KEY = "SoulEntityId";

    public SoulItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item soulItem, ResourceLocation entityId) {
        ItemStack stack = new ItemStack(soulItem);
        stack.getOrCreateTag().putString(ENTITY_KEY, entityId.toString());
        return stack;
    }

    public static String getEntityId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(ENTITY_KEY);
    }

    public static Component getEntityName(String entityId) {
        ResourceLocation location = ResourceLocation.tryParse(entityId);
        if (location != null) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);
            if (type != null && ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
                return type.getDescription();
            }
        }
        return Component.literal(entityId);
    }

    @Override
    public Component getName(ItemStack stack) {
        String entityId = getEntityId(stack);
        return entityId.isEmpty() ? super.getName(stack) : Component.translatable("item.yixiangshouronglu.soul.named", getEntityName(entityId));
    }
}
