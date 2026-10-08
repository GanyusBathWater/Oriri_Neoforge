package net.ganyusbathwater.oririmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.menu.SoulHarvesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SoulHarvesterScreen extends AbstractContainerScreen<SoulHarvesterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/soul_harvester.png");

    public SoulHarvesterScreen(SoulHarvesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 208; // 176 + 28 upgrade panel
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
    }

    private net.minecraft.world.entity.LivingEntity renderedEntity = null;
    private String lastEntityId = null;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.getScaledProgress();
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, x + 102, y + 36, 0, 240, progress, 15);
        }

        net.minecraft.world.item.ItemStack shardStack = this.menu.getSlot(0).getItem();
        if (shardStack.is(net.ganyusbathwater.oririmod.item.ModItems.SOUL_SHARD.get())) {
            net.minecraft.nbt.CompoundTag tag = shardStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (tag.contains("captured_entity")) {
                String entityId = tag.getString("captured_entity");
                if (!entityId.equals(lastEntityId)) {
                    lastEntityId = entityId;
                    java.util.Optional<net.minecraft.world.entity.EntityType<?>> optType = net.minecraft.world.entity.EntityType.byString(entityId);
                    if (optType.isPresent()) {
                        net.minecraft.world.entity.Entity entity = optType.get().create(this.minecraft.level);
                        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                            renderedEntity = living;
                        } else {
                            renderedEntity = null;
                        }
                    } else {
                        renderedEntity = null;
                    }
                }
            } else {
                lastEntityId = null;
                renderedEntity = null;
            }
        } else {
            lastEntityId = null;
            renderedEntity = null;
        }

        if (renderedEntity != null) {
            int boxX = x + 16;
            int boxY = y + 16;
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics, boxX, boxY, boxX + 54, boxY + 54, 24, 
                0f, mouseX, mouseY, renderedEntity
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
