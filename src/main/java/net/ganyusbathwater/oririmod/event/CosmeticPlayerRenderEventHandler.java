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
    public static net.ganyusbathwater.oririmod.client.render.entity.MermaidCosmeticRenderer MERMAID_COSMETIC_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.MermaidHeadfinsRenderer MERMAID_HEADFINS_RENDERER;

    private static boolean hasCurioEquipped(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(item).isPresent())
                .orElse(false);
    }



    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.isSpectator()) return;
        
        if (hasCurioEquipped(player, ModItems.ESSENCE_OF_DARKNESS.get())) {
            // Cancel the Vanilla render event completely to hide Vanilla items and body.
            event.setCanceled(true);
            
            if (AURORA_COSMETIC_RENDERER != null) {
                float partialTick = event.getPartialTick();
                float entityYaw = net.minecraft.util.Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
                
                float f5 = 0.0F;
                float f8 = 0.0F;
                if (!player.isPassenger() && player.isAlive()) {
                    f5 = player.walkAnimation.speed(partialTick);
                    f8 = player.walkAnimation.position(partialTick);
                    if (f5 > 1.0F) f5 = 1.0F;
                }
                float netHeadYaw = net.minecraft.util.Mth.lerp(partialTick, player.yHeadRotO, player.yHeadRot) - entityYaw;
                float headPitch = net.minecraft.util.Mth.lerp(partialTick, player.xRotO, player.getXRot());
                float ageInTicks = player.tickCount + partialTick;
                
                // Manually populate vanilla bone rotations so we can copy them
                net.minecraft.client.player.AbstractClientPlayer cp = (net.minecraft.client.player.AbstractClientPlayer) player;
                net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> pModel = event.getRenderer().getModel();
                
                pModel.attackTime = player.getAttackAnim(partialTick);
                pModel.riding = player.isPassenger();
                pModel.young = player.isBaby();
                pModel.crouching = player.isCrouching();
                
                net.minecraft.client.model.HumanoidModel.ArmPose mainPose = net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel.getArmPose(cp, net.minecraft.world.InteractionHand.MAIN_HAND);
                net.minecraft.client.model.HumanoidModel.ArmPose offPose = net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel.getArmPose(cp, net.minecraft.world.InteractionHand.OFF_HAND);
                if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                    pModel.rightArmPose = mainPose;
                    pModel.leftArmPose = offPose;
                } else {
                    pModel.rightArmPose = offPose;
                    pModel.leftArmPose = mainPose;
                }
                
                pModel.setupAnim(cp, f8, f5, ageInTicks, netHeadYaw, headPitch);
                
                // Render our cosmetic as the root renderer
                AURORA_COSMETIC_RENDERER.render((net.minecraft.client.player.AbstractClientPlayer) player, entityYaw, partialTick, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            }
        }
        
        if (hasCurioEquipped(player, ModItems.MERMAID_SCALE.get())) {
            boolean inWater = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType)) || player.isVisuallySwimming();
            
            if (inWater) {
                // Hide the vanilla legs only in water
                event.getRenderer().getModel().rightLeg.visible = false;
                event.getRenderer().getModel().leftLeg.visible = false;
                event.getRenderer().getModel().rightPants.visible = false;
                event.getRenderer().getModel().leftPants.visible = false;
            }
            
            if (MERMAID_COSMETIC_RENDERER != null) {
                float partialTick = event.getPartialTick();
                float entityYaw = net.minecraft.util.Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot);
                
                float f5 = 0.0F;
                float f8 = 0.0F;
                if (!player.isPassenger() && player.isAlive()) {
                    f5 = player.walkAnimation.speed(partialTick);
                    f8 = player.walkAnimation.position(partialTick);
                    if (f5 > 1.0F) f5 = 1.0F;
                }
                float netHeadYaw = net.minecraft.util.Mth.lerp(partialTick, player.yHeadRotO, player.yHeadRot) - entityYaw;
                float headPitch = net.minecraft.util.Mth.lerp(partialTick, player.xRotO, player.getXRot());
                float ageInTicks = player.tickCount + partialTick;
                
                event.getRenderer().getModel().setupAnim((net.minecraft.client.player.AbstractClientPlayer) player, f8, f5, ageInTicks, netHeadYaw, headPitch);
            }
        }
    }
    
    @SubscribeEvent
    public static void onAddLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        if (MERMAID_COSMETIC_RENDERER == null) {
            MERMAID_COSMETIC_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.MermaidCosmeticRenderer(
                new net.minecraft.client.renderer.entity.EntityRendererProvider.Context(
                    net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher(),
                    net.minecraft.client.Minecraft.getInstance().getItemRenderer(),
                    net.minecraft.client.Minecraft.getInstance().getBlockRenderer(),
                    net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer(),
                    net.minecraft.client.Minecraft.getInstance().getResourceManager(),
                    net.minecraft.client.Minecraft.getInstance().getEntityModels(),
                    net.minecraft.client.Minecraft.getInstance().font
                )
            );
        }
        
        if (AURORA_COSMETIC_RENDERER == null) {
            AURORA_COSMETIC_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.AuroraCosmeticRenderer(
                new net.minecraft.client.renderer.entity.EntityRendererProvider.Context(
                    net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher(),
                    net.minecraft.client.Minecraft.getInstance().getItemRenderer(),
                    net.minecraft.client.Minecraft.getInstance().getBlockRenderer(),
                    net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer(),
                    net.minecraft.client.Minecraft.getInstance().getResourceManager(),
                    net.minecraft.client.Minecraft.getInstance().getEntityModels(),
                    net.minecraft.client.Minecraft.getInstance().font
                )
            );
        }

        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer<net.minecraft.client.player.AbstractClientPlayer, net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>> renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new net.ganyusbathwater.oririmod.client.render.layer.MermaidCosmeticLayer(playerRenderer));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderArm(net.neoforged.neoforge.client.event.RenderArmEvent event) {
        Player player = event.getPlayer();
        if (player.isSpectator()) return;
        
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
                animationState.setData(net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel.FIRST_PERSON, true);
                
                model.addAdditionalStateData(animatable, instanceId, animationState::setData);
                model.handleAnimations(animatable, instanceId, animationState, partialTick);
                
                software.bernie.geckolib.cache.object.GeoBone armBone = model.getAnimationProcessor().getBone(boneName);
                
                if (armBone != null) {
                    armBone.setRotX(0);
                    armBone.setRotY(0);
                    armBone.setRotZ(0);

                    com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
                    poseStack.pushPose();
                    
                    float vanillaX = isRightArm ? -0.3125f : 0.3125f; 
                    float vanillaY = 0.125f; 
                    float vanillaZ = 0.0f;
                    poseStack.translate(vanillaX, vanillaY, vanillaZ);
                    
                    poseStack.scale(-1.0f, -1.0f, 1.0f);
                    
                    float pivotX = armBone.getPivotX() / 16.0f;
                    float pivotY = armBone.getPivotY() / 16.0f;
                    float pivotZ = armBone.getPivotZ() / 16.0f;
                    poseStack.translate(-pivotX, -pivotY, -pivotZ);
                    
                    net.minecraft.client.renderer.RenderType renderType = AURORA_COSMETIC_RENDERER.getRenderType(animatable, model.getTextureResource(animatable), event.getMultiBufferSource(), partialTick);
                    com.mojang.blaze3d.vertex.VertexConsumer buffer = event.getMultiBufferSource().getBuffer(renderType);
                    
                    AURORA_COSMETIC_RENDERER.preRender(poseStack, animatable, model.getBakedModel(model.getModelResource(animatable)), event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    
                    AURORA_COSMETIC_RENDERER.renderRecursively(poseStack, animatable, armBone, renderType, event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    
                    poseStack.popPose();
                }
            }
        }
        
        if (hasCurioEquipped(player, ModItems.MERMAID_SCALE.get())) {
            if (MERMAID_COSMETIC_RENDERER != null) {
                boolean isRightArm = event.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
                String boneName = isRightArm ? "right_arm" : "left_arm";
                
                net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable animatable = MERMAID_COSMETIC_RENDERER.getAnimatable();
                software.bernie.geckolib.model.GeoModel<net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable> model = MERMAID_COSMETIC_RENDERER.getGeoModel();
                
                float partialTick = net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
                long instanceId = player.getId();
                software.bernie.geckolib.animation.AnimationState<net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable> animationState = 
                    new software.bernie.geckolib.animation.AnimationState<>(animatable, 0, 0, partialTick, false);
                animationState.setData(software.bernie.geckolib.constant.DataTickets.TICK, animatable.getTick(player));
                animationState.setData(software.bernie.geckolib.constant.DataTickets.ENTITY, player);
                animationState.setData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA, new software.bernie.geckolib.model.data.EntityModelData(false, false, 0, 0));
                animationState.setData(net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticModel.FIRST_PERSON, true);
                
                model.addAdditionalStateData(animatable, instanceId, animationState::setData);
                model.handleAnimations(animatable, instanceId, animationState, partialTick);
                
                software.bernie.geckolib.cache.object.GeoBone armBone = model.getAnimationProcessor().getBone(boneName);
                
                if (armBone != null) {
                    armBone.setRotX(0);
                    armBone.setRotY(0);
                    armBone.setRotZ(0);

                    com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
                    poseStack.pushPose();
                    
                    float vanillaX = isRightArm ? -0.3125f : 0.3125f; 
                    float vanillaY = 0.125f; 
                    float vanillaZ = 0.0f;
                    poseStack.translate(vanillaX, vanillaY, vanillaZ);
                    
                    poseStack.scale(-1.0f, -1.0f, 1.0f);
                    
                    float pivotX = armBone.getPivotX() / 16.0f;
                    float pivotY = armBone.getPivotY() / 16.0f;
                    float pivotZ = armBone.getPivotZ() / 16.0f;
                    poseStack.translate(-pivotX, -pivotY, -pivotZ);
                    
                    net.minecraft.client.renderer.RenderType renderType = MERMAID_COSMETIC_RENDERER.getRenderType(animatable, model.getTextureResource(animatable), event.getMultiBufferSource(), partialTick);
                    com.mojang.blaze3d.vertex.VertexConsumer buffer = event.getMultiBufferSource().getBuffer(renderType);
                    
                    int rawColor = CuriosApi.getCuriosInventory(player).map(inv -> {
                        return inv.findFirstCurio(ModItems.MERMAID_SCALE.get()).map(slotResult -> {
                            net.minecraft.world.item.ItemStack stack = slotResult.stack();
                            net.minecraft.world.item.DyeColor baseColor = stack.get(net.minecraft.core.component.DataComponents.BASE_COLOR);
                            if (baseColor != null) {
                                return baseColor.getTextureDiffuseColor();
                            }
                            return 0xFFFFFF;
                        }).orElse(0xFFFFFF);
                    }).orElse(0xFFFFFF);
                    
                    int color = software.bernie.geckolib.util.Color.ofOpaque(rawColor).getColor();
                    
                    MERMAID_COSMETIC_RENDERER.preRender(poseStack, animatable, model.getBakedModel(model.getModelResource(animatable)), event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
                    MERMAID_COSMETIC_RENDERER.renderRecursively(poseStack, animatable, armBone, renderType, event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
                    
                    poseStack.popPose();
                }
            }
        }
    }
}
