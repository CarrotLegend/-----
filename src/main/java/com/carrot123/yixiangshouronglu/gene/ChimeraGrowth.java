package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class ChimeraGrowth {
    public static final UUID BLOOD_ATTACK = UUID.fromString("dfcbd924-bfde-473d-8178-1b8f139861d2");
    public static final UUID GLUTTONY_HEALTH = UUID.fromString("11f94e79-7f50-405d-b706-80e54ba52040");
    public static final UUID GLUTTONY_ATTACK = UUID.fromString("2dca38a8-866b-4c3a-89f2-9d41d884cc2a");
    private static final String KILLS = "BloodthirstyKills";
    private static final String FOODS = "GluttonyFoods";
    private static final String ATTACK = "GluttonyAttackBonus";
    private static final int MAX_FOOD_BYTES = 1_048_576;

    public static double bloodDamage(long kills) {
        if (kills <= 0) {
            return 0.0D;
        }
        double value = 100.0D * (1.0D - Math.exp(-Math.pow(kills / 113.0D, 0.7D)));
        return Math.min(value, Math.nextDown(100.0D));
    }

    public static double gluttonyHealth(int foodTypes) {
        if (foodTypes <= 0) {
            return 0.0D;
        }
        double value = 151.0D * (1.0D - Math.exp(-foodTypes / 8.0D));
        return Math.min(value, Math.nextDown(151.0D));
    }

    public static long kills(ItemStack stack) {
        return stack.hasTag() ? Math.max(0, stack.getTag().getLong(KILLS)) : 0;
    }

    public static void addKill(ItemStack stack) {
        long kills = kills(stack);
        stack.getOrCreateTag().putLong(KILLS, kills == Long.MAX_VALUE ? kills : kills + 1);
    }

    public static Set<ResourceLocation> foods(ItemStack stack) {
        Set<ResourceLocation> foods = new LinkedHashSet<>();
        if (!stack.hasTag()) {
            return foods;
        }
        ListTag list = stack.getTag().getList(FOODS, Tag.TAG_STRING);
        int limit = Math.min(list.size(), ForgeRegistries.ITEMS.getKeys().size());
        int bytes = 0;
        for (int index = 0; index < limit; index++) {
            String value = list.getString(index);
            bytes += value.length() * 2 + 8;
            if (bytes > MAX_FOOD_BYTES) {
                break;
            }
            ResourceLocation id = value.length() <= 256 ? ResourceLocation.tryParse(value) : null;
            if (id != null && ForgeRegistries.ITEMS.containsKey(id)
                    && ForgeRegistries.ITEMS.getValue(id).isEdible()
                    && new ItemStack(ForgeRegistries.ITEMS.getValue(id)).getUseAnimation() == UseAnim.EAT) {
                foods.add(id);
            }
        }
        return foods;
    }

    public static void recordFood(ItemStack core, ItemStack consumed, LivingEntity eater) {
        if (!isFood(consumed, eater)) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(consumed.getItem());
        if (id == null || id.toString().length() > 256) {
            return;
        }
        Set<ResourceLocation> foods = foods(core);
        if (!foods.add(id)) {
            return;
        }
        ListTag list = new ListTag();
        int bytes = 0;
        for (ResourceLocation food : foods) {
            String value = food.toString();
            bytes += value.length() * 2 + 8;
            if (bytes > MAX_FOOD_BYTES) {
                break;
            }
            list.add(StringTag.valueOf(value));
        }
        core.getOrCreateTag().put(FOODS, list);
    }

    public static boolean isFood(ItemStack stack, LivingEntity eater) {
        return !stack.isEmpty() && stack.isEdible() && stack.getUseAnimation() == UseAnim.EAT
                && stack.getFoodProperties(eater) != null;
    }

    public static ItemStack equipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.findCurios(item).stream()
                        .filter(result -> !result.slotContext().cosmetic()
                                && "chimera_modification".equals(result.slotContext().identifier()))
                        .findFirst().map(result -> result.stack()).orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    public static void refresh(ServerPlayer player) {
        boolean active = player.isAlive() && !player.isRemoved() && !player.hasDisconnected();
        ItemStack blood = active ? equipped(player, ModItems.BLOODTHIRSTY_CHIMERA.get()) : ItemStack.EMPTY;
        ItemStack gluttony = active ? equipped(player, ModItems.GLUTTONOUS_CHIMERA.get()) : ItemStack.EMPTY;
        update(player.getAttribute(Attributes.ATTACK_DAMAGE), BLOOD_ATTACK, "Bloodthirsty Chimera attack",
                blood.isEmpty() ? 0 : bloodDamage(kills(blood)));
        float previousMaxHealth = player.getMaxHealth();
        update(player.getAttribute(Attributes.MAX_HEALTH), GLUTTONY_HEALTH, "Gluttonous Chimera health",
                gluttony.isEmpty() ? 0 : gluttonyHealth(foods(gluttony).size()));
        double attack = gluttony.isEmpty() ? 0 : player.getMaxHealth() * 0.10D;
        update(player.getAttribute(Attributes.ATTACK_DAMAGE), GLUTTONY_ATTACK, "Gluttonous Chimera attack", attack);
        if (!gluttony.isEmpty() && (!gluttony.hasTag() || Double.compare(gluttony.getTag().getDouble(ATTACK), attack) != 0)) {
            gluttony.getOrCreateTag().putDouble(ATTACK, attack);
        }
        if (player.getMaxHealth() < previousMaxHealth && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void update(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier current = attribute.getModifier(id);
        if (current != null && current.getOperation() == AttributeModifier.Operation.ADDITION
                && Double.compare(current.getAmount(), amount) == 0) {
            return;
        }
        if (current != null) {
            attribute.removeModifier(id);
        }
        if (amount > 0 && Double.isFinite(amount)) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    public static double displayedAttack(ItemStack stack) {
        double value = stack.hasTag() ? stack.getTag().getDouble(ATTACK) : 0;
        return Double.isFinite(value) ? Math.max(0, value) : 0;
    }

    public static String format(double value, double limit) {
        return String.format(Locale.ROOT, "%.2f", Math.min(value, limit - 0.01D));
    }

    public static Multimap<Attribute, AttributeModifier> displayAttributes(ItemStack stack) {
        if (stack.is(ModItems.BLOODTHIRSTY_CHIMERA.get())) {
            return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BLOOD_ATTACK, "Bloodthirsty Chimera attack", bloodDamage(kills(stack)),
                            AttributeModifier.Operation.ADDITION));
        }
        return ImmutableMultimap.of(Attributes.MAX_HEALTH,
                new AttributeModifier(GLUTTONY_HEALTH, "Gluttonous Chimera health", gluttonyHealth(foods(stack).size()),
                        AttributeModifier.Operation.ADDITION), Attributes.ATTACK_DAMAGE,
                new AttributeModifier(GLUTTONY_ATTACK, "Gluttonous Chimera attack", displayedAttack(stack),
                        AttributeModifier.Operation.ADDITION));
    }
}
