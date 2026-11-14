package net.ganyusbathwater.oririmod.client.render.world;

import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;

/**
 * Samples local block light, held/equipped light sources (including {@link ModItems#MINERS_LANTERN}),
 * and forward line-of-sight light beacons so abyssal darkness recedes around illumination.
 */
final class AbyssalLightProbe {

    private static final Map<Item, Integer> HANDHELD_ITEM_LIGHT = Map.of(
            Items.LAVA_BUCKET, 15,
            Items.BLAZE_ROD, 10,
            Items.GLOWSTONE_DUST, 8,
            Items.PRISMARINE_CRYSTALS, 8,
            Items.GLOW_INK_SAC, 7,
            Items.GLOW_BERRIES, 7,
            Items.MAGMA_CREAM, 7,
            Items.SPECTRAL_ARROW, 6
    );

    private static final int[][] LOCAL_OFFSETS = {
            {0, 0, 0},
            {2, 0, 0}, {-2, 0, 0}, {0, 2, 0}, {0, -2, 0}, {0, 0, 2}, {0, 0, -2},
            {3, 1, 0}, {-3, 1, 0}, {0, 1, 3}, {0, 1, -3}
    };

    private static final float[] CONE_YAW_OFFSETS_RAD = {
            0.0F, 0.16F, -0.16F
    };

    private AbyssalLightProbe() {
    }

    static LightSample sample(ClientLevel level, Player player, Camera camera, BlockPos camPos) {
        int heldLevel = computeHeldLightLevel(level, player, camPos);
        int localLevel = sampleLocalBlockLight(level, camPos);
        float gazeStrength = sampleGazeConeLight(level, camera);

        float heldStrength = heldLevel / 15.0F;
        float localStrength = Mth.clamp((localLevel - 1.0F) / 14.0F, 0.0F, 1.0F);
        float combinedRelief = Mth.clamp(
                Math.max(Math.max(localStrength, heldStrength * 0.55F), gazeStrength * 0.72F),
                0.0F, 1.0F);

        return new LightSample(heldStrength, combinedRelief);
    }

    private static int computeHeldLightLevel(ClientLevel level, Player player, BlockPos pos) {
        int best = player.isOnFire() ? 12 : 0;

        best = Math.max(best, getStackLightEmission(level, pos, player.getMainHandItem()));
        best = Math.max(best, getStackLightEmission(level, pos, player.getOffhandItem()));
        best = Math.max(best, getStackLightEmission(level, pos, player.getItemBySlot(EquipmentSlot.HEAD)));

        Item minersLantern = ModItems.MINERS_LANTERN.get();
        if (player.getMainHandItem().is(minersLantern) || player.getOffhandItem().is(minersLantern)) {
            best = Math.max(best, 12);
        } else {
            boolean curioLantern = CuriosApi.getCuriosInventory(player)
                    .map(inv -> inv.findFirstCurio(minersLantern).isPresent())
                    .orElse(false);
            if (curioLantern) {
                best = Math.max(best, 12);
            }
        }
        return best;
    }

    private static int getStackLightEmission(ClientLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getLightEmission(level, pos);
        }
        return HANDHELD_ITEM_LIGHT.getOrDefault(item, 0);
    }

    private static int sampleLocalBlockLight(ClientLevel level, BlockPos camPos) {
        int maxLight = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int[] offset : LOCAL_OFFSETS) {
            cursor.set(camPos.getX() + offset[0], camPos.getY() + offset[1], camPos.getZ() + offset[2]);
            int bl = level.getBrightness(LightLayer.BLOCK, cursor);
            if (bl > maxLight) {
                maxLight = bl;
                if (maxLight == 15) {
                    break;
                }
            }
        }
        return maxLight;
    }

    private static float sampleGazeConeLight(ClientLevel level, Camera camera) {
        Vec3 camPos = camera.getPosition();
        Vector3f lookVec = camera.getLookVector();
        Vec3 baseLook = new Vec3(lookVec.x(), lookVec.y(), lookVec.z());

        float best = 0.0F;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (float yawOffset : CONE_YAW_OFFSETS_RAD) {
            Vec3 dir = yawOffset == 0.0F ? baseLook : baseLook.yRot(yawOffset);
            for (int step = 1; step <= 7; step++) {
                double dist = step * 3.0;
                cursor.set(
                        Mth.floor(camPos.x + dir.x * dist),
                        Mth.floor(camPos.y + dir.y * dist),
                        Mth.floor(camPos.z + dir.z * dist)
                );
                BlockState state = level.getBlockState(cursor);
                if (state.isSolidRender(level, cursor)) {
                    break;
                }
                int bl = level.getBrightness(LightLayer.BLOCK, cursor);
                if (bl >= 4) {
                    float brightnessNorm = Mth.clamp((bl - 3.0F) / 12.0F, 0.0F, 1.0F);
                    float distWeight = 1.0F - ((float) dist / 26.0F);
                    best = Math.max(best, brightnessNorm * distWeight);
                }
            }
        }
        return Mth.clamp(best, 0.0F, 1.0F);
    }

    record LightSample(float heldStrength, float relief) {
    }
}

