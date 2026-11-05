package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.registries.ForgeRegistries;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder("yixiangshouronglu")
@PrefixGameTestTemplate(false)
public final class ElementalRuntimeTests {
    private static ServerPlayer player(GameTestHelper helper) {
        var level = helper.getLevel(); var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "element-test"));
        var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection); connection.setProtocol(ConnectionProtocol.PLAY);
        level.getServer().getPlayerList().placeNewPlayer(connection, player);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1)); player.setPos(pos.getX(), pos.getY(), pos.getZ()); player.setHealth(20); return player;
    }
    private static Cow cow(GameTestHelper helper, ServerPlayer player) {
        var cow = EntityType.COW.create(helper.getLevel()); cow.setPos(player.getX() + 2, player.getY(), player.getZ()); cow.setNoAi(true); cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(1000); helper.getLevel().addFreshEntity(cow); return cow;
    }
    private static void install(ServerPlayer player, ResourceLocation gene, GeneTypes.Quality quality, int slot) {
        GeneCapability.get(player).set(GeneDefinitions.get(gene).region(), slot, GeneModuleItem.create(gene, quality));
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void domainDamageTimingAndLifecycle(GameTestHelper helper) {
        var player = player(helper); install(player, ElementalGenes.BLAZING_NERVES, GeneTypes.Quality.ALPHA, 0);
        install(player, ElementalGenes.WITHERED_RIBS, GeneTypes.Quality.GAMMA, 0); install(player, ExtendedGenes.FROZEN_BONE_MARROW, GeneTypes.Quality.GAMMA, 1);
        install(player, ExtendedGenes.VINE_MUSCLES, GeneTypes.Quality.GAMMA, 0);
        var critChance = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "crit_chance"));
        var critDamage = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "crit_damage"));
        if (critChance != null && critDamage != null) { player.getAttribute(critChance).setBaseValue(0); player.getAttribute(critDamage).setBaseValue(1); }
        var cow = cow(helper, player); var immune = EntityType.BLAZE.create(helper.getLevel()); immune.setPos(cow.position()); immune.setNoAi(true); helper.getLevel().addFreshEntity(immune);
        long now = player.server.overworld().getGameTime(); var result = GeneSkills.request(player, ElementalGenes.BLAZING_DOMAIN);
        helper.assertTrue(result.status().equals("ok") && result.readyAt() == now + 2400, "Successful activation starts full cooldown");
        var source = BlazingDomain.source(player);
        helper.assertTrue(source.is(DamageTypeTags.IS_FIRE) && source.getEntity() == player && !source.is(DamageTypes.PLAYER_ATTACK), "Player-owned fire, not melee");
        BlazingDomain.advance(player, now + 19); helper.assertTrue(cow.getHealth() == 1000, "No early pulse");
        for (int index = 1; index <= 30; index++) {
            cow.invulnerableTime = 0; float previous = cow.getHealth(); BlazingDomain.advance(player, now + index * 20);
            helper.assertTrue(cow.getHealth() == previous - 8, "Eight damage per pulse");
            BlazingDomain.advance(player, now + index * 20); helper.assertTrue(cow.getHealth() == previous - 8, "No duplicate pulse");
        }
        helper.assertTrue(cow.getHealth() == 760 && !BlazingDomain.active(player), "Thirty pulses then expiry");
        helper.assertTrue(immune.getHealth() == immune.getMaxHealth() && !cow.isOnFire(), "Fire immune target unaffected and no ignition");
        GeneMeleeEvents.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, player.server));
        helper.assertTrue(cow.getHealth() == 760 && !cow.hasEffect(MobEffects.WITHER) && !cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Domain does not trigger melee genes");
        GeneCapability.get(player).effectRuntime().remove("SkillRequestAfter");
        helper.assertTrue(GeneSkills.request(player, ElementalGenes.BLAZING_DOMAIN).status().equals("cooldown"), "Cooldown refuses reactivation");
        var saved = new PlayerGenes(); saved.load(GeneCapability.get(player).save());
        helper.assertTrue(GeneSkills.readyAt(saved, ElementalGenes.BLAZING_DOMAIN) == now + 2400, "Cooldown saved independently of active state");
        var observer = player(helper); boolean pvp = player.server.isPvpAllowed(); player.server.setPvpAllowed(false);
        helper.assertTrue(!BlazingDomain.canTarget(player, observer), "PVP disabled respected"); player.server.setPvpAllowed(pvp);
        install(observer, ElementalGenes.BLAZING_NERVES, GeneTypes.Quality.GAMMA, 0); BlazingDomain.start(observer);
        helper.assertTrue(BlazingDomain.start(player).equals("ok"), "Direct fixture restart"); player.setPos(player.getX() + 50, player.getY(), player.getZ()); cow.invulnerableTime = 0;
        BlazingDomain.advance(player, now + 20); helper.assertTrue(cow.getHealth() == 760, "Domain follows player");
        GeneCapability.get(player).set(GeneTypes.Region.NERVOUS, 0, ItemStack.EMPTY); GeneEffects.refresh(player);
        helper.assertTrue(!BlazingDomain.active(player) && BlazingDomain.active(observer), "Uninstall stops only this player");
        BlazingDomain.dimension(new PlayerEvent.PlayerChangedDimensionEvent(observer, observer.level().dimension(), net.minecraft.world.level.Level.NETHER));
        helper.assertTrue(!BlazingDomain.active(observer), "Dimension event ends field");
        install(player, ElementalGenes.BLAZING_NERVES, GeneTypes.Quality.BETA, 0); BlazingDomain.start(player);
        BlazingDomain.logout(new PlayerEvent.PlayerLoggedOutEvent(player)); helper.assertTrue(!BlazingDomain.active(player), "Logout clears active state");
        BlazingDomain.start(player); player.setHealth(0); BlazingDomain.advance(player, now + 1);
        helper.assertTrue(!BlazingDomain.active(player), "Death clears domain"); helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void ribsArmorAndProbability(GameTestHelper helper) {
        var player = player(helper); var cow = cow(helper, player);
        for (var quality : GeneTypes.Quality.values()) {
            install(player, ElementalGenes.WITHERED_RIBS, quality, 0); GeneEffects.refresh(player);
            helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == ElementalGenes.armor(quality), "Armor matches quality");
            player.getRandom().setSeed(1234); var expected = RandomSource.create(1234);
            for (int index = 0; index < 100; index++) {
                cow.removeEffect(MobEffects.WITHER); boolean proc = expected.nextDouble() < ElementalGenes.witherChance(quality);
                GeneMeleeEvents.damaged(new LivingDamageEvent(cow, player.damageSources().playerAttack(player), 1));
                GeneMeleeEvents.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, player.server)); var effect = cow.getEffect(MobEffects.WITHER);
                helper.assertTrue((effect != null) == proc && (effect == null || effect.getDuration() == 60 && effect.getAmplifier() == 0), "One quality-specific roll");
            }
        }
        cow.removeEffect(MobEffects.WITHER);
        var skeleton = EntityType.WITHER_SKELETON.create(helper.getLevel());
        helper.assertTrue(!skeleton.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), player), "Vanilla wither immunity respected");
        GeneMeleeEvents.damaged(new LivingDamageEvent(cow, player.damageSources().arrow(new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), player), player), 1));
        var canceled = new LivingDamageEvent(cow, player.damageSources().playerAttack(player), 1); canceled.setCanceled(true); GeneMeleeEvents.damaged(canceled);
        GeneMeleeEvents.damaged(new LivingDamageEvent(cow, player.damageSources().playerAttack(player), 0)); GeneMeleeEvents.damaged(new LivingDamageEvent(cow, BlazingDomain.source(player), 8));
        GeneMeleeEvents.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, player.server)); helper.assertTrue(!cow.hasEffect(MobEffects.WITHER), "Invalid and non-melee hits rejected");
        cow.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
        for (int index = 0; index < 100; index++) { GeneMeleeEvents.damaged(new LivingDamageEvent(cow, player.damageSources().playerAttack(player), 1)); }
        GeneMeleeEvents.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, player.server)); helper.assertTrue(cow.getEffect(MobEffects.WITHER).getAmplifier() == 2, "Higher wither preserved");
        GeneCapability.get(player).set(GeneTypes.Region.SKELETAL, 0, ItemStack.EMPTY); GeneEffects.refresh(player);
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0, "Uninstall removes armor"); helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void elementalThresholdAndPacket(GameTestHelper helper) {
        var player = player(helper); install(player, ElementalGenes.BLAZING_NERVES, GeneTypes.Quality.ALPHA, 0); var genes = GeneCapability.get(player);
        for (int count = 1; count <= 5; count++) {
            if (count > 1) {
                var id = new ResourceLocation("acc_test", "element_" + count); var region = count == 5 ? GeneTypes.Region.VISION : GeneTypes.Region.MUSCLE;
                if (GeneDefinitions.get(id) == null) { GeneDefinitions.register(new GeneDefinition(id, "fixture", "fixture", GeneTypes.Strength.TETH, region, 1, Set.of(), Set.of(GeneCategories.ELEMENTAL), null)); }
                genes.set(region, count == 5 ? 0 : count - 2, GeneModuleItem.create(id, GeneTypes.Quality.ALPHA));
            }
            for (var source : List.of(player.damageSources().magic(), player.damageSources().indirectMagic(player, player))) {
                var event = new LivingHurtEvent(player, source, 20); GeneDamageEvents.hurt(event); helper.assertTrue(event.getAmount() == (count >= 4 ? 3 : 20), "Four distinct genes activate exactly once");
            }
        }
        for (var source : List.of(player.damageSources().playerAttack(player), player.damageSources().inFire(), player.damageSources().fall(), player.damageSources().starve(), player.damageSources().fellOutOfWorld(), player.damageSources().explosion(player, player))) {
            var event = new LivingHurtEvent(player, source, 20); GeneDamageEvents.hurt(event); helper.assertTrue(event.getAmount() == 20, "Nonmagic unchanged");
        }
        genes.set(GeneTypes.Region.VISION, 0, ItemStack.EMPTY); genes.set(GeneTypes.Region.MUSCLE, 2, ItemStack.EMPTY);
        var event = new LivingHurtEvent(player, player.damageSources().magic(), 20); GeneDamageEvents.hurt(event); helper.assertTrue(event.getAmount() == 20, "Three genes disable reduction immediately");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try { var state = new GeneNetwork.DomainState(player.getUUID(), player.getId(), player.level().dimension().location(), 100, 600);
            GeneNetwork.DomainState.encode(state, buffer); helper.assertTrue(state.equals(GeneNetwork.DomainState.decode(buffer)), "Visual snapshot roundtrip");
        } finally { buffer.release(); }
        helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void synthesisCountsNbtAndInstallation(GameTestHelper helper) {
        for (var gene : List.of(ElementalGenes.BLAZING_NERVES, ElementalGenes.WITHERED_RIBS)) {
            var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(ModGeneRecipes.TYPE.get()).stream()
                    .filter(value -> value.getId().equals(gene)).findFirst().orElseThrow();
            var inputs = new SimpleContainer(4);
            for (var input : recipe.inputs()) { inputs.setItem((input.slot() + 1) % 4, input.ingredient().getItems()[0].copyWithCount(input.count())); }
            helper.assertTrue(recipe.matchesInputs(inputs, GeneTypes.Mode.SYNTHESIS) && recipe.duration() == 600, "Unordered synthesis and thirty second time");
            var wrong = inputs.getItem(1).copy(); inputs.getItem(1).getOrCreateTag().putString("EntityId", "minecraft:blaze");
            helper.assertTrue(!recipe.matchesInputs(inputs, GeneTypes.Mode.SYNTHESIS), "Wrong extract EntityId rejected"); inputs.setItem(1, wrong);
            var state = ModBlocks.GENE_SYNTHESIZER.get().defaultBlockState();
            var machine = new GeneSynthesizerBlockEntity(helper.absolutePos(BlockPos.ZERO), state); machine.setLevel(helper.getLevel());
            for (int slot = 0; slot < 4; slot++) { machine.inventory.setStackInSlot(slot, inputs.getItem(slot)); }
            for (int tick = 0; tick < 600; tick++) { GeneSynthesizerBlockEntity.tick(helper.getLevel(), machine.getBlockPos(), state, machine); }
            var result = machine.inventory.getStackInSlot(4);
            helper.assertTrue(GeneModuleItem.read(result).id().equals(gene) && GeneModuleItem.read(result).quality() == GeneTypes.Quality.ALPHA, "Correct alpha output NBT");
            for (int slot = 0; slot < 4; slot++) { helper.assertTrue(machine.inventory.getStackInSlot(slot).isEmpty(), "Exact input counts consumed"); }
            int body = gene.equals(ElementalGenes.BLAZING_NERVES) ? 0 : 8;
            helper.assertTrue(GeneService.check(new PlayerGenes(), result, body, false).equals("ok") && !GeneService.check(new PlayerGenes(), result, body == 0 ? 8 : 0, false).equals("ok"), "Correct body region required");
            helper.assertTrue(GeneDefinitions.get(gene).cost() == (body == 0 ? 8 : 6), "Capacity cost retained");
        }
        helper.succeed();
    }
}
