package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.dialogue.*;
import net.ganyusbathwater.oririmod.network.packet.ConversationActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Cinematic in-world NPC conversation screen (Hollow Knight / Elden Ring style):
 * - No full-screen blur or static portrait — the live world and NPC remain crisp
 *   with smooth letterbox framing and gentle FOV zoom (via ConversationHudHandler).
 * - Ornate obsidian-and-gold speech box with an elevated speaker nameplate banner.
 * - Retains the last spoken NPC line while choosing responses for full context.
 * - Custom RPG choice banners with hover glow and 1–9 keyboard quick-select.
 */
@OnlyIn(Dist.CLIENT)
public class ConversationScreen extends Screen {

    // ── Layout Constants ──
    private static final int LETTERBOX_HEIGHT = 24;
    private static final int SPEECH_BOX_HEIGHT = 86;
    private static final int SPEECH_BOX_BOTTOM_GAP = 10;
    private static final int SPEECH_BOX_MAX_WIDTH = 480;
    private static final int CHOICE_BTN_WIDTH = 246;
    private static final int CHOICE_BTN_HEIGHT = 26;
    private static final int CHOICE_BTN_GAP = 6;

    // ── Typewriter & Transitions ──
    private static final float CHARS_PER_SECOND = 30.0F;
    private static final int BLIP_INTERVAL = 3;
    private static final int FADE_IN_DURATION = 9; // ticks

    // ── Validation ──
    private static final double MAX_DISTANCE = 10.0;

    // ── State ──
    private final DialogueTree tree;
    private final int entityId;
    @Nullable
    private DialogueNode currentNode;

    private String fullText = "";
    private String lastSpeakerKey = null;
    private String lastSpokenText = "";
    private int revealedChars = 0;
    private float charAccumulator = 0.0F;
    private boolean textFullyRevealed = false;
    private int fadeInTicks = 0;
    private int lastBlipChar = -1;

    private final List<DialogueChoice> activeChoices = new ArrayList<>();

    @Nullable
    private ResourceLocation talkSoundLoc;

    /** Tracks whether HUD should be hidden and FOV focused. Checked by ConversationHudHandler. */
    private static boolean conversationActive = false;

    public ConversationScreen(DialogueTree tree, int entityId) {
        super(Component.empty());
        this.tree = tree;
        this.entityId = entityId;
    }

    public static boolean isConversationActive() {
        return conversationActive;
    }

    private int getSpeechBoxW() { return Math.min(SPEECH_BOX_MAX_WIDTH, this.width - 48); }
    private int getSpeechBoxX() { return (this.width - getSpeechBoxW()) / 2; }
    private int getSpeechBoxY() { return this.height - LETTERBOX_HEIGHT - SPEECH_BOX_BOTTOM_GAP - SPEECH_BOX_HEIGHT; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        conversationActive = true;

        if (tree.getTalkSound() != null && !tree.getTalkSound().isEmpty()) {
            String[] parts = tree.getTalkSound().split(":");
            if (parts.length == 2) {
                talkSoundLoc = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            }
        }

        if (currentNode == null) {
            setNode(tree.getStartNode());
        } else if (currentNode.type() == DialogueNodeType.PLAYER_CHOICE) {
            buildChoiceButtons(currentNode.choices());
        }
    }

    @Override
    public void removed() {
        super.removed();
        conversationActive = false;
    }

    @Override
    public void onClose() {
        conversationActive = false;
        super.onClose();
    }

    // ── Node Navigation ──

    private void setNode(@Nullable DialogueNode node) {
        this.clearWidgets();
        this.activeChoices.clear();
        this.currentNode = node;
        this.charAccumulator = 0.0F;
        this.lastBlipChar = -1;
        this.textFullyRevealed = false;

        if (node == null) {
            onClose();
            return;
        }

        if (node.speaker() != null && !node.speaker().isBlank()) {
            this.lastSpeakerKey = node.speaker();
        }

        switch (node.type()) {
            case NPC_LINE -> {
                this.fullText = node.text() != null
                        ? Component.translatable(node.text()).getString()
                        : "";
                this.lastSpokenText = this.fullText;
                this.revealedChars = 0;
            }
            case PLAYER_CHOICE -> {
                this.fullText = this.lastSpokenText;
                this.revealedChars = this.fullText.length();
                this.textFullyRevealed = true;
                if (node.choices() != null) {
                    this.activeChoices.addAll(node.choices());
                    buildChoiceButtons(node.choices());
                }
            }
            case ACTION -> executeAction(node.action());
        }
    }

    private void buildChoiceButtons(List<DialogueChoice> choices) {
        if (choices == null || choices.isEmpty()) return;

        int boxX = getSpeechBoxX();
        int boxW = getSpeechBoxW();
        int boxY = getSpeechBoxY();

        int btnW = Math.min(CHOICE_BTN_WIDTH, Math.max(180, boxW - 24));
        int btnX = boxX + boxW - btnW;
        int totalH = choices.size() * CHOICE_BTN_HEIGHT + (choices.size() - 1) * CHOICE_BTN_GAP;
        // Anchor neatly above the right edge of the speech box, staying clear of the top letterbox
        int startY = Math.max(LETTERBOX_HEIGHT + 24, boxY - totalH - 18);

        for (int i = 0; i < choices.size(); i++) {
            final DialogueChoice choice = choices.get(i);
            final int hotkeyIndex = i + 1;
            int btnY = startY + i * (CHOICE_BTN_HEIGHT + CHOICE_BTN_GAP);
            Component label = Component.translatable(choice.text());

            addRenderableWidget(new ChoiceBannerButton(
                    btnX,
                    btnY,
                    btnW,
                    CHOICE_BTN_HEIGHT,
                    hotkeyIndex,
                    label,
                    () -> selectChoice(choice)
            ));
        }
    }

    private void selectChoice(DialogueChoice choice) {
        DialogueNode next = tree.getNode(choice.nextNodeId());
        setNode(next);
    }

    private void executeAction(@Nullable String action) {
        if (action == null || "CLOSE".equals(action)) {
            onClose();
            return;
        }
        PacketDistributor.sendToServer(new ConversationActionPayload(entityId, action));
        onClose();
    }

    // ── Tick (Typewriter + Validation) ──

    @Override
    public void tick() {
        super.tick();
        fadeInTicks++;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            onClose();
            return;
        }

        var entity = mc.level.getEntity(entityId);
        if (entity == null || !entity.isAlive()) {
            onClose();
            return;
        }

        if (mc.player.distanceTo(entity) > MAX_DISTANCE) {
            onClose();
            return;
        }

        if (mc.player.hurtTime > 0) {
            onClose();
            return;
        }

        // Typewriter animation
        if (currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE && !textFullyRevealed) {
            charAccumulator += CHARS_PER_SECOND / 20.0F;
            int newChars = (int) charAccumulator;
            if (newChars > 0) {
                charAccumulator -= newChars;
                revealedChars = Math.min(revealedChars + newChars, fullText.length());

                if (talkSoundLoc != null) {
                    int blipIndex = revealedChars / BLIP_INTERVAL;
                    if (blipIndex > lastBlipChar && revealedChars < fullText.length()) {
                        lastBlipChar = blipIndex;
                        mc.player.playSound(
                                SoundEvent.createVariableRangeEvent(talkSoundLoc),
                                0.3F,
                                0.9F + mc.player.getRandom().nextFloat() * 0.3F
                        );
                    }
                }

                if (revealedChars >= fullText.length()) {
                    textFullyRevealed = true;
                }
            }
        }
    }

    // ── Input ──

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE) {
            advanceNpcLine();
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if ((keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE) {
            advanceNpcLine();
            return true;
        }

        // Number-key shortcuts (1-9) for player choices
        if (currentNode != null && currentNode.type() == DialogueNodeType.PLAYER_CHOICE && !activeChoices.isEmpty()) {
            int idx = -1;
            if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
                idx = keyCode - GLFW.GLFW_KEY_1;
            } else if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_9) {
                idx = keyCode - GLFW.GLFW_KEY_KP_1;
            }
            if (idx >= 0 && idx < activeChoices.size()) {
                selectChoice(activeChoices.get(idx));
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void advanceNpcLine() {
        if (currentNode == null) return;
        if (!textFullyRevealed) {
            revealedChars = fullText.length();
            textFullyRevealed = true;
        } else if (currentNode.nextNodeId() != null) {
            setNode(tree.getNode(currentNode.nextNodeId()));
        } else {
            onClose();
        }
    }

    // ── Rendering (No full-screen blur, no portrait, no voice pulse) ──

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        float t = Mth.clamp((fadeInTicks + partialTick) / (float) FADE_IN_DURATION, 0.0F, 1.0F);
        // Ease-out cubic for crisp cinematic entry
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        int alpha = Math.max(4, Math.round(eased * 255.0F));

        // Soft bottom gradient scrim so dialogue & choices read effortlessly while leaving the NPC sharp
        int scrimTop = Math.max(LETTERBOX_HEIGHT, getSpeechBoxY() - 56);
        gfx.fillGradient(0, scrimTop, this.width, this.height, 0x0007060D, withAlpha(0x07060D, (int) (0.72F * alpha)));

        // Smooth sliding cinematic letterbox bars (top & bottom)
        int barH = Math.round(LETTERBOX_HEIGHT * eased);
        if (barH > 0) {
            int barColor = withAlpha(0x07060B, Math.min(242, alpha));
            int trimColor = withAlpha(GOLD_DIM, (int) (0.75F * alpha));
            // Top bar
            gfx.fill(0, 0, this.width, barH, barColor);
            gfx.hLine(0, this.width - 1, barH, trimColor);
            // Bottom bar
            gfx.fill(0, this.height - barH, this.width, this.height, barColor);
            gfx.hLine(0, this.width - 1, this.height - barH - 1, trimColor);
        }

        // Speech Box & Elevated Speaker Nameplate
        renderSpeechBox(gfx, eased, alpha, partialTick);

        // Choice Section Prompt Header
        if (currentNode != null && currentNode.type() == DialogueNodeType.PLAYER_CHOICE && !activeChoices.isEmpty()) {
            int boxX = getSpeechBoxX();
            int boxW = getSpeechBoxW();
            int boxY = getSpeechBoxY();
            int btnW = Math.min(CHOICE_BTN_WIDTH, Math.max(180, boxW - 24));
            int btnX = boxX + boxW - btnW;
            int totalH = activeChoices.size() * CHOICE_BTN_HEIGHT + (activeChoices.size() - 1) * CHOICE_BTN_GAP;
            int startY = Math.max(LETTERBOX_HEIGHT + 24, boxY - totalH - 18);

            Component prompt = Component.translatable("screen.oririmod.conversation.choose");
            gfx.drawString(font, prompt, btnX + 2, startY - 13, withAlpha(GOLD_BRIGHT, alpha), true);
        }
    }

    private void renderSpeechBox(GuiGraphics gfx, float eased, int alpha, float partialTick) {
        int boxW = getSpeechBoxW();
        int boxX = getSpeechBoxX();
        int slideOffset = Math.round((1.0F - eased) * 14.0F);
        int boxY = getSpeechBoxY() + slideOffset;
        int boxH = SPEECH_BOX_HEIGHT;

        // Subtle shadow beneath speech box
        gfx.fill(boxX + 3, boxY + boxH, boxX + boxW - 2, boxY + boxH + 3, withAlpha(0x000000, (int) (0.45F * alpha)));

        // Obsidian-amethyst translucent gradient fill
        int topFill = withAlpha(0x131021, (int) (0.88F * alpha));
        int botFill = withAlpha(0x0A0812, (int) (0.93F * alpha));
        gfx.fill(boxX + 1, boxY, boxX + boxW - 1, boxY + boxH, withAlpha(OUTER_SHADOW, (int) (0.90F * alpha)));
        gfx.fill(boxX, boxY + 1, boxX + boxW, boxY + boxH - 1, withAlpha(OUTER_SHADOW, (int) (0.90F * alpha)));
        gfx.fillGradient(boxX + 2, boxY + 2, boxX + boxW - 2, boxY + boxH - 2, topFill, botFill);

        // Antique gold outer rim + amethyst top inner sheen
        int outerRim = withAlpha(BRONZE, (int) (0.92F * alpha));
        gfx.hLine(boxX + 2, boxX + boxW - 3, boxY + 1, outerRim);
        gfx.hLine(boxX + 2, boxX + boxW - 3, boxY + boxH - 2, outerRim);
        gfx.vLine(boxX + 1, boxY + 2, boxY + boxH - 3, outerRim);
        gfx.vLine(boxX + boxW - 2, boxY + 2, boxY + boxH - 3, outerRim);
        gfx.hLine(boxX + 4, boxX + boxW - 5, boxY + 2, withAlpha(AMETHYST, (int) (0.45F * alpha)));

        // Ornate corner filigree
        drawCornerFiligree(gfx, boxX + 1, boxY + 1, boxW - 2, boxH - 2, 10, withAlpha(GOLD, alpha));

        // Elevated Speaker Nameplate Banner perched on the top-left edge
        String speakerKey = (currentNode != null && currentNode.speaker() != null && !currentNode.speaker().isBlank())
                ? currentNode.speaker()
                : lastSpeakerKey;
        if (speakerKey != null && !speakerKey.isBlank()) {
            String speakerName = Component.translatable(speakerKey).getString();
            int nameW = font.width(speakerName);
            int plateW = nameW + 28;
            int plateH = 18;
            int plateX = boxX + 16;
            int plateY = boxY - 11;

            gfx.fill(plateX, plateY, plateX + plateW, plateY + plateH, withAlpha(OUTER_SHADOW, alpha));
            gfx.fillGradient(plateX + 1, plateY + 1, plateX + plateW - 1, plateY + plateH - 1,
                    withAlpha(0x241B33, alpha), withAlpha(0x130E1C, alpha));
            drawBorder(gfx, plateX, plateY, plateW, plateH, withAlpha(GOLD, alpha));
            gfx.hLine(plateX + 1, plateX + plateW - 2, plateY + 1, withAlpha(GOLD_BRIGHT, (int) (0.55F * alpha)));

            // Small decorative gold diamonds flanking the name
            drawDiamond(gfx, plateX + 7, plateY + plateH / 2, 2, withAlpha(GOLD, alpha));
            drawDiamond(gfx, plateX + plateW - 8, plateY + plateH / 2, 2, withAlpha(GOLD, alpha));

            gfx.drawString(font, Component.literal(speakerName).withStyle(ChatFormatting.BOLD),
                    plateX + 14, plateY + 5, withAlpha(TEXT_TITLE, alpha), true);
        }

        // Dialogue text (typewriter on NPC_LINE; full last spoken line retained on PLAYER_CHOICE)
        if (!fullText.isEmpty() && revealedChars > 0) {
            String visibleText = fullText.substring(0, Math.min(revealedChars, fullText.length()));
            int textX = boxX + 18;
            int textY = boxY + 16;
            int maxWidth = boxW - 36;

            List<FormattedCharSequence> lines = font.split(Component.literal(visibleText), maxWidth);
            int lineHeight = 12;
            int maxLines = Math.max(1, (boxH - 34) / lineHeight);
            int startLine = Math.max(0, lines.size() - maxLines);

            int textColor = withAlpha(TEXT_PRIMARY, alpha);
            for (int i = startLine; i < lines.size(); i++) {
                gfx.drawString(font, lines.get(i), textX, textY + (i - startLine) * lineHeight, textColor, false);
            }
        }

        // Advance prompt on completed NPC_LINE
        if (currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE && textFullyRevealed) {
            float bob = Mth.sin((fadeInTicks + partialTick) * 0.22F);
            int pulseAlpha = (int) (Mth.clamp(0.55F + 0.38F * bob, 0.2F, 1.0F) * alpha);
            int bobOffset = bob > 0.3F ? 1 : 0;

            String hint = Component.translatable("screen.oririmod.conversation.continue").getString();
            int hintW = font.width(hint);
            int hintX = boxX + boxW - hintW - 24;
            int hintY = boxY + boxH - 16;

            gfx.drawString(font, hint, hintX, hintY, withAlpha(TEXT_BODY, pulseAlpha), false);
            gfx.drawString(font, "▼", boxX + boxW - 18, hintY + bobOffset, withAlpha(GOLD_BRIGHT, pulseAlpha), false);
        }
    }

    // ── Custom RPG Choice Banner Button ──

    private static final class ChoiceBannerButton extends AbstractButton {
        private final int hotkeyNumber;
        private final Runnable onSelect;

        ChoiceBannerButton(int x, int y, int w, int h, int hotkeyNumber, Component label, Runnable onSelect) {
            super(x, y, w, h, label);
            this.hotkeyNumber = hotkeyNumber;
            this.onSelect = onSelect;
        }

        @Override
        public void onPress() {
            if (this.onSelect != null) {
                this.onSelect.run();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hovered = this.isHoveredOrFocused();

            // Slight graceful slide-left accent on hover
            int shift = hovered ? -3 : 0;
            int rx = x + shift;

            int bgTop = hovered ? 0xF029203E : 0xDE141022;
            int bgBot = hovered ? 0xF0181229 : 0xDE0B0914;
            int border = hovered ? GOLD_BRIGHT : AMETHYST_DIM;

            gfx.fill(rx, y, rx + w, y + h, OUTER_SHADOW);
            gfx.fillGradient(rx + 1, y + 1, rx + w - 1, y + h - 1, bgTop, bgBot);
            drawBorder(gfx, rx, y, w, h, border);

            // Left gold accent bar
            gfx.fill(rx + 1, y + 1, rx + (hovered ? 5 : 3), y + h - 1, hovered ? GOLD_BRIGHT : GOLD_DIM);
            if (hovered) {
                gfx.hLine(rx + 1, rx + w - 2, y + 1, withAlpha(GOLD_BRIGHT, 0x66));
            }

            Font font = Minecraft.getInstance().font;

            // Hotkey badge [1]
            int badgeX = rx + 9;
            int badgeY = y + (h - 14) / 2;
            gfx.fill(badgeX, badgeY, badgeX + 14, badgeY + 14, hovered ? 0xCC3D2B12 : 0xAA0C0A15);
            drawBorder(gfx, badgeX, badgeY, 14, 14, hovered ? GOLD_BRIGHT : GOLD_DIM);
            gfx.drawCenteredString(font, String.valueOf(hotkeyNumber), badgeX + 7, badgeY + 3,
                    hovered ? GOLD_BRIGHT : GOLD);

            // Choice label (truncated safely if exceptionally long)
            int textX = badgeX + 20;
            int maxTextW = w - (textX - rx) - 18;
            String label = truncate(font, getMessage().getString(), maxTextW);
            gfx.drawString(font, label, textX, y + (h - 8) / 2, hovered ? GOLD_BRIGHT : TEXT_PRIMARY, hovered);

            // Hover chevron on right
            if (hovered) {
                gfx.drawString(font, "▸", rx + w - 12, y + (h - 8) / 2, GOLD_BRIGHT, true);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
