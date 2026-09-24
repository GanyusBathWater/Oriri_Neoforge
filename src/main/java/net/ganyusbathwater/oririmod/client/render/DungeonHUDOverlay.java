package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.network.packet.SyncDungeonTimePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

public class DungeonHUDOverlay {
    
    private static int ticksActive = -1;
    private static boolean isComplete = false;
    private static long lastClientTick = 0;
    private static String objectiveText = null;
    private static String progressText = null;

    public static void updateTime(SyncDungeonTimePayload payload) {
        ticksActive = payload.ticksActive();
        isComplete = payload.isComplete();
        lastClientTick = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
    }

    public static void updateObjective(net.ganyusbathwater.oririmod.network.packet.SyncDungeonTimePayload payload) {
        objectiveText = payload.objectiveText();
        if (objectiveText != null && objectiveText.isBlank()) {
            objectiveText = null;
        }
        progressText = payload.progressText();
        if (progressText != null && progressText.isBlank()) {
            progressText = null;
        }
    }

    public static void reset() {
        ticksActive = -1;
        objectiveText = null;
        progressText = null;
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || ticksActive < 0) return;
        
        // Hide if not in a dungeon dimension
        if (!mc.level.dimension().location().getPath().startsWith("dungeon_")) {
            ticksActive = -1;
            return;
        }

        int currentTicks = ticksActive;
        if (!isComplete) {
            long currentClientTick = mc.level.getGameTime();
            currentTicks += (int)(currentClientTick - lastClientTick);
        }

        int seconds = (currentTicks / 20) % 60;
        int minutes = (currentTicks / 1200) % 60;
        int hours = currentTicks / 72000;

        String timeStr;
        if (hours > 0) {
            timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeStr = String.format("%02d:%02d", minutes, seconds);
        }

        Font font = mc.font;
        int screenWidth = guiGraphics.guiWidth();
        
        int textWidth = font.width(timeStr);
        int x = (screenWidth - textWidth) / 2;
        int y = 10; // Top middle of the screen

        // Render sleek timer background
        int padding = 4;
        guiGraphics.fill(x - padding, y - padding, x + textWidth + padding, y + font.lineHeight + padding, 0xAA000000);
        
        int color = isComplete ? 0x55FF55 : 0xFFFFFF; // Green if complete, white otherwise
        guiGraphics.drawString(font, timeStr, x, y, color, true);

        // Render Objective Text on the left side
        if (objectiveText != null && !isComplete) {
            int objY = screenWidth / 4; // somewhere below top left
            int objPadding = 5;
            int maxTextWidth = 150; // wrap text if it's too long
            var lines = font.split(net.minecraft.network.chat.Component.literal(objectiveText).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), maxTextWidth);
            
            int totalHeight = lines.size() * font.lineHeight;
            int boxWidth = 0;
            for (var line : lines) {
                int w = font.width(line);
                if (w > boxWidth) boxWidth = w;
            }

            int objX = 10;
            objY = (guiGraphics.guiHeight() - totalHeight) / 2; // Center vertically on the left

            // Draw a subtle background for the objective
            guiGraphics.fill(objX - objPadding, objY - objPadding, objX + boxWidth + objPadding, objY + totalHeight + objPadding, 0x88000000);
            
            int currentY = objY;
            for (var line : lines) {
                guiGraphics.drawString(font, line, objX, currentY, 0xFFFFFF, true);
                currentY += font.lineHeight;
            }
        }

        // Render Progress Text on the left side, slightly below where the objective would be
        if (progressText != null && !isComplete) {
            int objY = screenWidth / 4;
            int objPadding = 5;
            int maxTextWidth = 150;
            var lines = font.split(net.minecraft.network.chat.Component.literal(progressText).withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD), maxTextWidth);

            int totalHeight = lines.size() * font.lineHeight;
            int boxWidth = 0;
            for (var line : lines) {
                int w = font.width(line);
                if (w > boxWidth) boxWidth = w;
            }

            int objX = 10;
            // If objective text is rendered, push this down below it
            if (objectiveText != null) {
                var objLines = font.split(net.minecraft.network.chat.Component.literal(objectiveText).withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.ITALIC), maxTextWidth);
                int objHeight = objLines.size() * font.lineHeight;
                objY = (guiGraphics.guiHeight() - objHeight) / 2 + objHeight + 10;
            } else {
                objY = (guiGraphics.guiHeight() - totalHeight) / 2;
            }

            guiGraphics.fill(objX - objPadding, objY - objPadding, objX + boxWidth + objPadding, objY + totalHeight + objPadding, 0x88000000);
            
            int currentY = objY;
            for (var line : lines) {
                guiGraphics.drawString(font, line, objX, currentY, 0xFFFFFF, true);
                currentY += font.lineHeight;
            }
        }
    }
}
