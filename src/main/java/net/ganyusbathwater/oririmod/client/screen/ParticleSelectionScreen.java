package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.item.custom.magic.ParticleDebugWandItem;
import net.ganyusbathwater.oririmod.network.packet.SelectParticleEffectPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * ParticleSelectionScreen — the in-game GUI for choosing a particle effect
 * on the {@link ParticleDebugWandItem}.
 *
 * <p>Opened via Shift + Right Click.  Mirrors the layout and style of
 * {@link BossAttackSelectionScreen} so both debug wands feel consistent.</p>
 */
public class ParticleSelectionScreen extends Screen {

    private final ItemStack wandStack;
    private final InteractionHand hand;

    public ParticleSelectionScreen(ItemStack wandStack, InteractionHand hand) {
        super(Component.literal("Select Particle Effect"));
        this.wandStack = wandStack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        int buttonWidth  = 180;
        int buttonHeight = 20;
        int spacing      = 4;

        ParticleDebugWandItem.ParticleEffectType[] types =
                ParticleDebugWandItem.ParticleEffectType.values();

        // Two-column layout (same as BossAttackSelectionScreen)
        int columns    = 2;
        int rows       = (int) Math.ceil(types.length / (double) columns);
        int totalWidth = (buttonWidth * columns) + (spacing * (columns - 1));
        int totalHeight= (buttonHeight * rows)   + (spacing * (rows - 1));
        int startX     = (this.width  - totalWidth)  / 2;
        int startY     = (this.height - totalHeight) / 2;

        for (int i = 0; i < types.length; i++) {
            ParticleDebugWandItem.ParticleEffectType type = types[i];
            int col = i % columns;
            int row = i / columns;
            int x   = startX + col * (buttonWidth  + spacing);
            int y   = startY + row * (buttonHeight + spacing);

            this.addRenderableWidget(
                Button.builder(
                    Component.literal(ParticleDebugWandItem.prettyName(type)),
                    btn -> {
                        // Send to server so the NBT is updated on the real ItemStack
                        PacketDistributor.sendToServer(new SelectParticleEffectPayload(type));
                        this.onClose();
                    }
                ).bounds(x, y, buttonWidth, buttonHeight).build()
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
