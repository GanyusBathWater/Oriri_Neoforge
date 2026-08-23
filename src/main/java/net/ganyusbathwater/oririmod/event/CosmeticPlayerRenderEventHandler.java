package net.ganyusbathwater.oririmod.event;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.client.render.entity.AuroraCosmeticRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public class CosmeticPlayerRenderEventHandler {

    public static AuroraCosmeticRenderer AURORA_COSMETIC_RENDERER;

    private static boolean hasCurioEquipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(item).isPresent())
                .orElse(false);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        AURORA_COSMETIC_RENDERER = new AuroraCosmeticRenderer(event.getContext());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (hasCurioEquipped(player, ModItems.ESSENCE_OF_DARKNESS.get())) {
            event.setCanceled(true);
            if (AURORA_COSMETIC_RENDERER != null) {
                float partialTick = event.getPartialTick();
                // We use player.getYRot() for the entityYaw argument since the event doesn't provide it directly in 1.21.1
                float entityYaw = net.minecraft.util.Mth.lerp(partialTick, player.yRotO, player.getYRot());
                AURORA_COSMETIC_RENDERER.render((net.minecraft.client.player.AbstractClientPlayer) player, entityYaw, partialTick, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderArm(net.neoforged.neoforge.client.event.RenderArmEvent event) {
        Player player = event.getPlayer();
        if (hasCurioEquipped(player, ModItems.ESSENCE_OF_DARKNESS.get())) {
            event.setCanceled(true);
            if (AURORA_COSMETIC_RENDERER != null) {
                boolean isRightArm = event.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
                String boneName = isRightArm ? "right_arm" : "left_arm";
                
                net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable animatable = AURORA_COSMETIC_RENDERER.getAnimatable();
                software.bernie.geckolib.model.GeoModel<net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable> model = AURORA_COSMETIC_RENDERER.getGeoModel();
                
                float partialTick = net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
                long instanceId = player.getId();
                software.bernie.geckolib.animation.AnimationState<net.ganyusbathwater.oririmod.entity.custom.cosmetic.AuroraCosmeticAnimatable> animationState = 
                    new software.bernie.geckolib.animation.AnimationState<>(animatable, 0, 0, partialTick, false);
                animationState.setData(software.bernie.geckolib.constant.DataTickets.TICK, animatable.getTick(player));
                animationState.setData(software.bernie.geckolib.constant.DataTickets.ENTITY, player);
                animationState.setData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA, new software.bernie.geckolib.model.data.EntityModelData(false, false, 0, 0));
                animationState.setData(net.ganyusbathwater.oririmod.client.render.entity.model.AuroraCosmeticModel.FIRST_PERSON, true);
                
                model.addAdditionalStateData(animatable, instanceId, animationState::setData);
                model.handleAnimations(animatable, instanceId, animationState, partialTick);
                
                software.bernie.geckolib.cache.object.GeoBone armBone = model.getAnimationProcessor().getBone(boneName);
                
                if (armBone != null) {
                    // Disable all animations (both Vanilla copied and GeckoLib custom) in first person.
                    // Vanilla's PoseStack already applies the correct view bobbing and swing.
                    armBone.setRotX(0);
                    armBone.setRotY(0);
                    armBone.setRotZ(0);

                    com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
                    poseStack.pushPose();
                    
                    // The first-person poseStack from ItemInHandRenderer is heavily rotated, 
                    // and scaled by (-1, -1, 1) to force a Y-down, X-left coordinate system.
                    // This causes GeckoLib models to render mirrored and upside down.
                    
                    // 1. First, move the poseStack EXACTLY to the Vanilla shoulder pivot in Vanilla's space.
                    float vanillaX = isRightArm ? -0.3125f : 0.3125f; 
                    float vanillaY = 0.125f; 
                    float vanillaZ = 0.0f;
                    poseStack.translate(vanillaX, vanillaY, vanillaZ);
                    
                    // 2. Undo Vanilla's coordinate flip. This returns us to GeckoLib's native Y-up, X-right space!
                    // This fixes the "mirrored" texture issue and automatically corrects the upside-down direction!
                    poseStack.scale(-1.0f, -1.0f, 1.0f);
                    
                    // 3. Since we are already perfectly positioned at the shoulder, we pre-cancel 
                    // GeckoLib's automatic pivot translation (which will run during renderRecursively).
                    float pivotX = armBone.getPivotX() / 16.0f;
                    float pivotY = armBone.getPivotY() / 16.0f;
                    float pivotZ = armBone.getPivotZ() / 16.0f;
                    poseStack.translate(-pivotX, -pivotY, -pivotZ);
                    
                    net.minecraft.client.renderer.RenderType renderType = AURORA_COSMETIC_RENDERER.getRenderType(animatable, model.getTextureResource(animatable), event.getMultiBufferSource(), net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaTicks());
                    com.mojang.blaze3d.vertex.VertexConsumer buffer = event.getMultiBufferSource().getBuffer(renderType);
                    
                    AURORA_COSMETIC_RENDERER.renderRecursively(poseStack, animatable, armBone, renderType, event.getMultiBufferSource(), buffer, false, net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaTicks(), event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    
                    poseStack.popPose();
                }
            }
        }
    }
}
