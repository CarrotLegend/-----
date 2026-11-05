package com.carrot123.yixiangshouronglu.shopvalidation;

import com.carrot123.yixiangshouronglu.entity.LegendaryCarrotKingEntity;
import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.network.ShopNetwork;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import com.carrot123.yixiangshouronglu.shop.*;
import net.minecraft.core.BlockPos;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("yixiangshouronglu")
@PrefixGameTestTemplate(false)
public final class ShopRuntimeTests {
    private static LegendaryCarrotKingEntity npc(GameTestHelper helper) {
        var npc = ModEntities.LEGENDARY_CARROT_KING.get().create(helper.getLevel());
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        npc.setPos(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5); npc.setNoGravity(true);
        helper.getLevel().addFreshEntity(npc); return npc;
    }
    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"shop-test"));
        var connection=new Connection(PacketFlow.SERVERBOUND);
        var channel=new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        level.getServer().getPlayerList().placeNewPlayer(connection,player);
        player.setHealth(20);
        return player;
    }
    private static void act(CarrotKingShopMenu menu, ServerPlayer player, int action, int product) {
        menu.act(player, new ShopNetwork.Action(menu.containerId, menu.revision, ShopCapability.get(player).revision(), action, product));
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void catalogAndRealInventory(GameTestHelper helper) {
        var offers=ShopCatalog.offers();
        helper.assertTrue(offers.size()==2 && !offers.get(0).stack().isEmpty() && !offers.get(1).stack().isEmpty(), "Registered products required");
        var potion=offers.get(0).stack();
        helper.assertTrue(potion.is(Items.POTION) && new ResourceLocation("mutantmonsters:chemical_x").equals(ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(potion))), "Potion NBT must match chemical_x");
        helper.assertTrue(new ResourceLocation("mutantmore:compound_z").equals(ForgeRegistries.ITEMS.getKey(offers.get(1).stack().getItem())), "Compound Z identity");
        var player=helper.makeMockPlayer();
        for(int slot=0;slot<36;slot++){player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));}
        helper.assertTrue(ShopInventory.insert(ShopInventory.snapshot(player.getInventory()),potion).isEmpty(),"Full inventory must reject delivery");
        player.getInventory().setItem(5,ItemStack.EMPTY);
        var planned=ShopInventory.insert(ShopInventory.snapshot(player.getInventory()),potion).orElseThrow();
        helper.assertTrue(ItemStack.isSameItemSameTags(planned.get(5),potion),"Delivery retains potion NBT");
        helper.assertTrue(player.getInventory().getItem(5).isEmpty(),"Planning must not mutate inventory");
        helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void npcAndClosingMenuKeepInventory(GameTestHelper helper) {
        var npc=npc(helper); var player=serverPlayer(helper); player.setPos(npc.position());
        helper.assertTrue(npc.getMaxHealth()==20 && npc.isPersistenceRequired() && !npc.removeWhenFarAway(10000),"NPC health and persistence");
        var menu=new CarrotKingShopMenu(5,player.getInventory(),npc.getId(),npc.getUUID(),false); menu.page=CarrotKingShopMenu.SELL;
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND,7));
        helper.assertTrue(menu.stillValid(player),"Nearby NPC session valid");
        player.setPos(npc.getX()+20,npc.getY(),npc.getZ());
        helper.assertTrue(!menu.stillValid(player),"Remote session invalid");
        player.setPos(npc.position()); npc.discard();
        helper.assertTrue(!menu.stillValid(player),"Removed NPC session invalid");
        menu.removed(player);
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND)==7,"Closing preserves inventory exactly once");
        helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void welcomeSellBuyAndReplay(GameTestHelper helper) {
        var npc=npc(helper); var player=serverPlayer(helper); player.setPos(npc.position());
        npc.interact(player,InteractionHand.MAIN_HAND);
        var menu=(CarrotKingShopMenu)player.containerMenu;
        helper.assertTrue(menu.page==CarrotKingShopMenu.WELCOME && ShopCapability.get(player).welcomed(),"First interaction must greet and persist state");
        act(menu,player,3,0); helper.assertTrue(menu.status.equals("invalid"),"Cannot buy during welcome");
        act(menu,player,0,-1); helper.assertTrue(menu.page==CarrotKingShopMenu.BUY,"Continue opens buy page");
        player.closeContainer(); npc.interact(player,InteractionHand.MAIN_HAND); menu=(CarrotKingShopMenu)player.containerMenu;
        helper.assertTrue(menu.page==CarrotKingShopMenu.BUY,"Second interaction must skip welcome");
        act(menu,player,3,0); helper.assertTrue(menu.status.equals("insufficient_coins"),"Insufficient balance refused");
        act(menu,player,2,-1); player.getInventory().setItem(0,new ItemStack(Items.DIAMOND,4)); menu.broadcastChanges();
        var request=new ShopNetwork.Action(menu.containerId,menu.revision,ShopCapability.get(player).revision(),5,2);
        menu.act(player,request);
        helper.assertTrue(ShopCapability.get(player).coins()==20 && player.getInventory().countItem(Items.DIAMOND)==0,"Sale consumes stack and credits twenty");
        menu.act(player,request); helper.assertTrue(menu.status.equals("stale") && ShopCapability.get(player).coins()==20,"Replay cannot credit again");
        act(menu,player,1,-1);
        for(int slot=0;slot<36;slot++){player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));}
        menu.broadcastChanges(); act(menu,player,3,0); helper.assertTrue(menu.status.equals("inventory_full") && ShopCapability.get(player).coins()==20,"Full bag preserves coins");
        player.getInventory().setItem(0,ItemStack.EMPTY); menu.broadcastChanges(); act(menu,player,3,0);
        helper.assertTrue(ShopCapability.get(player).coins()==18 && player.getInventory().getItem(0).is(Items.POTION),"Purchase delivers potion and debits two");
        var saved=ShopCapability.get(player).save(); var copy=new PlayerShopData(); copy.load(saved);
        helper.assertTrue(copy.state().equals(ShopCapability.get(player).state()),"Wallet persistence roundtrip");
        helper.succeed();
    }
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void inventorySalesAndValidation(GameTestHelper helper) {
        var npc=npc(helper); var player=serverPlayer(helper); player.setPos(npc.position());
        npc.interact(player,InteractionHand.MAIN_HAND);
        var menu=(CarrotKingShopMenu)player.containerMenu; act(menu,player,0,-1); act(menu,player,2,-1);
        helper.assertTrue(menu.slots.size()==36,"Exactly thirty-six player slots");
        Item[] items={Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.NETHERITE_INGOT};
        long[] prices={1,2,5,20}; long balance=0;
        for(int index=0;index<4;index++) {
            var named=new ItemStack(items[index],3); named.setHoverName(net.minecraft.network.chat.Component.literal("metadata"));
            player.getInventory().setItem(0,named); player.getInventory().setItem(9,new ItemStack(items[index],5));
            player.getInventory().setItem(36,new ItemStack(items[index],2)); player.getInventory().setItem(40,new ItemStack(items[index],2));
            menu.broadcastChanges(); act(menu,player,4,index); balance+=prices[index];
            helper.assertTrue(menu.status.equals("sale_success") && ShopCapability.get(player).coins()==balance,"Single sale price");
            helper.assertTrue(player.getInventory().getItem(0).getCount()==2 && player.getInventory().getItem(0).hasCustomHoverName(),"Remaining NBT intact");
            act(menu,player,5,index); balance+=7*prices[index];
            helper.assertTrue(ShopCapability.get(player).coins()==balance && player.getInventory().getItem(9).isEmpty(),"Bulk removes split stacks");
            helper.assertTrue(player.getInventory().getItem(36).getCount()==2 && player.getInventory().getItem(40).getCount()==2,"Armor and offhand excluded");
            act(menu,player,4,index); helper.assertTrue(menu.status.equals("no_item") && ShopCapability.get(player).coins()==balance,"Empty inventory cannot credit");
        }
        act(menu,player,5,99); helper.assertTrue(menu.status.equals("invalid_product"),"Invalid sale index refused");
        var stale=new ShopNetwork.Action(menu.containerId,menu.revision,ShopCapability.get(player).revision(),5,0);
        player.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,8)); menu.act(player,stale);
        helper.assertTrue(menu.status.equals("stale") && player.getInventory().getItem(0).getCount()==8,"New inventory invalidates old request before tick sync");
        menu.setCarried(new ItemStack(Items.IRON_INGOT,2)); act(menu,player,5,0);
        helper.assertTrue(menu.status.equals("cursor_not_empty") && player.getInventory().getItem(0).getCount()==8,"Cursor transaction refused"); menu.setCarried(ItemStack.EMPTY);
        menu.quickMoveStack(player,27);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty() && player.getInventory().getItem(9).getCount()==8,"Hotbar shift move to main inventory");
        menu.quickMoveStack(player,0);
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(0).getCount()==8,"Main inventory shift move to hotbar");
        var wallet=ShopCapability.get(player); wallet.add(Long.MAX_VALUE-wallet.coins()); menu.broadcastChanges(); act(menu,player,5,0);
        helper.assertTrue(menu.status.equals("balance_limit") && player.getInventory().getItem(0).getCount()==8 && wallet.coins()==Long.MAX_VALUE,"Overflow is atomic refusal");
        player.closeContainer();
        helper.assertTrue(player.getInventory().getItem(0).getCount()==8,"Close does not duplicate or lose inventory");
        helper.succeed();
    }
}
