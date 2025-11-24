// java
package net.ganyusbathwater.oririmod.client.vestige;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.*;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.item.custom.vestiges.DuellantCortex;
import net.ganyusbathwater.oririmod.item.custom.vestiges.MirrorOfTheBlackSun;
import net.ganyusbathwater.oririmod.item.custom.vestiges.RelicOfThePast;
import net.ganyusbathwater.oririmod.util.vestiges.ExtraInventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

import static net.ganyusbathwater.oririmod.client.render.MagicIndicatorRender.textureExists;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class VestigeHudRenderer {

    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/gui/vestige_background.png");

    private static final int BG_WIDTH = 24;
    private static final int BG_HEIGHT = 24;
    private static final int ICON_SPACING = 4;
    private static final int MARGIN_LEFT = 6;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        List<ItemStack> vestigeStacks = getActiveVestigesForHud(player);
        if (vestigeStacks.isEmpty()) return;

        GuiGraphics gg = event.getGuiGraphics();
        PoseStack pose = gg.pose();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int baseX = MARGIN_LEFT;
        int totalHeight = vestigeStacks.size() * BG_HEIGHT + (vestigeStacks.size() - 1) * ICON_SPACING;
        int baseY = (screenHeight - totalHeight) / 2;

        pose.pushPose();
        for (int i = 0; i < vestigeStacks.size(); i++) {
            ItemStack stack = vestigeStacks.get(i);
            int x = baseX;
            int y = baseY + i * (BG_HEIGHT + ICON_SPACING);
            renderSingleVestige(gg, player, stack, x, y);
        }
        pose.popPose();
    }

    private static void renderSingleVestige(GuiGraphics gg, LocalPlayer player, ItemStack stack, int x, int y) {
        drawBackground(gg, x, y);
        drawItemCentered(gg, stack, x, y);

        VestigeEffect effect = getEffectFromStack(stack);
        int level = getVestigeLevel(stack);

        int textX = x + BG_WIDTH + 4;
        int textY = y + (BG_HEIGHT / 2) - 4;

        String statusText = getStatusText(player, effect, stack, level);
        String dynamicText = getDynamicNumberText(player, effect, stack, level);

        if (!statusText.isEmpty()) {
            gg.drawString(Minecraft.getInstance().font, statusText, textX, textY, 0xFFFFFF, true);
            textY += 10;
        }
        if (!dynamicText.isEmpty()) {
            gg.drawString(Minecraft.getInstance().font, dynamicText, textX, textY, 0xAAAAAA, true);
            textY += 10;
        }

        String enemyText = getEnemyCountText(player, stack);
        if (!enemyText.isEmpty()) {
            gg.drawString(Minecraft.getInstance().font, enemyText, textX, textY, 0xFF5555, true);
        }
    }

    private static List<ItemStack> getActiveVestigesForHud(LocalPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : ExtraInventoryUtil.readExtraInventory(player)) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof VestigeItem)) continue;
            result.add(stack);
        }

        return result;
    }

    private static void drawBackground(GuiGraphics gg, int x, int y) {
        if (!textureExists(BG_TEXTURE)) return;
        gg.blit(BG_TEXTURE, x, y, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    private static void drawItemCentered(GuiGraphics gg, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        int iconSize = 16;
        int itemX = x + (BG_WIDTH - iconSize) / 2;
        int itemY = y + (BG_HEIGHT - iconSize) / 2;
        gg.renderItem(stack, itemX, itemY);
        gg.renderItemDecorations(mc.font, stack, itemX, itemY);
    }

    private static String getStatusText(LocalPlayer player, VestigeEffect effect, ItemStack stack, int level) {
        if (effect == null) return "";

        int remaining = getRemainingCooldownTicks(player, stack, level);
        if (remaining > 0) {
            return "CD: " + (remaining / 20) + "s";
        }
        return "Ready";
    }

    private static String getDynamicNumberText(LocalPlayer player, VestigeEffect effect, ItemStack stack, int level) {
        if (effect == null) return "";

        int cdTicks = getRemainingCooldownTicks(player, stack, level);
        if (cdTicks > 0) {
            return "";
        }
        return "";
    }

    private static VestigeEffect getEffectFromStack(ItemStack stack) {
        if (!(stack.getItem() instanceof VestigeItem vestigeItem)) return null;
        int unlocked = vestigeItem.getUnlockedLevel(stack);
        for (int lvl = unlocked; lvl >= 1; lvl--) {
            if (!vestigeItem.isLevelEnabled(stack, lvl)) continue;
            var list = vestigeItem.getEffectsForLevelPublic(lvl);
            if (list == null || list.isEmpty()) continue;
            return list.get(0);
        }
        return null;
    }

    private static int getVestigeLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof VestigeItem vestigeItem)) return 1;
        return vestigeItem.getUnlockedLevel(stack);
    }

    private static int getRemainingCooldownTicks(LocalPlayer player, ItemStack stack, int level) {
        if (player == null || level <= 0) return 0;

        ResourceLocation key = null;

        if (stack.getItem() instanceof MirrorOfTheBlackSun) {
            key = BlackMirrorEffect.KEY_MIRROR;
        } else if (stack.getItem() instanceof RelicOfThePast) {
            key = VestigeEffects.KEY_RELIC;
        }

        if (key == null) return 0;

        return VestigeCooldownManager.getRemaining(player, key, level);
    }

    private static String getEnemyCountText(LocalPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof DuellantCortex)) {
            return "";
        }

        int hostiles = DuellantCortexManager.getClientHostileCount();

        return "Enemies: " + hostiles;
    }
}