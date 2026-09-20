package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.dialogue.*;
import net.ganyusbathwater.oririmod.network.packet.ConversationActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Gacha-style NPC conversation screen inspired by Genshin Impact / Honkai Star Rail.
 *
 * Layout:
 * ┌──────────────────────────────────────────────────────────────┐
 * │                                                              │
 * │   ┌───────────┐                                              │
 * │   │  NPC      │                                              │
 * │   │  Portrait │                             [Choice 1]       │
 * │   │  (2D)     │                             [Choice 2]       │
 * │   │           │                             [Choice 3]       │
 * │   └───────────┘                                              │
 * │                                                              │
 * │  ┌──────────────────────────────────────────────────────────┐│
 * │  │  Speaker Name                                            ││
 * │  │  "Dialogue text with typewriter effect..."               ││
 * │  │                                                     [▶]  ││
 * │  └──────────────────────────────────────────────────────────┘│
 * └──────────────────────────────────────────────────────────────┘
 *
 * Features:
 * - Typewriter text animation (~30 chars/sec)
 * - Left-click to skip animation or advance to next node
 * - Choice buttons appear on the right for PLAYER_CHOICE nodes
 * - 2D NPC portrait on the left with placeholder fallback
 * - Distance + entity alive + damage checks in tick()
 * - Smooth fade-in transition
 */
@OnlyIn(Dist.CLIENT)
public class ConversationScreen extends Screen {

    // ── Layout Constants ──
    private static final int SPEECH_BOX_HEIGHT = 90;
    private static final int SPEECH_BOX_MARGIN = 30;
    private static final int PORTRAIT_SIZE = 128;
    private static final int PORTRAIT_MARGIN_LEFT = 40;
    private static final int PORTRAIT_MARGIN_BOTTOM = 20;
    private static final int CHOICE_BTN_WIDTH = 220;
    private static final int CHOICE_BTN_HEIGHT = 24;
    private static final int CHOICE_BTN_GAP = 6;
    private static final int CHOICE_MARGIN_RIGHT = 50;

    // ── Colors ──
    private static final int C_BG = 0xCC0A0A14;          // Dark semi-transparent background
    private static final int C_SPEECH_BG = 0xDD0C0C18;   // Speech box background
    private static final int C_SPEECH_BORDER = 0xFF3A2A5A; // Speech box border (purple tint)
    private static final int C_SPEAKER_NAME = 0xFFFFD700; // Gold for speaker name
    private static final int C_DIALOGUE_TEXT = 0xFFE8E0D0; // Warm white for dialogue
    private static final int C_ADVANCE_HINT = 0x88AAAAAA; // Dim hint for "click to continue"

    // ── Typewriter ──
    private static final float CHARS_PER_SECOND = 30.0f;
    private static final int BLIP_INTERVAL = 3; // Play blip every N characters

    // ── Validation ──
    private static final double MAX_DISTANCE = 10.0;

    // ── State ──
    private final DialogueTree tree;
    private final int entityId;
    @Nullable
    private DialogueNode currentNode;
    private String fullText = "";
    private int revealedChars = 0;
    private float charAccumulator = 0f;
    private boolean textFullyRevealed = false;
    private int fadeInTicks = 0;
    private static final int FADE_IN_DURATION = 8; // ticks
    private int lastBlipChar = -1;

    // ── Portrait ──
    private static final ResourceLocation PLACEHOLDER_PORTRAIT =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/portrait/placeholder.png");
    @Nullable
    private ResourceLocation portraitTexture;

    // ── Sound ──
    @Nullable
    private ResourceLocation talkSoundLoc;

    /** Tracks whether HUD should be hidden. Checked by ConversationHudHandler. */
    private static boolean conversationActive = false;

    public ConversationScreen(DialogueTree tree, int entityId) {
        super(Component.empty());
        this.tree = tree;
        this.entityId = entityId;
    }

    public static boolean isConversationActive() {
        return conversationActive;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        conversationActive = true;

        // Parse portrait texture
        if (tree.getPortrait() != null && !tree.getPortrait().isEmpty()) {
            String[] parts = tree.getPortrait().split(":");
            if (parts.length == 2) {
                portraitTexture = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            }
        }
        if (portraitTexture == null) {
            portraitTexture = PLACEHOLDER_PORTRAIT;
        }

        // Parse talk sound
        if (tree.getTalkSound() != null && !tree.getTalkSound().isEmpty()) {
            String[] parts = tree.getTalkSound().split(":");
            if (parts.length == 2) {
                talkSoundLoc = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            }
        }

        // Start the dialogue
        setNode(tree.getStartNode());
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
        this.currentNode = node;
        this.charAccumulator = 0f;
        this.lastBlipChar = -1;
        this.textFullyRevealed = false;

        if (node == null) {
            // End of dialogue
            onClose();
            return;
        }

        switch (node.type()) {
            case NPC_LINE -> {
                this.fullText = node.text() != null
                        ? Component.translatable(node.text()).getString()
                        : "";
                this.revealedChars = 0;
            }
            case PLAYER_CHOICE -> {
                this.fullText = "";
                this.revealedChars = 0;
                this.textFullyRevealed = true;
                buildChoiceButtons(node.choices());
            }
            case ACTION -> {
                executeAction(node.action());
            }
        }
    }

    private void buildChoiceButtons(List<DialogueChoice> choices) {
        int btnX = width - CHOICE_MARGIN_RIGHT - CHOICE_BTN_WIDTH;
        int totalHeight = choices.size() * CHOICE_BTN_HEIGHT + (choices.size() - 1) * CHOICE_BTN_GAP;
        int startY = (height - SPEECH_BOX_HEIGHT - SPEECH_BOX_MARGIN) / 2 - totalHeight / 2 + 20;

        for (int i = 0; i < choices.size(); i++) {
            final DialogueChoice choice = choices.get(i);
            String choiceText = Component.translatable(choice.text()).getString();
            int btnY = startY + i * (CHOICE_BTN_HEIGHT + CHOICE_BTN_GAP);

            addRenderableWidget(Button.builder(
                    Component.literal(choiceText),
                    btn -> {
                        DialogueNode next = tree.getNode(choice.nextNodeId());
                        setNode(next);
                    })
                    .pos(btnX, btnY)
                    .size(CHOICE_BTN_WIDTH, CHOICE_BTN_HEIGHT)
                    .build());
        }
    }

    private void executeAction(@Nullable String action) {
        if (action == null || "CLOSE".equals(action)) {
            onClose();
            return;
        }

        // Send action to server, then close conversation
        PacketDistributor.sendToServer(new ConversationActionPayload(entityId, action));
        onClose();
    }

    // ── Tick (Typewriter + Validation) ──

    @Override
    public void tick() {
        super.tick();
        fadeInTicks++;

        // ── Validation checks ──
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            onClose();
            return;
        }

        // Entity alive check
        var entity = mc.level.getEntity(entityId);
        if (entity == null || !entity.isAlive()) {
            onClose();
            return;
        }

        // Distance check
        if (mc.player.distanceTo(entity) > MAX_DISTANCE) {
            onClose();
            return;
        }

        // Damage check (close on hit)
        if (mc.player.hurtTime > 0) {
            onClose();
            return;
        }

        // ── Typewriter animation ──
        if (currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE && !textFullyRevealed) {
            charAccumulator += CHARS_PER_SECOND / 20.0f; // 20 ticks/sec
            int newChars = (int) charAccumulator;
            if (newChars > 0) {
                charAccumulator -= newChars;
                revealedChars = Math.min(revealedChars + newChars, fullText.length());

                // Play blip sound at intervals
                if (talkSoundLoc != null) {
                    int blipIndex = revealedChars / BLIP_INTERVAL;
                    if (blipIndex > lastBlipChar && revealedChars < fullText.length()) {
                        lastBlipChar = blipIndex;
                        // Play the talk blip at low volume
                        mc.player.playSound(
                                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(talkSoundLoc),
                                0.3f,
                                0.9f + mc.player.getRandom().nextFloat() * 0.3f
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
        // Let widgets handle clicks first
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE) {
            if (!textFullyRevealed) {
                // Skip typewriter — reveal all text
                revealedChars = fullText.length();
                textFullyRevealed = true;
            } else {
                // Advance to next node
                if (currentNode.nextNodeId() != null) {
                    setNode(tree.getNode(currentNode.nextNodeId()));
                } else {
                    // No next node — end conversation
                    onClose();
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC closes the dialogue
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            onClose();
            return true;
        }
        // Space or Enter also advances dialogue
        if ((keyCode == 32 || keyCode == 257) && currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE) {
            if (!textFullyRevealed) {
                revealedChars = fullText.length();
                textFullyRevealed = true;
            } else {
                if (currentNode.nextNodeId() != null) {
                    setNode(tree.getNode(currentNode.nextNodeId()));
                } else {
                    onClose();
                }
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Rendering ──

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Fix background blur overlap by initializing screen background first
        this.renderBackground(gfx, mouseX, mouseY, partialTick);

        // Calculate fade-in alpha
        float fadeAlpha = Math.min(1.0f, (fadeInTicks + partialTick) / FADE_IN_DURATION);
        int alpha = (int) (fadeAlpha * 255);
        if (alpha < 4) alpha = 4; // minimum to be visible

        // ── Full-screen darkening background ──
        int bgAlpha = (int) (0.75f * alpha);
        gfx.fill(0, 0, width, height, (bgAlpha << 24) | 0x0A0A14);

        // ── Portrait ──
        renderPortrait(gfx, alpha);

        // ── Speech Box ──
        renderSpeechBox(gfx, alpha);

        // ── Choice buttons (rendered by super.render normally, but we do it manually to prevent double blur) ──
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partialTick);
        }

        // ── Advance hint ──
        if (currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE && textFullyRevealed) {
            String hint = "▶ Click to continue";
            int hintX = width - SPEECH_BOX_MARGIN - font.width(hint) - 15;
            int hintY = height - SPEECH_BOX_MARGIN - 18;
            // Pulsing alpha
            int pulseAlpha = (int) (128 + 80 * Math.sin(fadeInTicks * 0.15));
            gfx.drawString(font, hint, hintX, hintY, (pulseAlpha << 24) | 0xAAAAAA, false);
        }

        // ── Choice label ──
        if (currentNode != null && currentNode.type() == DialogueNodeType.PLAYER_CHOICE) {
            int labelX = width - CHOICE_MARGIN_RIGHT - CHOICE_BTN_WIDTH;
            int totalHeight = currentNode.choices().size() * CHOICE_BTN_HEIGHT
                    + (currentNode.choices().size() - 1) * CHOICE_BTN_GAP;
            int startY = (height - SPEECH_BOX_HEIGHT - SPEECH_BOX_MARGIN) / 2 - totalHeight / 2 + 20;
            gfx.drawString(font, "Choose a response:", labelX, startY - 14, C_SPEAKER_NAME, true);
        }
    }

    private void renderPortrait(GuiGraphics gfx, int alpha) {
        if (portraitTexture == null) return;

        int portraitX = PORTRAIT_MARGIN_LEFT;
        int portraitY = height - SPEECH_BOX_HEIGHT - SPEECH_BOX_MARGIN - PORTRAIT_SIZE - PORTRAIT_MARGIN_BOTTOM;

        // Portrait background/frame
        int frameMargin = 4;
        gfx.fill(
                portraitX - frameMargin, portraitY - frameMargin,
                portraitX + PORTRAIT_SIZE + frameMargin, portraitY + PORTRAIT_SIZE + frameMargin,
                (Math.min(alpha, 200) << 24) | 0x1A1A2E
        );
        // Border
        drawBorder(gfx, portraitX - frameMargin, portraitY - frameMargin,
                PORTRAIT_SIZE + frameMargin * 2, PORTRAIT_SIZE + frameMargin * 2,
                ((int)(alpha * 0.7f) << 24) | 0x3A2A5A);

        // Portrait image (rendered at full alpha once faded in)
        gfx.blit(portraitTexture, portraitX, portraitY, 0, 0,
                PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE);
    }

    private void renderSpeechBox(GuiGraphics gfx, int alpha) {
        int boxX = SPEECH_BOX_MARGIN;
        int boxY = height - SPEECH_BOX_HEIGHT - SPEECH_BOX_MARGIN;
        int boxW = width - SPEECH_BOX_MARGIN * 2;
        int boxH = SPEECH_BOX_HEIGHT;

        // Speech box background
        int bgA = (int) (0.87f * alpha);
        gfx.fill(boxX, boxY, boxX + boxW, boxY + boxH, (bgA << 24) | 0x0C0C18);

        // Border with gradient feel
        drawBorder(gfx, boxX, boxY, boxW, boxH,
                ((int)(alpha * 0.6f) << 24) | 0x3A2A5A);
        // Inner glow line at top
        gfx.hLine(boxX + 1, boxX + boxW - 2, boxY + 1,
                ((int)(alpha * 0.15f) << 24) | 0x7A6A9A);

        // Speaker name
        if (currentNode != null && currentNode.speaker() != null) {
            String speakerName = Component.translatable(currentNode.speaker()).getString();
            gfx.drawString(font, speakerName, boxX + 16, boxY + 10, C_SPEAKER_NAME, true);
        }

        // Dialogue text (typewriter)
        if (currentNode != null && currentNode.type() == DialogueNodeType.NPC_LINE && !fullText.isEmpty()) {
            String visibleText = fullText.substring(0, revealedChars);

            // Word-wrap the visible text
            int textX = boxX + 16;
            int textY = boxY + 28;
            int maxWidth = boxW - 32;

            List<net.minecraft.util.FormattedCharSequence> lines =
                    font.split(Component.literal(visibleText), maxWidth);

            int lineHeight = 12;
            int maxLines = (boxH - 36) / lineHeight;
            // If text overflows, show only the last visible lines (scroll effect)
            int startLine = Math.max(0, lines.size() - maxLines);
            for (int i = startLine; i < lines.size(); i++) {
                gfx.drawString(font, lines.get(i), textX, textY + (i - startLine) * lineHeight,
                        C_DIALOGUE_TEXT, false);
            }
        }
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.hLine(x, x + w - 1, y, color);
        gfx.hLine(x, x + w - 1, y + h - 1, color);
        gfx.vLine(x, y, y + h - 1, color);
        gfx.vLine(x + w - 1, y, y + h - 1, color);
    }
}
