package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonActionPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.UUID;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Screen 3: Party Sanctuary Board — 4-slot party roster with leader/status badges,
 * Tab-autocompleting invite field, live countdown/cooldown bar, and party actions.
 */
@OnlyIn(Dist.CLIENT)
public class DungeonKeeperScreen extends Screen {

    private static final int SLOT_HEIGHT = 22;
    private static final int SLOT_GAP = 4;
    private static final int MAX_PARTY_SIZE = 4;
    private static final int DEFAULT_PHASE_TICKS = 200; // 10s start countdown & 10s cancel cooldown
    private static final float MAX_CLIENT_LOOKAHEAD_TICKS = 8.0F; // never outrun server by more than 0.4s

    private OpenDungeonScreenPayload data;
    private final boolean isLeader;

    // Monotonic phase & countdown/cooldown tracking so server lag never causes the timer or bar to flip backward
    private int activePhase; // 0 = lobby, 1 = starting, 2 = cooldown
    private int totalCountdownTicks;
    private int serverRemainingTicks;
    private float displayedRemainingTicks;
    private int displayedSeconds;
    private long lastRenderMillis;

    private EditBox inviteBox;
    private String currentSuggestion = "";

    private int inviteBoxX;
    private int inviteBoxY;
    private int inviteBoxW;

    public DungeonKeeperScreen(OpenDungeonScreenPayload data) {
        super(Component.literal(data.dungeonDisplayName()));
        this.data = data;
        Minecraft mc = Minecraft.getInstance();
        UUID myId = mc.player != null ? mc.player.getUUID() : UUID.randomUUID();
        this.isLeader = data.leaderId().equals(myId);
        this.lastRenderMillis = Util.getMillis();
        resetPhaseTracking(data);
    }

    public boolean isSameParty(UUID partyId) {
        return this.data != null && this.data.partyId().equals(partyId);
    }

    public void updateFromPayload(OpenDungeonScreenPayload newData) {
        int prevPhase = this.activePhase;
        int newPhase = computePhase(newData);
        boolean membersChanged = !this.data.memberIds().equals(newData.memberIds())
                || !this.data.memberStatuses().equals(newData.memberStatuses());

        String savedInviteText = (this.inviteBox != null) ? this.inviteBox.getValue() : "";
        this.data = newData;

        if (newPhase != prevPhase) {
            resetPhaseTracking(newData);
            this.rebuildWidgets();
            if (this.inviteBox != null && !savedInviteText.isEmpty()) {
                this.inviteBox.setValue(savedInviteText);
            }
        } else {
            int incomingRemaining = Math.abs(newData.startTicksRemaining());
            // Keep remaining ticks strictly non-increasing within the same phase
            this.serverRemainingTicks = Math.min(this.serverRemainingTicks, incomingRemaining);
            if (membersChanged) {
                this.rebuildWidgets();
                if (this.inviteBox != null && !savedInviteText.isEmpty()) {
                    this.inviteBox.setValue(savedInviteText);
                }
            }
        }
    }

    private static int computePhase(OpenDungeonScreenPayload payload) {
        if (payload.isStarting()) return 1;
        if (payload.startTicksRemaining() < 0) return 2;
        return 0;
    }

    private void resetPhaseTracking(OpenDungeonScreenPayload payload) {
        this.activePhase = computePhase(payload);
        int rem = Math.abs(payload.startTicksRemaining());
        this.totalCountdownTicks = Math.max(DEFAULT_PHASE_TICKS, rem);
        this.serverRemainingTicks = rem;
        this.displayedRemainingTicks = rem;
        this.displayedSeconds = Math.max(0, (int) Math.ceil(rem / 20.0));
        this.lastRenderMillis = Util.getMillis();
    }

    private int getPanelW() { return Math.min(348, this.width - 20); }
    private int getPanelH() { return Math.min(246, this.height - 20); }

    private int getPanelLeft() { return (this.width - getPanelW()) / 2; }
    private int getPanelTop()  { return (this.height - getPanelH()) / 2; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        int left = getPanelLeft();
        int top  = getPanelTop();
        int pw   = getPanelW();
        int ph   = getPanelH();

        int statusZoneY = top + 168;
        int buttonY = top + ph - 30;

        // ── Invite Zone (leader only when neither starting nor on cooldown) ──
        if (isLeader && !data.isStarting() && data.startTicksRemaining() >= 0) {
            inviteBoxX = left + 62;
            inviteBoxY = statusZoneY;
            inviteBoxW = pw - 134;

            inviteBox = new EditBox(font, inviteBoxX + 4, inviteBoxY + 5, inviteBoxW - 8, 12,
                    Component.translatable("screen.oririmod.dungeon.invite_placeholder"));
            inviteBox.setBordered(false);
            inviteBox.setMaxLength(40);
            inviteBox.setTextColor(TEXT_PRIMARY);
            inviteBox.setTextColorUneditable(TEXT_MUTED);
            inviteBox.setHint(Component.translatable("screen.oririmod.dungeon.invite_placeholder")
                    .withStyle(ChatFormatting.DARK_GRAY));
            inviteBox.setResponder(text -> {
                currentSuggestion = "";
                inviteBox.setSuggestion("");
                if (!text.isEmpty()) {
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        String lowerText = text.toLowerCase(Locale.ROOT);
                        for (PlayerInfo info : connection.getOnlinePlayers()) {
                            String name = info.getProfile().getName();
                            if (name.toLowerCase(Locale.ROOT).startsWith(lowerText) && !name.equalsIgnoreCase(text)) {
                                currentSuggestion = name.substring(text.length());
                                inviteBox.setSuggestion(currentSuggestion);
                                break;
                            }
                        }
                    }
                }
            });
            addRenderableWidget(inviteBox);

            addRenderableWidget(new SanctuaryButton(
                    inviteBoxX + inviteBoxW + 6,
                    inviteBoxY,
                    54,
                    18,
                    Component.translatable("screen.oririmod.dungeon.invite"),
                    ButtonTone.PRIMARY,
                    this::invite
            ));
        } else {
            inviteBox = null;
        }

        // ── Footer Action Buttons ──
        if (data.isStarting()) {
            addRenderableWidget(new SanctuaryButton(
                    left + pw / 2 - 48,
                    buttonY,
                    96,
                    20,
                    Component.translatable("screen.oririmod.dungeon.cancel_start"),
                    ButtonTone.CRIMSON,
                    () -> {
                        sendAction("CANCEL_START", "");
                        onClose();
                    }
            ));
        } else {
            if (isLeader) {
                addRenderableWidget(new SanctuaryButton(
                        left + pw - 90,
                        buttonY,
                        78,
                        20,
                        Component.translatable("screen.oririmod.dungeon.start"),
                        ButtonTone.EMERALD,
                        () -> sendAction("START", "")
                ));
            }

            addRenderableWidget(new SanctuaryButton(
                    left + (isLeader ? pw - 176 : pw - 96),
                    buttonY,
                    isLeader ? 80 : 84,
                    20,
                    Component.translatable("screen.oririmod.dungeon.leave_party"),
                    ButtonTone.CRIMSON,
                    () -> {
                        sendAction("LEAVE", "");
                        onClose();
                    }
            ));
        }

        // Accept / Decline buttons for non-leader pending members
        UUID myId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
        if (!isLeader && myId != null) {
            boolean isPending = false;
            int myIndex = data.memberIds().indexOf(myId);
            if (myIndex >= 0 && myIndex < data.memberStatuses().size()) {
                isPending = "PENDING".equals(data.memberStatuses().get(myIndex));
            } else {
                isPending = data.memberStatuses().stream().anyMatch("PENDING"::equals);
            }
            if (isPending) {
                addRenderableWidget(new SanctuaryButton(
                        left + 12,
                        buttonY,
                        70,
                        20,
                        Component.translatable("screen.oririmod.dungeon.accept"),
                        ButtonTone.EMERALD,
                        () -> {
                            sendAction("ACCEPT", "");
                            onClose();
                        }
                ));
                addRenderableWidget(new SanctuaryButton(
                        left + 88,
                        buttonY,
                        70,
                        20,
                        Component.translatable("screen.oririmod.dungeon.decline"),
                        ButtonTone.CRIMSON,
                        () -> {
                            sendAction("DECLINE", "");
                            onClose();
                        }
                ));
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int left = getPanelLeft();
        int top  = getPanelTop();
        int pw   = getPanelW();
        int ph   = getPanelH();

        drawSanctuaryPanel(gfx, left, top, pw, ph);

        // Title Zone
        gfx.drawCenteredString(font,
                Component.literal("⚔ " + data.dungeonDisplayName() + " ⚔").withStyle(ChatFormatting.BOLD),
                left + pw / 2, top + 12, TEXT_TITLE);
        drawGildedDivider(gfx, left + 12, top + 29, pw - 24, GOLD);

        // Party Header & Size Pill
        int partyY = top + 37;
        gfx.drawString(font,
                Component.translatable("screen.oririmod.dungeon.party_header"),
                left + 14, partyY + 2, GOLD_BRIGHT, true);

        int totalMembers = 1 + data.memberIds().size();
        String sizePill = totalMembers + " / " + MAX_PARTY_SIZE;
        int pillW = font.width(sizePill) + 10;
        drawPill(gfx, font, sizePill, left + pw - pillW - 14, partyY, 0xCC1A1526, BRONZE, GOLD);

        // 4 Party Slot Cards
        int slotX = left + 14;
        int slotW = pw - 28;
        int slotY = partyY + 17;

        renderLeaderSlot(gfx, slotX, slotY, slotW, SLOT_HEIGHT);
        slotY += SLOT_HEIGHT + SLOT_GAP;

        int memberCount = Math.min(3, data.memberIds().size());
        for (int i = 0; i < memberCount; i++) {
            String name = data.memberNames().get(i);
            String status = data.memberStatuses().get(i);
            renderMemberSlot(gfx, slotX, slotY, slotW, SLOT_HEIGHT, name, status);
            slotY += SLOT_HEIGHT + SLOT_GAP;
        }

        for (int i = memberCount; i < 3; i++) {
            renderEmptySlot(gfx, slotX, slotY, slotW, SLOT_HEIGHT);
            slotY += SLOT_HEIGHT + SLOT_GAP;
        }

        // Divider above Invite / Status Zone
        drawGildedDivider(gfx, left + 12, top + 160, pw - 24, GOLD_DIM);

        // Invite / Countdown / Cooldown Zone
        int statusZoneY = top + 168;
        if (data.isStarting()) {
            renderCountdownBar(gfx, left + 14, statusZoneY, pw - 28, true);
        } else if (data.startTicksRemaining() < 0) {
            renderCountdownBar(gfx, left + 14, statusZoneY, pw - 28, false);
        } else if (isLeader && inviteBox != null) {
            gfx.drawString(font,
                    Component.translatable("screen.oririmod.dungeon.invite_label"),
                    left + 14, statusZoneY + 5, TEXT_PRIMARY, false);
            int boxBorder = inviteBox.isFocused() ? GOLD : BRONZE;
            drawRecessedWell(gfx, inviteBoxX, inviteBoxY, inviteBoxW, 18, boxBorder);
        } else {
            gfx.drawCenteredString(font,
                    Component.translatable("screen.oririmod.dungeon.waiting_leader"),
                    left + pw / 2, statusZoneY + 5, TEXT_MUTED);
        }

        // Footer Divider
        drawGildedDivider(gfx, left + 12, top + ph - 36, pw - 24, GOLD_DIM);
    }

    private void renderLeaderSlot(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fillGradient(x, y, x + w, y + h, 0xE6261E14, 0xE618130E);
        drawBorder(gfx, x, y, w, h, GOLD_DIM);
        gfx.fill(x + 1, y + 1, x + 4, y + h - 1, GOLD);

        // Crown diamond pip
        drawDiamond(gfx, x + 13, y + h / 2, 3, GOLD_BRIGHT);

        String leaderName = getLeaderName();
        String badge = Component.translatable("screen.oririmod.dungeon.role_leader").getString();
        int badgeW = font.width(badge) + 10;
        String clipped = truncate(font, leaderName, w - badgeW - 32);
        gfx.drawString(font, clipped, x + 23, y + (h - 8) / 2, GOLD_BRIGHT, true);

        drawPill(gfx, font, badge, x + w - badgeW - 6, y + (h - 13) / 2, 0xCC3B2B14, GOLD, GOLD_BRIGHT);
    }

    private void renderMemberSlot(GuiGraphics gfx, int x, int y, int w, int h, String name, String status) {
        gfx.fillGradient(x, y, x + w, y + h, 0xDD171526, 0xDD100F1C);
        drawBorder(gfx, x, y, w, h, BRONZE);

        int stripeColor;
        int pillBg;
        int pillBorder;
        int pillText;
        switch (status) {
            case "ACCEPTED" -> {
                stripeColor = EMERALD;
                pillBg = EMERALD_DARK;
                pillBorder = EMERALD;
                pillText = EMERALD_LIGHT;
            }
            case "PENDING" -> {
                float pulse = 0.72F + 0.28F * Mth.sin((System.currentTimeMillis() % 2000L) / 2000.0F * Mth.TWO_PI);
                stripeColor = withAlpha(AMBER, (int) (pulse * 255));
                pillBg = AMBER_DARK;
                pillBorder = AMBER;
                pillText = AMBER_LIGHT;
            }
            default -> {
                stripeColor = CRIMSON;
                pillBg = CRIMSON_DARK;
                pillBorder = CRIMSON;
                pillText = CRIMSON_LIGHT;
            }
        }

        gfx.fill(x + 1, y + 1, x + 4, y + h - 1, stripeColor);
        drawDiamond(gfx, x + 13, y + h / 2, 2, stripeColor);

        int badgeW = font.width(status) + 10;
        String clipped = truncate(font, name, w - badgeW - 32);
        gfx.drawString(font, clipped, x + 23, y + (h - 8) / 2, TEXT_PRIMARY, false);

        drawPill(gfx, font, status, x + w - badgeW - 6, y + (h - 13) / 2, pillBg, pillBorder, pillText);
    }

    private void renderEmptySlot(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0x990A0912);
        drawBorder(gfx, x, y, w, h, 0xFF28222C);
        drawDiamond(gfx, x + 13, y + h / 2, 2, 0xFF3E3845);
        gfx.drawString(font,
                Component.translatable("screen.oririmod.dungeon.empty_slot"),
                x + 23, y + (h - 8) / 2, TEXT_MUTED, false);
    }

    private void renderCountdownBar(GuiGraphics gfx, int x, int y, int w, boolean starting) {
        long nowMillis = Util.getMillis();
        float dtSec = Mth.clamp((nowMillis - this.lastRenderMillis) / 1000.0F, 0.0F, 0.1F);
        this.lastRenderMillis = nowMillis;

        // Smoothly glide displayedRemainingTicks strictly downward:
        // - Catch up smoothly if behind the latest server sync
        // - Otherwise drift at most MAX_CLIENT_LOOKAHEAD_TICKS ahead of serverRemainingTicks,
        //   decelerating smoothly during structure-loading lag spikes instead of overshooting and flipping back.
        float minAllowed = Math.max(0.0F, this.serverRemainingTicks - MAX_CLIENT_LOOKAHEAD_TICKS);
        if (this.displayedRemainingTicks > this.serverRemainingTicks) {
            float lagBehind = this.displayedRemainingTicks - this.serverRemainingTicks;
            float speed = 20.0F + lagBehind * 4.0F;
            this.displayedRemainingTicks = Math.max(minAllowed, this.displayedRemainingTicks - speed * dtSec);
        } else if (this.displayedRemainingTicks > minAllowed) {
            float headroom = (this.displayedRemainingTicks - minAllowed) / MAX_CLIENT_LOOKAHEAD_TICKS;
            float speed = 20.0F * headroom;
            this.displayedRemainingTicks = Math.max(minAllowed, this.displayedRemainingTicks - speed * dtSec);
        }

        // Strict monotonic seconds readout so the displayed number NEVER jumps upward
        int candidateSeconds = Math.max(0, (int) Math.ceil(this.displayedRemainingTicks / 20.0D));
        this.displayedSeconds = Math.min(this.displayedSeconds, candidateSeconds);

        float progress = Mth.clamp(this.displayedRemainingTicks / (float) Math.max(1, this.totalCountdownTicks), 0.0F, 1.0F);

        int barColor = starting ? AMBER : CRIMSON;
        int barBright = starting ? AMBER_LIGHT : CRIMSON_LIGHT;
        int barBg = starting ? AMBER_DARK : CRIMSON_DARK;

        String label = starting
                ? "Starting in " + this.displayedSeconds + "s..."
                : "Cooldown: " + this.displayedSeconds + "s...";

        drawRecessedWell(gfx, x, y, w, 18, barColor);
        int innerW = Math.round((w - 4) * progress);
        if (innerW > 0) {
            gfx.fillGradient(x + 2, y + 2, x + 2 + innerW, y + 16, withAlpha(barColor, 0xAA), withAlpha(barBg, 0xDD));
            gfx.hLine(x + 2, x + 1 + innerW, y + 2, withAlpha(barBright, 0xCC));
        }
        gfx.drawCenteredString(font, label, x + w / 2, y + 5, barBright);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inviteBox != null && inviteBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_TAB && !currentSuggestion.isEmpty()) {
                inviteBox.setValue(inviteBox.getValue() + currentSuggestion);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                invite();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getLeaderName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(data.leaderId())) {
            return mc.player.getName().getString();
        }
        var connection = mc.getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(data.leaderId());
            if (info != null) {
                return info.getProfile().getName();
            }
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
}
