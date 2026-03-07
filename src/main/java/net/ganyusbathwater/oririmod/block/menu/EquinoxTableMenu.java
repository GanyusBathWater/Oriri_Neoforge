package net.ganyusbathwater.oririmod.block.menu;

import net.ganyusbathwater.oririmod.block.entity.EquinoxTableBlockEntity;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.recipe.EquinoxTableRecipe;
import net.ganyusbathwater.oririmod.recipe.ModRecipeTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class EquinoxTableMenu extends AbstractContainerMenu {

    // ── GUI dimensions (screen-space, derived from 1024×1024 ÷ 4) ──
    // The actual GUI background is ~700×590 px in the 1024 texture,
    // rendered at 175×148 screen px. Adjust these if slot positions drift.
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 166;

    // ── Slot positions (screen-space x, y for the 16×16 item area) ──
    // Cross-shaped input (5 slots)
    private static final int CROSS_TOP_X = 35, CROSS_TOP_Y = 14;
    private static final int CROSS_LEFT_X = 17, CROSS_LEFT_Y = 32;
    private static final int CROSS_CENTER_X = 35, CROSS_CENTER_Y = 32;
    private static final int CROSS_RIGHT_X = 53, CROSS_RIGHT_Y = 32;
    private static final int CROSS_BOTTOM_X = 35, CROSS_BOTTOM_Y = 50;
    // Template slot
    private static final int TEMPLATE_X = 89, TEMPLATE_Y = 17;
    // Output slot
    private static final int OUTPUT_X = 141, OUTPUT_Y = 32;
    // Ghost / mana-cost display position (NOT a real slot — rendered by Screen)
    public static final int GHOST_X = 89, GHOST_Y = 48;

    // ── Player inventory origin ──
    private static final int INV_X = 8;
    private static final int INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    // ── Internal ──
    private final ItemStackHandler handler;
    private final Level level;
    private final EquinoxTableBlockEntity blockEntity;
    private final ContainerData data; // index 0 = manaCost of matched recipe

    // Cached recipe result
    private @Nullable EquinoxTableRecipe currentRecipe;

    /** Client-side constructor (used by MenuType factory on the client) */
    public EquinoxTableMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, null);
    }

    /** Server-side constructor */
    public EquinoxTableMenu(int containerId, Inventory playerInv,
            @Nullable EquinoxTableBlockEntity blockEntity) {
        super(ModMenuTypes.EQUINOX_TABLE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.handler = blockEntity != null ? blockEntity.getItemHandler()
                : new ItemStackHandler(EquinoxTableBlockEntity.SLOT_COUNT);
        this.level = playerInv.player.level();
        this.data = new SimpleContainerData(1); // [0] = mana cost

        // ── Cross-shaped input slots (0-4) ──
        addSlot(new InputSlot(0, CROSS_TOP_X, CROSS_TOP_Y)); // Top
        addSlot(new InputSlot(1, CROSS_LEFT_X, CROSS_LEFT_Y)); // Left
        addSlot(new InputSlot(2, CROSS_CENTER_X, CROSS_CENTER_Y)); // Center
        addSlot(new InputSlot(3, CROSS_RIGHT_X, CROSS_RIGHT_Y)); // Right
        addSlot(new InputSlot(4, CROSS_BOTTOM_X, CROSS_BOTTOM_Y)); // Bottom

        // ── Template slot (5) ──
        addSlot(new InputSlot(5, TEMPLATE_X, TEMPLATE_Y));

        // ── Output slot (6) — take-only, consumes inputs + mana on take ──
        addSlot(new SlotItemHandler(handler, 6, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (!level.isClientSide()) {
                    // Check & consume mana
                    int cost = data.get(0);
                    if (cost > 0 && !player.isCreative()) {
                        ModManaUtil.tryConsumeMana(player, cost);
                    }
                    // Consume one of each input
                    for (int i = 0; i < 6; i++) {
                        handler.extractItem(i, 1, false);
                    }
                    // Re-check for further crafting
                    updateRecipeOutput();
                }
                super.onTake(player, stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                if (level.isClientSide())
                    return currentRecipe != null;
                int cost = data.get(0);
                if (cost > 0 && !player.isCreative()) {
                    return ModManaUtil.getMana(player) >= cost;
                }
                return currentRecipe != null;
            }
        });

        // ── Player inventory (slots 7-33) ──
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }

        // ── Player hotbar (slots 34-42) ──
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }

        // Sync mana cost to client
        addDataSlots(data);

        // Initial recipe check
        updateRecipeOutput();
    }

    /** Called whenever any slot in the container changes. */
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateRecipeOutput();
    }

    /** Re-scan recipes and update the output slot + mana cost data. */
    private void updateRecipeOutput() {
        // Build a RecipeInput from the 6 input slots
        RecipeInput input = new RecipeInput() {
            @Override
            public ItemStack getItem(int index) {
                if (index < 0 || index >= 6)
                    return ItemStack.EMPTY;
                return handler.getStackInSlot(index);
            }

            @Override
            public int size() {
                return 6;
            }
        };

        Optional<RecipeHolder<EquinoxTableRecipe>> match = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.EQUINOX_TABLE.get(), input, level);

        if (match.isPresent()) {
            EquinoxTableRecipe recipe = match.get().value();
            currentRecipe = recipe;
            handler.setStackInSlot(EquinoxTableBlockEntity.SLOT_OUTPUT,
                    recipe.assemble(input, level.registryAccess()));
            data.set(0, recipe.getManaCost());
        } else {
            currentRecipe = null;
            handler.setStackInSlot(EquinoxTableBlockEntity.SLOT_OUTPUT, ItemStack.EMPTY);

            // Placeholder for alignment/testing: if the center slot is filled, show 100
            // mana cost
            if (!handler.getStackInSlot(EquinoxTableBlockEntity.SLOT_CENTER).isEmpty()) {
                data.set(0, 100);
            } else {
                data.set(0, 0);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        int containerSlots = 7; // 0-6 = equinox table
        int invStart = containerSlots; // 7
        int invEnd = invStart + 27; // 34
        int hotbarEnd = invEnd + 9; // 43

        if (index == EquinoxTableBlockEntity.SLOT_OUTPUT) {
            // Output → player inventory
            if (!moveItemStackTo(current, invStart, hotbarEnd, true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(current, original);
        } else if (index < containerSlots) {
            // Equinox input/template → player inventory
            if (!moveItemStackTo(current, invStart, hotbarEnd, false))
                return ItemStack.EMPTY;
        } else if (index < invEnd) {
            // Player inventory → equinox inputs (try 0-5), else hotbar
            if (!moveItemStackTo(current, 0, 6, false)) {
                if (!moveItemStackTo(current, invEnd, hotbarEnd, false))
                    return ItemStack.EMPTY;
            }
        } else {
            // Hotbar → equinox inputs, else inventory
            if (!moveItemStackTo(current, 0, 6, false)) {
                if (!moveItemStackTo(current, invStart, invEnd, false))
                    return ItemStack.EMPTY;
            }
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (current.getCount() == original.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, current);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null)
            return true;
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    /** Used by the screen to read the current mana cost for the ghost display. */
    public int getManaCost() {
        return data.get(0);
    }

    /** Used by the screen to know if a recipe is currently matched. */
    public boolean hasRecipe() {
        return currentRecipe != null || data.get(0) > 0;
    }

    private class InputSlot extends SlotItemHandler {
        public InputSlot(int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            updateRecipeOutput();
        }
    }
}
