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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Dungeon Keeper NPC screen.
 * Shows dungeon info and a party management panel (leader can invite/kick, start the run).
 *
 * Layout (500 × 260 panel):
 * ┌──────────────────────────────────────────────────────┐
 * │  ⚔  [Dungeon Name]                                   │
 * │  [Description line 1]                                │
 * │  [Description line 2]                                │
 * │─────────────────────────────────────────────────────│
 * │  Party                                               │
 * │   ● [Leader name]          (you)                     │
 * │   ● [Member 1]             Accepted / Pending / --   │
 * │   ● [Member 2]             ...                       │
 * │   ● [Member 3]             ...                       │
 * │─────────────────────────────────────────────────────│
 * │  Invite: [_____________] [Invite]                    │
 * │                           [Leave Party]  [Start ▶]   │
 * └──────────────────────────────────────────────────────┘
 */
@OnlyIn(Dist.CLIENT)
public class DungeonKeeperScreen extends Screen {

    // ── Dimensions ───────────────────────────────────────────────────────────
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 240;

    // ── Colors ───────────────────────────────────────────────────────────────
    private static final int C_BG         = 0xDD0A0A14; // very dark navy
    private static final int C_BORDER     = 0xFF6A4A2A; // warm gold-brown
    private static final int C_TITLE      = 0xFFFFD700; // gold
    private static final int C_DESC       = 0xFFCCBB99; // parchment
    private static final int C_DIVIDER    = 0xFF4A3A2A;
    private static final int C_LABEL      = 0xFFEEDDAA;
    private static final int C_ACCEPT     = 0xFF55FF55;
    private static final int C_PENDING    = 0xFFFFAA00;
    private static final int C_DECLINE    = 0xFFFF5555;
    private static final int C_MUTED      = 0xFF777766;

    // ── Screen data ──────────────────────────────────────────────────────────
    private final OpenDungeonScreenPayload data;
    private final boolean isLeader;

    // ── Widgets ──────────────────────────────────────────────────────────────
    private EditBox inviteBox;

    public DungeonKeeperScreen(OpenDungeonScreenPayload data) {
        super(Component.literal(data.dungeonDisplayName()));
        this.data = data;
        UUID myId = net.minecraft.client.Minecraft.getInstance().player != null
                ? net.minecraft.client.Minecraft.getInstance().player.getUUID()
                : UUID.randomUUID();
        this.isLeader = data.leaderId().equals(myId);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = (width - PANEL_W) / 2;
        int top  = (height - PANEL_H) / 2;

        // ── Invite box (leader only) ─────────────────────────────────────────
        int inviteY = top + PANEL_H - 55;
        if (isLeader) {
            inviteBox = new EditBox(font, left + 12, inviteY, 150, 18,
                    Component.literal("Player name"));
            inviteBox.setMaxLength(40);
            inviteBox.setHint(Component.literal("Player name").withStyle(ChatFormatting.DARK_GRAY));
            addRenderableWidget(inviteBox);

            addRenderableWidget(Button.builder(Component.literal("Invite"), btn -> invite())
                    .pos(left + 168, inviteY)
                    .size(50, 18)
                    .build());
        }

        // ── Start / Leave buttons ────────────────────────────────────────────
        int bottomY = top + PANEL_H - 28;
        if (isLeader) {
            addRenderableWidget(Button.builder(
                    Component.literal("Start ▶").withStyle(ChatFormatting.GREEN),
                    btn -> sendAction("START", ""))
                    .pos(left + PANEL_W - 80, bottomY)
                    .size(72, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(
                Component.literal("Leave Party").withStyle(ChatFormatting.RED),
                btn -> { sendAction("LEAVE", ""); onClose(); })
                .pos(left + (isLeader ? PANEL_W - 160 : PANEL_W - 90), bottomY)
                .size(isLeader ? 74 : 82, 20)
                .build());

        // ── Accept / Decline (for non-leader invited members) ────────────────
        UUID myId = net.minecraft.client.Minecraft.getInstance().player != null
                ? net.minecraft.client.Minecraft.getInstance().player.getUUID() : null;
        if (!isLeader && myId != null) {
            boolean isPending = data.memberStatuses().stream()
                    .anyMatch(s -> s.equals("PENDING"));
            if (isPending) {
                addRenderableWidget(Button.builder(
                        Component.literal("Accept").withStyle(ChatFormatting.GREEN),
                        btn -> { sendAction("ACCEPT", ""); onClose(); })
                        .pos(left + 10, bottomY)
                        .size(70, 20)
                        .build());
                addRenderableWidget(Button.builder(
                        Component.literal("Decline").withStyle(ChatFormatting.RED),
                        btn -> { sendAction("DECLINE", ""); onClose(); })
                        .pos(left + 86, bottomY)
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

        // Border
        drawBorder(gfx, left, top, PANEL_W, PANEL_H, C_BORDER);

        // Title row
        int titleY = top + 10;
        gfx.drawCenteredString(font,
                Component.literal("⚔  " + data.dungeonDisplayName()).withStyle(ChatFormatting.BOLD),
                left + PANEL_W / 2, titleY, C_TITLE);

        // Description
        int descY = titleY + 16;
        String desc = data.dungeonDescription();
        if (!desc.isBlank()) {
            List<String> lines = wrapText(desc, PANEL_W - 24);
            for (String line : lines) {
                gfx.drawString(font, line, left + 12, descY, C_DESC, false);
                descY += 10;
            }
        }

        // Divider
        int divY = top + 55;
        gfx.hLine(left + 8, left + PANEL_W - 8, divY, C_DIVIDER);

        // Party header
        gfx.drawString(font, "Party", left + 12, divY + 6, C_LABEL, false);

        // Leader row
        int rowY = divY + 20;
        gfx.drawString(font, "● " + getLeaderName() + "  (Leader)", left + 16, rowY, C_ACCEPT, false);
        rowY += 14;

        // Member rows
        for (int i = 0; i < data.memberIds().size(); i++) {
            String name = data.memberNames().get(i);
            String status = data.memberStatuses().get(i);
            int color = switch (status) {
                case "ACCEPTED" -> C_ACCEPT;
                case "PENDING"  -> C_PENDING;
                default         -> C_DECLINE;
            };
            String label = "● " + name;
            gfx.drawString(font, label, left + 16, rowY, C_MUTED, false);
            gfx.drawString(font, status, left + 200, rowY, color, false);
            rowY += 14;
        }

        // Empty slots
        int emptySlots = 3 - data.memberIds().size();
        for (int i = 0; i < emptySlots; i++) {
            gfx.drawString(font, "● (empty slot)", left + 16, rowY, C_MUTED, false);
            rowY += 14;
        }

        // Divider above buttons
        int btmDivY = top + PANEL_H - 66;
        gfx.hLine(left + 8, left + PANEL_W - 8, btmDivY, C_DIVIDER);

        if (isLeader) {
            gfx.drawString(font, "Invite:", left + 12, top + PANEL_H - 50, C_LABEL, false);
        }

        super.render(gfx, mouseX, mouseY, partial);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getLeaderName() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.getUUID().equals(data.leaderId())) {
            return player.getName().getString();
        }
        // Could be cached from a prior packet; fall back to "Party Leader"
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

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (font.width(test) > maxWidth) {
                if (!current.isEmpty()) lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }
}
