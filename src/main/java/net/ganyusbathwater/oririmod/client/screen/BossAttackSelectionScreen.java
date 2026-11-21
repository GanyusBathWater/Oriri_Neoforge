package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.item.custom.magic.BossAttackDebugWandItem;
import net.ganyusbathwater.oririmod.network.packet.SelectBossAttackPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Programmatic crimson-and-gold Boss Arsenal Selector for the Boss Attack Debug Wand.
 */
public class BossAttackSelectionScreen extends Screen {

    private final ItemStack wandStack;
    private final InteractionHand hand;

    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;

    public BossAttackSelectionScreen(ItemStack wandStack, InteractionHand hand) {
        super(Component.literal("Select Boss Attack"));
        this.wandStack = wandStack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 162;
        int buttonHeight = 20;
        int gapX = 6;
        int gapY = 5;

        BossAttackDebugWandItem.BossAttackType[] types = BossAttackDebugWandItem.BossAttackType.values();
        int columns = 2;
        int rows = (int) Math.ceil(types.length / (double) columns);

        int gridW = buttonWidth * columns + gapX * (columns - 1);
        int gridH = buttonHeight * rows + gapY * Math.max(0, rows - 1);

        this.panelW = gridW + 28;
        this.panelH = gridH + 78;
        this.panelLeft = (this.width - this.panelW) / 2;
        this.panelTop = (this.height - this.panelH) / 2;

        int startX = this.panelLeft + 14;
        int startY = this.panelTop + 38;

        for (int i = 0; i < types.length; i++) {
            BossAttackDebugWandItem.BossAttackType type = types[i];
            int col = i % columns;
            int row = i / columns;

            int x = startX + col * (buttonWidth + gapX);
            int y = startY + row * (buttonHeight + gapY);

            this.addRenderableWidget(new SanctuaryButton(
                    x, y, buttonWidth, buttonHeight,
                    Component.literal(BossAttackDebugWandItem.prettyName(type)),
                    ButtonTone.CRIMSON,
                    () -> {
                        PacketDistributor.sendToServer(new SelectBossAttackPayload(type));
                        this.onClose();
                    }
            ));
        }

        this.addRenderableWidget(new SanctuaryButton(
                this.width / 2 - 44,
                this.panelTop + this.panelH - 28,
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

        drawSanctuaryPanel(gfx, panelLeft, panelTop, panelW, panelH, CRIMSON, 0xFF632026);
        gfx.drawCenteredString(this.font,
                this.title.copy().withStyle(ChatFormatting.BOLD),
                this.width / 2, panelTop + 12, CRIMSON_LIGHT);
        drawGildedDivider(gfx, panelLeft + 14, panelTop + 28, panelW - 28, CRIMSON);
        drawGildedDivider(gfx, panelLeft + 14, panelTop + panelH - 35, panelW - 28, GOLD_DIM);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
