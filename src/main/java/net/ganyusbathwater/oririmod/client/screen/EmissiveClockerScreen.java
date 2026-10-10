package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.OpenClockerScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncClockerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class EmissiveClockerScreen extends Screen {

    private final net.minecraft.core.BlockPos pos;

    private CustomCycleButton modeButton;
    private EditBox delayBox;
    private EditBox timeOnBox;
    private EditBox timeOffBox;
    private CustomCycleButton useSecondsButton;
    private CustomCycleButton emitLightButton;

    private String currentMode;
    private final int initialDelay;
    private final int initialTimeOn;
    private final int initialTimeOff;
    private final boolean initialUseSeconds;
    private final boolean initialEmitLight;

    private static final List<String> MODES = Arrays.asList("REPEATER", "PULSE", "CLOCK");

    public EmissiveClockerScreen(OpenClockerScreenPayload payload) {
        super(Component.literal("Configure Emissive Clocker"));
        this.pos = payload.pos();
        this.currentMode = MODES.get(Math.max(0, Math.min(2, payload.mode())));
        this.initialDelay = payload.delay();
        this.initialTimeOn = payload.timeOn();
        this.initialTimeOff = payload.timeOff();
        this.initialUseSeconds = payload.useSeconds();
        this.initialEmitLight = payload.emitLight();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int midX = this.width / 2;

        this.modeButton = new CustomCycleButton(
                midX - 70, 0, 140, 20,
                "Mode", MODES, currentMode,
                val -> {
                    this.currentMode = val;
                    this.updateUIState();
                }
        );
        this.modeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Select the redstone behavior mode")));
        this.addRenderableWidget(this.modeButton);

        this.delayBox = new EditBox(this.font, midX - 70, 0, 140, 20, Component.literal("Delay"));
        this.delayBox.setMaxLength(10);
        this.delayBox.setFilter(s -> s.matches("\\d*"));
        this.delayBox.setValue(String.valueOf(initialDelay));
        this.delayBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Initial delay on power")));
        this.addRenderableWidget(this.delayBox);

        this.timeOnBox = new EditBox(this.font, midX - 70, 0, 140, 20, Component.literal("Time On"));
        this.timeOnBox.setMaxLength(10);
        this.timeOnBox.setFilter(s -> s.matches("\\d*"));
        this.timeOnBox.setValue(String.valueOf(initialTimeOn));
        this.timeOnBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Duration ON")));
        this.addRenderableWidget(this.timeOnBox);

        this.timeOffBox = new EditBox(this.font, midX - 70, 0, 140, 20, Component.literal("Time Off"));
        this.timeOffBox.setMaxLength(10);
        this.timeOffBox.setFilter(s -> s.matches("\\d*"));
        this.timeOffBox.setValue(String.valueOf(initialTimeOff));
        this.timeOffBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Duration OFF")));
        this.addRenderableWidget(this.timeOffBox);

        this.useSecondsButton = new CustomCycleButton(
                midX - 70, 0, 140, 20,
                "Unit", Arrays.asList("Ticks", "Seconds"), initialUseSeconds ? "Seconds" : "Ticks",
                val -> {}
        );
        this.useSecondsButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Time unit for the input fields")));
        this.addRenderableWidget(this.useSecondsButton);

        this.emitLightButton = new CustomCycleButton(
                midX - 70, 0, 140, 20,
                "Emit Light", Arrays.asList("Yes", "No"), initialEmitLight ? "Yes" : "No",
                val -> {}
        );
        this.emitLightButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Whether this block visually emits real block light")));
        this.addRenderableWidget(this.emitLightButton);

        this.addRenderableWidget(Button.builder(Component.literal("Save").withStyle(ChatFormatting.GREEN), b -> saveAndClose())
                .bounds(this.width / 2 - 70, this.height - 35, 140, 20).build());

        updateUIState();
    }

    private void updateUIState() {
        if (delayBox == null) return;
        
        delayBox.visible = false;
        timeOnBox.visible = false;
        timeOffBox.visible = false;

        switch (currentMode) {
            case "REPEATER" -> {
                delayBox.visible = true;
            }
            case "PULSE" -> {
                delayBox.visible = true;
                timeOnBox.visible = true;
            }
            case "CLOCK" -> {
                delayBox.visible = true;
                timeOnBox.visible = true;
                timeOffBox.visible = true;
            }
        }

        java.util.List<net.minecraft.client.gui.components.AbstractWidget> visibleWidgets = new java.util.ArrayList<>();
        visibleWidgets.add(modeButton);
        if (delayBox.visible) visibleWidgets.add(delayBox);
        if (timeOnBox.visible) visibleWidgets.add(timeOnBox);
        if (timeOffBox.visible) visibleWidgets.add(timeOffBox);
        visibleWidgets.add(useSecondsButton);
        visibleWidgets.add(emitLightButton);
        
        int topY = 45;
        int bottomY = this.height - 45;
        int availableHeight = bottomY - topY;
        
        int widgetCount = visibleWidgets.size();
        int spacing = 36;
        int totalHeight = widgetCount * 20 + (widgetCount - 1) * (spacing - 20);
        
        if (totalHeight > availableHeight) {
             spacing = (availableHeight - (widgetCount * 20)) / Math.max(1, widgetCount - 1) + 20;
             totalHeight = widgetCount * 20 + (widgetCount - 1) * (spacing - 20);
        }
        
        int currentY = topY + (availableHeight - totalHeight) / 2;
        for (net.minecraft.client.gui.components.AbstractWidget w : visibleWidgets) {
             w.setY(currentY);
             currentY += spacing;
        }
    }

    private void saveAndClose() {
        int delay = 20;
        int timeOn = 20;
        int timeOff = 20;
        try { delay = Integer.parseInt(delayBox.getValue()); } catch (NumberFormatException ignored) {}
        try { timeOn = Integer.parseInt(timeOnBox.getValue()); } catch (NumberFormatException ignored) {}
        try { timeOff = Integer.parseInt(timeOffBox.getValue()); } catch (NumberFormatException ignored) {}

        boolean useSeconds = this.useSecondsButton.values.get(this.useSecondsButton.currentIndex).equals("Seconds");
        boolean emitLight = this.emitLightButton.values.get(this.emitLightButton.currentIndex).equals("Yes");
        int modeIndex = MODES.indexOf(currentMode);
        if (modeIndex < 0) modeIndex = 0;

        PacketDistributor.sendToServer(new SyncClockerDataPayload(
                pos, modeIndex, delay, timeOn, timeOff, useSeconds, emitLight
        ));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), this.width / 2, 15, 0xFFFFFF);
        String coordText = "XYZ: " + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ();
        guiGraphics.drawCenteredString(this.font, Component.literal(coordText).withStyle(ChatFormatting.GRAY), this.width / 2, 30, 0xFFFFFF);

        if (this.modeButton.visible) {
            renderLabelWithTooltip(guiGraphics, "Mode", this.modeButton.getX(), this.modeButton.getY() - 10, mouseX, mouseY, 
                "REPEATER: Passes signal through after a delay.",
                "PULSE: Emits a single pulse of a set duration.",
                "CLOCK: Alternates ON/OFF repeatedly while powered.");
        }
        if (this.delayBox.visible) {
            String label = currentMode.equals("REPEATER") ? "Delay" : "Initial Delay";
            renderLabelWithTooltip(guiGraphics, label, this.delayBox.getX(), this.delayBox.getY() - 10, mouseX, mouseY, 
                "REPEATER: Time before the output state changes.",
                "PULSE: Time to wait before emitting the pulse.",
                "CLOCK: Initial delay before the clock starts ticking.");
        }
        if (this.timeOnBox.visible) {
            renderLabelWithTooltip(guiGraphics, "Duration ON", this.timeOnBox.getX(), this.timeOnBox.getY() - 10, mouseX, mouseY, 
                "The duration the redstone signal remains ON.");
        }
        if (this.timeOffBox.visible) {
            renderLabelWithTooltip(guiGraphics, "Duration OFF", this.timeOffBox.getX(), this.timeOffBox.getY() - 10, mouseX, mouseY, 
                "The duration the redstone signal remains OFF between ticks.");
        }
        if (this.useSecondsButton.visible) {
            renderLabelWithTooltip(guiGraphics, "Time Unit", this.useSecondsButton.getX(), this.useSecondsButton.getY() - 10, mouseX, mouseY, 
                "Ticks: 20 ticks = 1 second. Fast and precise.",
                "Seconds: Standard real-time seconds.");
        }
        if (this.emitLightButton.visible) {
            renderLabelWithTooltip(guiGraphics, "Lighting", this.emitLightButton.getX(), this.emitLightButton.getY() - 10, mouseX, mouseY, 
                "If enabled, the block will visually emit light when powered.",
                "Purely cosmetic, does not affect redstone.");
        }
    }

    private void renderLabelWithTooltip(GuiGraphics guiGraphics, String label, int x, int y, int mouseX, int mouseY, String... tooltipLines) {
        String fullLabel = label + " (?)";
        guiGraphics.drawString(this.font, fullLabel, x, y, 0xDDDDDD);
        
        int textWidth = this.font.width(fullLabel);
        if (mouseX >= x && mouseX <= x + textWidth && mouseY >= y && mouseY <= y + 9) {
            java.util.List<Component> tooltip = new java.util.ArrayList<>();
            for (String line : tooltipLines) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private class CustomCycleButton extends AbstractButton {
        private final String prefix;
        public List<String> values;
        public int currentIndex;
        private final java.util.function.Consumer<String> onValueChange;

        public CustomCycleButton(int x, int y, int width, int height, String prefix, List<String> values, String initialValue, java.util.function.Consumer<String> onValueChange) {
            super(x, y, width, height, Component.literal(""));
            this.prefix = prefix;
            this.values = values;
            this.currentIndex = Math.max(0, values.indexOf(initialValue));
            this.onValueChange = onValueChange;
            this.updateText();
        }

        @Override
        public void onPress() {
            this.currentIndex = (this.currentIndex + 1) % this.values.size();
            this.updateText();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.active && this.visible && this.clicked(mouseX, mouseY)) {
                if (button == 0) { // Left click
                    this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                    this.onPress();
                    return true;
                } else if (button == 1) { // Right click
                    this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                    this.currentIndex = (this.currentIndex - 1 + this.values.size()) % this.values.size();
                    this.updateText();
                    return true;
                }
            }
            return false;
        }

        private void updateText() {
            this.setMessage(Component.literal(prefix + ": " + this.values.get(this.currentIndex)));
            this.onValueChange.accept(this.values.get(this.currentIndex));
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
