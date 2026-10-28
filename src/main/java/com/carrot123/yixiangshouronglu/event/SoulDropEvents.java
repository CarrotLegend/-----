package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulDropEvents {
    private static final String DROPPED_KEY = "yixiangshouronglu_soul_dropped";
    private static final Set<ResourceLocation> ALLOWED_ENTITIES = Set.of(
            new ResourceLocation("mutantmonsters", "mutant_zombie"),
            new ResourceLocation("mutantmonsters", "mutant_skeleton"),
            new ResourceLocation("mutantmonsters", "mutant_creeper"),
            new ResourceLocation("mutantmonsters", "mutant_enderman")
    );

    private SoulDropEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }
        if (mob.getPersistentData().getBoolean(DROPPED_KEY)) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null || !ALLOWED_ENTITIES.contains(id)) {
            return;
        }
        mob.getPersistentData().putBoolean(DROPPED_KEY, true);
        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), id);
        event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY() + 0.3D, mob.getZ(), soul));
    }
}
