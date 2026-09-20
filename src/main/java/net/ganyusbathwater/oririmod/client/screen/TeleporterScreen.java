package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.network.packet.OpenTeleporterScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.UpdateTeleporterIDPayload;
import net.ganyusbathwater.oririmod.block.custom.TeleporterBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class TeleporterScreen extends Screen {
    private final BlockPos pos;
    private final String initialId;
    private final boolean originObstructed;
    private final boolean destObstructed;
    private EditBox idBox;

    public TeleporterScreen(OpenTeleporterScreenPayload payload) {
        super(Component.translatable("gui.oririmod.teleporter.title"));
        this.pos = payload.pos();
        this.initialId = payload.currentId();
        this.originObstructed = payload.originObstructed();
        this.destObstructed = payload.destObstructed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int midX = this.width / 2;
        int midY = this.height / 2;

        this.idBox = new EditBox(this.font, midX - 100, midY - 20, 200, 20, Component.translatable("gui.oririmod.teleporter.id"));
        this.idBox.setValue(this.initialId);
        this.idBox.setMaxLength(64);
        this.addRenderableWidget(this.idBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.oririmod.teleporter.save").withStyle(ChatFormatting.GREEN), b -> saveAndClose())
                .bounds(midX - 100, midY + 10, 200, 20).build());
    }

    private void saveAndClose() {
        String newId = this.idBox.getValue().trim();
        PacketDistributor.sendToServer(new UpdateTeleporterIDPayload(this.pos, newId));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("gui.oririmod.teleporter.frequency_id"), this.idBox.getX(), this.idBox.getY() - 10, 0xDDDDDD);
        
        if (this.originObstructed) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.oririmod.teleporter.origin_obstructed").withStyle(ChatFormatting.RED), this.width / 2, this.height / 2 + 35, 0xFF0000);
        } else if (this.destObstructed) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.oririmod.teleporter.obstructed").withStyle(ChatFormatting.RED), this.width / 2, this.height / 2 + 35, 0xFF0000);
        }
    }
}
