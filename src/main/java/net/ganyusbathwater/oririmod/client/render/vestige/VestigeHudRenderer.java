// file: `src/main/java/net/ganyusbathwater/oririmod/client/render/vestige/VestigeHudRenderer.java`
package net.ganyusbathwater.oririmod.client.render.vestige;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.effect.vestiges.DuellantCortextEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.MirrorOfTheVoidEffect;
import net.ganyusbathwater.oririmod.effect.vestiges.RelicOfThePastEffect;
import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.item.custom.vestiges.DuellantCortex;
import net.ganyusbathwater.oririmod.item.custom.vestiges.MirrorOfTheVoid;
import net.ganyusbathwater.oririmod.item.custom.vestiges.RelicOfThePast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import top.theillusivec4.curios.api.CuriosApi;

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

        int textX = x + BG_WIDTH + 4;
        int textY = y + (BG_HEIGHT / 2) - 4;

        String statusText = getStatusText(player, stack);
        if (!statusText.isEmpty()) {
            gg.drawString(Minecraft.getInstance().font, statusText, textX, textY, 0xFFFFFF, true);
            textY += 10;
        }

        String enemyText = getEnemyCountText(player, stack);
        if (!enemyText.isEmpty()) {
            gg.drawString(Minecraft.getInstance().font, enemyText, textX, textY, 0xFF5555, true);
        }
    }

    private static List<ItemStack> getActiveVestigesForHud(LocalPlayer player) {
        List<ItemStack> result = new ArrayList<>();

        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            inv.getCurios().forEach((slotId, handler) -> {
                var stacks = handler.getStacks();
                int slots = stacks.getSlots();

                for (int i = 0; i < slots; i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.isEmpty()) continue;
                    if (!(stack.getItem() instanceof VestigeItem)) continue;
                    if (VestigeItem.getUnlockedLevel(stack) <= 0) continue;
                    result.add(stack);
                }
            });
        });

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

    private static String getStatusText(LocalPlayer player, ItemStack stack) {
        int remainingSeconds = getRemainingCooldownSeconds(player, stack);
        if (remainingSeconds > 0) {
            if (stack.getItem() instanceof RelicOfThePast) {
                return Component.translatable("hud.oririmod.relic_of_the_past.cooldown", remainingSeconds).getString();
            }
            return Component.translatable("hud.oririmod.black_mirror.cooldown", remainingSeconds).getString();
        }

        if (stack.getItem() instanceof RelicOfThePast) {
            return Component.translatable("tooltip.oririmod.relic_of_the_past.ready").getString();
        }
        return Component.translatable("tooltip.oririmod.black_mirror.ready").getString();
    }

    private static int getRemainingCooldownSeconds(LocalPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return 0;

        if (stack.getItem() instanceof MirrorOfTheVoid) {
            return MirrorOfTheVoidEffect.getActiveCooldownSecondsForHud(player);
        }
        if (stack.getItem() instanceof RelicOfThePast) {
            return RelicOfThePastEffect.getActiveCooldownSecondsForHud(player);
        }

        return 0;
    }

    private static String getEnemyCountText(LocalPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof DuellantCortex)) return "";
        int hostiles = DuellantCortextEffect.countMonsters(player);
        return "Enemies: " + hostiles;
    }
}