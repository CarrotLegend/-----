package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ContainmentExtractItem extends Item {
    private static final String ENTITY_ID = "EntityId";

    public ContainmentExtractItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);

        if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ModItems.CONTAINMENT_EXTRACT.get());
        stack.getOrCreateTag().putString(ENTITY_ID, id.toString());
        return stack;
    }

    public static String getEntityId(ItemStack stack) {
        if (!stack.is(ModItems.CONTAINMENT_EXTRACT.get())) {
            return "";
        }

        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains(ENTITY_ID, 8)) {
            return "";
        }

        return tag.getString(ENTITY_ID);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = ResourceLocation.tryParse(getEntityId(stack));

        if (id == null) {
            return super.getName(stack);
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);

        Component entityName = type == null
                ? Component.literal(id.toString())
                : type.getDescription();

        return Component.translatable(
                "item.yixiangshouronglu.containment_extract.named",
                entityName
        );
    }
}
