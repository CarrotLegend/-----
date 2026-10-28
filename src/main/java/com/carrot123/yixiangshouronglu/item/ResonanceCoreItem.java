package com.carrot123.yixiangshouronglu.item;

import com.carrot123.yixiangshouronglu.menu.ResonanceMenu;
import com.carrot123.yixiangshouronglu.resonance.ResonanceLinks;
import com.carrot123.yixiangshouronglu.resonance.ResonanceLinks.Link;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public final class ResonanceCoreItem extends Item implements ICurioItem {
    public ResonanceCoreItem(Properties properties) {
        super(properties);
    }

    public static void bindOrUnbind(ItemStack stack, ServerPlayer player, BlockPos pos, boolean unlink) {
        Link link = new Link(player.level().dimension().location().toString(), pos.immutable());
        boolean success = unlink ? ResonanceLinks.remove(stack, link) : ResonanceLinks.add(stack, link);
        String key = unlink ? (success ? "message.yixiangshouronglu.unbound" : "message.yixiangshouronglu.not_bound") : success ? "message.yixiangshouronglu.bound" : ResonanceLinks.contains(stack, link) ? "message.yixiangshouronglu.already_bound" : "message.yixiangshouronglu.bind_limit";
        player.displayClientMessage(Component.translatable(key), true);
        if (success) {
            player.getInventory().setChanged();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var entries = ResonanceMenu.snapshots(serverPlayer, stack);
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, menuPlayer) -> new ResonanceMenu(id, inventory, entries), Component.translatable("screen.yixiangshouronglu.resonance")), buffer -> ResonanceMenu.write(buffer, entries));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String translated = Component.translatable("tooltip.yixiangshouronglu.resonance_core.bind").getString();
        int start = translated.indexOf("{goldflow/}");
        int end = translated.lastIndexOf("{goldflow/}");
        String message = start >= 0 && end > start ? translated.substring(start + 11, end) : translated;
        tooltip.add(goldFlow(message));
        tooltip.add(Component.translatable("tooltip.yixiangshouronglu.resonance_core.open").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static Component goldFlow(String message) {
        MutableComponent line = Component.empty();
        double time = System.currentTimeMillis() / 125.0D;
        for (int index = 0; index < message.length(); index++) {
            double wave = (Math.sin(time - index * 0.65D) + 1.0D) / 2.0D;
            int red = 225 + (int) (30 * wave);
            int green = 143 + (int) (99 * wave);
            int blue = 21 + (int) (114 * wave);
            int color = (red << 16) | (green << 8) | blue;
            line.append(Component.literal(String.valueOf(message.charAt(index))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        }
        return line;
    }
}
