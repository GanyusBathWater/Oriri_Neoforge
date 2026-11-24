package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Creative invisible technical block that emits a 3D floating room-filling fog volume centered
 * on the placed block's position (spreading equally up and down, even when suspended mid-air).
 * <p>
 * Respects solid room walls, floors, and ceilings so fog fills enclosed halls cleanly without
 * leaking into adjacent rooms, while dissolving smoothly in open air. Automatically converts
 * into chunk-attached {@link net.ganyusbathwater.oririmod.world.fog.FogAnchor}s via
 * {@link net.ganyusbathwater.oririmod.worldgen.processor.StructureFogProcessor}.
 */
public class VolumeFogSourceBlock extends FogSourceBlock {

    public VolumeFogSourceBlock(Properties properties) {
        super(properties);
    }

    /**
     * Horizontal spread radius (blocks) for density 1..5.
     */
    public static float getVolumeRadiusXZ(int density) {
        return 6.0F + density * 2.5F; // 8.5 .. 18.5 blocks
    }

    /**
     * Vertical half-height (blocks above AND below the placed emitter) for density 1..5.
     */
    public static float getVolumeHalfHeight(int density) {
        return 4.5F + density * 1.5F; // ±6.0 .. ±12.0 blocks (12..24 blocks total vertical span)
    }

    /**
     * Atmospheric playable optical density weight (0..1) for density 1..5.
     */
    public static float getVolumeAlphaForDensity(int density) {
        return 0.30F + density * 0.12F; // 0.42 .. 0.90
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!player.isCreative()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            FogColor nextColor = state.getValue(COLOR).next();
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(COLOR, nextColor), Block.UPDATE_CLIENTS);
                player.displayClientMessage(
                        Component.translatable("message.oririmod.fog_source.color",
                                Component.translatable("color.oririmod.fog." + nextColor.getSerializedName())
                                        .withStyle(nextColor.chatColor)),
                        true);
            }
        } else {
            int nextDensity = state.getValue(DENSITY) >= MAX_DENSITY ? MIN_DENSITY : state.getValue(DENSITY) + 1;
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(DENSITY, nextDensity), Block.UPDATE_CLIENTS);
                int radius = Math.round(getVolumeRadiusXZ(nextDensity));
                int halfH = Math.round(getVolumeHalfHeight(nextDensity));
                player.displayClientMessage(
                        Component.translatable("message.oririmod.volume_fog_source.density",
                                nextDensity, MAX_DENSITY, radius, halfH),
                        true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (context.isHoldingItem(ModBlocks.VOLUME_FOG_SOURCE.asItem())
                || context.isHoldingItem(ModBlocks.FOG_SOURCE.asItem()))
                ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return !useContext.getItemInHand().is(ModBlocks.VOLUME_FOG_SOURCE.asItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.oririmod.volume_fog_source.line1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.oririmod.volume_fog_source.line2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
