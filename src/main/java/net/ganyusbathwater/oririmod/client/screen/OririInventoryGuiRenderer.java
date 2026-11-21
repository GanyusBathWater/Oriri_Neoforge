package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Programmatic renderer for the optional Oriri Survival, Creative, and Curios
 * player inventory GUIs, styled after the dark purple-obsidian Soul Harvester
 * reliquary with antique gold corner filigree and a chromatic gold-amethyst-cyan
 * edge accent.
 */
@OnlyIn(Dist.CLIENT)
public final class OririInventoryGuiRenderer {

    static final int CHASSIS_TOP     = 0xF4251336;
    static final int CHASSIS_BOT     = 0xF6130A1E;
    static final int CHASSIS_BORDER  = 0xFF5D2A73;
    static final int CRIMSON_TRIM    = 0xFF7D1B38;

    static final int SLOT_BORDER     = 0xFF312042;
    static final int SLOT_FILL       = 0xFF140D1F;
    static final int HOTBAR_BORDER   = 0xFF422C57;
    static final int EQUIP_BORDER    = 0xFF593D6E;

    private static Method cachedCurioIsCosmetic;
    private static boolean curioReflectionAttempted = false;

    private OririInventoryGuiRenderer() {}

    // ── Shared Reliquary Chassis with Chromatic Edge Line ──

    static void drawReliquaryChassis(GuiGraphics gui, int x, int y, int w, int h) {
        drawReliquaryChassis(gui, x, y, w, h, true, true, true, true);
    }

    static void drawReliquaryChassis(GuiGraphics gui, int x, int y, int w, int h,
                                     boolean filigreeTL, boolean filigreeTR,
                                     boolean filigreeBL, boolean filigreeBR) {
        // Outer drop shadow
        gui.fill(x + 2, y + h, x + w - 1, y + h + 2, 0x66000000);
        gui.fill(x + w, y + 2, x + w + 2, y + h - 1, 0x66000000);

        // Chamfered obsidian outline
        gui.fill(x + 1, y, x + w - 1, y + h, OUTER_SHADOW);
        gui.fill(x, y + 1, x + w, y + h - 1, OUTER_SHADOW);

        // Deep dark purple-obsidian interior gradient (Soul Harvester palette)
        gui.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, CHASSIS_TOP, CHASSIS_BOT);

        // Outer amethyst frame + crimson lower trim
        drawBorder(gui, x + 1, y + 1, w - 2, h - 2, CHASSIS_BORDER);
        gui.hLine(x + 2, x + w - 3, y + h - 3, CRIMSON_TRIM);

        // Chromatic gold -> amethyst -> cyan accent edge along the top rim
        int mid = x + w / 2;
        fillHorizontalGradient(gui, x + 4, y + 2, mid, y + 3, GOLD_BRIGHT, AMETHYST);
        fillHorizontalGradient(gui, mid, y + 2, x + w - 4, y + 3, AMETHYST, CYAN_LIGHT);

        // Selective antique gold corner filigree (omits corners fused with active creative tabs)
        drawSelectiveCornerFiligree(gui, x + 1, y + 1, w - 2, h - 2, 8, GOLD,
                filigreeTL, filigreeTR, filigreeBL, filigreeBR);
    }

    private static void drawSelectiveCornerFiligree(GuiGraphics gfx, int x, int y, int w, int h, int len, int color,
                                                    boolean tl, boolean tr, boolean bl, boolean br) {
        int r = x + w - 1;
        int b = y + h - 1;
        int stud = withAlpha(color, 0xCC);

        if (tl) {
            gfx.hLine(x + 1, x + len, y, color);
            gfx.vLine(x, y + 1, y + len, color);
            gfx.fill(x + 2, y + 2, x + 4, y + 4, stud);
        }
        if (tr) {
            gfx.hLine(r - len, r - 1, y, color);
            gfx.vLine(r, y + 1, y + len, color);
            gfx.fill(r - 3, y + 2, r - 1, y + 4, stud);
        }
        if (bl) {
            gfx.hLine(x + 1, x + len, b, color);
            gfx.vLine(x, b - len, b - 1, color);
            gfx.fill(x + 2, b - 3, x + 4, b - 1, stud);
        }
        if (br) {
            gfx.hLine(r - len, r - 1, b, color);
            gfx.vLine(r, b - len, b - 1, color);
            gfx.fill(r - 3, b - 3, r - 1, b - 1, stud);
        }
    }

    static void drawSlot(GuiGraphics gui, int sx, int sy, int border, int fill) {
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, border);
        gui.fill(sx, sy, sx + 16, sy + 16, fill);
        gui.hLine(sx, sx + 15, sy, 0x44000000);
        gui.vLine(sx, sy, sy + 15, 0x33000000);
    }

    static void drawPortraitChamber(GuiGraphics gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, OUTER_SHADOW);
        drawBorder(gui, x1, y1, x2 - x1, y2 - y1, GOLD_DIM);
        gui.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF1F122E, 0xFF0D0816);
        int mid = (x1 + x2) / 2;
        fillHorizontalGradient(gui, x1 + 2, y1 + 1, mid, y1 + 2, withAlpha(GOLD, 0x66), withAlpha(AMETHYST, 0x66));
        fillHorizontalGradient(gui, mid, y1 + 1, x2 - 2, y1 + 2, withAlpha(AMETHYST, 0x66), withAlpha(CYAN_LIGHT, 0x66));
    }

    // ── Survival Inventory (InventoryScreen: 176 × 166) ──

    public static void renderSurvivalBg(GuiGraphics gui, int x, int y, int w, int h,
                                        float xMouse, float yMouse, LivingEntity player) {
        drawReliquaryChassis(gui, x, y, w, h);

        // Armor column (4 slots at x+8, y+8/26/44/62)
        for (int i = 0; i < 4; i++) {
            drawSlot(gui, x + 8, y + 8 + i * 18, EQUIP_BORDER, SLOT_FILL);
        }

        // Player 3D preview chamber (26, 8) -> (75, 78)
        drawPortraitChamber(gui, x + 26, y + 8, x + 75, y + 78);

        // Offhand slot (77, 62) with subtle gold trim
        drawSlot(gui, x + 77, y + 62, GOLD_DIM, SLOT_FILL);

        // 2×2 Crafting bay (98..132, 18..52) + arrow + output slot (154, 28)
        drawRecessedWell(gui, x + 95, y + 15, 40, 40, 0xFF3E2650);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                drawSlot(gui, x + 98 + col * 18, y + 18 + row * 18, SLOT_BORDER, SLOT_FILL);
            }
        }

        // Chromatic crafting arrow (137..150, cy = 35)
        int ay = y + 35;
        fillHorizontalGradient(gui, x + 137, ay - 1, x + 148, ay + 2, AMETHYST, CYAN_LIGHT);
        gui.vLine(x + 147, ay - 3, ay + 3, CYAN_LIGHT);
        gui.vLine(x + 148, ay - 2, ay + 2, CYAN_LIGHT);
        gui.vLine(x + 149, ay - 1, ay + 1, GOLD_BRIGHT);

        // Output slot (154, 28) with gilded double rim
        drawBorder(gui, x + 152, y + 26, 20, 20, withAlpha(GOLD, 0x77));
        drawSlot(gui, x + 154, y + 28, GOLD, 0xFF1A1128);

        // Gilded divider above player inventory
        drawGildedDivider(gui, x + 8, y + 80, w - 16, GOLD_DIM);

        // Main inventory (3×9 at x+8, y+84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(gui, x + 8 + col * 18, y + 84 + row * 18, SLOT_BORDER, SLOT_FILL);
            }
        }

        // Hotbar (1×9 at x+8, y+142)
        for (int col = 0; col < 9; col++) {
            drawSlot(gui, x + 8 + col * 18, y + 142, HOTBAR_BORDER, SLOT_FILL);
        }

        // Render live 3D player model inside portrait chamber (exact vanilla bounds)
        if (player != null) {
            gui.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, xMouse, yMouse, player);
        }
    }

    public static void renderSurvivalTitle(GuiGraphics gui, Font font, Component title,
                                           int titleLabelX, int titleLabelY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, TEXT_TITLE, true);
    }

    // ── Curios Inventory Screen (Main 176×166 Panel + Dynamic Left Curios Wing) ──

    public static void renderCuriosBg(GuiGraphics gui, int leftPos, int topPos, int imageHeight,
                                      int mouseX, int mouseY, LivingEntity player,
                                      List<Slot> allSlots, List<Renderable> renderables) {
        // 1. Collect all visible left-side Curio slots (negative x in CuriosContainer)
        List<Slot> curioSlots = new ArrayList<>();
        int minSlotX = Integer.MAX_VALUE;
        int maxSlotX = Integer.MIN_VALUE;
        int minSlotY = Integer.MAX_VALUE;
        int maxSlotY = Integer.MIN_VALUE;

        for (Slot slot : allSlots) {
            if (slot != null && slot.isActive() && slot.x < 0 && slot.x > -1000) {
                curioSlots.add(slot);
                if (slot.x < minSlotX) minSlotX = slot.x;
                if (slot.x > maxSlotX) maxSlotX = slot.x;
                if (slot.y < minSlotY) minSlotY = slot.y;
                if (slot.y > maxSlotY) maxSlotY = slot.y;
            }
        }

        // 2. Check if Curios rendered its Cosmetic toggle tab above the left panel
        boolean hasCosmeticTab = false;
        if (renderables != null) {
            for (Renderable r : renderables) {
                if (r != null && r.getClass().getSimpleName().equals("CosmeticButton")) {
                    hasCosmeticTab = true;
                    break;
                }
            }
        }

        // 3. Draw optional Cosmetic top tab behind the Curios side panel
        if (hasCosmeticTab && !curioSlots.isEmpty()) {
            int tabX = leftPos - 31;
            int tabY = topPos - 23;
            int tabW = 28;
            int tabH = 24;
            gui.fill(tabX + 1, tabY, tabX + tabW - 1, tabY + tabH, OUTER_SHADOW);
            gui.fill(tabX, tabY + 1, tabX + tabW, tabY + tabH, OUTER_SHADOW);
            gui.fillGradient(tabX + 1, tabY + 1, tabX + tabW - 1, tabY + tabH, CHASSIS_TOP, 0xF41B0E2A);
            gui.hLine(tabX + 1, tabX + tabW - 2, tabY, GOLD_DIM);
            gui.vLine(tabX, tabY + 1, tabY + tabH - 1, GOLD_DIM);
            gui.vLine(tabX + tabW - 1, tabY + 1, tabY + tabH - 1, GOLD_DIM);
            int mid = tabX + tabW / 2;
            fillHorizontalGradient(gui, tabX + 2, tabY + 1, mid, tabY + 2, GOLD_BRIGHT, AMETHYST);
            fillHorizontalGradient(gui, mid, tabY + 1, tabX + tabW - 2, tabY + 2, AMETHYST, CYAN_LIGHT);
        }

        // 4. Draw matching Oriri Reliquary Curios side panel hugging all active Curio slots
        if (!curioSlots.isEmpty()) {
            int sideX = leftPos + minSlotX - 8;
            int sideY = topPos;
            int sideRight = leftPos + maxSlotX + 16 + 8;
            int sideW = Math.max(30, sideRight - sideX);
            int sideH = Math.max(32, (maxSlotY + 16 + 7));

            // Keep a crisp 2px gap or flush edge next to the main inventory panel
            drawReliquaryChassis(gui, sideX, sideY, sideW, sideH, true, true, true, true);

            for (Slot slot : curioSlots) {
                boolean cosmetic = isCosmeticCurioSlot(slot);
                int border = cosmetic ? CYAN : EQUIP_BORDER;
                int fill = cosmetic ? 0xFF101929 : SLOT_FILL;
                drawSlot(gui, leftPos + slot.x, topPos + slot.y, border, fill);
            }
        }

        // 5. Draw the standard Oriri Survival Inventory chassis + live 3D player model
        renderSurvivalBg(gui, leftPos, topPos, 176, imageHeight, (float) mouseX, (float) mouseY, player);
    }

    private static boolean isCosmeticCurioSlot(Slot slot) {
        if (slot == null) return false;
        if (!curioReflectionAttempted) {
            curioReflectionAttempted = true;
            try {
                Class<?> curioSlotClass = Class.forName("top.theillusivec4.curios.common.inventory.CurioSlot");
                cachedCurioIsCosmetic = curioSlotClass.getMethod("isCosmetic");
            } catch (Throwable ignored) {
                cachedCurioIsCosmetic = null;
            }
        }
        if (cachedCurioIsCosmetic != null && cachedCurioIsCosmetic.getDeclaringClass().isInstance(slot)) {
            try {
                return Boolean.TRUE.equals(cachedCurioIsCosmetic.invoke(slot));
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    // ── Creative Inventory (CreativeModeInventoryScreen: 195 × 136) ──

    public static void renderCreativeMainPanel(GuiGraphics gui, int x, int y, int w, int h,
                                               CreativeModeTab selectedTab, EditBox searchBox,
                                               float scrollOffs, boolean canScroll,
                                               boolean selTL, boolean selTR, boolean selBL, boolean selBR,
                                               int mouseX, int mouseY, float partialTick) {
        drawReliquaryChassis(gui, x, y, w, h, !selTL, !selTR, !selBL, !selBR);

        boolean isInventoryTab = selectedTab.getType() == CreativeModeTab.Type.INVENTORY;

        if (isInventoryTab) {
            // Armor slots: (54, 6), (54, 33), (108, 6), (108, 33)
            drawSlot(gui, x + 54, y + 6, EQUIP_BORDER, SLOT_FILL);
            drawSlot(gui, x + 54, y + 33, EQUIP_BORDER, SLOT_FILL);
            drawSlot(gui, x + 108, y + 6, EQUIP_BORDER, SLOT_FILL);
            drawSlot(gui, x + 108, y + 33, EQUIP_BORDER, SLOT_FILL);

            // Offhand slot: (35, 20)
            drawSlot(gui, x + 35, y + 20, GOLD_DIM, SLOT_FILL);

            // Player 3D preview chamber: (73, 6) -> (105, 49)
            drawPortraitChamber(gui, x + 73, y + 6, x + 105, y + 49);

            // Main inventory (3×9 at x+9, y+54)
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    drawSlot(gui, x + 9 + col * 18, y + 54 + row * 18, SLOT_BORDER, SLOT_FILL);
                }
            }

            // Hotbar (1×9 at x+9, y+112)
            for (int col = 0; col < 9; col++) {
                drawSlot(gui, x + 9 + col * 18, y + 112, HOTBAR_BORDER, SLOT_FILL);
            }

            // Destroy Item (Trash) slot at (173, 112)
            drawSlot(gui, x + 173, y + 112, CRIMSON, 0xFF240C12);
            int tx = x + 173 + 8;
            int ty = y + 112 + 8;
            for (int d = -3; d <= 3; d++) {
                gui.fill(tx + d, ty + d, tx + d + 1, ty + d + 1, 0x88FF6B6B);
                gui.fill(tx + d, ty - d, tx + d + 1, ty - d + 1, 0x88FF6B6B);
            }
        } else {
            // Recessed 5×9 item catalog grid (x+9, y+18)
            drawRecessedWell(gui, x + 7, y + 16, 164, 92, 0xFF3E2650);
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 9; col++) {
                    drawSlot(gui, x + 9 + col * 18, y + 18 + row * 18, SLOT_BORDER, SLOT_FILL);
                }
            }

            // Hotbar (1×9 at x+9, y+112)
            for (int col = 0; col < 9; col++) {
                drawSlot(gui, x + 9 + col * 18, y + 112, HOTBAR_BORDER, SLOT_FILL);
            }

            // Search box well (if tab has search bar)
            if (selectedTab.hasSearchBar() && searchBox != null) {
                int sbx = searchBox.getX() - 2;
                int sby = y + 4;
                int sbw = searchBox.getWidth() + 4;
                int sbh = 11;
                drawRecessedWell(gui, sbx, sby, sbw, sbh, searchBox.isFocused() ? CYAN_LIGHT : GOLD_DIM);
            }

            // Custom scrollbar track + thumb at (x+175, y+18, w=12, h=112)
            if (selectedTab.canScroll()) {
                int trackX = x + 175;
                int trackY = y + 18;
                int trackW = 12;
                int trackH = 112;
                drawRecessedWell(gui, trackX, trackY, trackW, trackH, 0xFF3E2650);

                int thumbY = trackY + (int) ((float) (trackH - 17) * Mth.clamp(scrollOffs, 0.0F, 1.0F)) + 1;
                int thumbH = 15;
                if (canScroll) {
                    gui.fillGradient(trackX + 1, thumbY, trackX + trackW - 1, thumbY + thumbH, 0xFF472C66, 0xFF28183D);
                    drawBorder(gui, trackX + 1, thumbY, trackW - 2, thumbH, GOLD);
                    fillHorizontalGradient(gui, trackX + 2, thumbY + 1, trackX + trackW - 2, thumbY + 2, GOLD_BRIGHT, CYAN_LIGHT);
                    gui.hLine(trackX + 3, trackX + trackW - 4, thumbY + 6, withAlpha(GOLD_BRIGHT, 0x88));
                    gui.hLine(trackX + 3, trackX + trackW - 4, thumbY + 9, withAlpha(GOLD_BRIGHT, 0x88));
                } else {
                    gui.fill(trackX + 1, thumbY, trackX + trackW - 1, thumbY + thumbH, 0xFF1C1428);
                    drawBorder(gui, trackX + 1, thumbY, trackW - 2, thumbH, 0xFF38284A);
                }
            }
        }

        if (searchBox != null) {
            searchBox.render(gui, mouseX, mouseY, partialTick);
        }
    }

    public static void renderCreativePlayerPreview(GuiGraphics gui, int x, int y, int mouseX, int mouseY, LivingEntity player) {
        if (player == null) return;
        gui.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                gui, x + 73, y + 6, x + 105, y + 49, 20, 0.0625F, (float) mouseX, (float) mouseY, player);
    }

    /**
     * Renders a top or bottom Creative tab aligned cleanly to the main panel's
     * 1px outer-shadow / 1px border geometry so outer corners and inner seams never clip or step.
     */
    public static void renderCreativeTabButton(GuiGraphics gui, Font font, CreativeModeTab tab,
                                               int tabX, int tabY, boolean selected, boolean isTop,
                                               int leftPos, int topPos, int imageWidth, int imageHeight) {
        int tabW = 26;
        int tabH = 32;
        boolean flushLeft = (tabX == leftPos);
        boolean flushRight = (tabX + tabW == leftPos + imageWidth);

        // Outer shadow sits at [tabX, tabX + tabW - 1]; visible border sits at [tabX + 1, tabX + tabW - 2],
        // matching the main panel's [leftPos + 1, leftPos + imageWidth - 2] borders identically.
        int bLeft = tabX + 1;
        int bRight = tabX + tabW - 2;

        if (isTop) {
            if (selected) {
                int drawY = tabY;
                // Outer chamfered shadow stops strictly at topPos + 1 so it never cuts into the chromatic edge
                gui.fill(tabX + 1, drawY, tabX + tabW - 1, topPos + 1, OUTER_SHADOW);
                gui.fill(tabX, drawY + 1, tabX + tabW, topPos + 1, OUTER_SHADOW);

                // Seamless interior bridging right into the panel top (topPos + 3)
                gui.fillGradient(bLeft + 1, drawY + 2, bRight, topPos + 1, CHASSIS_TOP, CHASSIS_TOP);
                gui.fill(bLeft + 1, topPos + 1, bRight, topPos + 4, CHASSIS_TOP);

                // Top gilded cap & chromatic accent line
                gui.hLine(bLeft + 1, bRight - 1, drawY + 1, GOLD);
                int mid = (bLeft + bRight + 1) / 2;
                fillHorizontalGradient(gui, bLeft + 2, drawY + 2, mid, drawY + 3, GOLD_BRIGHT, AMETHYST);
                fillHorizontalGradient(gui, mid, drawY + 2, bRight - 1, drawY + 3, AMETHYST, CYAN_LIGHT);

                // Left & right borders: flush outer edges flow straight into the chassis side border;
                // inner edges meet the chassis top rim cleanly at topPos + 1
                gui.vLine(bLeft, drawY + 2, flushLeft ? topPos + 3 : topPos + 1, GOLD);
                gui.vLine(bRight, drawY + 2, flushRight ? topPos + 3 : topPos + 1, GOLD);
                if (flushLeft) {
                    gui.vLine(tabX, topPos, topPos + 3, OUTER_SHADOW);
                }
                if (flushRight) {
                    gui.vLine(tabX + tabW - 1, topPos, topPos + 3, OUTER_SHADOW);
                }
            } else {
                int drawY = tabY + 2;
                int bottomY = topPos + 1;
                gui.fill(tabX + 1, drawY, tabX + tabW - 1, bottomY, OUTER_SHADOW);
                gui.fill(tabX, drawY + 1, tabX + tabW, bottomY, OUTER_SHADOW);
                gui.fillGradient(bLeft + 1, drawY + 2, bRight, bottomY, 0xEE180D24, 0xEE0F0818);
                int border = 0xFF472859;
                gui.hLine(bLeft + 1, bRight - 1, drawY + 1, border);
                gui.vLine(bLeft, drawY + 2, bottomY - 1, border);
                gui.vLine(bRight, drawY + 2, bottomY - 1, border);
            }
        } else {
            int panelBot = topPos + imageHeight;
            if (selected) {
                int bottomEdge = tabY + tabH - 1;
                // Outer chamfered shadow starts strictly at panelBot - 1
                gui.fill(tabX, panelBot - 1, tabX + tabW, bottomEdge, OUTER_SHADOW);
                gui.fill(tabX + 1, panelBot - 1, tabX + tabW - 1, bottomEdge + 1, OUTER_SHADOW);

                // Seamless interior bridging over the bottom crimson/amethyst rim
                gui.fill(bLeft + 1, panelBot - 4, bRight, panelBot - 1, CHASSIS_BOT);
                gui.fillGradient(bLeft + 1, panelBot - 1, bRight, bottomEdge - 1, CHASSIS_BOT, 0xF61B0E2A);

                // Bottom gilded cap & chromatic accent line
                gui.hLine(bLeft + 1, bRight - 1, bottomEdge - 1, GOLD);
                int mid = (bLeft + bRight + 1) / 2;
                fillHorizontalGradient(gui, bLeft + 2, bottomEdge - 2, mid, bottomEdge - 1, GOLD_BRIGHT, AMETHYST);
                fillHorizontalGradient(gui, mid, bottomEdge - 2, bRight - 1, bottomEdge - 1, AMETHYST, CYAN_LIGHT);

                // Left & right borders: flush outer edges flow smoothly from chassis side border
                gui.vLine(bLeft, flushLeft ? panelBot - 4 : panelBot - 2, bottomEdge - 2, GOLD);
                gui.vLine(bRight, flushRight ? panelBot - 4 : panelBot - 2, bottomEdge - 2, GOLD);
                if (flushLeft) {
                    gui.vLine(tabX, panelBot - 4, panelBot, OUTER_SHADOW);
                }
                if (flushRight) {
                    gui.vLine(tabX + tabW - 1, panelBot - 4, panelBot, OUTER_SHADOW);
                }
            } else {
                int topEdge = panelBot - 1;
                int bottomEdge = tabY + tabH - 3;
                gui.fill(tabX, topEdge, tabX + tabW, bottomEdge, OUTER_SHADOW);
                gui.fill(tabX + 1, topEdge, tabX + tabW - 1, bottomEdge + 1, OUTER_SHADOW);
                gui.fillGradient(bLeft + 1, topEdge, bRight, bottomEdge - 1, 0xEE120A1C, 0xEE0B0613);
                int border = 0xFF472859;
                gui.hLine(bLeft + 1, bRight - 1, bottomEdge - 1, border);
                gui.vLine(bLeft, topEdge, bottomEdge - 2, border);
                gui.vLine(bRight, topEdge, bottomEdge - 2, border);
            }
        }

        // Render tab item icon at exact vanilla offset & z=100
        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 100.0F);
        int iconX = tabX + 5;
        int iconY = tabY + 8 + (isTop ? 1 : -1);
        ItemStack icon = tab.getIconItem();
        gui.renderItem(icon, iconX, iconY);
        gui.renderItemDecorations(font, icon, iconX, iconY);
        gui.pose().popPose();
    }

    public static void renderCreativeTabTitle(GuiGraphics gui, Font font, CreativeModeTab selectedTab) {
        if (selectedTab.showTitle()) {
            RenderSystem.disableBlend();
            gui.drawString(font, selectedTab.getDisplayName(), 8, 6, TEXT_TITLE, true);
        }
    }
}
