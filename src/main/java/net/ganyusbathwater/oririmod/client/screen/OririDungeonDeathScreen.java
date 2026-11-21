package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonDeathActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Dramatic crimson-and-obsidian Dungeon Death Screen with life crystal indicators
 * and a live 5-second recharge bar built into the Respawn button.
 */
@OnlyIn(Dist.CLIENT)
public class OririDungeonDeathScreen extends Screen {

    private static final int COOLDOWN_MS = 5000;
    private static final int PANEL_W = 268;
    private static final int PANEL_H = 192;

    private final int livesRemaining;
    private final long openTimeMs;

    private SanctuaryButton respawnButton;

    public OririDungeonDeathScreen(int livesRemaining) {
        super(Component.translatable("deathScreen.title"));
        this.livesRemaining = livesRemaining;
        this.openTimeMs = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void init() {
        super.init();

        int midX = this.width / 2;
        int panelTop = (this.height - PANEL_H) / 2;
        int btnW = 212;
        int btnX = midX - btnW / 2;
        int btnStartY = panelTop + 104;

        this.respawnButton = new SanctuaryButton(
                btnX,
                btnStartY,
                btnW,
                22,
                Component.literal("Respawn (5)"),
                ButtonTone.EMERALD,
                () -> {
                    PacketDistributor.sendToServer(new DungeonDeathActionPayload("RESPAWN"));
                    this.onClose();
                }
        );
        this.respawnButton.active = false;
        this.addRenderableWidget(this.respawnButton);

        this.addRenderableWidget(new SanctuaryButton(
                btnX,
                btnStartY + 28,
                btnW,
                22,
                Component.translatable("screen.oririmod.dungeon.death.give_up"),
                ButtonTone.CRIMSON,
                () -> {
                    PacketDistributor.sendToServer(new DungeonDeathActionPayload("GIVE_UP"));
                    this.onClose();
                }
        ));

        this.addRenderableWidget(new SanctuaryButton(
                btnX,
                btnStartY + 56,
                btnW,
                20,
                Component.translatable("deathScreen.titleScreen"),
                ButtonTone.NEUTRAL,
                () -> {
                    if (this.minecraft != null && this.minecraft.level != null) {
                        this.minecraft.level.disconnect();
                        this.minecraft.setScreen(new TitleScreen());
                    }
                }
        ));

        updateRespawnState();
    }

    @Override
    public void tick() {
        super.tick();
        updateRespawnState();
    }

    private void updateRespawnState() {
        if (this.respawnButton == null) return;
        long elapsed = System.currentTimeMillis() - this.openTimeMs;
        if (elapsed >= COOLDOWN_MS) {
            this.respawnButton.active = true;
            this.respawnButton.setFillProgress(-1.0F, 0);
            this.respawnButton.setMessage(Component.translatable("screen.oririmod.dungeon.death.respawn"));
        } else {
            this.respawnButton.active = false;
            float progress = Mth.clamp(elapsed / (float) COOLDOWN_MS, 0.0F, 1.0F);
            int secondsLeft = (int) Math.ceil((COOLDOWN_MS - elapsed) / 1000.0);
            this.respawnButton.setFillProgress(progress, 0x66277A48);
            this.respawnButton.setMessage(
                    Component.translatable("screen.oririmod.dungeon.death.respawn_wait", secondsLeft));
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Deep crimson vignette over the world (single clean pass — no double background)
        gfx.fillGradient(0, 0, this.width, this.height, 0x8832060A, 0xB8180306);
        int edgeBand = Math.max(24, this.height / 7);
        gfx.fillGradient(0, 0, this.width, edgeBand, 0x88080002, 0x00080002);
        gfx.fillGradient(0, this.height - edgeBand, this.width, this.height, 0x00080002, 0xAA080002);

        int midX = this.width / 2;
        int panelLeft = midX - PANEL_W / 2;
        int panelTop  = (this.height - PANEL_H) / 2;

        // Crimson-trimmed obsidian sanctuary card
        drawSanctuaryPanel(gfx, panelLeft, panelTop, PANEL_W, PANEL_H, CRIMSON, 0xFF6B2329);

        // Smooth live progress update every frame for silky recharge bar
        updateRespawnState();

        // Title (1.5× scale for crisp readability inside the card)
        gfx.pose().pushPose();
        float scale = 1.5F;
        gfx.pose().scale(scale, scale, 1.0F);
        gfx.drawCenteredString(this.font,
                Component.translatable("screen.oririmod.dungeon.death.title").withStyle(ChatFormatting.BOLD),
                Math.round(midX / scale),
                Math.round((panelTop + 16) / scale),
                CRIMSON_LIGHT);
        gfx.pose().popPose();

        drawGildedDivider(gfx, panelLeft + 18, panelTop + 38, PANEL_W - 36, CRIMSON);

        // Visual life crystals + remaining lives label
        int crystalY = panelTop + 56;
        if (this.livesRemaining > 0) {
            int displayCount = Math.min(this.livesRemaining, 5);
            int spacing = 18;
            int startX = midX - ((displayCount - 1) * spacing) / 2;
            float pulse = 0.78F + 0.22F * Mth.sin((System.currentTimeMillis() % 2400L) / 2400.0F * Mth.TWO_PI);

            for (int i = 0; i < displayCount; i++) {
                int cx = startX + i * spacing;
                drawDiamond(gfx, cx, crystalY, 6, withAlpha(CRIMSON, (int) (0x55 * pulse)));
                drawDiamond(gfx, cx, crystalY, 4, CRIMSON_LIGHT);
                drawDiamond(gfx, cx, crystalY, 2, GOLD_BRIGHT);
            }

            gfx.drawCenteredString(this.font,
                    Component.translatable("screen.oririmod.dungeon.death.lives", this.livesRemaining),
                    midX, panelTop + 72, GOLD_BRIGHT);
        } else {
            // Shattered final life seal
            drawDiamond(gfx, midX, crystalY, 6, 0xFF2E1216);
            drawDiamond(gfx, midX, crystalY, 4, 0xFF5A232B);
            gfx.hLine(midX - 5, midX + 5, crystalY, 0xFF0A0507);
            gfx.vLine(midX, crystalY - 5, crystalY + 5, 0xFF0A0507);

            gfx.drawCenteredString(this.font,
                    Component.translatable("screen.oririmod.dungeon.death.final_life").withStyle(ChatFormatting.BOLD),
                    midX, panelTop + 72, CRIMSON_LIGHT);
        }

        drawGildedDivider(gfx, panelLeft + 18, panelTop + 92, PANEL_W - 36, 0xFF5E242A);
    }
}
