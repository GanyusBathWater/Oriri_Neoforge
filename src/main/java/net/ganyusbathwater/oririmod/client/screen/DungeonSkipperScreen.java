package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonSkipperActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSkipperScreenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Programmatic Dungeon Debug & Stage Controller Console.
 */
@OnlyIn(Dist.CLIENT)
public class DungeonSkipperScreen extends Screen {

    private static final int PANEL_W = 348;

    private OpenDungeonSkipperScreenPayload state;
    private int panelLeft;
    private int panelTop;
    private int panelH;

    public DungeonSkipperScreen(OpenDungeonSkipperScreenPayload initialPayload) {
        super(Component.translatable("screen.oririmod.dungeon_skipper.title"));
        this.state = initialPayload;
    }

    public void updateState(OpenDungeonSkipperScreenPayload newState) {
        this.state = newState;
        this.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        this.panelH = Math.min(286, this.height - 14);
        this.panelLeft = (this.width - PANEL_W) / 2;
        this.panelTop = (this.height - this.panelH) / 2;

        int midX = this.width / 2;
        int colLeftX = midX - 160;
        int colRightX = midX + 6;
        int btnW = 154;
        int btnH = 19;
        int startY = this.panelTop + 98;
        int spacingY = 22;

        boolean inDungeon = state.inDungeon();
        boolean canGoBack = inDungeon && (state.currentStageIndex() > 0 || "COMPLETE".equals(state.currentStageState()));
        boolean isPending = inDungeon && "PENDING".equals(state.currentStageState());

        // ── Left Column: Stage Navigation & Teleportation ──
        addBtn(colLeftX, startY, btnW, btnH, "« Previous Stage", ButtonTone.PRIMARY, canGoBack,
                "Rewinds 1 stage, despawns current mobs, and reverts doors, area modifiers, Magic Barrier Cores & switches.",
                () -> sendAction("PREVIOUS_STAGE"));

        addBtn(colLeftX, startY + spacingY, btnW, btnH, "↻ Restart Current Stage", ButtonTone.PRIMARY, inDungeon,
                "Resets and restarts the current stage from scratch, reverting its blocks and switches.",
                () -> sendAction("RESTART_STAGE"));

        addBtn(colLeftX, startY + spacingY * 2, btnW, btnH, "Skip Current Stage »", ButtonTone.EMERALD, inDungeon,
                "Force-completes the active stage (including key drops, doors, and area modifiers) and advances.",
                () -> sendAction("SKIP_STAGE"));

        addBtn(colLeftX, startY + spacingY * 3, btnW, btnH, "⚡ Force Trigger Stage", ButtonTone.CYAN, isPending,
                "Immediately activates a PENDING stage without needing to step into its STAGE_TRIGGER zone.",
                () -> sendAction("FORCE_START_STAGE"));

        addBtn(colLeftX, startY + spacingY * 4, btnW, btnH, "Teleport to Stage", ButtonTone.NEUTRAL, inDungeon,
                "Teleports you to the current stage's Player Spawn, Trigger, or Spawn Point.",
                () -> { sendAction("TP_STAGE"); this.onClose(); });

        addBtn(colLeftX, startY + spacingY * 5, btnW, btnH, "TP to Respawn Point", ButtonTone.CYAN,
                inDungeon && state.hasRespawnPos(),
                "Teleports you directly to the active respawn point to verify its placement.",
                () -> { sendAction("TP_RESPAWN"); this.onClose(); });

        // ── Right Column: Debug Settings & Diagnostics ──
        addBtn(colRightX, startY, btnW, btnH,
                "Infinite Lives: " + (state.infiniteLives() ? "ON (∞)" : "OFF"),
                state.infiniteLives() ? ButtonTone.EMERALD : ButtonTone.NEUTRAL, inDungeon,
                "Prevents losing lives on death so you can test death areas, respawn points, and softlocks infinitely.",
                () -> sendAction("TOGGLE_INFINITE_LIVES"));

        addBtn(colRightX, startY + spacingY, btnW, btnH,
                "Chat Debug Info: " + (state.debugLogging() ? "ON" : "OFF"),
                state.debugLogging() ? ButtonTone.EMERALD : ButtonTone.NEUTRAL, inDungeon,
                "Shows hidden stage transitions, triggers, and completion events in the chat.",
                () -> sendAction("TOGGLE_DEBUG_LOG"));

        addBtn(colRightX, startY + spacingY * 2, btnW, btnH,
                "3D Debug Zones: " + (state.showDebugZones() ? "ON" : "OFF"),
                state.showDebugZones() ? ButtonTone.CYAN : ButtonTone.NEUTRAL, inDungeon,
                "Renders 3D wireframe boxes & labels for Death Areas, Triggers, Goals, Spawns, Switches, Doors & Puzzles.",
                () -> sendAction("TOGGLE_DEBUG_ZONES"));

        addBtn(colRightX, startY + spacingY * 3, btnW, btnH, "Highlight Stage Mobs", ButtonTone.PRIMARY, inDungeon,
                "Applies Glowing (30s) to all alive dungeon mobs so you can spot enemies stuck in walls or caves.",
                () -> sendAction("HIGHLIGHT_MOBS"));

        addBtn(colRightX, startY + spacingY * 4, btnW, btnH, "Print Stage Diagnostics", ButtonTone.NEUTRAL, inDungeon,
                "Prints detailed hidden info to chat: remaining mob coords, switch states, block match diffs, and trigger coords.",
                () -> sendAction("PRINT_DIAGNOSTICS"));

        addBtn(colRightX, startY + spacingY * 5, btnW, btnH, "☠ Test Respawn (Die)", ButtonTone.CRIMSON, inDungeon,
                "Triggers dungeon death (switches to Survival if in Creative) to test the death screen and respawn point.",
                () -> { sendAction("TEST_RESPAWN_DEATH"); this.onClose(); });

        // ── Bottom Rows: Key Grant, Secret Verification & Next Run ──
        int row7Y = startY + spacingY * 6 + 4;
        addBtn(colLeftX, row7Y, btnW, btnH, "Grant Stage Key", ButtonTone.CYAN, inDungeon,
                "Gives you a Mana Destabilizer key to test Magic Barrier Core locks.",
                () -> sendAction("GRANT_STAGE_KEY"));

        addBtn(colRightX, row7Y, btnW, btnH, "✔ Check Secrets & Chests", ButtonTone.PRIMARY, inDungeon,
                "Checks if all Secret Chests and Secret Buttons were placed without issues AND verifies that they are still physically present and intact in the dungeon.",
                () -> sendAction("CHECK_SECRETS"));

        int row8Y = row7Y + spacingY;
        addBtn(colLeftX, row8Y, btnW, btnH,
                "Next Run All Secrets: " + (state.spawnAllSecretsNext() ? "ON" : "OFF"),
                state.spawnAllSecretsNext() ? ButtonTone.PRIMARY : ButtonTone.NEUTRAL, true,
                "Toggles whether the NEXT generated dungeon spawns all secret chests and buttons instead of only 3.",
                () -> sendAction("TOGGLE_NEXT_SECRETS"));

        addBtn(colRightX, row8Y, btnW, btnH,
                Component.translatable("screen.oririmod.dungeon.close").getString(),
                ButtonTone.NEUTRAL, true, null, this::onClose);
    }

    private void addBtn(int x, int y, int w, int h, String label, ButtonTone tone, boolean active,
                        String tooltip, Runnable action) {
        SanctuaryButton btn = new SanctuaryButton(x, y, w, h, Component.literal(label), tone, action);
        btn.active = active;
        if (tooltip != null) {
            btn.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        this.addRenderableWidget(btn);
    }

    private void sendAction(String action) {
        PacketDistributor.sendToServer(new DungeonSkipperActionPayload(action, 0));
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int midX = this.width / 2;
        drawSanctuaryPanel(gfx, panelLeft, panelTop, PANEL_W, panelH, GOLD, BRONZE);

        // Header title
        gfx.drawCenteredString(this.font,
                this.title.copy().withStyle(ChatFormatting.BOLD),
                midX, panelTop + 9, TEXT_TITLE);

        // Recessed telemetry card
        int cardX = panelLeft + 12;
        int cardY = panelTop + 23;
        int cardW = PANEL_W - 24;
        int cardH = 58;
        drawRecessedWell(gfx, cardX, cardY, cardW, cardH, BRONZE);

        if (state.inDungeon()) {
            String stageProgress = (state.currentStageIndex() + 1) + " / " + Math.max(1, state.totalStages());
            String line1 = String.format("Dungeon: §f%s §7| Stage (§e%s§7): §b%s",
                    state.dungeonId(), stageProgress, state.currentStageId());
            gfx.drawString(this.font, truncate(this.font, line1, cardW - 76), cardX + 7, cardY + 6, TEXT_BODY, false);

            // State badge pill top-right inside telemetry card
            String st = state.currentStageState();
            int pillBg = "ACTIVE".equals(st) ? EMERALD_DARK : ("PENDING".equals(st) ? AMBER_DARK : CYAN_DARK);
            int pillBorder = "ACTIVE".equals(st) ? EMERALD : ("PENDING".equals(st) ? AMBER : CYAN);
            int pillTxt = "ACTIVE".equals(st) ? EMERALD_LIGHT : ("PENDING".equals(st) ? AMBER_LIGHT : CYAN_LIGHT);
            int pillW = this.font.width(st) + 10;
            drawPill(gfx, this.font, st, cardX + cardW - pillW - 6, cardY + 4, pillBg, pillBorder, pillTxt);

            String respawnStr = state.hasRespawnPos()
                    ? "[" + state.respawnPos().getX() + ", " + state.respawnPos().getY() + ", " + state.respawnPos().getZ() + "]"
                    : "None";
            String livesStr = state.infiniteLives() ? "∞ (" + state.playerLives() + ")" : String.valueOf(state.playerLives());
            String line2 = "Type: §f" + state.currentStageType()
                    + " §7| Lives: " + (state.infiniteLives() ? "§a" : "§c") + livesStr;
            gfx.drawString(this.font, truncate(this.font, line2, cardW - 14), cardX + 7, cardY + 20, TEXT_BODY, false);

            String secretsMode = state.spawnAllSecretsCurrent() ? "All Secrets" : "Normal (3)";
            String line3 = "Respawn: §b" + respawnStr + " §7| Mode: §d" + secretsMode;
            gfx.drawString(this.font, truncate(this.font, line3, cardW - 14), cardX + 7, cardY + 32, TEXT_BODY, false);

            String line4 = "Secrets: " + state.secretsStatusSummary();
            gfx.drawString(this.font, truncate(this.font, line4, cardW - 14), cardX + 7, cardY + 44, TEXT_BODY, false);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.literal("Not currently inside an active dungeon instance."),
                    midX, cardY + 18, CRIMSON_LIGHT);
            gfx.drawCenteredString(this.font,
                    Component.literal("Enter a dungeon for stage controls, or configure next-run below."),
                    midX, cardY + 33, TEXT_MUTED);
        }

        // Column headers above controls
        gfx.drawString(this.font, "Stage Control", midX - 160, panelTop + 86, GOLD, false);
        gfx.drawString(this.font, "Diagnostics & Toggles", midX + 6, panelTop + 86, CYAN_LIGHT, false);
    }
}
