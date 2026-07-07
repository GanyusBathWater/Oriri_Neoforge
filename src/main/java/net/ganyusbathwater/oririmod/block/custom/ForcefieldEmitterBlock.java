package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ganyusbathwater.oririmod.block.entity.ForcefieldEmitterBlockEntity;
import net.ganyusbathwater.oririmod.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class ForcefieldEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<ForcefieldEmitterBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(ForcefieldVariant::values).fieldOf("variant").forGetter(ForcefieldEmitterBlock::getVariant),
            propertiesCodec()
    ).apply(instance, ForcefieldEmitterBlock::new));

    private final ForcefieldVariant variant;

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ForcefieldEmitterBlock(ForcefieldVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public ForcefieldVariant getVariant() {
        return this.variant;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForcefieldEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FORCEFIELD_EMITTER.get(), ForcefieldEmitterBlockEntity::tick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ForcefieldEmitterBlockEntity emitter) {
            if (emitter.isActive()) {
                if (random.nextFloat() < 0.5f) {
                    double dx = (random.nextDouble() - 0.5) * 1.5;
                    double dy = (random.nextDouble() - 0.5) * 1.5;
                    double dz = (random.nextDouble() - 0.5) * 1.5;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                            pos.getX() + 0.5 + dx,
                            pos.getY() + 12.0 / 16.0 + dy,
                            pos.getZ() + 0.5 + dz,
                            0, 0, 0);
                }
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ForcefieldEmitterBlockEntity emitter) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            
            // If the item doesn't have an isActive tag (e.g. freshly crafted or from creative mode), it defaults to true.
            // If it drops from a broken block, the loot table will apply either isActive: false (normal) or isActive: true (silk touch).
            if (customData.contains("isActive")) {
                emitter.setActive(customData.copyTag().getBoolean("isActive"));
            } else {
                emitter.setActive(true);
            }
        }
    }
}
