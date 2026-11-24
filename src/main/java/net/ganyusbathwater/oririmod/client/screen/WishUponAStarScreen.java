package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.dungeon.data.StarWishAttribute;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.network.packet.SelectWishAttributePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Celestial Sanctuary screen opened when consuming "Wish Upon a Star".
 * Lets the player pick (or swap) a permanent attribute blessing bound to
 * the originating dungeon.
 */
@OnlyIn(Dist.CLIENT)
public class WishUponAStarScreen extends Screen {

    private static final int PANEL_W = 316;
    private static final int PANEL_H = 228;

    private static final int CARD_W = 136;
    private static final int CARD_H = 34;
    private static final int CARD_GAP_X = 12;
    private static final int CARD_GAP_Y = 8;

    private final String dungeonId;
    private final String dungeonDisplayName;
    @Nullable
    private final StarWishAttribute currentAttribute;
    private final boolean offhand;

    @Nullable
    private StarWishAttribute selectedAttribute = null;
    private SanctuaryButton confirmButton;

    private final ItemStack starIcon = new ItemStack(ModItems.WISH_UPON_A_STAR.get());

    public WishUponAStarScreen(String dungeonId, String dungeonDisplayName, String currentAttributeId, boolean offhand) {
        super(Component.translatable("screen.oririmod.wish.title"));
        this.dungeonId = dungeonId;
        this.dungeonDisplayName = dungeonDisplayName;
        this.currentAttribute = StarWishAttribute.byId(currentAttributeId);
        this.offhand = offhand;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        int buttonY = top + PANEL_H - 30;

        this.confirmButton = addRenderableWidget(new SanctuaryButton(
                left + 16,
                buttonY,
                148,
                20,
                Component.translatable(this.currentAttribute == null
                        ? "screen.oririmod.wish.confirm"
                        : "screen.oririmod.wish.confirm_change"),
                ButtonTone.PRIMARY,
                this::confirmWish
        ));
        this.confirmButton.active = this.selectedAttribute != null && this.selectedAttribute != this.currentAttribute;

        addRenderableWidget(new SanctuaryButton(
                left + PANEL_W - 132,
                buttonY,
                116,
                20,
                Component.translatable("screen.oririmod.wish.cancel"),
                ButtonTone.NEUTRAL,
                this::onClose
        ));
    }

    private void confirmWish() {
        if (this.selectedAttribute == null || this.selectedAttribute == this.currentAttribute) {
            return;
        }
        PacketDistributor.sendToServer(new SelectWishAttributePayload(
                this.dungeonId,
                this.selectedAttribute.getId(),
                this.offhand
        ));
        onClose();
    }

    private int getGridLeft() {
        int left = (this.width - PANEL_W) / 2;
        int totalGridW = CARD_W * 2 + CARD_GAP_X;
        return left + (PANEL_W - totalGridW) / 2;
    }

    private int getGridTop() {
        int top = (this.height - PANEL_H) / 2;
        return top + 66;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridLeft = getGridLeft();
            int gridTop = getGridTop();
            StarWishAttribute[] attrs = StarWishAttribute.values();
            for (int i = 0; i < attrs.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = gridLeft + col * (CARD_W + CARD_GAP_X);
                int cy = gridTop + row * (CARD_H + CARD_GAP_Y);
                if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                    StarWishAttribute clicked = attrs[i];
                    if (clicked != this.currentAttribute) {
                        this.selectedAttribute = clicked;
                        if (this.confirmButton != null) {
                            this.confirmButton.active = true;
                        }
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(
                                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.15F));
                        }
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int cx = this.width / 2;
        int left = cx - PANEL_W / 2;
        int top = (this.height - PANEL_H) / 2;

        // Main obsidian & gilded sanctuary panel
        drawSanctuaryPanel(gfx, left, top, PANEL_W, PANEL_H, GOLD, 0xFF5D4623);

        // Subtle twinkling starfield inside the header strip
        drawHeaderStars(gfx, left + 6, top + 6, PANEL_W - 12, 48);

        // Glowing star icon emblem + Title + Dungeon subtitle
        gfx.renderItem(this.starIcon, left + 16, top + 12);
        gfx.drawString(this.font,
                Component.translatable("screen.oririmod.wish.title").withStyle(ChatFormatting.BOLD),
                left + 38, top + 12, GOLD_BRIGHT, true);
        gfx.drawString(this.font,
                Component.translatable("screen.oririmod.wish.dungeon_origin", this.dungeonDisplayName),
                left + 38, top + 24, TEXT_BODY, false);

        // Subtitle / current status line
        Component statusLine = (this.currentAttribute == null)
                ? Component.translatable("screen.oririmod.wish.prompt_new")
                : Component.translatable("screen.oririmod.wish.prompt_swap",
                        Component.translatable(this.currentAttribute.getNameKey()),
                        Component.translatable(this.currentAttribute.getBonusKey()));
        gfx.drawCenteredString(this.font, statusLine, cx, top + 44,
                this.currentAttribute == null ? CYAN_LIGHT : GOLD);

        drawGildedDivider(gfx, left + 16, top + 57, PANEL_W - 32, GOLD_DIM);

        // 2x3 Attribute Cards
        int gridLeft = getGridLeft();
        int gridTop = getGridTop();
        StarWishAttribute[] attrs = StarWishAttribute.values();

        for (int i = 0; i < attrs.length; i++) {
            StarWishAttribute attr = attrs[i];
            int col = i % 2;
            int row = i / 2;
            int cardX = gridLeft + col * (CARD_W + CARD_GAP_X);
            int cardY = gridTop + row * (CARD_H + CARD_GAP_Y);

            boolean isCurrent = attr == this.currentAttribute;
            boolean isSelected = attr == this.selectedAttribute;
            boolean hovered = !isCurrent
                    && mouseX >= cardX && mouseX < cardX + CARD_W
                    && mouseY >= cardY && mouseY < cardY + CARD_H;

            drawAttributeCard(gfx, cardX, cardY, attr, isCurrent, isSelected, hovered);
        }

        drawGildedDivider(gfx, left + 16, top + PANEL_H - 38, PANEL_W - 32, GOLD_DIM);
    }

    private void drawAttributeCard(GuiGraphics gfx, int x, int y, StarWishAttribute attr,
                                   boolean isCurrent, boolean isSelected, boolean hovered) {
        int accent = attr.getAccentColor();
        int bgTop;
        int bgBot;
        int borderCol;

        if (isCurrent) {
            bgTop = 0xE01A1529;
            bgBot = 0xE0100D1A;
            borderCol = GOLD_DIM;
        } else if (isSelected) {
            bgTop = 0xEE282042;
            bgBot = 0xEE17122B;
            borderCol = GOLD_BRIGHT;
        } else if (hovered) {
            bgTop = 0xE6221B38;
            bgBot = 0xE6141024;
            borderCol = accent;
        } else {
            bgTop = 0xDC141122;
            bgBot = 0xDC0C0A16;
            borderCol = 0xFF3E3156;
        }

        gfx.fillGradient(x, y, x + CARD_W, y + CARD_H, bgTop, bgBot);
        drawBorder(gfx, x, y, CARD_W, CARD_H, borderCol);

        // Left colored vertical accent bar
        gfx.fill(x + 2, y + 3, x + 5, y + CARD_H - 3, isCurrent ? GOLD : accent);

        // Attribute Name
        Component name = Component.translatable(attr.getNameKey());
        int nameColor = isCurrent ? GOLD_BRIGHT : (isSelected ? 0xFFFFFFFF : TEXT_PRIMARY);
        gfx.drawString(this.font, name, x + 10, y + 6, nameColor, false);

        // Bonus value line
        Component bonus = Component.translatable(attr.getBonusKey());
        gfx.drawString(this.font, bonus, x + 10, y + 19, accent, false);

        // Status tag on the right side
        if (isCurrent) {
            Component activeTag = Component.translatable("screen.oririmod.wish.active_badge");
            int tw = this.font.width(activeTag);
            gfx.fill(x + CARD_W - tw - 9, y + 5, x + CARD_W - 4, y + 16, 0xAA4A3614);
            gfx.drawString(this.font, activeTag, x + CARD_W - tw - 6, y + 7, GOLD_BRIGHT, false);
        } else if (isSelected) {
            drawDiamond(gfx, x + CARD_W - 11, y + CARD_H / 2, 4, GOLD_BRIGHT);
        }
    }

    private void drawHeaderStars(GuiGraphics gfx, int x, int y, int w, int h) {
        double t = (Util.getMillis() % 60_000L) * 0.0025D;
        for (int i = 0; i < 18; i++) {
            int sx = x + ((i * 53 + 17) % Math.max(1, w - 4));
            int sy = y + ((i * 29 + 7) % Math.max(1, h - 4));
            float tw = 0.45F + 0.55F * Mth.sin((float) (t + i * 1.37D));
            int alpha = Math.round(tw * 120.0F);
            int rgb = (i % 3 == 0) ? 0xFFE596 : ((i % 3 == 1) ? 0x8BE9FD : 0xFFFFFF);
            gfx.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | rgb);
        }
    }
}
