package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonSkipperActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSkipperScreenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class DungeonSkipperScreen extends Screen {

    private OpenDungeonSkipperScreenPayload state;

    public DungeonSkipperScreen(OpenDungeonSkipperScreenPayload initialPayload) {
        super(Component.translatable("screen.oririmod.dungeon_skipper.title"));
        this.state = initialPayload;
    }

    public void updateState(OpenDungeonSkipperScreenPayload newState) {
        this.state = newState;
        this.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int midX = this.width / 2;
        int colLeftX = midX - 155;
        int colRightX = midX + 5;
        int btnWidth = 150;
        int btnHeight = 20;
        int startY = 82;
        int spacingY = 23;

        boolean inDungeon = state.inDungeon();
        boolean canGoBack = inDungeon && (state.currentStageIndex() > 0 || "COMPLETE".equals(state.currentStageState()));
        boolean isPending = inDungeon && "PENDING".equals(state.currentStageState());

        // ── Left Column: Stage Navigation & Teleportation ──
        Button prevStageBtn = Button.builder(
                        Component.literal("« Previous Stage").withStyle(canGoBack ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY),
                        b -> sendAction("PREVIOUS_STAGE"))
                .bounds(colLeftX, startY, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Rewinds 1 stage, despawns current mobs, and reverts doors, area modifiers, Magic Barrier Cores & switches.")))
                .build();
        prevStageBtn.active = canGoBack;
        this.addRenderableWidget(prevStageBtn);

        Button restartStageBtn = Button.builder(
                        Component.literal("↻ Restart Current Stage").withStyle(inDungeon ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY),
                        b -> sendAction("RESTART_STAGE"))
                .bounds(colLeftX, startY + spacingY, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Resets and restarts the current stage from scratch, reverting its blocks and switches.")))
                .build();
        restartStageBtn.active = inDungeon;
        this.addRenderableWidget(restartStageBtn);

        Button skipStageBtn = Button.builder(
                        Component.literal("Skip Current Stage »").withStyle(inDungeon ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY),
                        b -> sendAction("SKIP_STAGE"))
                .bounds(colLeftX, startY + spacingY * 2, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Force-completes the active stage (including key drops, doors, and area modifiers) and advances.")))
                .build();
        skipStageBtn.active = inDungeon;
        this.addRenderableWidget(skipStageBtn);

        Button forceTriggerBtn = Button.builder(
                        Component.literal("⚡ Force Trigger Stage").withStyle(isPending ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY),
                        b -> sendAction("FORCE_START_STAGE"))
                .bounds(colLeftX, startY + spacingY * 3, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Immediately activates a PENDING stage without needing to step into its STAGE_TRIGGER zone.")))
                .build();
        forceTriggerBtn.active = isPending;
        this.addRenderableWidget(forceTriggerBtn);

        Button tpStageBtn = Button.builder(
                        Component.literal("Teleport to Stage").withStyle(inDungeon ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY),
                        b -> {
                            sendAction("TP_STAGE");
                            this.onClose();
                        })
                .bounds(colLeftX, startY + spacingY * 4, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Teleports you to the current stage's Player Spawn, Trigger, or Spawn Point.")))
                .build();
        tpStageBtn.active = inDungeon;
        this.addRenderableWidget(tpStageBtn);

        Button tpRespawnBtn = Button.builder(
                        Component.literal("TP to Respawn Point").withStyle(inDungeon && state.hasRespawnPos() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY),
                        b -> {
                            sendAction("TP_RESPAWN");
                            this.onClose();
                        })
                .bounds(colLeftX, startY + spacingY * 5, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Teleports you directly to the active respawn point to verify its placement.")))
                .build();
        tpRespawnBtn.active = inDungeon && state.hasRespawnPos();
        this.addRenderableWidget(tpRespawnBtn);

        // ── Right Column: Debug Settings & Diagnostics ──
        Button infLivesBtn = Button.builder(
                        Component.literal("Infinite Lives: ").append(
                                Component.literal(state.infiniteLives() ? "ON (∞)" : "OFF")
                                        .withStyle(state.infiniteLives() ? ChatFormatting.GREEN : ChatFormatting.RED)),
                        b -> sendAction("TOGGLE_INFINITE_LIVES"))
                .bounds(colRightX, startY, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Prevents losing lives on death so you can test death areas, respawn points, and softlocks infinitely.")))
                .build();
        infLivesBtn.active = inDungeon;
        this.addRenderableWidget(infLivesBtn);

        Button debugLogBtn = Button.builder(
                        Component.literal("Chat Debug Info: ").append(
                                Component.literal(state.debugLogging() ? "ON" : "OFF")
                                        .withStyle(state.debugLogging() ? ChatFormatting.GREEN : ChatFormatting.RED)),
                        b -> sendAction("TOGGLE_DEBUG_LOG"))
                .bounds(colRightX, startY + spacingY, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Shows hidden stage transitions, triggers, and completion events in the chat.")))
                .build();
        debugLogBtn.active = inDungeon;
        this.addRenderableWidget(debugLogBtn);

        Button debugZonesBtn = Button.builder(
                        Component.literal("3D Debug Zones: ").append(
                                Component.literal(state.showDebugZones() ? "ON" : "OFF")
                                        .withStyle(state.showDebugZones() ? ChatFormatting.AQUA : ChatFormatting.RED)),
                        b -> sendAction("TOGGLE_DEBUG_ZONES"))
                .bounds(colRightX, startY + spacingY * 2, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Renders 3D wireframe boxes & labels for Death Areas, Triggers, Goals, Spawns, Switches, Doors & Puzzles.")))
                .build();
        debugZonesBtn.active = inDungeon;
        this.addRenderableWidget(debugZonesBtn);

        Button highlightMobsBtn = Button.builder(
                        Component.literal("Highlight Stage Mobs").withStyle(inDungeon ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY),
                        b -> sendAction("HIGHLIGHT_MOBS"))
                .bounds(colRightX, startY + spacingY * 3, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Applies Glowing (30s) to all alive dungeon mobs so you can spot enemies stuck in walls or caves.")))
                .build();
        highlightMobsBtn.active = inDungeon;
        this.addRenderableWidget(highlightMobsBtn);

        Button diagnosticsBtn = Button.builder(
                        Component.literal("Print Stage Diagnostics").withStyle(inDungeon ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY),
                        b -> sendAction("PRINT_DIAGNOSTICS"))
                .bounds(colRightX, startY + spacingY * 4, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Prints detailed hidden info to chat: remaining mob coords, switch states, block match diffs, and trigger coords.")))
                .build();
        diagnosticsBtn.active = inDungeon;
        this.addRenderableWidget(diagnosticsBtn);

        Button testRespawnDeathBtn = Button.builder(
                        Component.literal("☠ Test Respawn (Die)").withStyle(inDungeon ? ChatFormatting.RED : ChatFormatting.DARK_GRAY),
                        b -> {
                            sendAction("TEST_RESPAWN_DEATH");
                            this.onClose();
                        })
                .bounds(colRightX, startY + spacingY * 5, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Triggers dungeon death (switches to Survival if in Creative) to test the death screen and respawn point.")))
                .build();
        testRespawnDeathBtn.active = inDungeon;
        this.addRenderableWidget(testRespawnDeathBtn);

        // ── Bottom Rows: Key Grant, Secret Verification & Next Dungeon All Secrets ──
        int bottomY = startY + spacingY * 6 + 3;
        Button grantKeyBtn = Button.builder(
                        Component.literal("Grant Stage Key").withStyle(inDungeon ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY),
                        b -> sendAction("GRANT_STAGE_KEY"))
                .bounds(colLeftX, bottomY, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Gives you a Mana Destabilizer key to test Magic Barrier Core locks.")))
                .build();
        grantKeyBtn.active = inDungeon;
        this.addRenderableWidget(grantKeyBtn);

        Button checkSecretsBtn = Button.builder(
                        Component.literal("✔ Check Secrets & Chests").withStyle(inDungeon ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY),
                        b -> sendAction("CHECK_SECRETS"))
                .bounds(colRightX, bottomY, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Checks if all Secret Chests and Secret Buttons were placed without issues AND verifies that they are still physically present and intact in the dungeon.")))
                .build();
        checkSecretsBtn.active = inDungeon;
        this.addRenderableWidget(checkSecretsBtn);

        int row8Y = bottomY + spacingY;
        this.addRenderableWidget(Button.builder(
                        Component.literal("Next Run All Secrets: ").append(
                                Component.literal(state.spawnAllSecretsNext() ? "ON" : "OFF")
                                        .withStyle(state.spawnAllSecretsNext() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY)),
                        b -> sendAction("TOGGLE_NEXT_SECRETS"))
                .bounds(colLeftX, row8Y, btnWidth, btnHeight)
                .tooltip(Tooltip.create(Component.literal("Toggles whether the NEXT generated dungeon spawns all secret chests and buttons instead of only 3.")))
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.oririmod.dungeon.close"), b -> this.onClose())
                .bounds(colRightX, row8Y, btnWidth, btnHeight)
                .build());
    }

    private void sendAction(String action) {
        PacketDistributor.sendToServer(new DungeonSkipperActionPayload(action, 0));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int midX = this.width / 2;
        // Draw panel backdrop
        guiGraphics.fill(midX - 165, 8, midX + 165, 78, 0xAA000000);
        guiGraphics.renderOutline(midX - 165, 8, 330, 70, 0xFF555555);

        guiGraphics.drawCenteredString(this.font,
                this.title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                midX, 13, 0xFFFFFF);

        if (state.inDungeon()) {
            String stageProgress = (state.currentStageIndex() + 1) + " / " + Math.max(1, state.totalStages());
            String line1 = String.format("Dungeon: §f%s §7| Stage (§e%s§7): §b%s",
                    state.dungeonId(), stageProgress, state.currentStageId());
            guiGraphics.drawCenteredString(this.font, Component.literal(line1).withStyle(ChatFormatting.GRAY), midX, 26, 0xFFFFFF);

            ChatFormatting stateColor = switch (state.currentStageState()) {
                case "PENDING" -> ChatFormatting.YELLOW;
                case "ACTIVE" -> ChatFormatting.GREEN;
                case "COMPLETE" -> ChatFormatting.AQUA;
                default -> ChatFormatting.GRAY;
            };
            String respawnStr = state.hasRespawnPos()
                    ? "[" + state.respawnPos().getX() + ", " + state.respawnPos().getY() + ", " + state.respawnPos().getZ() + "]"
                    : "None";
            String livesStr = state.infiniteLives() ? "∞ (" + state.playerLives() + ")" : String.valueOf(state.playerLives());

            Component line2 = Component.literal("Type: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(state.currentStageType()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" | State: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(state.currentStageState()).withStyle(stateColor))
                    .append(Component.literal(" | Lives: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(livesStr).withStyle(state.infiniteLives() ? ChatFormatting.GREEN : ChatFormatting.RED));
            guiGraphics.drawCenteredString(this.font, line2, midX, 38, 0xFFFFFF);

            String secretsMode = state.spawnAllSecretsCurrent() ? "All Secrets" : "Normal (3)";
            String line3 = "Active Respawn: §b" + respawnStr + " §7| Secrets Mode: §d" + secretsMode;
            guiGraphics.drawCenteredString(this.font, Component.literal(line3).withStyle(ChatFormatting.GRAY), midX, 50, 0xFFFFFF);

            String line4 = "Secrets Status: " + state.secretsStatusSummary();
            guiGraphics.drawCenteredString(this.font, Component.literal(line4).withStyle(ChatFormatting.GRAY), midX, 62, 0xFFFFFF);
        } else {
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("Not currently inside an active dungeon instance.").withStyle(ChatFormatting.RED),
                    midX, 34, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("Enter a dungeon to use stage controls, or configure next-run settings below.").withStyle(ChatFormatting.GRAY),
                    midX, 48, 0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

