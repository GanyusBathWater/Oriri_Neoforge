package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.menu.SoulCollectorMenu;
import net.ganyusbathwater.oririmod.item.custom.SoulShardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Programmatic open-book GUI for the {@code Soul Collector}, styled after {@code soul_collector.png}
 * (ornate purple/crimson/gold cover, warm parchment two-page spread, swirling cyan-purple portal slot,
 * live mob preview + kill progress bar, and a hoverable '?' bookmark in the upper-right corner).
 */
public class SoulCollectorScreen extends AbstractContainerScreen<SoulCollectorMenu> {

    private static final int BOOK_W = 220;
    private static final int BOOK_H = 136;

    // Upper-right '?' bookmark tab bounds (relative to leftPos / topPos)
    private static final int HELP_X = BOOK_W - 22;
    private static final int HELP_Y = -6;
    private static final int HELP_W = 16;
    private static final int HELP_H = 22;

    private LivingEntity renderedEntity = null;
    private String lastEntityId = null;
    private boolean previewErrorLogged = false;

    public SoulCollectorScreen(SoulCollectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BOOK_W;
        this.imageHeight = 232;
        this.titleLabelX = 16;
        this.titleLabelY = 12;
        this.inventoryLabelX = SoulCollectorMenu.INV_X;
        this.inventoryLabelY = SoulCollectorMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        ItemStack shard = this.menu.getSlot(0).getItem();
        boolean hasShard = SoulShardItem.hasBoundEntity(shard);
        boolean completed = hasShard && SoulShardItem.isCompleted(shard);

        updatePreviewEntity(shard, hasShard);

        // === 1. OPEN BOOK COVER (purple / crimson / gold trim matching soul_collector.png) ===
        drawBookFrame(gui, x, y);

        // === 2. LEFT PAGE: Swirling cyan/purple portal frame around the single Soul Shard slot ===
        int slotScreenX = x + SoulCollectorMenu.SHARD_SLOT_X;
        int slotScreenY = y + SoulCollectorMenu.SHARD_SLOT_Y;
        drawPortalSlot(gui, slotScreenX, slotScreenY, completed, partialTick);

        // Left page subtitle under the portal slot
        Component leftStatus = !hasShard
                ? Component.translatable("gui.oririmod.soul_collector.insert_shard")
                : (completed
                        ? Component.translatable("gui.oririmod.soul_collector.empowered")
                        : Component.translatable("gui.oririmod.soul_collector.collecting"));
        int leftStatusColor = !hasShard ? 0x6B5846 : (completed ? 0x8E24AA : 0x0B7A8A);
        drawCenteredScaled(gui, leftStatus, x + 56, y + 98, leftStatusColor);

        // === 3. RIGHT PAGE: Mob Preview + Kill Progress Bar ===
        int rightPageCenterX = x + 162;
        if (hasShard) {
            // Framed portrait box on parchment
            int boxX = x + 132;
            int boxY = y + 18;
            int boxW = 60;
            int boxH = 54;
            gui.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, completed ? 0xFFD4A238 : 0xFF6B3E7A);
            gui.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF22172B);

            if (this.renderedEntity != null) {
                renderPreviewEntity(gui, boxX + 2, boxY + 2, boxX + boxW - 2, boxY + boxH - 2, mouseX, mouseY);
            }

            // Mob name
            String entityId = SoulShardItem.getBoundEntityId(shard);
            Optional<EntityType<?>> opt = entityId != null ? EntityType.byString(entityId) : Optional.empty();
            Component mobName = opt.isPresent()
                    ? opt.get().getDescription()
                    : Component.literal(String.valueOf(entityId));
            drawCenteredScaled(gui, mobName, rightPageCenterX, y + 77, 0xFF3E261A);

            // Kill progress bar
            int kills = SoulShardItem.getKills(shard);
            int required = SoulShardItem.getRequiredKills(shard);
            float ratio = Mth.clamp((float) kills / Math.max(1, required), 0.0F, 1.0F);

            int barX = x + 122;
            int barY = y + 92;
            int barW = 80;
            int barH = 10;
            gui.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF3D2647);
            gui.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1124);
            int fillW = Mth.floor(barW * ratio);
            if (fillW > 0) {
                int cLeft = completed ? 0xFFC058F0 : 0xFF4A1E9E;
                int cRight = completed ? 0xFFFFD859 : 0xFF18D2E8;
                fillHorizontalGradient(gui, barX, barY, barX + fillW, barY + barH, cLeft, cRight);
            }

            // Numeric kill count / completion badge
            Component progText = completed
                    ? Component.translatable("gui.oririmod.soul_collector.complete", required, required)
                    : Component.translatable("gui.oririmod.soul_collector.kills", kills, required);
            int progColor = completed ? 0x7A1FA2 : 0x3D2A1E;
            gui.drawString(this.font, progText, rightPageCenterX - this.font.width(progText) / 2, y + 106, progColor, false);
        } else {
            // Empty right page prompt
            Component emptyTitle = Component.translatable("gui.oririmod.soul_collector.empty_title");
            gui.drawString(this.font, emptyTitle, rightPageCenterX - this.font.width(emptyTitle) / 2, y + 40, 0x5C4033, false);

            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                    Component.translatable("gui.oririmod.soul_collector.empty_desc"), 84);
            int ly = y + 58;
            for (var line : lines) {
                gui.drawString(this.font, line, rightPageCenterX - this.font.width(line) / 2, ly, 0x7A6252, false);
                ly += 10;
            }
        }

        // === 4. UPPER-RIGHT '?' BOOKMARK TAB ===
        boolean hoveringHelp = isHoveringHelp(mouseX, mouseY);
        drawHelpBookmark(gui, x + HELP_X, y + HELP_Y, hoveringHelp);

        // === 5. PLAYER INVENTORY PANEL BELOW THE BOOK ===
        drawInventoryPanel(gui, x + SoulCollectorMenu.INV_X - 8, y + SoulCollectorMenu.INV_Y - 16);
    }

    private void drawBookFrame(GuiGraphics gui, int x, int y) {
        // Outer dark obsidian border
        gui.fill(x, y, x + BOOK_W, y + BOOK_H, 0xFF1D1026);
        // Ornate purple leather cover
        gui.fill(x + 2, y + 2, x + BOOK_W - 2, y + BOOK_H - 2, 0xFF4D1F63);
        // Deep crimson accent bottom edge (matching soul_collector.png clasp/edges)
        gui.fill(x + 2, y + BOOK_H - 6, x + BOOK_W - 2, y + BOOK_H - 2, 0xFF7D1B38);
        // Gold corner filigree
        int gold = 0xFFD9A63A;
        gui.fill(x + 2, y + 2, x + 12, y + 4, gold);
        gui.fill(x + 2, y + 2, x + 4, y + 12, gold);
        gui.fill(x + BOOK_W - 12, y + 2, x + BOOK_W - 2, y + 4, gold);
        gui.fill(x + BOOK_W - 4, y + 2, x + BOOK_W - 2, y + 12, gold);
        gui.fill(x + 2, y + BOOK_H - 4, x + 12, y + BOOK_H - 2, gold);
        gui.fill(x + BOOK_W - 12, y + BOOK_H - 4, x + BOOK_W - 2, y + BOOK_H - 2, gold);

        // Page stack rim (warm tan)
        gui.fill(x + 6, y + 6, x + BOOK_W - 6, y + BOOK_H - 6, 0xFFCBB68E);
        // Left & right cream parchment pages
        gui.fill(x + 9, y + 8, x + BOOK_W / 2 - 2, y + BOOK_H - 9, 0xFFF3E7CE);
        gui.fill(x + BOOK_W / 2 + 2, y + 8, x + BOOK_W - 9, y + BOOK_H - 9, 0xFFF3E7CE);

        // Center spine crease + shadow
        int mid = x + BOOK_W / 2;
        gui.fill(mid - 3, y + 8, mid - 1, y + BOOK_H - 9, 0xFFDCCBA8);
        gui.fill(mid - 1, y + 6, mid + 1, y + BOOK_H - 6, 0xFF6B4E3B);
        gui.fill(mid + 1, y + 8, mid + 3, y + BOOK_H - 9, 0xFFDCCBA8);

        // Decorative page header rule on left page
        gui.fill(x + 16, y + 24, mid - 10, y + 25, 0xFFBFA880);
    }

    private void drawPortalSlot(GuiGraphics gui, int slotX, int slotY, boolean completed, float partialTick) {
        int cx = slotX + 8;
        int cy = slotY + 8;
        // Wrap phase into [0, 2*PI) using monotonic wall time so Mth.sin() never hits
        // 32-bit float->int saturation on long-running worlds or stalls when ticks are frozen.
        float time = (float) ((Util.getMillis() * 0.0016D) % (Math.PI * 2.0D));

        // Animated swirling portal halo around the shard slot
        int outerR = 26;
        for (int dy = -outerR; dy < outerR; dy += 2) {
            for (int dx = -outerR; dx < outerR; dx += 2) {
                float pdx = dx + 1.0F;
                float pdy = dy + 1.0F;
                float dist = Mth.sqrt(pdx * pdx + pdy * pdy);
                if (dist > outerR || dist < 7.0F) continue;
                float angle = (float) Math.atan2(pdy, pdx);
                float swirl = Mth.sin(angle * 3.0F - dist * 0.25F + time);
                int color;
                if (completed) {
                    color = swirl > 0.2F ? 0xCCFFD257 : (swirl > -0.35F ? 0xBB9E32E8 : 0x883A1266);
                } else {
                    color = swirl > 0.25F ? 0xCC1AD8EC : (swirl > -0.3F ? 0xBB5622A8 : 0x88200D4A);
                }
                gui.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, color);
            }
        }

        // Crisp recessed item slot frame (18x18 around the 16x16 slot)
        int border = completed ? 0xFFE6B83A : 0xFF28D5E8;
        gui.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, border);
        gui.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF190E26);
    }

    private void drawHelpBookmark(GuiGraphics gui, int bx, int by, boolean hovered) {
        int ribbonOuter = hovered ? 0xFFFFD859 : 0xFFB8882C;
        int ribbonFill = hovered ? 0xFFB5264B : 0xFF8C1C3A;
        // Ribbon body + pointed bookmark notch at bottom
        gui.fill(bx, by, bx + HELP_W, by + HELP_H - 4, ribbonOuter);
        gui.fill(bx + 1, by + 1, bx + HELP_W - 1, by + HELP_H - 4, ribbonFill);
        // Notch tails
        gui.fill(bx, by + HELP_H - 4, bx + 5, by + HELP_H, ribbonOuter);
        gui.fill(bx + 1, by + HELP_H - 4, bx + 4, by + HELP_H - 1, ribbonFill);
        gui.fill(bx + HELP_W - 5, by + HELP_H - 4, bx + HELP_W, by + HELP_H, ribbonOuter);
        gui.fill(bx + HELP_W - 4, by + HELP_H - 4, bx + HELP_W - 1, by + HELP_H - 1, ribbonFill);

        // '?' mark centered on bookmark
        gui.drawString(this.font, "?", bx + (HELP_W - this.font.width("?")) / 2 + 1, by + 5,
                hovered ? 0xFFFFFFA0 : 0xFFFFD859, true);
    }

    private void drawInventoryPanel(GuiGraphics gui, int px, int py) {
        int pw = 176;
        int ph = 94;
        // Dark ornate panel matching the book cover
        gui.fill(px, py, px + pw, py + ph, 0xFF1D1026);
        gui.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF3A2347);
        gui.fill(px + 2, py + 2, px + pw - 2, py + ph - 2, 0xFF2A1B36);
        gui.fill(px + 2, py + 2, px + pw - 2, py + 3, 0xFF9C7830);

        // Render slot recesses for all 36 player inventory slots
        int lockedSlot = this.menu.getCollectorSlotIndex();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int idx = col + row * 9 + 9;
                drawInvSlotBox(gui, this.leftPos + SoulCollectorMenu.INV_X + col * 18,
                        this.topPos + SoulCollectorMenu.INV_Y + row * 18, idx == lockedSlot);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawInvSlotBox(gui, this.leftPos + SoulCollectorMenu.INV_X + col * 18,
                    this.topPos + SoulCollectorMenu.HOTBAR_Y, col == lockedSlot);
        }
    }

    private void drawInvSlotBox(GuiGraphics gui, int sx, int sy, boolean locked) {
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, locked ? 0xFF8C6424 : 0xFF140C1C);
        gui.fill(sx, sy, sx + 16, sy + 16, locked ? 0xFF3E294D : 0xFF443154);
    }

    private void drawCenteredScaled(GuiGraphics gui, Component text, int centerX, int y, int color) {
        int w = this.font.width(text);
        gui.drawString(this.font, text, centerX - w / 2, y, color, false);
    }

    private static void fillHorizontalGradient(GuiGraphics gui, int x1, int y1, int x2, int y2, int cLeft, int cRight) {
        int width = Math.max(1, x2 - x1);
        int a1 = (cLeft >>> 24) & 0xFF, r1 = (cLeft >>> 16) & 0xFF, g1 = (cLeft >>> 8) & 0xFF, b1 = cLeft & 0xFF;
        int a2 = (cRight >>> 24) & 0xFF, r2 = (cRight >>> 16) & 0xFF, g2 = (cRight >>> 8) & 0xFF, b2 = cRight & 0xFF;
        for (int i = 0; i < width; i++) {
            float t = (float) i / width;
            int a = (int) Mth.lerp(t, a1, a2);
            int r = (int) Mth.lerp(t, r1, r2);
            int g = (int) Mth.lerp(t, g1, g2);
            int b = (int) Mth.lerp(t, b1, b2);
            int col = (a << 24) | (r << 16) | (g << 8) | b;
            gui.fill(x1 + i, y1, x1 + i + 1, y2, col);
        }
    }

    private void renderPreviewEntity(GuiGraphics gui, int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        LivingEntity entity = this.renderedEntity;
        if (entity == null) {
            return;
        }
        try {
            // Keep tickCount >= 20 and advancing with game time so custom renderers that suppress
            // spawn-frame glitches (entity.tickCount < 2, e.g. Silent Nereid / Mermaid / Deviartras /
            // Venomous Plant) render normally and GeckoLib/vanilla idle animations play cleanly.
            entity.tickCount = (int) ((Util.getMillis() / 50L) % 1_000_000L) + 20;

            float bbH = Math.max(0.5F, entity.getBbHeight());
            float bbW = Math.max(0.5F, entity.getBbWidth());
            float maxDim = Math.max(bbH, bbW);
            int scale = Mth.clamp(Math.round(36.0F / maxDim), 8, 28);

            // Flush prior gui.fill() quads, reset shader tint, and push Z well in front of the z=0 box
            gui.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float prevBodyRotO = entity.yBodyRotO;
            // Align yBodyRotO near 180 deg so Mth.rotLerp(1.0F, yBodyRotO, 180+delta) doesn't wrap by 360 deg on the left side
            entity.yBodyRotO = 180.0F;
            gui.pose().pushPose();
            gui.pose().translate(0.0, 0.0, 100.0);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        gui, x1, y1, x2, y2, scale, 0.0625F, mouseX, mouseY, entity);
            } finally {
                entity.yBodyRotO = prevBodyRotO;
                gui.pose().popPose();
            }
        } catch (Throwable t) {
            if (!this.previewErrorLogged) {
                this.previewErrorLogged = true;
                OririMod.LOGGER.warn("Failed to render Soul Collector preview entity '{}'", this.lastEntityId, t);
            }
        }
    }

    private void updatePreviewEntity(ItemStack shard, boolean hasShard) {
        if (!hasShard || this.minecraft == null || this.minecraft.level == null) {
            this.lastEntityId = null;
            this.renderedEntity = null;
            this.previewErrorLogged = false;
            return;
        }
        String entityId = SoulShardItem.getBoundEntityId(shard);
        if (entityId != null && !entityId.equals(this.lastEntityId)) {
            this.lastEntityId = entityId;
            this.previewErrorLogged = false;
            Optional<EntityType<?>> opt = EntityType.byString(entityId);
            if (opt.isPresent()) {
                Entity created = opt.get().create(this.minecraft.level);
                if (created instanceof LivingEntity living) {
                    living.tickCount = 20;
                    living.setPos(0.0, -1000.0, 0.0);
                    living.refreshDimensions();
                    this.renderedEntity = living;
                } else {
                    this.renderedEntity = null;
                }
            } else {
                this.renderedEntity = null;
            }
        }
    }

    private boolean isHoveringHelp(int mouseX, int mouseY) {
        int rx = mouseX - this.leftPos;
        int ry = mouseY - this.topPos;
        return rx >= HELP_X && rx <= HELP_X + HELP_W && ry >= HELP_Y && ry <= HELP_Y + HELP_H;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3E261A, false);
        gui.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFD9C7E8, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Note: AbstractContainerScreen.render() already invokes renderBackground() in MC 1.21.1
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);

        if (isHoveringHelp(mouseX, mouseY)) {
            List<Component> helpLines = List.of(
                    Component.translatable("gui.oririmod.soul_collector.help.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.oririmod.soul_collector.help.step1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.soul_collector.help.step2").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.soul_collector.help.step3").withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.oririmod.soul_collector.help.tiers").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("gui.oririmod.soul_collector.help.step4").withStyle(ChatFormatting.LIGHT_PURPLE)
            );
            gui.renderComponentTooltip(this.font, helpLines, mouseX, mouseY);
        }
    }
}
