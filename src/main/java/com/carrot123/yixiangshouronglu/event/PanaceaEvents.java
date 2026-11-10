package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.PanaceaItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class PanaceaEvents {
    private PanaceaEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getEffectInstance().getEffect().getCategory() == MobEffectCategory.HARMFUL
                && isEquipped(player)) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInstantHarm(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity() instanceof ServerPlayer player && source.is(DamageTypes.INDIRECT_MAGIC)
                && isEquipped(player) && isVanillaPotionHarm(player, source)) {
            event.setCanceled(true);
        }
    }

    private static boolean isEquipped(ServerPlayer player) {
        ICurioStacksHandler charm = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(inventory -> inventory.getStacksHandler(PanaceaItem.SLOT)).orElse(null);
        if (charm == null) {
            return false;
        }
        var stacks = charm.getStacks();
        Item panacea = ModItems.PANACEA.get();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            if (stacks.getStackInSlot(slot).is(panacea)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVanillaPotionHarm(ServerPlayer player, DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof ThrownPotion potion && potion.getType() == EntityType.POTION) {
            return containsInstantHarm(PotionUtils.getMobEffects(potion.getItem()));
        }
        if (direct instanceof AreaEffectCloud cloud && cloud.getType() == EntityType.AREA_EFFECT_CLOUD) {
            return containsInstantHarm(cloud.getPotion().getEffects());
        }
        return direct == player && source.getEntity() == player && player.isUsingItem()
                && player.getUseItemRemainingTicks() <= 0 && player.getUseItem().is(Items.POTION)
                && containsInstantHarm(PotionUtils.getMobEffects(player.getUseItem()));
    }

    private static boolean containsInstantHarm(List<MobEffectInstance> effects) {
        for (MobEffectInstance effect : effects) {
            if (effect.getEffect() == MobEffects.HARM) {
                return true;
            }
        }
        return false;
    }
}
