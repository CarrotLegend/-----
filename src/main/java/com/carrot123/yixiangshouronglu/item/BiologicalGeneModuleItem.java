package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.gene.BiologicalGene;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class BiologicalGeneModuleItem extends Item {
    private static final String TEXT = "tooltip.yixiangshouronglu.biological_gene_module.";

    public BiologicalGeneModuleItem() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack fromFragment(ItemStack fragment) {
        if (GeneFragmentItem.sourceType(fragment) == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag data = fragment.getTag();
        return create(new ResourceLocation(data.getString("EntityId")), data.getCompound("EntityData"));
    }

    public static ItemStack displayStack(BiologicalGene gene) {
        var source = gene.displaySource();
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(source)) {
            return ItemStack.EMPTY;
        }
        var data = new CompoundTag();
        data.putString("id", source.toString());
        data.putUUID("UUID", new UUID(0, 0));
        return create(source, data);
    }

    private static ItemStack create(ResourceLocation source, CompoundTag snapshot) {
        ItemStack stack = new ItemStack(ModItems.BIOLOGICAL_GENE_MODULE.get());
        CompoundTag data = stack.getOrCreateTag();
        data.putString("GeneType", BiologicalGene.fromSource(source).id);
        data.putString("SourceEntityId", source.toString());
        data.put("EntityData", snapshot.copy());
        return stack;
    }

    @Nullable
    public static BiologicalGene gene(ItemStack stack) {
        if (!stack.is(ModItems.BIOLOGICAL_GENE_MODULE.get()) || stack.getCount() != 1 || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        ResourceLocation source = ResourceLocation.tryParse(tag.getString("SourceEntityId"));
        if (source == null) {
            return null;
        }
        BiologicalGene gene = BiologicalGene.fromSource(source);
        if (!gene.id.equals(tag.getString("GeneType"))) {
            return null;
        }
        var fragmentData = new CompoundTag();
        fragmentData.putString("EntityId", source.toString());
        fragmentData.put("EntityData", tag.getCompound("EntityData"));
        return GeneFragmentItem.sourceType(fragmentData) == null ? null : gene;
    }

    @Override
    public Component getName(ItemStack stack) {
        BiologicalGene gene = gene(stack);
        return gene == null ? super.getName(stack) : Component.translatable(getDescriptionId() + "." + gene.id);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        BiologicalGene gene = gene(stack);
        if (gene == null) {
            return;
        }
        String regionKey = gene.installationRegionKey();
        if (regionKey != null) {
            line(tooltip, "installation_slot", Component.translatable(regionKey));
        }
        if (gene.armor > 0) {
            line(tooltip, "armor", gene.armor);
        }
        if (gene.health > 0) {
            line(tooltip, "health", gene.health);
        }
        if (gene.attack > 0) {
            line(tooltip, "attack", gene.attack);
        }
        if (gene == BiologicalGene.SPINAL_VOIDIZATION) {
            line(tooltip, "void", BiologicalGene.VOID_CHANCE_PERCENT, BiologicalGene.VOID_REDUCTION_PERCENT);
        }
        if (gene == BiologicalGene.MOLTEN_SKELETON) {
            line(tooltip, "shock");
            line(tooltip, "shock_damage", BiologicalGene.SHOCK_DAMAGE);
            line(tooltip, "shock_fire", BiologicalGene.SHOCK_FIRE_SECONDS);
            line(tooltip, "shock_cooldown", BiologicalGene.SHOCK_COOLDOWN / 20);
        }
        switch (gene) {
            case SOULFIRE_REACTOR_CORE -> {
                line(tooltip, "all_damage", BiologicalGene.SOULFIRE_DAMAGE_PERCENT);
                line(tooltip, "blazing_brand", BiologicalGene.BRAND_CHANCE_PERCENT, BiologicalGene.BRAND_TICKS / 20);
                line(tooltip, "burning_damage", BiologicalGene.BURNING_DAMAGE_PERCENT);
            }
            case WAR_INSTINCT -> {
                line(tooltip, "ranged_damage", BiologicalGene.RANGED_DAMAGE_PERCENT);
                line(tooltip, "crit_chance", BiologicalGene.CRIT_CHANCE_POINTS);
            }
            case ABYSSAL_CIRCULATION -> {
                line(tooltip, "movement_speed", BiologicalGene.MOVE_SPEED_PERCENT);
                line(tooltip, "swim_speed", BiologicalGene.SWIM_SPEED_PERCENT);
                line(tooltip, "water_healing", BiologicalGene.WATER_HEAL);
            }
            case DESERT_MUSCLE_REINFORCEMENT ->
                    line(tooltip, "stun", BiologicalGene.STUN_CHANCE_PERCENT, BiologicalGene.STUN_TICKS / 20);
            case CURSED_MUSCLE_FIBERS -> {
                line(tooltip, "attack_speed", BiologicalGene.ATTACK_SPEED_PERCENT);
                line(tooltip, "rage");
                line(tooltip, "rage_damage", BiologicalGene.RAGE_DAMAGE_PERCENT);
                line(tooltip, "rage_max", BiologicalGene.MAX_RAGE);
                line(tooltip, "rage_expiration", BiologicalGene.RAGE_TICKS / 20);
            }
            case THUNDER_CORE -> {
                line(tooltip, "cold_damage", BiologicalGene.COLD_DAMAGE);
                line(tooltip, "bolt", BiologicalGene.BOLT_CHANCE_PERCENT);
                line(tooltip, "bolt_damage", BiologicalGene.BOLT_DAMAGE);
            }
            case SKY_STORM_CORE -> {
                line(tooltip, "all_damage", BiologicalGene.SKY_DAMAGE_PERCENT);
                line(tooltip, "sky_hits", BiologicalGene.SKY_HITS);
                line(tooltip, "bolt_damage", BiologicalGene.SKY_BOLT_DAMAGE);
                line(tooltip, "sky_slow", BiologicalGene.SKY_SLOW_TICKS / 20);
                line(tooltip, "shock_cooldown", BiologicalGene.SKY_COOLDOWN / 20);
            }
            case FALLEN_COMBAT_NERVES -> {
                line(tooltip, "fallen_key", Component.keybind("key.yixiangshouronglu.fallen_form"));
                line(tooltip, "attack_speed", BiologicalGene.FALLEN_BONUS_PERCENT);
                line(tooltip, "all_damage", BiologicalGene.FALLEN_BONUS_PERCENT);
                line(tooltip, "fallen_life_steal", BiologicalGene.FALLEN_BONUS_PERCENT);
                line(tooltip, "fallen_duration", BiologicalGene.FALLEN_TICKS / 20);
                line(tooltip, "shock_cooldown", BiologicalGene.FALLEN_COOLDOWN / 20);
            }
            case ANNIHILATION_REACTOR_CORE -> {
                line(tooltip, "all_damage", BiologicalGene.ANNIHILATION_DAMAGE_PERCENT);
                line(tooltip, "annihilation_cooldown", BiologicalGene.ANNIHILATION_COOLDOWN / 20);
                line(tooltip, "annihilation_damage", BiologicalGene.ANNIHILATION_HEALTH_PERCENT);
            }
            case ANCHOR_MUSCLE_FIBERS -> {
                line(tooltip, "anchor_chance", BiologicalGene.ANCHOR_CHANCE_PERCENT);
                line(tooltip, "anchor_damage", BiologicalGene.ANCHOR_DAMAGE);
                line(tooltip, "anchor_slow", BiologicalGene.ANCHOR_SLOW_TICKS / 20);
            }
            case PRIMORDIAL_ICE_CRYSTALLIZATION -> {
                line(tooltip, "chill_chance", BiologicalGene.CHILL_CHANCE_PERCENT);
                line(tooltip, "chill_duration", BiologicalGene.CHILL_TICKS / 20, BiologicalGene.CHILL_STACKS);
                line(tooltip, "chill_damage", BiologicalGene.CHILL_DAMAGE);
                line(tooltip, "chill_slow");
                line(tooltip, "chill_weakness");
            }
            case GLANDULAR_REGENERATION -> {
                line(tooltip, "glandular_poison", BiologicalGene.GLANDULAR_POISON_CHANCE_PERCENT,
                        BiologicalGene.GLANDULAR_POISON_TICKS / 20);
                line(tooltip, "glandular_heal", BiologicalGene.GLANDULAR_HEAL);
            }
            case SANDSTORM_VISION -> line(tooltip, "crit_chance", BiologicalGene.SANDSTORM_CRIT_PERCENT);
            case ANCIENT_DRAGON_SPINE -> {
                line(tooltip, "knockback_resistance", BiologicalGene.ANCIENT_KNOCKBACK_RESISTANCE_PERCENT);
                line(tooltip, "fall_immunity");
            }
            case LAVA_DIGESTION -> {
                line(tooltip, "fire_immunity");
                line(tooltip, "burning_attack", BiologicalGene.LAVA_ATTACK_PERCENT);
            }
            case ABOMINATION_SPINAL_CORD -> line(tooltip, "abomination_wither", BiologicalGene.ABOMINATION_WITHER_TICKS / 20);
            case CHORUS_TRANSLOCATION -> {
                line(tooltip, "movement_speed", BiologicalGene.CHORUS_SPEED_PERCENT);
                line(tooltip, "dodge_chance", BiologicalGene.CHORUS_DODGE_PERCENT);
            }
            case EXTREME_COLD_BREATH -> {
                line(tooltip, "extreme_cold_chance", BiologicalGene.EXTREME_COLD_CHANCE_PERCENT);
                line(tooltip, "extreme_cold_damage", BiologicalGene.EXTREME_COLD_DAMAGE_PERCENT);
            }
            case SOLAR_RETINA -> {
                line(tooltip, "crit_chance", BiologicalGene.SOLAR_CRIT_PERCENT);
                line(tooltip, "solar_beam_chance", BiologicalGene.SOLAR_BEAM_CHANCE_PERCENT);
                line(tooltip, "solar_beam_damage", BiologicalGene.SOLAR_BEAM_DAMAGE);
            }
            case INCINERATION_GLAND -> {
                line(tooltip, "burn_proc");
                line(tooltip, "burn_damage");
                line(tooltip, "burn_cap");
            }
            case FREEZING_GLAND -> {
                line(tooltip, "freeze_proc");
            }
            case LIGHTNING_CHAIN_GLAND -> {
                line(tooltip, "chain_proc");
                line(tooltip, "chain_first");
                line(tooltip, "chain_range");
                line(tooltip, "chain_jump");
                line(tooltip, "shock_cooldown", 8);
            }
            case UNDYING_BODY -> {
                line(tooltip, "undying_regen");
            }
            case PRECISION_MUSCLE_FIBERS -> {
                line(tooltip, "bow_damage");
            }
            case EXPLOSIVE_GLAND -> {
                line(tooltip, "burst_proc");
                line(tooltip, "burst_damage");
                line(tooltip, "burst_terrain");
            }
            case ENDER_SYNAPSE -> {
                line(tooltip, "movement_speed", 10);
                line(tooltip, "dodge_chance", 5);
                line(tooltip, "leap_key", Component.keybind("key.yixiangshouronglu.fallen_form"));
                line(tooltip, "leap_range");
                line(tooltip, "leap_attack");
                line(tooltip, "shock_cooldown", BiologicalGene.ENDER_TELEPORT_COOLDOWN / 20);
            }
            case FROST_CORE_RETINA -> {
                line(tooltip, "crit_chance", 10);
                line(tooltip, "frost_damage");
            }
            case COMBAT_NERVES -> {
                line(tooltip, "combat_nerves_block");
                line(tooltip, "combat_nerves_immunity");
            }
            case BLAZING_MUSCLES -> {
                line(tooltip, "blazing_muscles_ignite");
                line(tooltip, "blazing_muscles_fire_immune");
            }
            case QUICKSAND_METABOLISM -> line(tooltip, "quicksand_hunger");
            case FROZEN_CIRCULATORY_TISSUE -> line(tooltip, "frozen_circulation_bonus");
            case PLANT_MUSCLE_FIBERS -> {
                line(tooltip, "plant_reach");
                line(tooltip, "plant_slow");
            }
            case ARACHNID_APONEUROSIS -> {
                line(tooltip, "movement_speed", 15);
                line(tooltip, "arachnid_jump");
            }
            default -> {}
        }
    }

    private static void line(List<Component> tooltip, String effect, Object... args) {
        tooltip.add(Component.translatable(TEXT + effect, args).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
