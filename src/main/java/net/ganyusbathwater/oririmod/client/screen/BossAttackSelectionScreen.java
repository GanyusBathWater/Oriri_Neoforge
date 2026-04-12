package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.item.custom.magic.BossAttackDebugWandItem;
import net.ganyusbathwater.oririmod.network.packet.SelectBossAttackPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class BossAttackSelectionScreen extends Screen {
    private final ItemStack wandStack;
    private final InteractionHand hand;

    public BossAttackSelectionScreen(ItemStack wandStack, InteractionHand hand) {
        super(Component.literal("Select Boss Attack"));
        this.wandStack = wandStack;
        this.hand = hand;
    }

    @Override
    protected void init() {
        int buttonWidth = 160;
        int buttonHeight = 20;
        int spacing = 4;
        
        BossAttackDebugWandItem.BossAttackType[] types = BossAttackDebugWandItem.BossAttackType.values();
        
        // Layout as two columns
        int columns = 2;
        int rows = (int) Math.ceil(types.length / (double) columns);
        
        int totalWidth = (buttonWidth * columns) + (spacing * (columns - 1));
        int totalHeight = (buttonHeight * rows) + (spacing * (rows - 1));
        
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - totalHeight) / 2;

        for (int i = 0; i < types.length; i++) {
            BossAttackDebugWandItem.BossAttackType type = types[i];
            int col = i % columns;
            int row = i / columns;
            
            int x = startX + col * (buttonWidth + spacing);
            int y = startY + row * (buttonHeight + spacing);
            
            this.addRenderableWidget(Button.builder(Component.literal(BossAttackDebugWandItem.prettyName(type)), (button) -> {
                PacketDistributor.sendToServer(new SelectBossAttackPayload(type));
                this.onClose();
            }).bounds(x, y, buttonWidth, buttonHeight).build());
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
