package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity;
import net.ganyusbathwater.oririmod.network.packet.OpenTeleporterScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.UpdateTeleporterIDPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Programmatic Arcane Rift Monolith GUI for the {@code Teleporter}, inspired by {@code teleporter.png}
 * (carved dark basalt frame, dynamic dyed arcane rift diamond vortex, live coordinates & obstruction badge,
 * custom recessed Frequency ID field, and hoverable '?' guide).
 */
@OnlyIn(Dist.CLIENT)
public class TeleporterScreen extends Screen {

    private static final int PANEL_W = 248;
    private static final int PANEL_H = 178;

    private final BlockPos pos;
    private final String initialId;
    private final boolean originObstructed;
    private final boolean destObstructed;

    private int left;
    private int top;
    private int riftBaseRgb = 0x00AAFF;

    private EditBox idBox;
    private RiftActionButton clearButton;
    private RiftActionButton cancelButton;
    private RiftActionButton saveButton;

    public TeleporterScreen(OpenTeleporterScreenPayload payload) {
        super(Component.translatable("gui.oririmod.teleporter.title"));
        this.pos = payload.pos();
        this.initialId = payload.currentId() == null ? "" : payload.currentId();
        this.originObstructed = payload.originObstructed();
        this.destObstructed = payload.destObstructed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;

        // Pull the actual teleporter's dyed rift color from the client BlockEntity if available
        if (this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.pos) instanceof TeleporterBlockEntity be) {
            this.riftBaseRgb = be.getBaseColor() & 0xFFFFFF;
        }

        int inputX = this.left + 20;
        int inputY = this.top + 96;
        int inputW = PANEL_W - 40;

        String preserved = this.idBox != null ? this.idBox.getValue() : this.initialId;

        this.idBox = new EditBox(this.font, inputX + 6, inputY + 6, inputW - 28, 12,
                Component.translatable("gui.oririmod.teleporter.id"));
        this.idBox.setBordered(false);
        this.idBox.setMaxLength(64);
        this.idBox.setTextColor(0xE8F6FF);
        this.idBox.setTextColorUneditable(0x7A8FA6);
        this.idBox.setValue(preserved);
        this.idBox.setHint(Component.translatable("gui.oririmod.teleporter.placeholder").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.idBox);
        this.setInitialFocus(this.idBox);

        this.clearButton = new RiftActionButton(
                inputX + inputW - 18, inputY + 2, 16, 16,
                Component.literal("x"), false, true,
                () -> {
                    this.idBox.setValue("");
                    this.setFocused(this.idBox);
                }
        );
        this.addRenderableWidget(this.clearButton);

        int btnY = this.top + PANEL_H - 32;
        this.cancelButton = new RiftActionButton(
                this.left + 20, btnY, 92, 20,
                Component.translatable("gui.oririmod.teleporter.cancel"), false, false,
                this::onClose
        );
        this.addRenderableWidget(this.cancelButton);

        this.saveButton = new RiftActionButton(
                this.left + 120, btnY, 108, 20,
                Component.translatable("gui.oririmod.teleporter.save"), true, false,
                this::saveAndClose
        );
        this.addRenderableWidget(this.saveButton);
    }

    private void saveAndClose() {
        String newId = this.idBox.getValue().trim();
        PacketDistributor.sendToServer(new UpdateTeleporterIDPayload(this.pos, newId));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isObstructed() {
        return this.originObstructed || this.destObstructed;
    }

    private int activeAccentRgb() {
        return isObstructed() ? 0xFF4258 : this.riftBaseRgb;
    }

    private int getHelpX() {
        return this.left + PANEL_W - 22;
    }

    private int getHelpY() {
        return this.top + 8;
    }

    private boolean isHoveringHelp(int mouseX, int mouseY) {
        int hx = getHelpX();
        int hy = getHelpY();
        return mouseX >= hx && mouseX <= hx + 14 && mouseY >= hy && mouseY <= hy + 14;
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gui);

        float time = (this.minecraft != null && this.minecraft.level != null)
                ? (this.minecraft.level.getGameTime() + partialTick) * 0.08F
                : 0.0F;

        int accent = activeAccentRgb();
        int accentBright = blendRgb(accent, 0xFFFFFF, 0.45F);
        int accentDim = blendRgb(accent, 0x0B121D, 0.55F);

        // === 1. CARVED BASALT MONOLITH FRAME ===
        drawMonolithPanel(gui, this.left, this.top, PANEL_W, PANEL_H, accent, accentDim);

        // === 2. HEADER TITLE ===
        int centerX = this.left + PANEL_W / 2;
        gui.drawCenteredString(this.font, this.title, centerX, this.top + 10, 0xFF000000 | accentBright);

        // === 3. ANIMATED DIAMOND ARCANE RIFT + SIDE INFO ===
        int riftCx = centerX;
        int riftCy = this.top + 48;
        drawDiamondRift(gui, riftCx, riftCy, accent, accentBright, time);

        // Energy conduits left & right of diamond bezel
        int pulse = (int) ((Mth.sin(time * 1.4F) * 0.5F + 0.5F) * 20);
        gui.fill(this.left + 14, riftCy, riftCx - 25, riftCy + 1, 0xFF000000 | accentDim);
        gui.fill(riftCx + 26, riftCy, this.left + PANEL_W - 14, riftCy + 1, 0xFF000000 | accentDim);
        // Subtle traveling spark pixel along each conduit
        int sparkOffset = (int) ((time * 14.0F) % 62.0F);
        int leftSparkX = (riftCx - 26) - sparkOffset;
        int rightSparkX = (riftCx + 26) + sparkOffset;
        if (leftSparkX > this.left + 15) {
            gui.fill(leftSparkX - 1, riftCy, leftSparkX + 2, riftCy + 1, 0xCC000000 | accentBright);
        }
        if (rightSparkX < this.left + PANEL_W - 16) {
            gui.fill(rightSparkX - 1, riftCy, rightSparkX + 2, riftCy + 1, 0xCC000000 | accentBright);
        }

        // Left side of diamond: Coordinates
        int leftInfoX = this.left + 16;
        gui.drawString(this.font, Component.translatable("gui.oririmod.teleporter.coords"), leftInfoX, this.top + 32, 0xFF6E859E, false);
        String coordLine = this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ();
        gui.drawString(this.font, coordLine, leftInfoX, this.top + 54, 0xFFB8CCE0, false);

        // Right side of diamond: Rift Status Pill
        int rightInfoX = riftCx + 28;
        gui.drawString(this.font, Component.translatable("gui.oririmod.teleporter.status"), rightInfoX, this.top + 32, 0xFF6E859E, false);
        boolean hasFreq = this.idBox != null && !this.idBox.getValue().trim().isEmpty();
        Component statusText;
        int statusColor;
        if (isObstructed()) {
            statusText = Component.translatable("gui.oririmod.teleporter.status_blocked");
            statusColor = 0xFFFF5A6E;
        } else if (hasFreq) {
            statusText = Component.translatable("gui.oririmod.teleporter.status_active");
            statusColor = 0xFF000000 | accentBright;
        } else {
            statusText = Component.translatable("gui.oririmod.teleporter.status_unlinked");
            statusColor = 0xFF8EA1B5;
        }
        // Pulsing status indicator dot
        int dotAlpha = isObstructed() ? (180 + pulse * 3) : (hasFreq ? (195 + pulse * 2) : 140);
        int dotCol = (Mth.clamp(dotAlpha, 0, 255) << 24) | (statusColor & 0xFFFFFF);
        gui.fill(rightInfoX, this.top + 56, rightInfoX + 4, this.top + 60, dotCol);
        gui.drawString(this.font, statusText, rightInfoX + 7, this.top + 54, statusColor, false);

        // Horizontal divider below rift header
        int divY = this.top + 74;
        gui.fill(this.left + 12, divY, this.left + PANEL_W - 12, divY + 1, 0xFF1F2E3F);
        gui.fill(centerX - 36, divY, centerX + 36, divY + 1, 0xFF000000 | accentDim);

        // === 4. FREQUENCY ID RECESSED INPUT BOX ===
        int inputX = this.left + 20;
        int inputY = this.top + 96;
        int inputW = PANEL_W - 40;
        int inputH = 20;
        gui.drawString(this.font, Component.translatable("gui.oririmod.teleporter.frequency_id"),
                inputX, inputY - 12, 0xFF9CB4CC, false);

        boolean focused = this.idBox != null && this.idBox.isFocused();
        int boxBorder = focused ? (0xFF000000 | accent) : 0xFF283A4E;
        gui.fill(inputX - 1, inputY - 1, inputX + inputW + 1, inputY + inputH + 1, boxBorder);
        gui.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF090E16);
        // Subtle inner top shadow
        gui.fill(inputX, inputY, inputX + inputW, inputY + 1, 0xFF05080D);

        // === 5. WARNING / HINT STRIP ===
        int msgY = this.top + 123;
        if (this.originObstructed) {
            drawAlertStrip(gui, this.left + 20, msgY, PANEL_W - 40,
                    Component.translatable("gui.oririmod.teleporter.origin_obstructed"));
        } else if (this.destObstructed) {
            drawAlertStrip(gui, this.left + 20, msgY, PANEL_W - 40,
                    Component.translatable("gui.oririmod.teleporter.obstructed"));
        } else {
            Component hint = Component.translatable("gui.oririmod.teleporter.hint");
            gui.drawString(this.font, hint, centerX - this.font.width(hint) / 2, msgY + 3, 0xFF647B94, false);
        }

        // === 6. HELP '?' BADGE ===
        drawHelpBadge(gui, getHelpX(), getHelpY(), isHoveringHelp(mouseX, mouseY), accent, accentBright);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (this.clearButton != null && this.idBox != null) {
            this.clearButton.visible = !this.idBox.getValue().isEmpty();
        }
        super.render(gui, mouseX, mouseY, partialTick);

        if (isHoveringHelp(mouseX, mouseY)) {
            List<Component> lines = List.of(
                    Component.translatable("gui.oririmod.teleporter.help.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.translatable("gui.oririmod.teleporter.help.line1").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.teleporter.help.line2").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.teleporter.help.line3").withStyle(ChatFormatting.DARK_AQUA)
            );
            gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private void drawMonolithPanel(GuiGraphics gui, int x, int y, int w, int h, int accent, int accentDim) {
        // Notched dark basalt silhouette
        gui.fill(x + 3, y, x + w - 3, y + h, 0xFF060A10);
        gui.fill(x, y + 3, x + w, y + h - 3, 0xFF060A10);
        gui.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF15202D);

        // Chiseled stone bevel
        gui.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF223346);
        gui.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF121C28);

        // Deep void interior gradient
        gui.fillGradient(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF101925, 0xFF0A1018);

        // Glowing top & bottom rift accent trim bars
        gui.fill(x + 16, y + 2, x + w - 16, y + 3, 0xFF000000 | accentDim);
        gui.fill(x + w / 2 - 42, y + 2, x + w / 2 + 42, y + 3, 0xFF000000 | accent);
        gui.fill(x + 16, y + h - 3, x + w - 16, y + h - 2, 0xFF000000 | accentDim);

        // Four corner rune brackets
        int runeCol = 0xFF000000 | accent;
        // Top-left
        gui.fill(x + 4, y + 4, x + 12, y + 5, runeCol);
        gui.fill(x + 4, y + 4, x + 5, y + 12, runeCol);
        // Top-right
        gui.fill(x + w - 12, y + 4, x + w - 4, y + 5, runeCol);
        gui.fill(x + w - 5, y + 4, x + w - 4, y + 12, runeCol);
        // Bottom-left
        gui.fill(x + 4, y + h - 5, x + 12, y + h - 4, runeCol);
        gui.fill(x + 4, y + h - 12, x + 5, y + h - 4, runeCol);
        // Bottom-right
        gui.fill(x + w - 12, y + h - 5, x + w - 4, y + h - 4, runeCol);
        gui.fill(x + w - 5, y + h - 12, x + w - 4, y + h - 4, runeCol);
    }

    private void drawDiamondRift(GuiGraphics gui, int cx, int cy, int accent, int accentBright, float time) {
        int outer = 22;
        int bezel = 18;
        int deepVoid = blendRgb(accent, 0x050A12, 0.78F);

        for (int dy = -outer; dy < outer; dy += 2) {
            for (int dx = -outer; dx < outer; dx += 2) {
                float px = dx + 1.0F;
                float py = dy + 1.0F;
                float manhattan = Math.abs(px) + Math.abs(py);
                if (manhattan > outer) continue;

                if (manhattan > bezel) {
                    // Carved stone diamond rim
                    int rimCol = (px + py < 0) ? 0xFF33495F : 0xFF172330;
                    if (manhattan > outer - 2.0F) {
                        rimCol = 0xFF080D14;
                    }
                    gui.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, rimCol);
                } else {
                    float dist = Mth.sqrt(px * px + py * py);
                    float angle = (float) Math.atan2(py, px);
                    float swirl = Mth.sin(angle * 3.0F - dist * 0.34F + time * 1.2F);
                    int col;
                    if (dist < 4.5F) {
                        col = 0xFF000000 | blendRgb(accentBright, 0xFFFFFF, 0.55F);
                    } else if (swirl > 0.25F) {
                        col = 0xEE000000 | accentBright;
                    } else if (swirl > -0.35F) {
                        col = 0xDD000000 | accent;
                    } else {
                        col = 0xFF000000 | deepVoid;
                    }
                    gui.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, col);
                }
            }
        }
    }

    private void drawAlertStrip(GuiGraphics gui, int x, int y, int w, Component text) {
        gui.fill(x, y, x + w, y + 14, 0xFF5A1622);
        gui.fill(x + 1, y + 1, x + w - 1, y + 13, 0xFF280B12);
        gui.drawString(this.font, text, x + (w - this.font.width(text)) / 2, y + 3, 0xFFFF6B7A, false);
    }

    private void drawHelpBadge(GuiGraphics gui, int hx, int hy, boolean hovered, int accent, int accentBright) {
        int border = hovered ? (0xFF000000 | accentBright) : 0xFF2E4359;
        int fill = hovered ? (0xFF000000 | blendRgb(accent, 0x0E1824, 0.72F)) : 0xFF111B27;
        gui.fill(hx, hy, hx + 14, hy + 14, border);
        gui.fill(hx + 1, hy + 1, hx + 13, hy + 13, fill);
        gui.drawString(this.font, "?", hx + (14 - this.font.width("?")) / 2 + 1, hy + 3,
                hovered ? 0xFFFFFFFF : (0xFF000000 | accentBright), false);
    }

    private static int blendRgb(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = Mth.clamp((int) Mth.lerp(t, r1, r2), 0, 255);
        int g = Mth.clamp((int) Mth.lerp(t, g1, g2), 0, 255);
        int b = Mth.clamp((int) Mth.lerp(t, b1, b2), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private class RiftActionButton extends AbstractButton {
        private final boolean primary;
        private final boolean compact;
        private final Runnable onClick;

        RiftActionButton(int x, int y, int w, int h, Component msg, boolean primary, boolean compact, Runnable onClick) {
            super(x, y, w, h, msg);
            this.primary = primary;
            this.compact = compact;
            this.onClick = onClick;
        }

        @Override
        public void onPress() {
            this.onClick.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hovered = isHoveredOrFocused();
            int accent = activeAccentRgb();
            int accentBright = blendRgb(accent, 0xFFFFFF, 0.4F);

            if (this.compact) {
                int bg = hovered ? 0xFF33202A : 0xFF141E2B;
                int txt = hovered ? 0xFFFF6B7A : 0xFF6E859E;
                gui.fill(x, y, x + w, y + h, bg);
                gui.drawString(font, getMessage(), x + (w - font.width(getMessage())) / 2, y + (h - 8) / 2, txt, false);
                return;
            }

            if (this.primary) {
                int border = hovered ? (0xFF000000 | accentBright) : (0xFF000000 | accent);
                int topCol = hovered ? (0xFF000000 | blendRgb(accent, 0x1A2E42, 0.55F)) : (0xFF000000 | blendRgb(accent, 0x102030, 0.72F));
                int botCol = hovered ? (0xFF000000 | blendRgb(accent, 0x0E1B28, 0.70F)) : 0xFF0D1724;
                gui.fill(x, y, x + w, y + h, border);
                gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, topCol, botCol);
                gui.fill(x + 2, y + 1, x + w - 2, y + 2, 0x66000000 | accentBright);
            } else {
                int border = hovered ? 0xFF47627E : 0xFF283A4E;
                int topCol = hovered ? 0xFF1F2E3F : 0xFF16212E;
                int botCol = hovered ? 0xFF15202D : 0xFF0F1722;
                gui.fill(x, y, x + w, y + h, border);
                gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, topCol, botCol);
            }

            int textColor = this.primary
                    ? (hovered ? 0xFFFFFFFF : (0xFF000000 | blendRgb(accentBright, 0xFFFFFF, 0.4F)))
                    : (hovered ? 0xFFE2EEF8 : 0xFFA2B8CE);
            gui.drawString(font, getMessage(),
                    x + (w - font.width(getMessage())) / 2,
                    y + (h - 8) / 2,
                    textColor, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
