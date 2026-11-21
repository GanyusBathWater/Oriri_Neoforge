package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.client.render.world.AnimeImpactState;
import net.ganyusbathwater.oririmod.client.render.world.TimestopState;
import net.ganyusbathwater.oririmod.client.skybox.LunarSkyboxState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Programmatic Amethyst-and-Cyan Visual FX Debug Console.
 */
public class VisualDebugSelectionScreen extends Screen {

    private static final int PANEL_W = 248;
    private static final int PANEL_H = 156;

    private SanctuaryButton lunarSkyboxButton;

    public VisualDebugSelectionScreen() {
        super(Component.literal("Visual Debug Menu"));
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int btnW = PANEL_W - 28;
        int btnX = left + 14;
        int startY = top + 38;

        boolean lunarOn = LunarSkyboxState.isEnabled();
        this.lunarSkyboxButton = new SanctuaryButton(
                btnX, startY, btnW, 20,
                Component.literal("Lunar Skybox: " + (lunarOn ? "ON" : "OFF")),
                lunarOn ? ButtonTone.EMERALD : ButtonTone.NEUTRAL,
                () -> {
                    boolean newState = LunarSkyboxState.toggle();
                    this.lunarSkyboxButton.setMessage(Component.literal("Lunar Skybox: " + (newState ? "ON" : "OFF")));
                    this.lunarSkyboxButton.setTone(newState ? ButtonTone.EMERALD : ButtonTone.NEUTRAL);
                }
        );
        this.addRenderableWidget(this.lunarSkyboxButton);

        this.addRenderableWidget(new SanctuaryButton(
                btnX, startY + 26, btnW, 20,
                Component.literal("Trigger Anime Impact (1s)"),
                ButtonTone.CYAN,
                () -> {
                    AnimeImpactState.triggerImpact(1000);
                    this.onClose();
                }
        ));

        this.addRenderableWidget(new SanctuaryButton(
                btnX, startY + 52, btnW, 20,
                Component.literal("Trigger Timestop (10s)"),
                ButtonTone.PRIMARY,
                () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        TimestopState.triggerTimestop(10000, mc.player.position());
                    }
                    this.onClose();
                }
        ));

        this.addRenderableWidget(new SanctuaryButton(
                this.width / 2 - 44,
                top + PANEL_H - 28,
                88,
                20,
                Component.translatable("screen.oririmod.dungeon.close"),
                ButtonTone.NEUTRAL,
                this::onClose
        ));
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        drawSanctuaryPanel(gfx, left, top, PANEL_W, PANEL_H, AMETHYST, AMETHYST_DIM);
        gfx.drawCenteredString(this.font,
                this.title.copy().withStyle(ChatFormatting.BOLD),
                this.width / 2, top + 12, CYAN_LIGHT);
        drawGildedDivider(gfx, left + 14, top + 28, PANEL_W - 28, AMETHYST);
        drawGildedDivider(gfx, left + 14, top + PANEL_H - 35, PANEL_W - 28, AMETHYST_DIM);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
