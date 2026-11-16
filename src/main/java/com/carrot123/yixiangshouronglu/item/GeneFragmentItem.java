package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class GeneFragmentItem extends Item {
    public GeneFragmentItem() {
        super(new Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    public static CompoundTag sourceData(ResourceLocation id, CompoundTag snapshot) {
        CompoundTag data = new CompoundTag();
        data.putString("EntityId", id.toString());
        snapshot.putString("id", id.toString());
        data.put("EntityData", snapshot);
        return data;
    }

    @Nullable
    public static EntityType<?> sourceType(ItemStack stack) {
        if (!stack.is(ModItems.GENE_FRAGMENT.get())) {
            return null;
        }
        return sourceType(stack.getTag());
    }

    @Nullable
    static EntityType<?> sourceType(CompoundTag data) {
        if (data == null || !data.contains("EntityData", Tag.TAG_COMPOUND)) {
            return null;
        }
        String id = data.getString("EntityId");
        CompoundTag snapshot = data.getCompound("EntityData");
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !id.equals(snapshot.getString("id")) || !snapshot.hasUUID("UUID")
                || !ForgeRegistries.ENTITY_TYPES.containsKey(key)) {
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
        return type == EntityType.PLAYER ? null : type;
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> source = sourceType(stack);
        return source == null ? super.getName(stack)
                : Component.translatable("item.yixiangshouronglu.gene_fragment.named", source.getDescription());
    }
}
