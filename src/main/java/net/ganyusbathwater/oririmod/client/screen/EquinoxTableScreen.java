package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.block.menu.EquinoxTableMenu;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Programmatic Celestial Equinox Altar GUI for the Equinox Table.
 * Balances warm Solar Gold and deep Lunar Indigo/Cyan around the 5-slot arcane cross,
 * template socket, live mana-cost crystal, and gilded output forge slot.
 */
public class EquinoxTableScreen extends AbstractContainerScreen<EquinoxTableMenu> {

    // Exact slot origins from EquinoxTableMenu
    private static final int CROSS_TOP_X = 35, CROSS_TOP_Y = 14;
    private static final int CROSS_LEFT_X = 17, CROSS_LEFT_Y = 32;
    private static final int CROSS_CENTER_X = 35, CROSS_CENTER_Y = 32;
    private static final int CROSS_RIGHT_X = 53, CROSS_RIGHT_Y = 32;
    private static final int CROSS_BOTTOM_X = 35, CROSS_BOTTOM_Y = 50;
    private static final int TEMPLATE_X = 89, TEMPLATE_Y = 17;
    private static final int OUTPUT_X = 141, OUTPUT_Y = 32;
    private static final int INV_X = 8, INV_Y = 84, HOTBAR_Y = 142;

    // Compact '?' help badge in upper-right corner
    private static final int HELP_X = 158;
    private static final int HELP_Y = 4;
    private static final int HELP_W = 12;
    private static final int HELP_H = 11;

    // Mana cost pill below output slot
    private static final int COST_PILL_X = 126;
    private static final int COST_PILL_Y = 53;
    private static final int COST_PILL_W = 44;
    private static final int COST_PILL_H = 13;

    public EquinoxTableScreen(EquinoxTableMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = EquinoxTableMenu.IMAGE_WIDTH;
        this.imageHeight = EquinoxTableMenu.IMAGE_HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = 4;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 74;
    }

    private float getAnimTime(float partialTick) {
        // Monotonic wall-clock time wrapped in double precision (~1.6 rad/s, matching 20 tps * 0.08)
        // so animations never stall when ticks are frozen or lose float precision on old worlds.
        return (float) ((net.minecraft.Util.getMillis() * 0.0016D) % (Math.PI * 200.0D));
    }

    private int getPlayerMana() {
        Minecraft mc = Minecraft.getInstance();
        return (mc.player != null) ? ModManaUtil.getMana(mc.player) : 0;
    }

    private boolean isPlayerCreative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isCreative();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        float time = getAnimTime(partialTick);
        boolean hasRecipe = menu.hasRecipe();
        int manaCost = menu.getManaCost();
        int playerMana = getPlayerMana();
        boolean canAfford = !hasRecipe || isPlayerCreative() || playerMana >= manaCost;

        // === 1. CELESTIAL OBSIDIAN-GOLD CHASSIS ===
        drawSanctuaryPanel(gui, x, y, w, h, GOLD, BRONZE);

        // Subtle dual-tone solar/lunar accent line across top inner edge
        fillHorizontalGradient(gui, x + 6, y + 2, x + w / 2, y + 3, GOLD_BRIGHT, AMETHYST);
        fillHorizontalGradient(gui, x + w / 2, y + 2, x + w - 6, y + 3, AMETHYST, CYAN_LIGHT);

        // Recessed upper altar chamber
        drawRecessedWell(gui, x + 7, y + 12, w - 14, 57, 0xFF3A2D42);

        // Subtle celestial aura ring behind the cross matrix
        int crossCx = x + CROSS_CENTER_X + 8;
        int crossCy = y + CROSS_CENTER_Y + 8;
        drawCelestialAura(gui, crossCx, crossCy, hasRecipe, canAfford, time);

        // === 2. CONDUIT LINES LINKING MATRIX -> TEMPLATE/MANA -> OUTPUT ===
        int conduitColor = hasRecipe ? (canAfford ? withAlpha(CYAN_LIGHT, 0xB8) : withAlpha(CRIMSON_LIGHT, 0xA0)) : 0x664A3E5C;
        int conduitDim = hasRecipe ? (canAfford ? withAlpha(GOLD, 0x80) : withAlpha(CRIMSON, 0x70)) : 0x44362C47;

        // Cross arms (center to 4 cardinal slots)
        gui.fill(crossCx - 1, y + CROSS_TOP_Y + 16, crossCx + 1, y + CROSS_BOTTOM_Y, conduitDim);
        gui.fill(x + CROSS_LEFT_X + 16, crossCy - 1, x + CROSS_RIGHT_X, crossCy + 1, conduitDim);

        // Conduit from Right cross slot to Template / Mana column
        int midColX = x + TEMPLATE_X + 8;
        gui.hLine(x + CROSS_RIGHT_X + 17, midColX - 10, crossCy, conduitColor);
        // Vertical bridge linking Template (17..33) <-> Mana Crystal (48..64)
        gui.vLine(midColX, y + TEMPLATE_Y + 17, y + EquinoxTableMenu.GHOST_Y - 2, conduitColor);
        // Horizontal conduit from center column to Output slot
        int arrowStartX = x + TEMPLATE_X + 19;
        int arrowEndX = x + OUTPUT_X - 4;
        gui.fill(arrowStartX, crossCy - 1, arrowEndX, crossCy + 1, conduitDim);
        gui.hLine(arrowStartX, arrowEndX - 1, crossCy, conduitColor);

        // Arrowhead pointing into output slot
        gui.vLine(arrowEndX - 3, crossCy - 3, crossCy + 3, conduitColor);
        gui.vLine(arrowEndX - 2, crossCy - 2, crossCy + 2, conduitColor);
        gui.vLine(arrowEndX - 1, crossCy - 1, crossCy + 1, conduitColor);

        // Traveling energy sparks when a recipe is active
        if (hasRecipe) {
            int sparkCol = canAfford ? CYAN_LIGHT : CRIMSON_LIGHT;
            int span = Math.max(4, arrowEndX - arrowStartX - 4);
            int sparkOffset = (int) ((time * 9.0F) % span);
            gui.fill(arrowStartX + sparkOffset, crossCy - 1, arrowStartX + sparkOffset + 3, crossCy + 1, sparkCol);
        }

        // === 3. CROSS MATRIX SLOTS (5) ===
        drawAltarSlot(gui, x + CROSS_TOP_X, y + CROSS_TOP_Y, GOLD_DIM, false);
        drawAltarSlot(gui, x + CROSS_LEFT_X, y + CROSS_LEFT_Y, GOLD_DIM, false);
        drawAltarSlot(gui, x + CROSS_RIGHT_X, y + CROSS_RIGHT_Y, GOLD_DIM, false);
        drawAltarSlot(gui, x + CROSS_BOTTOM_X, y + CROSS_BOTTOM_Y, GOLD_DIM, false);
        drawAltarSlot(gui, x + CROSS_CENTER_X, y + CROSS_CENTER_Y, hasRecipe ? GOLD_BRIGHT : GOLD, true);

        // === 4. TEMPLATE SOCKET ===
        drawAltarSlot(gui, x + TEMPLATE_X, y + TEMPLATE_Y, AMETHYST, false);
        // Subtle "T" micro-etch when empty
        if (!menu.getSlot(5).hasItem()) {
            gui.drawCenteredString(this.font, "✦", x + TEMPLATE_X + 8, y + TEMPLATE_Y + 4, 0x557B5EA7);
        }

        // === 5. LIVE MANA CRYSTAL ORB AT (GHOST_X, GHOST_Y) ===
        int ghostX = x + EquinoxTableMenu.GHOST_X;
        int ghostY = y + EquinoxTableMenu.GHOST_Y;
        drawManaCrystalSlot(gui, ghostX, ghostY, hasRecipe, canAfford, time);

        // === 6. EQUINOX OUTPUT FORGE SLOT ===
        int outputBorder = hasRecipe ? (canAfford ? EMERALD_LIGHT : CRIMSON_LIGHT) : GOLD;
        // Double gilded frame around output
        gui.fill(x + OUTPUT_X - 3, y + OUTPUT_Y - 3, x + OUTPUT_X + 19, y + OUTPUT_Y + 19, OUTER_SHADOW);
        drawBorder(gui, x + OUTPUT_X - 3, y + OUTPUT_Y - 3, 22, 22, withAlpha(outputBorder, 0x88));
        drawAltarSlot(gui, x + OUTPUT_X, y + OUTPUT_Y, outputBorder, hasRecipe);

        // Compact Mana Cost pill below Output when a recipe is active
        if (hasRecipe) {
            int pillBg = canAfford ? CYAN_DARK : CRIMSON_DARK;
            int pillBorder = canAfford ? CYAN : CRIMSON;
            int pillText = canAfford ? CYAN_LIGHT : CRIMSON_LIGHT;
            String costStr = "✦ " + manaCost;
            int px = x + COST_PILL_X;
            int py = y + COST_PILL_Y;
            gui.fill(px + 1, py, px + COST_PILL_W - 1, py + COST_PILL_H, pillBg);
            gui.fill(px, py + 1, px + COST_PILL_W, py + COST_PILL_H - 1, pillBg);
            drawBorder(gui, px, py, COST_PILL_W, COST_PILL_H, pillBorder);
            gui.drawCenteredString(this.font, costStr, px + COST_PILL_W / 2, py + 3, pillText);
        }

        // === 7. DIVIDER & PLAYER INVENTORY ===
        drawGildedDivider(gui, x + 8, y + 71, w - 16, GOLD_DIM);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawInvSlot(gui, x + INV_X + col * 18, y + INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawInvSlot(gui, x + INV_X + col * 18, y + HOTBAR_Y);
        }

        // === 8. UPPER-RIGHT '?' HELP BADGE ===
        boolean hoveringHelp = isHoveringHelp(mouseX, mouseY);
        int hx = x + HELP_X;
        int hy = y + HELP_Y;
        gui.fill(hx, hy, hx + HELP_W, hy + HELP_H, hoveringHelp ? 0xFF382816 : 0xFF1A1626);
        drawBorder(gui, hx, hy, HELP_W, HELP_H, hoveringHelp ? GOLD_BRIGHT : GOLD_DIM);
        gui.drawCenteredString(this.font, "?", hx + HELP_W / 2, hy + 2, hoveringHelp ? GOLD_BRIGHT : GOLD);
    }

    private void drawCelestialAura(GuiGraphics gui, int cx, int cy, boolean active, boolean canAfford, float time) {
        float pulse = 0.70F + 0.30F * Mth.sin(time * 1.4F);
        int outerR = 26;
        int ringCol = active
                ? (canAfford ? withAlpha(CYAN, (int) (0x34 * pulse)) : withAlpha(CRIMSON, (int) (0x34 * pulse)))
                : 0x22685488;
        int innerCol = active ? withAlpha(GOLD, (int) (0x38 * pulse)) : 0x18D9A94E;

        // Subtle diamond celestial ring behind the cross slots
        for (int i = -outerR; i <= outerR; i++) {
            int rem = outerR - Math.abs(i);
            gui.fill(cx + i, cy - rem, cx + i + 1, cy - rem + 1, ringCol);
            gui.fill(cx + i, cy + rem, cx + i + 1, cy + rem + 1, ringCol);
        }
        int innerR = 19;
        for (int i = -innerR; i <= innerR; i++) {
            int rem = innerR - Math.abs(i);
            gui.fill(cx + i, cy - rem, cx + i + 1, cy - rem + 1, innerCol);
            gui.fill(cx + i, cy + rem, cx + i + 1, cy + rem + 1, innerCol);
        }
    }

    private void drawManaCrystalSlot(GuiGraphics gui, int sx, int sy, boolean hasRecipe, boolean canAfford, float time) {
        int cx = sx + 8;
        int cy = sy + 8;
        int border = hasRecipe ? (canAfford ? CYAN : CRIMSON) : 0xFF352C44;
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, border);
        gui.fill(sx, sy, sx + 16, sy + 16, 0xFF0A0914);

        if (hasRecipe) {
            float pulse = 0.72F + 0.28F * Mth.sin(time * 1.8F);
            int halo = canAfford ? withAlpha(CYAN, (int) (0x55 * pulse)) : withAlpha(CRIMSON, (int) (0x55 * pulse));
            int body = canAfford ? CYAN : CRIMSON;
            int core = canAfford ? CYAN_LIGHT : CRIMSON_LIGHT;
            drawDiamond(gui, cx, cy, 6, halo);
            drawDiamond(gui, cx, cy, 4, body);
            drawDiamond(gui, cx, cy, 2, core);
            gui.fill(cx - 1, cy - 2, cx, cy - 1, 0xFFFFFFFF);
        } else {
            // Dormant empty crystal outline
            drawDiamond(gui, cx, cy, 4, 0x444A3D66);
            drawDiamond(gui, cx, cy, 2, 0x55221B33);
        }
    }

    private void drawAltarSlot(GuiGraphics gui, int sx, int sy, int border, boolean highlight) {
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, border);
        gui.fill(sx, sy, sx + 16, sy + 16, highlight ? 0xFF1D172B : 0xFF13101F);
        gui.hLine(sx, sx + 15, sy, 0x55000000);
        gui.vLine(sx, sy, sy + 15, 0x44000000);
    }

    private void drawInvSlot(GuiGraphics gui, int sx, int sy) {
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF282035);
        gui.fill(sx, sy, sx + 16, sy + 16, 0xFF14111F);
        gui.hLine(sx, sx + 15, sy, 0x44000000);
    }

    private boolean isHoveringHelp(int mouseX, int mouseY) {
        int rx = mouseX - this.leftPos;
        int ry = mouseY - this.topPos;
        return rx >= HELP_X && rx <= HELP_X + HELP_W && ry >= HELP_Y && ry <= HELP_Y + HELP_H;
    }

    private boolean isHoveringManaInfo(int mouseX, int mouseY) {
        int ghostX = this.leftPos + EquinoxTableMenu.GHOST_X;
        int ghostY = this.topPos + EquinoxTableMenu.GHOST_Y;
        if (mouseX >= ghostX && mouseX < ghostX + 16 && mouseY >= ghostY && mouseY < ghostY + 16) {
            return true;
        }
        int px = this.leftPos + COST_PILL_X;
        int py = this.topPos + COST_PILL_Y;
        return mouseX >= px && mouseX < px + COST_PILL_W && mouseY >= py && mouseY < py + COST_PILL_H;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_TITLE, true);
        gui.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_BODY, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);

        if (menu.hasRecipe() && isHoveringManaInfo(mouseX, mouseY)) {
            int manaCost = menu.getManaCost();
            int playerMana = getPlayerMana();
            boolean canAfford = isPlayerCreative() || playerMana >= manaCost;
            ChatFormatting costColor = canAfford ? ChatFormatting.GREEN : ChatFormatting.RED;

            List<Component> lines = List.of(
                    Component.translatable("gui.oririmod.equinox.mana_cost",
                            Component.literal(String.valueOf(manaCost)).withStyle(costColor))
                            .withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.oririmod.equinox.your_mana",
                            Component.literal(String.valueOf(playerMana)).withStyle(ChatFormatting.WHITE))
                            .withStyle(ChatFormatting.GRAY)
            );
            gui.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        } else if (isHoveringHelp(mouseX, mouseY)) {
            List<Component> helpLines = List.of(
                    Component.translatable("gui.oririmod.equinox.help.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.oririmod.equinox.help.line1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.equinox.help.line2").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("gui.oririmod.equinox.help.line3").withStyle(ChatFormatting.AQUA)
            );
            gui.renderComponentTooltip(this.font, helpLines, mouseX, mouseY);
        }
    }
}
