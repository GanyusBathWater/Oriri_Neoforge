package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.OpenClockerScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncClockerDataPayload;
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
 * Programmatic Clockwork Redstone Lamp Console for the {@code Emissive Clocker}, inspired by
 * {@code emissive_clocker_on.png} and {@code emissive_clocker_off.png} (steel/iron casing, copper lattice trim,
 * live pulsing diamond lamp & real-time redstone waveform oscilloscope, direct 3-tab mode selector,
 * quick -/+ timing steppers, and custom toggle switches).
 */
@OnlyIn(Dist.CLIENT)
public class EmissiveClockerScreen extends Screen {

    private static final int PANEL_W = 264;
    private static final int PANEL_H = 224;
    private static final String[] MODES = {"REPEATER", "PULSE", "CLOCK"};

    private final BlockPos pos;

    private int left;
    private int top;

    private int currentModeIndex;
    private int delayVal;
    private int timeOnVal;
    private int timeOffVal;
    private boolean useSeconds;
    private boolean emitLight;

    private final ModeTabButton[] modeTabs = new ModeTabButton[3];
    private EditBox delayBox;
    private EditBox timeOnBox;
    private EditBox timeOffBox;

    private StepButton delayMinus, delayPlus;
    private StepButton onMinus, onPlus;
    private StepButton offMinus, offPlus;

    private TogglePillButton unitToggle;
    private TogglePillButton lightToggle;
    private ConsoleActionButton cancelButton;
    private ConsoleActionButton saveButton;

    public EmissiveClockerScreen(OpenClockerScreenPayload payload) {
        super(Component.translatable("gui.oririmod.clocker.title"));
        this.pos = payload.pos();
        this.currentModeIndex = Mth.clamp(payload.mode(), 0, 2);
        this.delayVal = Math.max(1, payload.delay());
        this.timeOnVal = Math.max(1, payload.timeOn());
        this.timeOffVal = Math.max(1, payload.timeOff());
        this.useSeconds = payload.useSeconds();
        this.emitLight = payload.emitLight();
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

        // Preserve unsaved typed numbers across window resize
        syncValuesFromBoxes();

        // === 1. 3-TAB MODE SELECTOR ===
        int tabAreaX = this.left + 14;
        int tabY = this.top + 70;
        int tabGap = 4;
        int tabW = (PANEL_W - 28 - tabGap * 2) / 3;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            this.modeTabs[i] = new ModeTabButton(
                    tabAreaX + i * (tabW + tabGap), tabY, tabW, 20,
                    MODES[i], idx,
                    () -> {
                        this.currentModeIndex = idx;
                        updateRowVisibility();
                    }
            );
            this.addRenderableWidget(this.modeTabs[i]);
        }

        // === 2. TIMING INPUT ROWS (Delay, Duration ON, Duration OFF) ===
        int rowBaseY = this.top + 100;
        int rowStep = 22;
        int ctrlX = this.left + 114;

        this.delayMinus = new StepButton(ctrlX, rowBaseY, "-", () -> stepField(this.delayBox, -1));
        this.delayBox = createNumericBox(ctrlX + 20, rowBaseY, this.delayVal);
        this.delayPlus = new StepButton(ctrlX + 84, rowBaseY, "+", () -> stepField(this.delayBox, +1));
        this.addRenderableWidget(this.delayMinus);
        this.addRenderableWidget(this.delayBox);
        this.addRenderableWidget(this.delayPlus);

        this.onMinus = new StepButton(ctrlX, rowBaseY + rowStep, "-", () -> stepField(this.timeOnBox, -1));
        this.timeOnBox = createNumericBox(ctrlX + 20, rowBaseY + rowStep, this.timeOnVal);
        this.onPlus = new StepButton(ctrlX + 84, rowBaseY + rowStep, "+", () -> stepField(this.timeOnBox, +1));
        this.addRenderableWidget(this.onMinus);
        this.addRenderableWidget(this.timeOnBox);
        this.addRenderableWidget(this.onPlus);

        this.offMinus = new StepButton(ctrlX, rowBaseY + rowStep * 2, "-", () -> stepField(this.timeOffBox, -1));
        this.timeOffBox = createNumericBox(ctrlX + 20, rowBaseY + rowStep * 2, this.timeOffVal);
        this.offPlus = new StepButton(ctrlX + 84, rowBaseY + rowStep * 2, "+", () -> stepField(this.timeOffBox, +1));
        this.addRenderableWidget(this.offMinus);
        this.addRenderableWidget(this.timeOffBox);
        this.addRenderableWidget(this.offPlus);

        // === 3. TOGGLE ROW (Time Unit & Emit Light) ===
        int toggleY = this.top + 168;
        int toggleGap = 8;
        int toggleW = (PANEL_W - 28 - toggleGap) / 2;

        this.unitToggle = new TogglePillButton(
                this.left + 14, toggleY, toggleW, 20,
                () -> Component.translatable("gui.oririmod.clocker.unit_label",
                        this.useSeconds ? Component.translatable("gui.oririmod.clocker.seconds") : Component.translatable("gui.oririmod.clocker.ticks")),
                () -> this.useSeconds,
                0xFF28C8E0, 0xFFFFB74D,
                () -> this.useSeconds = !this.useSeconds
        );
        this.addRenderableWidget(this.unitToggle);

        this.lightToggle = new TogglePillButton(
                this.left + 14 + toggleW + toggleGap, toggleY, toggleW, 20,
                () -> Component.translatable("gui.oririmod.clocker.light_label",
                        this.emitLight ? Component.translatable("gui.oririmod.clocker.on") : Component.translatable("gui.oririmod.clocker.off")),
                () -> this.emitLight,
                0xFFFFC14D, 0xFF6B4226,
                () -> this.emitLight = !this.emitLight
        );
        this.addRenderableWidget(this.lightToggle);

        // === 4. BOTTOM ACTION BAR ===
        int btnY = this.top + PANEL_H - 28;
        this.cancelButton = new ConsoleActionButton(
                this.left + 14, btnY, 96, 20,
                Component.translatable("gui.oririmod.clocker.cancel"), false,
                this::onClose
        );
        this.addRenderableWidget(this.cancelButton);

        this.saveButton = new ConsoleActionButton(
                this.left + 118, btnY, 132, 20,
                Component.translatable("gui.oririmod.clocker.save"), true,
                this::saveAndClose
        );
        this.addRenderableWidget(this.saveButton);

        updateRowVisibility();
    }

    private EditBox createNumericBox(int x, int y, int initialValue) {
        EditBox box = new EditBox(this.font, x + 5, y + 4, 52, 11, Component.empty());
        box.setBordered(false);
        box.setMaxLength(7);
        box.setFilter(s -> s.matches("\\d*"));
        box.setTextColor(0xFFFFE7B8);
        box.setValue(String.valueOf(initialValue));
        return box;
    }

    private void stepField(EditBox box, int dir) {
        int delta = hasShiftDown() ? dir * 10 : dir;
        int cur = parsePositive(box, 1);
        int next = Mth.clamp(cur + delta, 1, 999999);
        box.setValue(String.valueOf(next));
        syncValuesFromBoxes();
    }

    private void updateRowVisibility() {
        boolean showOn = this.currentModeIndex >= 1;   // PULSE, CLOCK
        boolean showOff = this.currentModeIndex == 2;  // CLOCK

        this.onMinus.visible = showOn;
        this.timeOnBox.visible = showOn;
        this.onPlus.visible = showOn;

        this.offMinus.visible = showOff;
        this.timeOffBox.visible = showOff;
        this.offPlus.visible = showOff;
    }

    private void syncValuesFromBoxes() {
        if (this.delayBox != null) this.delayVal = parsePositive(this.delayBox, this.delayVal);
        if (this.timeOnBox != null) this.timeOnVal = parsePositive(this.timeOnBox, this.timeOnVal);
        if (this.timeOffBox != null) this.timeOffVal = parsePositive(this.timeOffBox, this.timeOffVal);
    }

    private static int parsePositive(EditBox box, int fallback) {
        if (box == null) return Math.max(1, fallback);
        try {
            int parsed = Integer.parseInt(box.getValue());
            return Math.max(1, parsed);
        } catch (NumberFormatException e) {
            return Math.max(1, fallback);
        }
    }

    private void saveAndClose() {
        syncValuesFromBoxes();
        PacketDistributor.sendToServer(new SyncClockerDataPayload(
                this.pos,
                this.currentModeIndex,
                this.delayVal,
                this.timeOnVal,
                this.timeOffVal,
                this.useSeconds,
                this.emitLight
        ));
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

    private int getHelpX() {
        return this.left + PANEL_W - 22;
    }

    private int getHelpY() {
        return this.top + 8;
    }

    private boolean isHoveringHelp(int mouseX, int mouseY) {
        int hx = getHelpX(), hy = getHelpY();
        return mouseX >= hx && mouseX <= hx + 14 && mouseY >= hy && mouseY <= hy + 14;
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gui);
        syncValuesFromBoxes();

        // Monotonic wall-clock time (20 ticks/sec) in double precision so the oscilloscope never snaps back
        // during tick freezes or loses 32-bit float precision on long-running worlds.
        double ticksNow = (net.minecraft.Util.getMillis() % 86_400_000L) * 0.02D;

        // === 1. METALLIC & COPPER CLOCKWORK CHASSIS ===
        drawChassisPanel(gui, this.left, this.top, PANEL_W, PANEL_H);

        // === 2. HEADER TITLE + COORDS + '?' HELP BADGE ===
        gui.drawString(this.font, this.title, this.left + 14, this.top + 11, 0xFFFFD480, false);
        String coords = this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ();
        gui.drawString(this.font, coords, this.left + PANEL_W - 28 - this.font.width(coords), this.top + 11, 0xFF7E8794, false);
        drawHelpBadge(gui, getHelpX(), getHelpY(), isHoveringHelp(mouseX, mouseY));

        // === 3. LIVE SIGNAL OSCILLOSCOPE + MINI EMISSIVE LAMP ===
        drawSignalPreviewStrip(gui, this.left + 14, this.top + 27, PANEL_W - 28, 36, ticksNow);

        // === 4. TIMING INPUT ROWS & RECESSED FRAMES ===
        int rowBaseY = this.top + 100;
        int rowStep = 22;
        int labelX = this.left + 16;
        int boxX = this.left + 134;
        String unitTag = this.useSeconds ? "sec" : "ticks";

        // Row 1: Delay / Initial Delay
        Component delayLabel = Component.translatable(
                this.currentModeIndex == 0 ? "gui.oririmod.clocker.delay" : "gui.oririmod.clocker.initial_delay");
        drawTimingRowChrome(gui, labelX, boxX, rowBaseY, delayLabel, unitTag, this.delayBox.isFocused());

        // Row 2: Duration ON (PULSE & CLOCK)
        if (this.timeOnBox.visible) {
            drawTimingRowChrome(gui, labelX, boxX, rowBaseY + rowStep,
                    Component.translatable("gui.oririmod.clocker.duration_on"), unitTag, this.timeOnBox.isFocused());
        }

        // Row 3: Duration OFF (CLOCK only)
        if (this.timeOffBox.visible) {
            drawTimingRowChrome(gui, labelX, boxX, rowBaseY + rowStep * 2,
                    Component.translatable("gui.oririmod.clocker.duration_off"), unitTag, this.timeOffBox.isFocused());
        }

        // Subtle explanatory note in empty timing space for REPEATER / PULSE modes (word-wrapped to stay inside panel)
        int maxHintWidth = PANEL_W - 32;
        if (this.currentModeIndex == 0) {
            int hy = rowBaseY + rowStep + 2;
            for (var line : this.font.split(Component.translatable("gui.oririmod.clocker.mode_hint_repeater"), maxHintWidth)) {
                gui.drawString(this.font, line, this.left + 16, hy, 0xFF7B8390, false);
                hy += 10;
            }
            gui.drawString(this.font, Component.translatable("gui.oririmod.clocker.shift_step_hint"),
                    this.left + 16, Math.max(hy + 2, rowBaseY + rowStep * 2 + 4), 0xFF5D6470, false);
        } else if (this.currentModeIndex == 1) {
            int hy = rowBaseY + rowStep * 2 + 1;
            for (var line : this.font.split(Component.translatable("gui.oririmod.clocker.mode_hint_pulse"), maxHintWidth)) {
                gui.drawString(this.font, line, this.left + 16, hy, 0xFF7B8390, false);
                hy += 9;
            }
        }

        // Divider above toggles
        int divY = this.top + 162;
        gui.fill(this.left + 14, divY, this.left + PANEL_W - 14, divY + 1, 0xFF342A24);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        renderHoverTooltips(gui, mouseX, mouseY);
    }

    private void drawChassisPanel(GuiGraphics gui, int x, int y, int w, int h) {
        // Outer ink outline
        gui.fill(x, y, x + w, y + h, 0xFF121317);
        // Steel/stone outer rim matching emissive_clocker block border
        gui.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF9EA4AE);
        gui.fill(x + 2, y + 2, x + w - 1, y + h - 1, 0xFF595F69);
        // Warm copper/bronze inner frame
        gui.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF7A4422);
        gui.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xFF3B2214);
        // Dark iron instrument body
        gui.fillGradient(x + 5, y + 5, x + w - 5, y + h - 5, 0xFF202229, 0xFF15171C);

        // Decorative copper rivets at the 4 inner corners
        int rivet = 0xFFB87038;
        gui.fill(x + 7, y + 7, x + 9, y + 9, rivet);
        gui.fill(x + w - 9, y + 7, x + w - 7, y + 9, rivet);
        gui.fill(x + 7, y + h - 9, x + 9, y + h - 7, rivet);
        gui.fill(x + w - 9, y + h - 9, x + w - 7, y + h - 7, rivet);
    }

    /**
     * Renders the live miniature Emissive Clocker lamp icon + real-time scrolling redstone waveform preview.
     */
    private void drawSignalPreviewStrip(GuiGraphics gui, int x, int y, int w, int h, double ticksNow) {
        // Copper bezel + deep obsidian oscilloscope screen
        gui.fill(x, y, x + w, y + h, 0xFF6B3D1F);
        gui.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0C0E13);

        // Compute proportional simulation period (preserves exact ON/OFF duty ratios even for very short or long values)
        int d = Math.max(1, this.delayVal);
        int on = Math.max(1, this.timeOnVal);
        int off = Math.max(1, this.timeOffVal);
        float mult = this.useSeconds ? 20.0F : 1.0F;
        float dTicks = d * mult;
        float onTicks = on * mult;
        float offTicks = off * mult;

        float rawPeriod;
        if (this.currentModeIndex == 0) {
            // Repeater demo: delay wait -> equal ON plateau -> drop -> rest
            float hold = Math.max(14.0F, Math.min(dTicks, 40.0F));
            rawPeriod = dTicks + hold + Math.max(14.0F, hold);
        } else if (this.currentModeIndex == 1) {
            // Pulse demo: initial delay -> ON pulse -> rest gap
            rawPeriod = dTicks + onTicks + Math.max(16.0F, (dTicks + onTicks) * 0.35F);
        } else {
            // Clock demo: exact ON + OFF cycle
            rawPeriod = onTicks + offTicks;
        }

        // Normalize visual playback speed smoothly into [16..140] display ticks while keeping exact phase ratios
        float displayPeriod = Mth.clamp(rawPeriod, 16.0F, 140.0F);
        float timeScale = displayPeriod / Math.max(1.0F, rawPeriod);
        float simD = dTicks * timeScale;
        float simOn = onTicks * timeScale;
        float simOff = offTicks * timeScale;

        double periodD = displayPeriod;
        float nowPhase = (float) (((ticksNow % periodD) + periodD) % periodD);
        boolean currentHigh = sampleSignalHigh(nowPhase, displayPeriod, simD, simOn, simOff);

        // === Left: Miniature 26x26 Diamond Emissive Clocker Lamp ===
        int lampX = x + 5;
        int lampY = y + 5;
        drawMiniClockerLamp(gui, lampX, lampY, 26, currentHigh);

        // Vertical copper separator
        int waveX = x + 37;
        int waveY = y + 3;
        int waveW = w - 42;
        int waveH = h - 6;
        gui.fill(waveX - 3, y + 2, waveX - 2, y + h - 2, 0xFF3E2414);

        // Compact cadence label above the waveform (cleanly separated from trace)
        String unitShort = this.useSeconds ? "s" : "t";
        String summary = switch (this.currentModeIndex) {
            case 0 -> "Delay " + d + unitShort;
            case 1 -> "D:" + d + unitShort + "  ON:" + on + unitShort;
            default -> "ON:" + on + unitShort + "  OFF:" + off + unitShort;
        };
        gui.drawString(this.font, summary, waveX + 3, waveY + 1, 0xFF9FA8B8, false);

        // Subtle oscilloscope gridlines (below caption row)
        int plotTop = waveY + 11;
        int highY = plotTop + 2;
        int lowY = waveY + waveH - 3;
        for (int gx = waveX + 18; gx < waveX + waveW; gx += 18) {
            gui.fill(gx, plotTop, gx + 1, lowY + 1, 0xFF171B24);
        }
        gui.fill(waveX, lowY, waveX + waveW, lowY + 1, 0xFF232733);
        gui.fill(waveX, highY, waveX + waveW, highY + 1, 0xFF1C202B);

        // 1-px scrolling square-wave trace flowing right -> left directly into the lamp;
        // leftmost pixel (px == 0, right beside the lamp) samples EXACTLY simNow == currentHigh.
        int lastPx = waveW - 1;
        float ticksPerPx = displayPeriod / (waveW * 0.55F);
        boolean prevHigh = currentHigh;
        for (int px = 0; px <= lastPx; px++) {
            double sampleT = ticksNow + px * (double) ticksPerPx;
            float wrapped = (float) (((sampleT % periodD) + periodD) % periodD);
            boolean high = sampleSignalHigh(wrapped, displayPeriod, simD, simOn, simOff);
            int sy = high ? highY : lowY;

            if (high) {
                gui.fill(waveX + px, highY + 1, waveX + px + 1, lowY, this.emitLight ? 0x38FFB74D : 0x35E53935);
            }
            int traceCol = high ? (this.emitLight ? 0xFFFFCA58 : 0xFFFF5252) : 0xFF6B3A2C;
            gui.fill(waveX + px, sy, waveX + px + 1, sy + 2, traceCol);

            // Vertical rising/falling edge connector
            if (px > 0 && high != prevHigh) {
                gui.fill(waveX + px, highY, waveX + px + 1, lowY + 2, 0xFFFF8A50);
            }
            prevHigh = high;
        }

        // Live left-edge playhead dot right next to the lamp, locked to the exact same currentHigh state
        int headY = currentHigh ? highY : lowY;
        gui.fill(waveX - 1, headY - 1, waveX + 2, headY + 3, currentHigh ? 0xFFFFF1B8 : 0xFF9E5A3C);
    }

    private boolean sampleSignalHigh(float phase, float period, float dTicks, float onTicks, float offTicks) {
        if (this.currentModeIndex == 0) {
            // Repeater: delayed high plateau
            float hold = Math.max(6.0F, (period - dTicks) * 0.5F);
            return phase >= dTicks && phase < (dTicks + hold);
        } else if (this.currentModeIndex == 1) {
            // Pulse: wait delay, then emit single pulse of duration ON
            return phase >= dTicks && phase < (dTicks + onTicks);
        } else {
            // Clock: alternating ON / OFF with exact duty cycle
            return phase < onTicks;
        }
    }

    /**
     * Draws a miniature stylized Emissive Clocker face echoing {@code emissive_clocker_on.png} / {@code _off.png}.
     */
    private void drawMiniClockerLamp(GuiGraphics gui, int x, int y, int size, boolean lit) {
        // Outer metallic rim
        gui.fill(x, y, x + size, y + size, 0xFFBCC2CC);
        gui.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF7B818C);
        // Inner lamp glass base
        int bgCol = lit ? (this.emitLight ? 0xFFFFD885 : 0xFFD97236) : 0xFF703B1D;
        gui.fill(x + 2, y + 2, x + size - 2, y + size - 2, bgCol);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2 - 2;
        for (int dy = -r; dy < r; dy += 2) {
            for (int dx = -r; dx < r; dx += 2) {
                float m = Math.abs(dx + 1.0F) + Math.abs(dy + 1.0F);
                if (m >= r - 2.2F && m <= r + 0.5F) {
                    // Iconic dark diamond lattice frame from emissive_clocker texture
                    gui.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, 0xFF1F120A);
                } else if (m < r - 2.2F) {
                    int coreCol;
                    if (lit) {
                        coreCol = (m < 4.0F)
                                ? (this.emitLight ? 0xFFFFF6D6 : 0xFFFF8A65)
                                : (this.emitLight ? 0xFFFFB74D : 0xFFE53935);
                    } else {
                        coreCol = (m < 4.0F) ? 0xFF8C4C26 : 0xFF6B381B;
                    }
                    gui.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, coreCol);
                }
            }
        }
    }

    private void drawTimingRowChrome(GuiGraphics gui, int labelX, int boxX, int y, Component label, String unitTag, boolean focused) {
        // Label with subtle '?' affordance
        gui.drawString(this.font, label, labelX, y + 4, 0xFFD4CFC8, false);

        // Recessed copper-trimmed numeric box (60x16)
        int bw = 60, bh = 16;
        int border = focused ? 0xFFFFB74D : 0xFF5A3924;
        gui.fill(boxX - 1, y - 1, boxX + bw + 1, y + bh + 1, border);
        gui.fill(boxX, y, boxX + bw, y + bh, 0xFF0E1015);
        gui.fill(boxX, y, boxX + bw, y + 1, 0xFF08090C);

        // Unit pill on right of + button
        int unitX = this.left + 219;
        gui.drawString(this.font, unitTag, unitX, y + 4, this.useSeconds ? 0xFF4ADAF0 : 0xFFFFC266, false);
    }

    private void drawHelpBadge(GuiGraphics gui, int hx, int hy, boolean hovered) {
        int border = hovered ? 0xFFFFD480 : 0xFF6B4126;
        int fill = hovered ? 0xFF4A2A14 : 0xFF1B1D24;
        gui.fill(hx, hy, hx + 14, hy + 14, border);
        gui.fill(hx + 1, hy + 1, hx + 13, hy + 13, fill);
        gui.drawString(this.font, "?", hx + (14 - this.font.width("?")) / 2 + 1, hy + 3,
                hovered ? 0xFFFFFFFF : 0xFFFFD480, false);
    }

    private void renderHoverTooltips(GuiGraphics gui, int mouseX, int mouseY) {
        if (isHoveringHelp(mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.oririmod.clocker.help.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.oririmod.clocker.help.repeater").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.clocker.help.pulse").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.clocker.help.clock").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.oririmod.clocker.help.tip").withStyle(ChatFormatting.YELLOW)
            ), mouseX, mouseY);
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (this.modeTabs[i].isMouseOver(mouseX, mouseY)) {
                String key = switch (i) {
                    case 0 -> "gui.oririmod.clocker.tooltip.repeater";
                    case 1 -> "gui.oririmod.clocker.tooltip.pulse";
                    default -> "gui.oririmod.clocker.tooltip.clock";
                };
                gui.renderComponentTooltip(this.font, List.of(
                        Component.literal(MODES[i]).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        Component.translatable(key).withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
        }

        int rowBaseY = this.top + 100;
        int rowStep = 22;
        if (mouseX >= this.left + 14 && mouseX <= this.left + 110) {
            if (mouseY >= rowBaseY && mouseY <= rowBaseY + 16) {
                gui.renderComponentTooltip(this.font, List.of(
                        Component.translatable("gui.oririmod.clocker.tooltip.delay").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
            if (this.timeOnBox.visible && mouseY >= rowBaseY + rowStep && mouseY <= rowBaseY + rowStep + 16) {
                gui.renderComponentTooltip(this.font, List.of(
                        Component.translatable("gui.oririmod.clocker.tooltip.on").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
            if (this.timeOffBox.visible && mouseY >= rowBaseY + rowStep * 2 && mouseY <= rowBaseY + rowStep * 2 + 16) {
                gui.renderComponentTooltip(this.font, List.of(
                        Component.translatable("gui.oririmod.clocker.tooltip.off").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
        }

        if (this.unitToggle.isMouseOver(mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.oririmod.clocker.tooltip.unit").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        } else if (this.lightToggle.isMouseOver(mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.oririmod.clocker.tooltip.light").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    // ─── Custom Programmatic Widgets ───────────────────────────────────────────────────────────────

    private class ModeTabButton extends AbstractButton {
        private final int modeIdx;
        private final Runnable onSelect;

        ModeTabButton(int x, int y, int w, int h, String label, int modeIdx, Runnable onSelect) {
            super(x, y, w, h, Component.literal(label));
            this.modeIdx = modeIdx;
            this.onSelect = onSelect;
        }

        @Override
        public void onPress() {
            this.onSelect.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean selected = (currentModeIndex == this.modeIdx);
            boolean hovered = isHoveredOrFocused();

            int border = selected ? 0xFFFFB74D : (hovered ? 0xFF8C5A34 : 0xFF3E2D24);
            int topCol = selected ? 0xFF5A3118 : (hovered ? 0xFF2E2724 : 0xFF1A1C22);
            int botCol = selected ? 0xFF361D10 : (hovered ? 0xFF211E1F : 0xFF13151A);

            gui.fill(x, y, x + w, y + h, border);
            gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, topCol, botCol);
            if (selected) {
                gui.fill(x + 2, y + 1, x + w - 2, y + 3, 0xFFFFD480);
            }

            int textCol = selected ? 0xFFFFF2CC : (hovered ? 0xFFDAD4CC : 0xFF8B909B);
            gui.drawString(font, getMessage(),
                    x + (w - font.width(getMessage())) / 2,
                    y + (h - 8) / 2 + (selected ? 1 : 0),
                    textCol, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class StepButton extends AbstractButton {
        private final Runnable onStep;

        StepButton(int x, int y, String symbol, Runnable onStep) {
            super(x, y, 16, 16, Component.literal(symbol));
            this.onStep = onStep;
        }

        @Override
        public void onPress() {
            this.onStep.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean hovered = isHoveredOrFocused();
            gui.fill(x, y, x + w, y + h, hovered ? 0xFFFFB74D : 0xFF5A3924);
            gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                    hovered ? 0xFF472A18 : 0xFF252226,
                    hovered ? 0xFF2B190E : 0xFF19181D);
            gui.drawString(font, getMessage(),
                    x + (w - font.width(getMessage())) / 2 + 1,
                    y + (h - 8) / 2,
                    hovered ? 0xFFFFF2CC : 0xFFC7B8A8, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class TogglePillButton extends AbstractButton {
        private final java.util.function.Supplier<Component> labelSupplier;
        private final java.util.function.BooleanSupplier activeSupplier;
        private final int onPipColor;
        private final int offPipColor;
        private final Runnable onToggle;

        TogglePillButton(int x, int y, int w, int h,
                         java.util.function.Supplier<Component> labelSupplier,
                         java.util.function.BooleanSupplier activeSupplier,
                         int onPipColor, int offPipColor, Runnable onToggle) {
            super(x, y, w, h, labelSupplier.get());
            this.labelSupplier = labelSupplier;
            this.activeSupplier = activeSupplier;
            this.onPipColor = onPipColor;
            this.offPipColor = offPipColor;
            this.onToggle = onToggle;
        }

        @Override
        public void onPress() {
            this.onToggle.run();
            this.setMessage(this.labelSupplier.get());
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean isOn = this.activeSupplier.getAsBoolean();
            boolean hovered = isHoveredOrFocused();
            Component label = this.labelSupplier.get();

            int border = hovered ? 0xFFFFB74D : (isOn ? 0xFF7D502E : 0xFF3E3530);
            gui.fill(x, y, x + w, y + h, border);
            gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                    hovered ? 0xFF2E2828 : 0xFF1E2027,
                    hovered ? 0xFF1F1B1D : 0xFF15171D);

            // Indicator LED pip on left
            int pipCol = isOn ? this.onPipColor : this.offPipColor;
            gui.fill(x + 6, y + 7, x + 11, y + 13, 0xFF0B0C10);
            gui.fill(x + 7, y + 8, x + 10, y + 12, pipCol);

            gui.drawString(font, label,
                    x + 16 + (w - 20 - font.width(label)) / 2,
                    y + (h - 8) / 2,
                    hovered ? 0xFFFFFFFF : 0xFFDDD6CC, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class ConsoleActionButton extends AbstractButton {
        private final boolean primary;
        private final Runnable onClick;

        ConsoleActionButton(int x, int y, int w, int h, Component msg, boolean primary, Runnable onClick) {
            super(x, y, w, h, msg);
            this.primary = primary;
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

            if (this.primary) {
                int border = hovered ? 0xFFFFE082 : 0xFFC77D32;
                int topCol = hovered ? 0xFF6B3B16 : 0xFF4D2A12;
                int botCol = hovered ? 0xFF42220D : 0xFF2E180A;
                gui.fill(x, y, x + w, y + h, border);
                gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, topCol, botCol);
                gui.fill(x + 2, y + 1, x + w - 2, y + 2, 0x77FFE082);
            } else {
                int border = hovered ? 0xFF7B818C : 0xFF3E434D;
                int topCol = hovered ? 0xFF2B2E36 : 0xFF202229;
                int botCol = hovered ? 0xFF1D1F26 : 0xFF16181E;
                gui.fill(x, y, x + w, y + h, border);
                gui.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, topCol, botCol);
            }

            int txt = this.primary
                    ? (hovered ? 0xFFFFFFFF : 0xFFFFE7B8)
                    : (hovered ? 0xFFE6EAEF : 0xFFA7AFB9);
            gui.drawString(font, getMessage(),
                    x + (w - font.width(getMessage())) / 2,
                    y + (h - 8) / 2,
                    txt, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
