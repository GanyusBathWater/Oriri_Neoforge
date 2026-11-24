package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.client.screen.ArcanaAdvancementRenderer;
import net.ganyusbathwater.oririmod.item.custom.ArcanaItem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private AdvancementNode rootNode;
    @Shadow @Final private AdvancementWidget root;
    @Shadow @Final private Map<AdvancementHolder, AdvancementWidget> widgets;
    @Shadow private double scrollX;
    @Shadow private double scrollY;
    @Shadow private int minX;
    @Shadow private int minY;
    @Shadow private int maxX;
    @Shadow private int maxY;
    @Shadow private boolean centered;

    @Unique
    private boolean oriri$paddedBounds = false;

    @Unique
    private boolean oriri$isArcanaTab() {
        return ArcanaItem.ROOT_ADVANCEMENT_ID.equals(this.rootNode.holder().id());
    }

    @Inject(method = "drawContents", at = @At("HEAD"), cancellable = true)
    private void oriri$renderCustomArcanaTab(GuiGraphics gui, int x, int y, CallbackInfo ci) {
        if (!oriri$isArcanaTab()) {
            return;
        }

        // Add symmetric bottom/right scroll margin matching the ~16px top/left margin of the wheel
        if (!this.oriri$paddedBounds && this.maxX > Integer.MIN_VALUE / 2) {
            this.maxX += 18;
            this.maxY += 18;
            this.oriri$paddedBounds = true;
        }

        if (!this.centered) {
            this.scrollX = (double) (117 - (this.root.getX() + ArcanaAdvancementRenderer.SLOT_CENTER_OFFSET_X));
            this.scrollY = (double) (56 - (this.root.getY() + ArcanaAdvancementRenderer.SLOT_CENTER_OFFSET_Y));
            this.centered = true;
        }

        gui.enableScissor(x, y, x + ArcanaAdvancementRenderer.VIEW_W, y + ArcanaAdvancementRenderer.VIEW_H);
        gui.pose().pushPose();
        gui.pose().translate((float) x, (float) y, 0.0F);

        int sx = Mth.floor(this.scrollX);
        int sy = Mth.floor(this.scrollY);

        // 1. Pseudo-3D parallax starfield + nebula backdrop
        ArcanaAdvancementRenderer.renderStarfieldBackdrop(gui, this.scrollX, this.scrollY);

        // Gather completion status cleanly from the client advancement progress map
        Map<AdvancementHolder, Boolean> unlockedMap = new HashMap<>(this.widgets.size());
        int unlockedArcanaCount = 0;
        var connection = this.minecraft.getConnection();
        var clientAdvancements = connection != null ? connection.getAdvancements() : null;

        for (AdvancementHolder holder : this.widgets.keySet()) {
            boolean done = false;
            if (clientAdvancements != null) {
                AdvancementProgress prog = ((ClientAdvancementsAccessor) clientAdvancements).oriri$getProgress().get(holder);
                done = ArcanaAdvancementRenderer.isProgressDone(prog);
            }
            unlockedMap.put(holder, done);
            ResourceLocation id = holder.id();
            if (done && !ArcanaItem.ROOT_ADVANCEMENT_ID.equals(id) && !ArcanaItem.MASTERY_ADVANCEMENT_ID.equals(id)) {
                unlockedArcanaCount++;
            }
        }

        // 2. Celestial zodiac circle & constellation links (replaces vanilla right-angle stair lines)
        ArcanaAdvancementRenderer.renderArcanaWheel(gui, sx, sy, this.root, this.widgets, unlockedMap);

        // 3. Advancement slots (icons & frames)
        this.root.draw(gui, sx, sy);

        // 4. Compact X / 22 progress counter in top-right of the viewport
        ArcanaAdvancementRenderer.renderProgressBadge(gui, this.minecraft.font, unlockedArcanaCount,
                ArcanaItem.ArcanaType.values().length);

        gui.pose().popPose();
        gui.disableScissor();
        ci.cancel();
    }
}
