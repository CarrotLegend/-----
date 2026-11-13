package com.carrot123.yixiangshouronglu.menu;

import com.carrot123.yixiangshouronglu.block.entity.GeneResearchTableBlockEntity;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchCapability;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchDefinition;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchRegistry;
import com.carrot123.yixiangshouronglu.gene.research.GeneResearchService;
import com.carrot123.yixiangshouronglu.network.GeneResearchNetwork;
import com.carrot123.yixiangshouronglu.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class GeneResearchTableMenu extends AbstractContainerMenu {
    public static final int PAGE_RESEARCH = 0;
    public static final int PAGE_SYNTHESIS = 1;
    public static final int CHANGE_PAGE = 0;
    public static final int SELECT = 1;
    public static final int RUN = 2;
    public static final int MACHINE_SLOT_X = 220;
    public static final int MACHINE_SLOT_Y = 112;
    private final BlockPos pos;
    private final Player owner;
    private final GeneResearchTableBlockEntity table;
    private UUID session;
    private long revision;
    private int page;
    private ResourceLocation selection;
    private GeneResearchService.Outcome outcome;
    private boolean waiting;

    public GeneResearchTableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), null);
    }

    public GeneResearchTableMenu(int id, Inventory inventory, GeneResearchTableBlockEntity table) {
        this(id, inventory, table.getBlockPos(), table);
    }

    private GeneResearchTableMenu(int id, Inventory inventory, BlockPos pos, GeneResearchTableBlockEntity table) {
        super(ModMenus.GENE_RESEARCH_TABLE.get(), id);
        this.pos = pos;
        this.owner = inventory.player;
        this.table = table;
        this.session = table == null ? null : UUID.randomUUID();
        SimpleContainer items = table == null ? new SimpleContainer(2) : table.items();
        for (int index = 0; index < 2; index++) {
            int slot = index;
            addSlot(new Slot(items, slot, MACHINE_SLOT_X, MACHINE_SLOT_Y) {
                @Override public boolean isActive() {
                    return page == slot;
                }

                @Override public boolean mayPlace(ItemStack stack) {
                    return slot == PAGE_RESEARCH && isActive() && !GeneResearchDefinition.extractId(stack).isEmpty();
                }

                @Override public boolean mayPickup(Player player) {
                    return isActive() && player == owner;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, row * 9 + column + 9, 68 + column * 18, 178 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 68 + column * 18, 236));
        }
    }

    public int page() { return page; }
    public ResourceLocation selection() { return selection; }
    public GeneResearchService.Outcome outcome() { return outcome; }
    public boolean ready() { return session != null && !waiting; }

    public void request(int action, int requestedPage, ResourceLocation target) {
        if (!ready()) {
            return;
        }
        waiting = true;
        String extract = action == RUN && page == PAGE_RESEARCH
                ? GeneResearchDefinition.extractId(getSlot(0).getItem()) : "";
        GeneResearchNetwork.send(new GeneResearchNetwork.Action(containerId, session, revision, action, requestedPage,
                target == null ? "" : target.toString(), extract));
    }

    public void act(ServerPlayer player, GeneResearchNetwork.Action action) {
        if (player.containerMenu != this || table == null || player != owner) {
            return;
        }
        if (!stillValid(player) || player.isSpectator()) {
            respond(player, new GeneResearchService.Outcome("invalid_table", 0));
            return;
        }
        if (!session.equals(action.session()) || revision != action.revision()) {
            respond(player, new GeneResearchService.Outcome("stale", 0));
            return;
        }
        revision++;
        var data = GeneResearchCapability.get(player);
        ResourceLocation target = action.target().isEmpty() ? null : ResourceLocation.tryParse(action.target());
        GeneResearchDefinition definition = GeneResearchRegistry.get(target);
        if (action.action() == CHANGE_PAGE && (action.page() == PAGE_RESEARCH || action.page() == PAGE_SYNTHESIS)) {
            page = action.page();
            selection = null;
            respond(player, null);
        } else if (action.page() != page || data == null) {
            respond(player, new GeneResearchService.Outcome("stale", 0));
        } else if (action.action() == SELECT) {
            if (definition == null || page == PAGE_SYNTHESIS && data.progress(target) < 100) {
                respond(player, new GeneResearchService.Outcome("no_selection", 0));
                return;
            }
            selection = target;
            respond(player, null);
        } else if (action.action() == RUN) {
            if (definition == null || !definition.id().equals(selection)) {
                respond(player, new GeneResearchService.Outcome("no_selection", 0));
                return;
            }
            GeneResearchService.Outcome result = page == PAGE_RESEARCH
                    ? GeneResearchService.research(player, table, definition, action.extract())
                    : GeneResearchService.synthesize(player, table, definition);
            if (result.key().equals("research_success")) {
                GeneResearchNetwork.syncPlayer(player);
            }
            broadcastChanges();
            respond(player, result);
        } else {
            respond(player, new GeneResearchService.Outcome("stale", 0));
        }
    }

    public void respond(ServerPlayer player, GeneResearchService.Outcome result) {
        GeneResearchNetwork.syncMenu(player, new GeneResearchNetwork.MenuState(containerId, session, revision, page,
                selection == null ? "" : selection.toString(), result == null ? "" : result.key(), result == null ? 0 : result.gain()));
    }

    public void apply(GeneResearchNetwork.MenuState state) {
        if (table != null || state.container() != containerId || session != null && !session.equals(state.session())
                || state.revision() < revision) {
            return;
        }
        session = state.session();
        revision = state.revision();
        page = state.page();
        selection = state.selection().isEmpty() ? null : ResourceLocation.tryParse(state.selection());
        outcome = state.status().isEmpty() ? null : new GeneResearchService.Outcome(state.status(), state.gain());
        waiting = false;
    }

    @Override public boolean stillValid(Player player) {
        return player == owner && (table == null || !table.isRemoved() && player.level() == table.getLevel()
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                && player.level().hasChunkAt(pos) && player.level().getBlockEntity(pos) == table);
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (stillValid(player) && (slot < 0 || slot >= 2 || slot == page)) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || index < 2 && index != page) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (page != PAGE_RESEARCH || !getSlot(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
