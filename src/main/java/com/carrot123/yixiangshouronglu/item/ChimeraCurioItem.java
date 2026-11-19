package com.carrot123.yixiangshouronglu.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

abstract class ChimeraCurioItem extends Item implements ICurioItem {
    protected ChimeraCurioItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        if (!isChimeraSlot(context)) {
            return false;
        }
        if (context.entity() == null) {
            return true;
        }
        return CuriosApi.getCuriosInventory(context.entity())
                .map(inventory -> inventory.findCurios(this).stream()
                        .map(result -> result.slotContext())
                        .noneMatch(slot -> isChimeraSlot(slot) && slot.index() != context.index()))
                .orElse(false);
    }

    protected static boolean isChimeraSlot(SlotContext context) {
        return "chimera_modification".equals(context.identifier()) && !context.cosmetic();
    }

    protected static boolean isEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.findCurios(item).stream()
                        .anyMatch(result -> isChimeraSlot(result.slotContext())))
                .orElse(false);
    }
}
