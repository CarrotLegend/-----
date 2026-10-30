package com.carrot123.yixiangshouronglu.event;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.item.SoulItem;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.resonance.ResonanceRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(
        modid = AnomalousContainmentChronicles.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SoulDropEvents {
    private static final String DROPPED_KEY = "yixiangshouronglu_soul_dropped";

    private static final ResourceLocation BOSS =
            new ResourceLocation(AnomalousContainmentChronicles.MOD_ID, "tenacious_shield");

    private static final ResourceLocation WITHER =
            new ResourceLocation("minecraft", "wither");

    private static final Set<ResourceLocation> ALLOWED_ENTITIES = Set.of(
            new ResourceLocation("mutantmonsters", "mutant_zombie"),
            new ResourceLocation("mutantmonsters", "mutant_skeleton"),
            new ResourceLocation("mutantmonsters", "mutant_creeper"),
            new ResourceLocation("mutantmonsters", "mutant_enderman"),
            new ResourceLocation("cataclysm", "amethyst_crab"),
            new ResourceLocation("cataclysm", "clawdian"),
            new ResourceLocation("cataclysm", "scylla"),
            new ResourceLocation("cataclysm", "the_leviathan"),
            new ResourceLocation("cataclysm", "ender_guardian"),
            new ResourceLocation("mutantmore", "mutant_jungle_zombie"),
            new ResourceLocation("mutantmore", "mutant_frozen_zombie"),
            new ResourceLocation("mutantmore", "mutant_husk"),
            new ResourceLocation("mutantmore", "mutant_hoglin"),
            new ResourceLocation("mutantmore", "mutant_wither_skeleton"),
            WITHER,
            BOSS
    );

    private SoulDropEvents() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());

        if (id == null || (!ALLOWED_ENTITIES.contains(id)
                && !ResonanceRules.isAntlersWendigo(id.toString()))) {
            return;
        }

        if (!id.equals(BOSS)
                && !id.equals(WITHER)
                && !(event.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }

        if (mob.getPersistentData().getBoolean(DROPPED_KEY)) {
            return;
        }

        mob.getPersistentData().putBoolean(DROPPED_KEY, true);

        ItemStack soul = SoulItem.create(ModItems.SOUL.get(), id);

        event.getDrops().add(
                new ItemEntity(
                        mob.level(),
                        mob.getX(),
                        mob.getY() + 0.3D,
                        mob.getZ(),
                        soul
                )
        );
    }
}
