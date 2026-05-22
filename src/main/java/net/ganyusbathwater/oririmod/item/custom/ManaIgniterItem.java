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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ManaIgniterItem extends Item {
    public ManaIgniterItem(Properties properties) {
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
        // Fallback: Place Aether Fire
        BlockPos firePos = pos.relative(context.getClickedFace());
        if (BaseFireBlock.canBePlacedAt(level, firePos, context.getHorizontalDirection())) {
            level.playSound(context.getPlayer(), firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            BlockState blockstate = ModBlocks.AETHER_FIRE_BLOCK.get().defaultBlockState();
            level.setBlock(firePos, blockstate, 11);

            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, firePos, context.getItemInHand());
                context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }
}
