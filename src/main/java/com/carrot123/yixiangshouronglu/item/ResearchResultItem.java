package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ResearchResultItem extends Item {
    public ResearchResultItem() {
        super(new Properties().stacksTo(1));
    }

    public static boolean canResearch(ItemStack fragment) {
        return GeneFragmentItem.sourceType(fragment) != null && validSnapshot(fragment.getTag()) != null;
    }

    public static ItemStack fromFragment(ItemStack fragment) {
        if (!canResearch(fragment)) {
            return ItemStack.EMPTY;
        }
        CompoundTag source = fragment.getTag();
        CompoundTag tag = new CompoundTag();
        tag.putString("EntityId", source.getString("EntityId"));
        tag.put("EntityData", source.getCompound("EntityData").copy());
        ItemStack result = new ItemStack(ModItems.RESEARCH_RESULT.get());
        result.setTag(tag);
        return result;
    }

    @Nullable
    public static EntityType<?> sourceType(ItemStack stack) {
        return stack.is(ModItems.RESEARCH_RESULT.get()) && stack.getCount() == 1 ? validSnapshot(stack.getTag()) : null;
    }

    @Nullable
    private static EntityType<?> validSnapshot(CompoundTag tag) {
        EntityType<?> type = GeneFragmentItem.sourceType(tag);
        if (type == null || !GeneExtractorItem.isSafeSnapshot(tag)) {
            return null;
        }
        var snapshot = tag.getCompound("EntityData");
        float health = snapshot.getFloat("Health");
        return snapshot.contains("Health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                && Float.isFinite(health) && health > 0 ? type : null;
    }

    @Override public Component getName(ItemStack stack) {
        EntityType<?> type = sourceType(stack);
        return type == null ? super.getName(stack)
                : Component.translatable("item.yixiangshouronglu.research_result.named", type.getDescription());
    }
}
