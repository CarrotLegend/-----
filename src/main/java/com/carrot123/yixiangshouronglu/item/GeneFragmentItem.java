package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

    public static ItemStack capture(LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null || target instanceof Player) {
            return ItemStack.EMPTY;
        }
        try {
            CompoundTag data = sourceData(id, target.saveWithoutId(new CompoundTag()));
            if (sourceType(data) == null) {
                return ItemStack.EMPTY;
            }
            ItemStack fragment = new ItemStack(ModItems.GENE_FRAGMENT.get());
            fragment.setTag(data);
            return fragment;
        } catch (RuntimeException | StackOverflowError exception) {
            LogUtils.getLogger().warn("Could not serialize a gene fragment for {}", id, exception);
            return ItemStack.EMPTY;
        }
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
                || !ForgeRegistries.ENTITY_TYPES.containsKey(key) || !GeneExtractorItem.isSafeSnapshot(data)) {
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
