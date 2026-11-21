package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.menu.SoulHarvesterMenu;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Programmatic Soul Forge Reliquary GUI for the Soul Harvester — sister visual style
 * to {@link SoulCollectorScreen} (obsidian-amethyst & crimson-gold frame, live 3D mob
 * specimen chamber, swirling soul-shard socket, animated siphon bar, and a color-coded
 * 5-slot upgrade dock with empty-slot tooltips).
 */
public class SoulHarvesterScreen extends AbstractContainerScreen<SoulHarvesterMenu> {

    private static final int MAIN_W = 176;
    private static final int MAIN_H = 222;
    private static final int WING_X = 174;
    private static final int WING_Y = 8;
    private static final int WING_W = 34;
    private static final int WING_H = 108;

    private static final int SHARD_X = 80;
    private static final int SHARD_Y = 36;
    private static final int BAR_X = 102;
    private static final int BAR_Y = 36;
    private static final int BAR_W = 63;
    private static final int BAR_H = 15;

    private static final int HELP_X = 158;
    private static final int HELP_Y = 4;
    private static final int HELP_W = 12;
    private static final int HELP_H = 11;

    private static final int[] UPGRADE_ACCENTS = {
            0xFF5EC8FF, // 0: Looting
            0xFF6EF07A, // 1: XP
            0xFF4EE8E8, // 2: Speed
            0xFFFF8833, // 3: Fire Aspect
            0xFFE85B88  // 4: Player Kill
    };

    private static final String[] UPGRADE_TOOLTIP_KEYS = {
            "gui.oririmod.soul_harvester.upgrade.looting",
            "gui.oririmod.soul_harvester.upgrade.xp",
            "gui.oririmod.soul_harvester.upgrade.speed",
            "gui.oririmod.soul_harvester.upgrade.fire_aspect",
            "gui.oririmod.soul_harvester.upgrade.player_kill"
    };

    private LivingEntity renderedEntity = null;
    private String lastEntityId = null;
    private boolean previewErrorLogged = false;

    public SoulHarvesterScreen(SoulHarvesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 208; // 176 main + 32 upgrade wing
        this.imageHeight = MAIN_H;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        ItemStack shardStack = this.menu.getSlot(0).getItem();
        String entityId = extractCapturedEntityId(shardStack);
        updatePreviewEntity(entityId);
        boolean hasShard = entityId != null;

        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();
        int scaledProgress = this.menu.getScaledProgress();
        boolean active = hasShard && maxProgress > 0 && progress > 0;

        // === 1. RIGHT UPGRADE DOCK WING (drawn first so main chassis overlaps cleanly) ===
        drawUpgradeWing(gui, x + WING_X, y + WING_Y, x, y);

        // === 2. MAIN SOUL RELIQUARY CHASSIS (purple-obsidian + crimson-gold trim) ===
        drawHarvesterChassis(gui, x, y);

        // === 3. MOB SPECIMEN CHAMBER (x+16..70, y+16..68) ===
        int boxX = x + 16;
        int boxY = y + 16;
        int boxW = 54;
        int boxH = 52;
        gui.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, OUTER_SHADOW);
        drawBorder(gui, boxX - 1, boxY - 1, boxW + 2, boxH + 2, hasShard ? GOLD : AMETHYST);
        gui.fillGradient(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF21142E, 0xFF110B1A);

        if (this.renderedEntity != null) {
            renderPreviewEntity(gui, boxX + 2, boxY + 2, boxX + boxW - 2, boxY + boxH - 2, mouseX, mouseY);
        } else {
            drawDiamond(gui, boxX + boxW / 2, boxY + boxH / 2, 10, 0x337B5EA7);
            drawDiamond(gui, boxX + boxW / 2, boxY + boxH / 2, 5, 0x554E3573);
        }

        // === 4. SWIRLING MINI SOUL SHARD SOCKET AT (80, 36) ===
        drawShardSocket(gui, x + SHARD_X, y + SHARD_Y, hasShard, partialTick);

        // === 5. ANIMATED SOUL SIPHON PROGRESS BAR AT (102, 36, 63×15) ===
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        gui.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, hasShard ? AMETHYST : 0xFF3A2847);
        gui.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF130B1D);
        gui.hLine(bx, bx + BAR_W - 1, by, 0x55000000);

        if (scaledProgress > 0) {
            int fillW = Mth.clamp(scaledProgress, 0, BAR_W);
            fillHorizontalGradient(gui, bx, by, bx + fillW, by + BAR_H, 0xFF5A1FB8, 0xFF20D8EC);
            // Bright specular top edge + leading soul-gold spark
            gui.hLine(bx, bx + fillW - 1, by + 1, 0x66FFFFFF);
            if (fillW >= 2) {
                gui.fill(bx + fillW - 2, by + 1, bx + fillW, by + BAR_H - 1, GOLD_BRIGHT);
            }
        }

        // Status / percent caption right above the progress bar
        String statusText;
        int statusColor;
        if (!hasShard) {
            statusText = Component.translatable("gui.oririmod.soul_harvester.no_shard").getString();
            statusColor = TEXT_MUTED;
        } else if (active) {
            int pct = Mth.clamp(Math.round((progress * 100.0F) / Math.max(1, maxProgress)), 0, 100);
            statusText = pct + "%";
            statusColor = CYAN_LIGHT;
        } else {
            statusText = Component.translatable("gui.oririmod.soul_harvester.ready").getString();
            statusColor = GOLD_BRIGHT;
        }
        gui.drawCenteredString(this.font, statusText, bx + BAR_W / 2, by - 11, statusColor);

        // Mob display name cleanly below the shard + progress zone
        if (hasShard) {
            Optional<EntityType<?>> opt = EntityType.byString(entityId);
            String mobName = opt.isPresent() ? opt.get().getDescription().getString() : entityId;
            String clipped = truncate(this.font, mobName, 88);
            gui.drawCenteredString(this.font, clipped, x + 123, y + 57, TEXT_TITLE);
        }

        // === 6. OUTPUT STORAGE VAULT (3×9 at 8, 72) ===
        drawRecessedWell(gui, x + 6, y + 70, 164, 56, 0xFF472B57);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(gui, x + 8 + col * 18, y + 72 + row * 18, 0xFF2E1E3D, 0xFF160E21);
            }
        }

        // === 7. PLAYER INVENTORY (3×9 at 8, 140 + hotbar at 8, 198) ===
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(gui, x + 8 + col * 18, y + 140 + row * 18, 0xFF281B36, 0xFF140D1E);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(gui, x + 8 + col * 18, y + 198, 0xFF332245, 0xFF140D1E);
        }

        // === 8. UPPER-RIGHT '?' HELP BADGE ===
        boolean hoveringHelp = isHoveringHelp(mouseX, mouseY);
        int hx = x + HELP_X;
        int hy = y + HELP_Y;
        gui.fill(hx, hy, hx + HELP_W, hy + HELP_H, hoveringHelp ? 0xFF7D1B38 : 0xFF2A1538);
        drawBorder(gui, hx, hy, HELP_W, HELP_H, hoveringHelp ? GOLD_BRIGHT : GOLD_DIM);
        gui.drawCenteredString(this.font, "?", hx + HELP_W / 2, hy + 2, hoveringHelp ? GOLD_BRIGHT : GOLD);
    }

    private void drawHarvesterChassis(GuiGraphics gui, int x, int y) {
        // Outer shadow & obsidian border
        gui.fill(x + 2, y + MAIN_H, x + MAIN_W - 1, y + MAIN_H + 2, 0x66000000);
        gui.fill(x + 1, y, x + MAIN_W - 1, y + MAIN_H, OUTER_SHADOW);
        gui.fill(x, y + 1, x + MAIN_W, y + MAIN_H - 1, OUTER_SHADOW);

        // Deep purple-obsidian interior gradient matching SoulCollectorScreen's cover palette
        gui.fillGradient(x + 2, y + 2, x + MAIN_W - 2, y + MAIN_H - 2, 0xF4251336, 0xF6130A1E);

        // Crimson-gold accent trim along upper/lower rims
        drawBorder(gui, x + 1, y + 1, MAIN_W - 2, MAIN_H - 2, 0xFF5D2A73);
        gui.hLine(x + 2, x + MAIN_W - 3, y + MAIN_H - 3, 0xFF7D1B38);
        gui.hLine(x + 3, x + MAIN_W - 4, y + 2, withAlpha(GOLD, 0x66));
        drawCornerFiligree(gui, x + 1, y + 1, MAIN_W - 2, MAIN_H - 2, 8, GOLD);
    }

    private void drawUpgradeWing(GuiGraphics gui, int wx, int wy, int screenX, int screenY) {
        gui.fill(wx, wy, wx + WING_W, wy + WING_H, OUTER_SHADOW);
        gui.fillGradient(wx + 1, wy + 1, wx + WING_W - 1, wy + WING_H - 1, 0xF0221233, 0xF212091C);
        drawBorder(gui, wx, wy, WING_W, WING_H, 0xFF5D2A73);
        gui.hLine(wx + 2, wx + WING_W - 2, wy + 1, withAlpha(GOLD, 0x66));

        // Tiny "+" header mark on wing
        gui.drawCenteredString(this.font, "+", wx + WING_W / 2 + 1, wy + 2, GOLD);

        // 5 Color-coded upgrade slots at (182, 20 + i*18)
        for (int i = 0; i < 5; i++) {
            int sx = screenX + 182;
            int sy = screenY + 20 + i * 18;
            boolean filled = this.menu.getSlot(1 + i).hasItem();
            int accent = UPGRADE_ACCENTS[i];
            int border = filled ? accent : withAlpha(accent, 0x88);
            drawSlotBox(gui, sx, sy, border, filled ? 0xFF1F1630 : 0xFF120B1C);
            // Tiny side indicator dot
            gui.fill(sx + 18, sy + 6, sx + 20, sy + 10, filled ? accent : withAlpha(accent, 0x55));
        }
    }

    private void drawShardSocket(GuiGraphics gui, int slotX, int slotY, boolean hasShard, float partialTick) {
        int cx = slotX + 8;
        int cy = slotY + 8;
        float time = (this.minecraft != null && this.minecraft.level != null)
                ? (this.minecraft.level.getGameTime() + partialTick) * 0.08F
                : 0.0F;

        if (hasShard) {
            int outerR = 13;
            for (int dy = -outerR; dy < outerR; dy += 2) {
                for (int dx = -outerR; dx < outerR; dx += 2) {
                    float pdx = dx + 1.0F;
                    float pdy = dy + 1.0F;
                    float dist = Mth.sqrt(pdx * pdx + pdy * pdy);
                    if (dist > outerR || dist < 7.0F) continue;
                    float angle = (float) Math.atan2(pdy, pdx);
                    float swirl = Mth.sin(angle * 3.0F - dist * 0.35F + time);
                    int col = swirl > 0.2F ? 0xCCFFD257 : (swirl > -0.35F ? 0xBB9E32E8 : 0x881AD8EC);
                    gui.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, col);
                }
            }
        }

        int border = hasShard ? GOLD_BRIGHT : AMETHYST;
        gui.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, border);
        gui.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF190E26);
    }

    private void drawSlotBox(GuiGraphics gui, int sx, int sy, int border, int fill) {
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, border);
        gui.fill(sx, sy, sx + 16, sy + 16, fill);
        gui.hLine(sx, sx + 15, sy, 0x44000000);
    }

    private String extractCapturedEntityId(ItemStack shardStack) {
        if (!shardStack.is(ModItems.SOUL_SHARD.get())) return null;
        CompoundTag tag = shardStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("captured_entity")) return null;
        String id = tag.getString("captured_entity");
        return id.isBlank() ? null : id;
    }

    private void updatePreviewEntity(String entityId) {
        if (entityId == null || this.minecraft == null || this.minecraft.level == null) {
            this.lastEntityId = null;
            this.renderedEntity = null;
            this.previewErrorLogged = false;
            return;
        }
        if (!entityId.equals(this.lastEntityId)) {
            this.lastEntityId = entityId;
            this.previewErrorLogged = false;
            Optional<EntityType<?>> optType = EntityType.byString(entityId);
            if (optType.isPresent()) {
                Entity created = optType.get().create(this.minecraft.level);
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

    private void renderPreviewEntity(GuiGraphics gui, int x1, int y1, int x2, int y2, int mouseX, int mouseY) {
        LivingEntity entity = this.renderedEntity;
        if (entity == null) return;

        try {
            if (this.minecraft != null && this.minecraft.level != null) {
                entity.tickCount = (int) (this.minecraft.level.getGameTime() % 1_000_000L) + 20;
            } else {
                entity.tickCount = Math.max(20, entity.tickCount + 1);
            }

            float maxDim = Math.max(0.5F, Math.max(entity.getBbHeight(), entity.getBbWidth()));
            int scale = Mth.clamp(Math.round(34.0F / maxDim), 8, 26);

            gui.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float prevBodyRotO = entity.yBodyRotO;
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
                OririMod.LOGGER.warn("Failed to render Soul Harvester preview entity '{}'", this.lastEntityId, t);
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
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_TITLE, true);
        gui.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFD9C7E8, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);

        if (isHoveringHelp(mouseX, mouseY)) {
            List<Component> helpLines = List.of(
                    Component.translatable("gui.oririmod.soul_harvester.help.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.oririmod.soul_harvester.help.line1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.soul_harvester.help.line2").withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.oririmod.soul_harvester.help.line3").withStyle(ChatFormatting.LIGHT_PURPLE)
            );
            gui.renderComponentTooltip(this.font, helpLines, mouseX, mouseY);
            return;
        }

        // Show helpful slot label when hovering an empty upgrade slot
        for (int i = 0; i < 5; i++) {
            int sx = this.leftPos + 182;
            int sy = this.topPos + 20 + i * 18;
            if (!this.menu.getSlot(1 + i).hasItem()
                    && mouseX >= sx && mouseX < sx + 16
                    && mouseY >= sy && mouseY < sy + 16) {
                gui.renderTooltip(this.font,
                        Component.translatable(UPGRADE_TOOLTIP_KEYS[i]).withStyle(ChatFormatting.GRAY),
                        mouseX, mouseY);
                break;
            }
        }
    }
}
