package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.custom.ElderwoodsPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class ElderwoodsIgniterItem extends Item {
    public ElderwoodsIgniterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(ModBlocks.MANA_CRYSTAL_BLOCK.get())) {
            BlockPos portalPos = pos.relative(context.getClickedFace());
            if (level.isEmptyBlock(portalPos)) {
                // Try X axis
                ElderwoodsPortalBlock.PortalShape shapeX = new ElderwoodsPortalBlock.PortalShape(level, portalPos,
                        Direction.Axis.X);
                if (shapeX.isComplete()) {
                    shapeX.createPortalBlocks();
                    level.playSound(context.getPlayer(), portalPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                            1.0F, 1.0F);
                    context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                            LivingEntity.getSlotForHand(context.getHand()));
                    return InteractionResult.SUCCESS;
                }

                // Try Z axis
                ElderwoodsPortalBlock.PortalShape shapeZ = new ElderwoodsPortalBlock.PortalShape(level, portalPos,
                        Direction.Axis.Z);
                if (shapeZ.isComplete()) {
                    shapeZ.createPortalBlocks();
                    level.playSound(context.getPlayer(), portalPos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                            1.0F, 1.0F);
                    context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                            LivingEntity.getSlotForHand(context.getHand()));
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
