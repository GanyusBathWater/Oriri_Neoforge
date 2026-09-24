package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.DungeonDeathActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class OririDungeonDeathScreen extends Screen {

    private final int livesRemaining;
    private final long openTimeMs;
    private static final int COOLDOWN_MS = 5000;
    
    private Button respawnButton;

    public OririDungeonDeathScreen(int livesRemaining) {
        super(Component.translatable("deathScreen.title"));
        this.livesRemaining = livesRemaining;
        this.openTimeMs = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int midX = this.width / 2;
        int midY = this.height / 2;

        this.respawnButton = Button.builder(
                Component.literal("Respawn (5)").withStyle(ChatFormatting.GRAY),
                btn -> {
                    PacketDistributor.sendToServer(new DungeonDeathActionPayload("RESPAWN"));
                    this.onClose();
                })
                .bounds(midX - 100, midY + 30, 200, 20)
                .build();
        this.respawnButton.active = false;
        this.addRenderableWidget(this.respawnButton);

        this.addRenderableWidget(Button.builder(
                Component.literal("Give Up").withStyle(ChatFormatting.RED),
                btn -> {
                    PacketDistributor.sendToServer(new DungeonDeathActionPayload("GIVE_UP"));
                    this.onClose();
                })
                .bounds(midX - 100, midY + 54, 200, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("deathScreen.titleScreen"),
                btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.level.disconnect();
                        this.minecraft.setScreen(new TitleScreen());
                    }
                })
                .bounds(midX - 100, midY + 78, 200, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.respawnButton != null && !this.respawnButton.active) {
            long elapsed = System.currentTimeMillis() - this.openTimeMs;
            if (elapsed >= COOLDOWN_MS) {
                this.respawnButton.active = true;
                this.respawnButton.setMessage(Component.literal("Respawn").withStyle(ChatFormatting.GREEN));
            } else {
                int secondsLeft = (int) Math.ceil((COOLDOWN_MS - elapsed) / 1000.0);
                this.respawnButton.setMessage(Component.literal("Respawn (" + secondsLeft + ")").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Draw a dark red gradient over the screen like the vanilla death screen
        gfx.fillGradient(0, 0, this.width, this.height, 0x60500000, 0x80300000);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);

        // Title
        gfx.pose().pushPose();
        gfx.pose().scale(2.0F, 2.0F, 2.0F);
        gfx.drawCenteredString(this.font, Component.literal("You Fell in the Dungeon").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), this.width / 4, (this.height / 2 - 70) / 2, 0xFFFFFF);
        gfx.pose().popPose();

        // Lives
        if (this.livesRemaining > 0) {
            gfx.drawCenteredString(this.font, Component.literal("Lives Remaining: " + this.livesRemaining).withStyle(ChatFormatting.YELLOW), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font, Component.literal("Final Life Lost!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
