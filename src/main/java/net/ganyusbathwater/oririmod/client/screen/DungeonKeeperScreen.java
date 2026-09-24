package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Screen 3: Party Management screen.
 *
 * Strict zone layout (320 × 240 panel):
 * ┌──────────────────────────────────────────────┐
 * │  ⚔ [Dungeon Name]                            │  ← Title Zone (y+0 to y+30)
 * │──────────────────────────────────────────────│
 * │  Party                                       │  ← Party Zone (y+35 to y+120)
 * │   ● Leader Name         (Leader)             │     Fixed 4 rows × 14px = 56px
 * │   ● Member 1            ACCEPTED             │
 * │   ● (empty slot)                             │
 * │   ● (empty slot)                             │
 * │──────────────────────────────────────────────│
 * │  Invite: [______________] [Invite]           │  ← Invite Zone (y+125 to y+155, leader only)
 * │──────────────────────────────────────────────│
 * │  [Accept] [Decline]   [Leave] [Start ▶]     │  ← Button Zone (y+160 to y+190)
 * └──────────────────────────────────────────────┘
 */
@OnlyIn(Dist.CLIENT)
public class DungeonKeeperScreen extends Screen {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 200;

    // Zone Y offsets (relative to panel top)
    private static final int TITLE_Y   = 10;
    private static final int DIV1_Y    = 30;
    private static final int PARTY_Y   = 38;
    private static final int PARTY_ROW = 14;
    private static final int DIV2_Y    = 110;
    private static final int INVITE_Y  = 118;
    private static final int DIV3_Y    = 145;
    private static final int BUTTON_Y  = 155;

    // Colors
    private static final int C_BG      = 0xDD0A0A14;
    private static final int C_BORDER  = 0xFF6A4A2A;
    private static final int C_TITLE   = 0xFFFFD700;
    private static final int C_DIVIDER = 0xFF4A3A2A;
    private static final int C_LABEL   = 0xFFEEDDAA;
    private static final int C_ACCEPT  = 0xFF55FF55;
    private static final int C_PENDING = 0xFFFFAA00;
    private static final int C_DECLINE = 0xFFFF5555;
    private static final int C_MUTED   = 0xFF777766;

    private final OpenDungeonScreenPayload data;
    private final boolean isLeader;
    private EditBox inviteBox;
    private long openClientTick;
    private String currentSuggestion = "";

    public DungeonKeeperScreen(OpenDungeonScreenPayload data) {
        super(Component.literal(data.dungeonDisplayName()));
        this.data = data;
        UUID myId = net.minecraft.client.Minecraft.getInstance().player != null
                ? net.minecraft.client.Minecraft.getInstance().player.getUUID()
                : UUID.randomUUID();
        this.isLeader = data.leaderId().equals(myId);
        this.openClientTick = net.minecraft.client.Minecraft.getInstance().level != null 
                ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : 0;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        // ── Invite Zone (leader only) ──
        if (isLeader && !data.isStarting() && data.startTicksRemaining() >= 0) {
            inviteBox = new EditBox(font, left + 65, top + INVITE_Y, 140, 18,
                    Component.literal("Player name"));
            inviteBox.setMaxLength(40);
            inviteBox.setHint(Component.literal("Player name").withStyle(ChatFormatting.DARK_GRAY));
            inviteBox.setResponder(text -> {
                currentSuggestion = "";
                inviteBox.setSuggestion("");
                if (!text.isEmpty()) {
                    var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        String lowerText = text.toLowerCase(java.util.Locale.ROOT);
                        for (net.minecraft.client.multiplayer.PlayerInfo info : connection.getOnlinePlayers()) {
                            String name = info.getProfile().getName();
                            if (name.toLowerCase(java.util.Locale.ROOT).startsWith(lowerText) && !name.equalsIgnoreCase(text)) {
                                currentSuggestion = name.substring(text.length());
                                inviteBox.setSuggestion(currentSuggestion);
                                break;
                            }
                        }
                    }
                }
            });
            addRenderableWidget(inviteBox);

            addRenderableWidget(Button.builder(Component.literal("Invite"), btn -> invite())
                    .pos(left + 210, top + INVITE_Y)
                    .size(50, 18)
                    .build());
        }

        // ── Button Zone ──
        int btnY = top + BUTTON_Y;

        if (data.isStarting()) {
            // When starting, anyone can cancel
            addRenderableWidget(Button.builder(
                    Component.literal("Cancel Start").withStyle(ChatFormatting.RED),
                    btn -> { sendAction("CANCEL_START", ""); onClose(); })
                    .pos(left + PANEL_W / 2 - 40, btnY)
                    .size(80, 20)
                    .build());
        } else {
            if (isLeader) {
                addRenderableWidget(Button.builder(
                        Component.literal("Start ▶").withStyle(ChatFormatting.GREEN),
                        btn -> sendAction("START", ""))
                        .pos(left + PANEL_W - 80, btnY)
                        .size(72, 20)
                        .build());
            }

            addRenderableWidget(Button.builder(
                    Component.literal("Leave Party").withStyle(ChatFormatting.RED),
                    btn -> { sendAction("LEAVE", ""); onClose(); })
                    .pos(left + (isLeader ? PANEL_W - 160 : PANEL_W - 90), btnY)
                    .size(isLeader ? 74 : 82, 20)
                    .build());
        }

        // Accept / Decline buttons for non-leader pending members
        UUID myId = net.minecraft.client.Minecraft.getInstance().player != null
                ? net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        if (!isLeader && myId != null) {
            boolean isPending = data.memberStatuses().stream()
                    .anyMatch(s -> s.equals("PENDING"));
            if (isPending) {
                addRenderableWidget(Button.builder(
                        Component.literal("Accept").withStyle(ChatFormatting.GREEN),
                        btn -> { sendAction("ACCEPT", ""); onClose(); })
                        .pos(left + 10, btnY)
                        .size(70, 20)
                        .build());
                addRenderableWidget(Button.builder(
                        Component.literal("Decline").withStyle(ChatFormatting.RED),
                        btn -> { sendAction("DECLINE", ""); onClose(); })
                        .pos(left + 86, btnY)
                        .size(70, 20)
                        .build());
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx, mouseX, mouseY, partial);

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        // Background
        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, C_BG);
        drawBorder(gfx, left, top, PANEL_W, PANEL_H, C_BORDER);

        // Render widgets on top
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partial);
        }

        // ── Title Zone ──
        gfx.drawCenteredString(font,
                Component.literal("⚔  " + data.dungeonDisplayName()).withStyle(ChatFormatting.BOLD),
                left + PANEL_W / 2, top + TITLE_Y, C_TITLE);
        gfx.hLine(left + 8, left + PANEL_W - 8, top + DIV1_Y, C_DIVIDER);

        // ── Party Zone ──
        gfx.drawString(font, "Party", left + 12, top + PARTY_Y, C_LABEL, true);

        int rowY = top + PARTY_Y + 16;
        // Leader row
        gfx.drawString(font, "● " + getLeaderName() + "  (Leader)", left + 16, rowY, C_ACCEPT, true);
        rowY += PARTY_ROW;

        // Member rows
        for (int i = 0; i < data.memberIds().size(); i++) {
            String name = data.memberNames().get(i);
            String status = data.memberStatuses().get(i);
            int color = switch (status) {
                case "ACCEPTED" -> C_ACCEPT;
                case "PENDING"  -> C_PENDING;
                default         -> C_DECLINE;
            };
            gfx.drawString(font, "● " + name, left + 16, rowY, C_MUTED, true);
            gfx.drawString(font, status, left + 200, rowY, color, true);
            rowY += PARTY_ROW;
        }

        // Empty slots
        int emptySlots = 3 - data.memberIds().size();
        for (int i = 0; i < emptySlots; i++) {
            gfx.drawString(font, "● (empty slot)", left + 16, rowY, C_MUTED, true);
            rowY += PARTY_ROW;
        }

        // ── Dividers ──
        gfx.hLine(left + 8, left + PANEL_W - 8, top + DIV2_Y, C_DIVIDER);

        if (data.isStarting()) {
            long currentTick = net.minecraft.client.Minecraft.getInstance().level != null 
                    ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : openClientTick;
            long passedTicks = currentTick - openClientTick;
            int ticksLeft = Math.max(0, data.startTicksRemaining() - (int) passedTicks);
            int secondsLeft = (int) Math.ceil(ticksLeft / 20.0);
            gfx.drawString(font, "Starting in " + secondsLeft + "s...", left + 12, top + INVITE_Y + 4, C_PENDING, true);
        } else if (data.startTicksRemaining() < 0) {
            long currentTick = net.minecraft.client.Minecraft.getInstance().level != null 
                    ? net.minecraft.client.Minecraft.getInstance().level.getGameTime() : openClientTick;
            long passedTicks = currentTick - openClientTick;
            int ticksLeft = Math.min(0, data.startTicksRemaining() + (int) passedTicks);
            int secondsLeft = Math.abs(ticksLeft) / 20;
            gfx.drawString(font, "Cooldown: " + secondsLeft + "s...", left + 12, top + INVITE_Y + 4, C_DECLINE, true);
        } else if (isLeader) {
            gfx.drawString(font, "Invite:", left + 12, top + INVITE_Y + 4, C_LABEL, true);
        }

        gfx.hLine(left + 8, left + PANEL_W - 8, top + DIV3_Y, C_DIVIDER);
    }

    // ── Helpers ──

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inviteBox != null && inviteBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB && !currentSuggestion.isEmpty()) {
                inviteBox.setValue(inviteBox.getValue() + currentSuggestion);
                return true;
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                invite();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getLeaderName() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.getUUID().equals(data.leaderId())) {
            return player.getName().getString();
        }
        return "Party Leader";
    }

    private void invite() {
        if (inviteBox == null) return;
        String name = inviteBox.getValue().trim();
        if (!name.isBlank()) {
            sendAction("INVITE", name);
            inviteBox.setValue("");
        }
    }

    private void sendAction(String action, String targetName) {
        PacketDistributor.sendToServer(new DungeonActionPayload(action, data.partyId(), targetName));
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.hLine(x, x + w - 1, y,         color);
        gfx.hLine(x, x + w - 1, y + h - 1, color);
        gfx.vLine(x,         y, y + h - 1, color);
        gfx.vLine(x + w - 1, y, y + h - 1, color);
    }
}
