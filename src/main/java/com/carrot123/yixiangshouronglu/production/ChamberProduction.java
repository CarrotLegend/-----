package com.carrot123.yixiangshouronglu.production;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.resonance.ResonanceRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class ChamberProduction {
    public record Recipe(Item item, int intervalTicks, String rateKey) {
    }

    private ChamberProduction() {
    }

    @Nullable
    public static Recipe forEntity(String entityId) {
        if (ResonanceRules.isAntlersWendigo(entityId)) {
            return new Recipe(ModItems.WENDIGO_EYE.get(), 6000, "screen.yixiangshouronglu.chamber.rate_five_minutes");
        }
        return switch (entityId) {
            case "mutantmonsters:mutant_zombie" -> new Recipe(Items.IRON_INGOT, 600, "screen.yixiangshouronglu.chamber.rate_two_per_minute");
            case "mutantmonsters:mutant_skeleton" -> new Recipe(Items.GOLD_INGOT, 600, "screen.yixiangshouronglu.chamber.rate_two_per_minute");
            case "mutantmonsters:mutant_creeper" -> new Recipe(Items.GUNPOWDER, 600, "screen.yixiangshouronglu.chamber.rate_two_per_minute");
            case "mutantmonsters:mutant_enderman" -> new Recipe(Items.ENDER_PEARL, 2400, "screen.yixiangshouronglu.chamber.rate_two_minutes");
            case "cataclysm:amethyst_crab" -> new Recipe(Items.AMETHYST_SHARD, 600, "screen.yixiangshouronglu.chamber.rate_two_per_minute");
            case "cataclysm:clawdian" -> cataclysmLacrima();
            case "yixiangshouronglu:tenacious_shield" -> new Recipe(ModItems.TENACIOUS_SHIELD_FRAGMENT.get(), 4800, "screen.yixiangshouronglu.chamber.rate_four_minutes");
            default -> null;
        };
    }

    @Nullable
    private static Recipe cataclysmLacrima() {
        ResourceLocation id = new ResourceLocation("cataclysm", "lacrima");
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? null : new Recipe(item, 1200, "screen.yixiangshouronglu.chamber.rate_one_per_minute");
    }
}
