package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.fluid.ModFluidTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

@EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
public final class ModFluidTypeClient {
    private static final ResourceLocation AETHER_STILL =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/aether_still");
    private static final ResourceLocation AETHER_FLOW =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/aether_flow");
    private static final ResourceLocation AETHER_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "block/aether_flow");

    private ModFluidTypeClient() {}

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return AETHER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return AETHER_FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return AETHER_OVERLAY; // optional, aber sicher für Renderpfade
            }
        }, ModFluidTypes.AETHER_TYPE.get());
    }
}