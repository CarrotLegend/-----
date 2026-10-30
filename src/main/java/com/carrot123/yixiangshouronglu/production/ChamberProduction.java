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
            return new Recipe(
                    ModItems.WENDIGO_EYE.get(),
                    6000,
                    "screen.yixiangshouronglu.chamber.rate_five_minutes"
            );
        }

        if (ResonanceRules.isMutantMoreZombie(entityId)) {
            return new Recipe(
                    Items.DIAMOND,
                    1200,
                    "screen.yixiangshouronglu.chamber.rate_one_per_minute"
            );
        }

        return switch (entityId) {
            case "mutantmonsters:mutant_zombie" ->
                    new Recipe(
                            Items.IRON_INGOT,
                            600,
                            "screen.yixiangshouronglu.chamber.rate_two_per_minute"
                    );

            case "mutantmonsters:mutant_skeleton" ->
                    new Recipe(
                            Items.GOLD_INGOT,
                            600,
                            "screen.yixiangshouronglu.chamber.rate_two_per_minute"
                    );

            case "mutantmonsters:mutant_creeper" ->
                    new Recipe(
                            Items.GUNPOWDER,
                            600,
                            "screen.yixiangshouronglu.chamber.rate_two_per_minute"
                    );

            case "mutantmonsters:mutant_enderman" ->
                    new Recipe(
                            Items.ENDER_PEARL,
                            2400,
                            "screen.yixiangshouronglu.chamber.rate_two_minutes"
                    );

            case "minecraft:wither" ->
                    new Recipe(
                            Items.NETHERITE_SCRAP,
                            1200,
                            "screen.yixiangshouronglu.chamber.rate_one_per_minute"
                    );

            case "cataclysm:amethyst_crab" ->
                    new Recipe(
                            Items.AMETHYST_SHARD,
                            600,
                            "screen.yixiangshouronglu.chamber.rate_two_per_minute"
                    );

            case "cataclysm:clawdian" ->
                    cataclysmLacrima();

            case "cataclysm:scylla" ->
                    externalItem(
                            "cataclysm",
                            "essence_of_the_storm",
                            6000,
                            "screen.yixiangshouronglu.chamber.rate_five_minutes"
                    );

            case "cataclysm:the_leviathan" ->
                    externalItem(
                            "cataclysm",
                            "abyssal_egg",
                            6000,
                            "screen.yixiangshouronglu.chamber.rate_five_minutes"
                    );

            case "cataclysm:ender_guardian" ->
                    externalItem(
                            "cataclysm",
                            "enderite_block",
                            6000,
                            "screen.yixiangshouronglu.chamber.rate_five_minutes"
                    );

            case "yixiangshouronglu:tenacious_shield" ->
                    new Recipe(
                            ModItems.TENACIOUS_SHIELD_FRAGMENT.get(),
                            4800,
                            "screen.yixiangshouronglu.chamber.rate_four_minutes"
                    );

            case "mutantmore:mutant_wither_skeleton" ->
                    new Recipe(
                            Items.COPPER_INGOT,
                            300,
                            "screen.yixiangshouronglu.chamber.rate_four_per_minute"
                    );

            case "mutantmore:mutant_blaze" ->
                    new Recipe(
                            Items.BLAZE_ROD,
                            1200,
                            "screen.yixiangshouronglu.chamber.rate_one_per_minute"
                    );

            case "mutantmore:mutant_hoglin" ->
                    new Recipe(
                            Items.COPPER_INGOT,
                            300,
                            "screen.yixiangshouronglu.chamber.rate_four_per_minute"
                    );

            case "iceandfire:fire_dragon" ->
                    externalItem(
                            "iceandfire",
                            "dragonsteel_fire_ingot",
                            4800,
                            "screen.yixiangshouronglu.chamber.rate_four_minutes"
                    );

            case "iceandfire:ice_dragon" ->
                    externalItem(
                            "iceandfire",
                            "dragonsteel_ice_ingot",
                            4800,
                            "screen.yixiangshouronglu.chamber.rate_four_minutes"
                    );

            case "iceandfire:lightning_dragon" ->
                    externalItem(
                            "iceandfire",
                            "dragonsteel_lightning_ingot",
                            4800,
                            "screen.yixiangshouronglu.chamber.rate_four_minutes"
                    );

            case "yixiangshouronglu:young_ender_dragon" ->
                    new Recipe(
                            Items.DRAGON_EGG,
                            4800,
                            "screen.yixiangshouronglu.chamber.rate_four_minutes"
                    );

            case "mokels_boss_mantyd:boss_mantyd" ->
                    new Recipe(
                            Items.DIAMOND,
                            1200,
                            "screen.yixiangshouronglu.chamber.rate_one_per_minute"
                    );

            case "theinkarena:blot" ->
                    externalItem(
                            "theinkarena",
                            "ink_waste",
                            600,
                            "screen.yixiangshouronglu.chamber.rate_two_per_minute"
                    );

            case "mythsandlegends:black_charro" ->
                    externalItem(
                            "mythsandlegends",
                            "tenebrite_shard",
                            4800,
                            "screen.yixiangshouronglu.chamber.rate_four_minutes"
                    );

            default -> null;
        };
    }

    @Nullable
    private static Recipe cataclysmLacrima() {
        return externalItem(
                "cataclysm",
                "lacrima",
                1200,
                "screen.yixiangshouronglu.chamber.rate_one_per_minute"
        );
    }

    @Nullable
    private static Recipe externalItem(
            String namespace,
            String path,
            int intervalTicks,
            String rateKey
    ) {
        ResourceLocation id = new ResourceLocation(namespace, path);

        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            return null;
        }

        Item item = ForgeRegistries.ITEMS.getValue(id);

        return item == null
                ? null
                : new Recipe(item, intervalTicks, rateKey);
    }
}
