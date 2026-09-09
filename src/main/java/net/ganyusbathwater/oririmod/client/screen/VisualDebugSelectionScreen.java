package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisualDebugSelectionScreen extends Screen {

    public VisualDebugSelectionScreen() {
        super(Component.literal("Visual Debug Menu"));
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int startX = (this.width - buttonWidth) / 2;
        int startY = (this.height - buttonHeight) / 2 - 40;

        // Button 1: Lunar Skybox Toggle
        Component initialText = Component.literal("Lunar Skybox: " + (LunarSkyboxState.isEnabled() ? "ON" : "OFF"));
        
        Button lunarSkyboxButton = Button.builder(initialText, (button) -> {
            boolean newState = LunarSkyboxState.toggle();
            button.setMessage(Component.literal("Lunar Skybox: " + (newState ? "ON" : "OFF")));
        }).bounds(startX, startY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(lunarSkyboxButton);
        
        Button animeImpactButton = Button.builder(Component.literal("Trigger Anime Impact (1s)"), (button) -> {
            net.ganyusbathwater.oririmod.client.render.world.AnimeImpactState.triggerImpact(1000);
            this.onClose(); // Close the screen so we can see the effect
        }).bounds(startX, startY + buttonHeight + 4, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(animeImpactButton);

        Button timestopButton = Button.builder(Component.literal("Trigger Timestop (10s)"), (button) -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                net.ganyusbathwater.oririmod.client.render.world.TimestopState.triggerTimestop(10000, mc.player.position());
            }
            this.onClose();
        }).bounds(startX, startY + (buttonHeight + 4) * 2, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(timestopButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
