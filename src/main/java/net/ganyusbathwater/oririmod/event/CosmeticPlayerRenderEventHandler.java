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
    public static net.ganyusbathwater.oririmod.client.render.entity.KitsuneCosmeticRenderer KITSUNE_COSMETIC_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.LamiaTailRenderer LAMIA_TAIL_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.LamiaEarsRenderer LAMIA_EARS_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.ElvenEarsRenderer ELVEN_EARS_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.SilentNereidCosmeticRenderer SILENT_NEREID_COSMETIC_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.SilentNereidHeadfinsRenderer SILENT_NEREID_HEADFINS_RENDERER;
    public static net.ganyusbathwater.oririmod.client.render.entity.LizardTailCosmeticRenderer LIZARD_TAIL_COSMETIC_RENDERER;

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
                
                boolean isVisuallySwimming = player.isVisuallySwimming();
                pModel.crouching = player.isCrouching() && !isVisuallySwimming;
                float swimAmount = player.getSwimAmount(partialTick);
                if (isVisuallySwimming && swimAmount < 0.01F) {
                    swimAmount = 1.0F;
                }
                pModel.swimAmount = swimAmount;
                
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
            handleAquaticScaleRenderPre(event, player, MERMAID_COSMETIC_RENDERER != null);
        }

        if (hasCurioEquipped(player, ModItems.SILENT_NEREID_SCALE.get())) {
            handleAquaticScaleRenderPre(event, player, SILENT_NEREID_COSMETIC_RENDERER != null);
        }

        if (hasCurioEquipped(player, ModItems.LAMIA_SCALE.get())) {
            // Hide player legs permanently while Lamia essence is equipped
            event.getRenderer().getModel().rightLeg.visible = false;
            event.getRenderer().getModel().leftLeg.visible = false;
            event.getRenderer().getModel().rightPants.visible = false;
            event.getRenderer().getModel().leftPants.visible = false;
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

        if (KITSUNE_COSMETIC_RENDERER == null) {
            KITSUNE_COSMETIC_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.KitsuneCosmeticRenderer(
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

        if (LAMIA_TAIL_RENDERER == null) {
            LAMIA_TAIL_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.LamiaTailRenderer(
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

        if (LAMIA_EARS_RENDERER == null) {
            LAMIA_EARS_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.LamiaEarsRenderer(
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

        if (ELVEN_EARS_RENDERER == null) {
            ELVEN_EARS_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.ElvenEarsRenderer(
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

        if (SILENT_NEREID_COSMETIC_RENDERER == null) {
            SILENT_NEREID_COSMETIC_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.SilentNereidCosmeticRenderer(
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

        if (SILENT_NEREID_HEADFINS_RENDERER == null) {
            SILENT_NEREID_HEADFINS_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.SilentNereidHeadfinsRenderer(
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

        if (LIZARD_TAIL_COSMETIC_RENDERER == null) {
            LIZARD_TAIL_COSMETIC_RENDERER = new net.ganyusbathwater.oririmod.client.render.entity.LizardTailCosmeticRenderer(
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
                playerRenderer.addLayer(new net.ganyusbathwater.oririmod.client.render.layer.KitsuneCosmeticLayer(playerRenderer));
                playerRenderer.addLayer(new net.ganyusbathwater.oririmod.client.render.layer.LamiaCosmeticLayer(playerRenderer));
                playerRenderer.addLayer(new net.ganyusbathwater.oririmod.client.render.layer.ElvenEarsCosmeticLayer(playerRenderer));
                playerRenderer.addLayer(new net.ganyusbathwater.oririmod.client.render.layer.SilentNereidCosmeticLayer(playerRenderer));
                playerRenderer.addLayer(new net.ganyusbathwater.oririmod.client.render.layer.LizardTailCosmeticLayer(playerRenderer));
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
                software.bernie.geckolib.cache.object.BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(animatable));
                
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
                    armBone.setPosX(0);
                    armBone.setPosY(0);
                    armBone.setPosZ(0);

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
                    
                    AURORA_COSMETIC_RENDERER.preRender(poseStack, animatable, bakedModel, event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    
                    AURORA_COSMETIC_RENDERER.renderRecursively(poseStack, animatable, armBone, renderType, event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    
                    poseStack.popPose();
                }
            }
        }
        
        if (hasCurioEquipped(player, ModItems.MERMAID_SCALE.get())) {
            if (MERMAID_COSMETIC_RENDERER != null) {
                int rawColor = CuriosApi.getCuriosInventory(player).map(inv ->
                        inv.findFirstCurio(ModItems.MERMAID_SCALE.get()).map(slotResult -> {
                            net.minecraft.world.item.ItemStack stack = slotResult.stack();
                            net.minecraft.world.item.DyeColor baseColor = stack.get(net.minecraft.core.component.DataComponents.BASE_COLOR);
                            return baseColor != null ? baseColor.getTextureDiffuseColor() : 0xFFFFFF;
                        }).orElse(0xFFFFFF)
                ).orElse(0xFFFFFF);

                renderFirstPersonAquaticArmFin(
                        event, player, MERMAID_COSMETIC_RENDERER,
                        MERMAID_COSMETIC_RENDERER.getGeoModel(player),
                        null, rawColor
                );
            }
        }

        if (hasCurioEquipped(player, ModItems.SILENT_NEREID_SCALE.get())) {
            if (SILENT_NEREID_COSMETIC_RENDERER != null) {
                int rawColor = CuriosApi.getCuriosInventory(player).map(inv ->
                        inv.findFirstCurio(ModItems.SILENT_NEREID_SCALE.get()).map(slotResult ->
                                slotResult.stack().getOrDefault(
                                        net.ganyusbathwater.oririmod.item.component.ModDataComponents.SILENT_NEREID_COLORS.get(),
                                        net.ganyusbathwater.oririmod.item.component.SilentNereidColors.DEFAULT
                                ).secondary()
                        ).orElse(net.ganyusbathwater.oririmod.item.component.SilentNereidColors.DEFAULT.secondary())
                ).orElse(net.ganyusbathwater.oririmod.item.component.SilentNereidColors.DEFAULT.secondary());

                renderFirstPersonAquaticArmFin(
                        event, player, SILENT_NEREID_COSMETIC_RENDERER,
                        SILENT_NEREID_COSMETIC_RENDERER.getGeoModel(player),
                        net.ganyusbathwater.oririmod.client.render.entity.SilentNereidCosmeticRenderer.SCALE2_TEXTURE,
                        rawColor
                );
            }
        }
    }

    private static void handleAquaticScaleRenderPre(RenderPlayerEvent.Pre event, Player player, boolean rendererReady) {
        boolean inWater = player.isInWater() || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType)) || player.isVisuallySwimming();

        if (inWater) {
            // Hide the vanilla legs only in water
            event.getRenderer().getModel().rightLeg.visible = false;
            event.getRenderer().getModel().leftLeg.visible = false;
            event.getRenderer().getModel().rightPants.visible = false;
            event.getRenderer().getModel().leftPants.visible = false;
        }

        if (rendererReady) {
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

    private static void renderFirstPersonAquaticArmFin(
            net.neoforged.neoforge.client.event.RenderArmEvent event,
            Player player,
            net.ganyusbathwater.oririmod.client.render.entity.template.AbstractPlayerCosmeticRenderer<net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable> renderer,
            software.bernie.geckolib.model.GeoModel<net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable> model,
            net.minecraft.resources.ResourceLocation textureOverride,
            int rawColor) {
        boolean isRightArm = event.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        String boneName = isRightArm ? "right_arm" : "left_arm";

        net.ganyusbathwater.oririmod.entity.custom.cosmetic.MermaidCosmeticAnimatable animatable = renderer.getAnimatable();
        software.bernie.geckolib.cache.object.BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource(animatable));

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
            armBone.setPosX(0);
            armBone.setPosY(0);
            armBone.setPosZ(0);

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

            net.minecraft.resources.ResourceLocation tex = textureOverride != null ? textureOverride : model.getTextureResource(animatable);
            net.minecraft.client.renderer.RenderType renderType = textureOverride != null
                    ? net.minecraft.client.renderer.RenderType.entityCutoutNoCull(tex)
                    : renderer.getRenderType(animatable, tex, event.getMultiBufferSource(), partialTick);
            com.mojang.blaze3d.vertex.VertexConsumer buffer = event.getMultiBufferSource().getBuffer(renderType);

            int color = software.bernie.geckolib.util.Color.ofOpaque(rawColor).getColor();

            renderer.preRender(poseStack, animatable, bakedModel, event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            renderer.renderRecursively(poseStack, animatable, armBone, renderType, event.getMultiBufferSource(), buffer, false, partialTick, event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);

            poseStack.popPose();
        }
    }
}
