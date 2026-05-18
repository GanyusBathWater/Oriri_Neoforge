package net.ganyusbathwater.oririmod.events;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.BlizzaEntity;
import net.ganyusbathwater.oririmod.entity.custom.DeviartrasEntity;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation TEX_0 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/mana_0.png");
    private static final ResourceLocation TEX_25 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/mana_25.png");
    private static final ResourceLocation TEX_50 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/mana_50.png");
    private static final ResourceLocation TEX_75 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/mana_75.png");
    private static final ResourceLocation TEX_100 = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/mana_100.png");

    // ── Blizza boss bar textures ──────────────────────────────────────────
    private static final ResourceLocation BLIZZA_BAR = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/blizza_boss_bar.png");
    private static final ResourceLocation BLIZZA_PROGRESS = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/blizza_progress.png");

    // ── Deviartras boss bar textures ──────────────────────────────────────
    private static final ResourceLocation DEVIARTRAS_BAR = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/deviartras_boss_bar.png");
    private static final ResourceLocation DEVIARTRAS_PROGRESS = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID,
            "textures/gui/deviartras_progress.png");

    // Boss bar dimensions (blizza_boss_bar.png is 200x20)
    private static final int BAR_W  = 200;
    private static final int BAR_H  = 20;
    // Progress bar dimensions (blizza_progress.png fits inside the custom bar)
    private static final int PROG_W = 182;
    private static final int PROG_H = 5;

    // ── Spawn title state ─────────────────────────────────────────────────
    /** Stores the end time (ms) for the Blizza title. Set externally by the network packet handler. */
    public static long blizzaTitleEndTime = 0;
    /** Stores the end time (ms) for the Deviartras title. */
    public static long deviartrasTitleEndTime = 0;
    private static final long TITLE_TOTAL_MS = 4000;
    private static final long TITLE_FADE_MS  = 500;

    /** Called on the client thread by the BlizzaSpawnTitlePayload handler. */
    public static void triggerBlizzaTitle() {
        blizzaTitleEndTime = System.currentTimeMillis() + TITLE_TOTAL_MS;
    }

    /** Called on the client thread by the DeviartrasSpawnTitlePayload handler. */
    public static void triggerDeviartrasTitle() {
        deviartrasTitleEndTime = System.currentTimeMillis() + TITLE_TOTAL_MS;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        if (mc.options.hideGui)
            return;

        Integer manaOpt = ModManaUtil.getManaIfPresent(player);
        Integer maxOpt = ModManaUtil.getMaxManaIfPresent(player);

        String manaText;
        int percentForTexture;
        if (manaOpt == null || maxOpt == null) {
            manaText = "syncing...";
            percentForTexture = 100;
        } else {
            int mana = manaOpt;
            int max = Math.max(1, maxOpt);
            manaText = mana + " / " + max;
            percentForTexture = (int) Math.round((mana * 100.0) / max);
        }
        ResourceLocation tex = selectTextureByPercent(percentForTexture);

        int x = 10;
        int y = mc.getWindow().getGuiScaledHeight() - 38;
        int size = 32;

        RenderSystem.enableBlend();
        gui.blit(tex, x, y, 0, 0, size, size, size, size);
        RenderSystem.disableBlend();

        int textX = x + size + 6;
        int textY = y + (size / 2) - 4;
        gui.drawString(mc.font, Component.literal(manaText), textX, textY, 0xFFFFFF, true);

        // ── Eye of the Storm: Blinding Blizzard Overlay ──
        long lastBlizz = player.getPersistentData().getLong("LastBlizzardTick");
        long lastSafeZn = player.getPersistentData().getLong("LastBlizzardSafeZoneTick");
        long gameTime = player.level().getGameTime();
        
        boolean isSafe = (gameTime - lastSafeZn <= 2);
        boolean isExp = (gameTime - lastBlizz <= 2);

        if (isExp || isSafe) {
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            RenderSystem.enableBlend();
            
            if (isExp) {
                // Solid Blinding White Fog background (~70% Opacity) exclusively for EXPOSED players
                gui.fill(0, 0, width, height, 0xB0FFFFFF);
            }

            // Falling Snow Cascade Simulator
            ResourceLocation SNOW_TEX = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
            
            // If strictly inside the magic circle safe zone, just show highly transparent falling snow to establish atmosphere! 
            float alphaOverlay = isExp ? 0.9f : 0.2f; 
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alphaOverlay);
            
            int texSize = 512;
            int mult = isExp ? 30 : 5; // Snow falls aggressively if exposed, lightly if safe
            int vOffset = (int) ((gameTime * mult) % texSize); 
            float uOffsetBase = isExp ? (float) (Math.sin(gameTime / 20.0) * 16.0) : 0f; // Lateral sway only for exposed players

            for (int screenX = 0; screenX < width; screenX += texSize) {
                for (int screenY = -texSize; screenY < height; screenY += texSize) {
                    gui.blit(SNOW_TEX, screenX, screenY + vOffset, (int) uOffsetBase, 0, texSize, texSize, texSize, texSize);
                }
            }
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset
        }

        // ── Custom boss bars ──────────────────────────────────────────────
        int nextY = 8;
        nextY = renderBlizzaBossBar(gui, mc, player, nextY);
        nextY = renderDeviartrasBossBar(gui, mc, player, nextY);

        // ── Blizza spawn title overlay ────────────────────────────────────
        renderBlizzaTitle(gui, mc);

        // ── Deviartras spawn title overlay ────────────────────────────
        renderDeviartrasTitle(gui, mc);
    }

    // ── Boss bar renderer ─────────────────────────────────────────────────
    private static int renderBlizzaBossBar(GuiGraphics gui, Minecraft mc, Player player, int startY) {
        BlizzaEntity blizza = null;
        double closestDist = 200.0 * 200.0;
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(200.0);
        for (BlizzaEntity b : player.level().getEntitiesOfClass(BlizzaEntity.class, searchBox, e -> e.isAlive())) {
            double d = player.distanceToSqr(b);
            if (d < closestDist) {
                closestDist = d;
                blizza = b;
            }
        }
        if (blizza == null) return startY;

        float healthFraction = Math.max(0f, Math.min(1f, blizza.getHealthFraction()));
        int screenW = mc.getWindow().getGuiScaledWidth();
        int barX = (screenW - BAR_W) / 2;
        int barY = startY;
        int progOffX = (BAR_W - PROG_W) / 2;
        int progOffY = ((BAR_H - PROG_H) / 2) + 1; // shifted 1 pixel down as requested
        int filledW = (int) (PROG_W * healthFraction);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Layer 1: background
        gui.blit(BLIZZA_BAR, barX, barY, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);
        // Layer 2: health fill (clipped)
        if (filledW > 0) {
            gui.blit(BLIZZA_PROGRESS, barX + progOffX, barY + progOffY, 0, 0, filledW, PROG_H, PROG_W, PROG_H);
        }
        // Layer 3: frame on top
        gui.blit(BLIZZA_BAR, barX, barY, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);

        RenderSystem.disableBlend();

        String bossName = blizza.getDisplayName().getString();
        int nameW = mc.font.width(bossName);
        gui.drawString(mc.font, Component.literal(bossName), (screenW - nameW) / 2, barY + BAR_H + 2, 0xFFFFFF, true);
        
        return startY + BAR_H + mc.font.lineHeight + 6;
    }

    private static int renderDeviartrasBossBar(GuiGraphics gui, Minecraft mc, Player player, int startY) {
        DeviartrasEntity deviartras = null;
        double closestDist = 200.0 * 200.0;
        net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(200.0);
        for (DeviartrasEntity b : player.level().getEntitiesOfClass(DeviartrasEntity.class, searchBox, e -> e.isAlive())) {
            double d = player.distanceToSqr(b);
            if (d < closestDist) {
                closestDist = d;
                deviartras = b;
            }
        }
        if (deviartras == null) return startY;

        float healthFraction = Math.max(0f, Math.min(1f, deviartras.getHealthFraction()));
        int screenW = mc.getWindow().getGuiScaledWidth();
        int barX = (screenW - BAR_W) / 2;
        int barY = startY; 
        int progOffX = (BAR_W - PROG_W) / 2;
        int progOffY = ((BAR_H - PROG_H) / 2) + 1; // shifted 1 pixel down as requested
        int filledW = (int) (PROG_W * healthFraction);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Layer 1: background
        gui.blit(DEVIARTRAS_BAR, barX, barY, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);
        // Layer 2: health fill (clipped)
        if (filledW > 0) {
            gui.blit(DEVIARTRAS_PROGRESS, barX + progOffX, barY + progOffY, 0, 0, filledW, PROG_H, PROG_W, PROG_H);
        }
        // Layer 3: frame on top
        gui.blit(DEVIARTRAS_BAR, barX, barY, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);

        RenderSystem.disableBlend();

        String bossName = deviartras.getDisplayName().getString();
        int nameW = mc.font.width(bossName);
        gui.drawString(mc.font, Component.literal(bossName), (screenW - nameW) / 2, barY + BAR_H + 2, 0xFFFFFF, true);
        
        return startY + BAR_H + mc.font.lineHeight + 6;
    }

    // ── Spawn title renderer ──────────────────────────────────────────────
    private static void renderBlizzaTitle(GuiGraphics gui, Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now > blizzaTitleEndTime) return;

        long remaining = blizzaTitleEndTime - now;
        float alpha;
        if (remaining > TITLE_TOTAL_MS - TITLE_FADE_MS) {
            alpha = 1f - ((remaining - (TITLE_TOTAL_MS - TITLE_FADE_MS)) / (float) TITLE_FADE_MS);
        } else if (remaining < TITLE_FADE_MS) {
            alpha = remaining / (float) TITLE_FADE_MS;
        } else {
            alpha = 1f;
        }

        int screenW  = mc.getWindow().getGuiScaledWidth();
        int screenH  = mc.getWindow().getGuiScaledHeight();
        int alphaInt = (int) (alpha * 255) << 24;

        String line1 = "Choir of Frozen Waves";
        String line2 = "Blizza the Sage of Water";
        int line1W   = mc.font.width(line1) * 2;
        int line2W   = mc.font.width(line2);
        int centerY  = screenH / 3;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gui.pose().pushPose();
        gui.pose().translate((screenW - line1W) / 2f, centerY - 10f, 0f);
        gui.pose().scale(2f, 2f, 1f);
        gui.drawString(mc.font, line1, 0, 0, 0xFFD700 | alphaInt, true);
        gui.pose().popPose();

        gui.drawString(mc.font, line2, (screenW - line2W) / 2, centerY + 16, 0xFFFFFF | alphaInt, true);

        RenderSystem.disableBlend();
    }

    // ── Deviartras spawn title renderer ────────────────────────────────
    private static void renderDeviartrasTitle(GuiGraphics gui, Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now > deviartrasTitleEndTime) return;

        long remaining = deviartrasTitleEndTime - now;
        float alpha;
        if (remaining > TITLE_TOTAL_MS - TITLE_FADE_MS) {
            alpha = 1f - ((remaining - (TITLE_TOTAL_MS - TITLE_FADE_MS)) / (float) TITLE_FADE_MS);
        } else if (remaining < TITLE_FADE_MS) {
            alpha = remaining / (float) TITLE_FADE_MS;
        } else {
            alpha = 1f;
        }

        int screenW  = mc.getWindow().getGuiScaledWidth();
        int screenH  = mc.getWindow().getGuiScaledHeight();
        int alphaInt = (int) (alpha * 255) << 24;

        String line1 = "Hatebound Dryad";
        String line2 = "Deviartras the Sage of Nature";
        int line1W   = mc.font.width(line1) * 2;
        int line2W   = mc.font.width(line2);
        int centerY  = screenH / 3;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Large title line in green
        gui.pose().pushPose();
        gui.pose().translate((screenW - line1W) / 2f, centerY - 10f, 0f);
        gui.pose().scale(2f, 2f, 1f);
        gui.drawString(mc.font, line1, 0, 0, 0x55FF55 | alphaInt, true);
        gui.pose().popPose();

        // Subtitle in gold
        gui.drawString(mc.font, line2, (screenW - line2W) / 2, centerY + 16, 0xFFD700 | alphaInt, true);

        RenderSystem.disableBlend();
    }

    private static ResourceLocation selectTextureByPercent(int percent) {
        if (percent <= 0)
            return TEX_0;
        if (percent <= 25)
            return TEX_25;
        if (percent <= 50)
            return TEX_50;
        if (percent <= 75)
            return TEX_75;
        return TEX_100;
    }

    @SubscribeEvent
    public static void onRegisterIClientItemExtensions(
            net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new net.ganyusbathwater.oririmod.client.render.item.JadeShieldRenderer();
                }
                return this.renderer;
            }
        }, net.ganyusbathwater.oririmod.item.ModItems.JADE_SHIELD.get());
    }
}