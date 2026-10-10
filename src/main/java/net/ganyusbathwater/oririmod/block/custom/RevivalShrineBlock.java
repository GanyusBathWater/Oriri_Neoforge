package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.item.custom.DogTagItem;
import net.ganyusbathwater.oririmod.block.entity.RevivalShrineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.component.CustomData;

import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

@SuppressWarnings("deprecation")
public class RevivalShrineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final com.mojang.serialization.MapCodec<RevivalShrineBlock> CODEC = simpleCodec(RevivalShrineBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public RevivalShrineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RevivalShrineBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        
        if (itemInHand.getItem() instanceof DogTagItem) {
            CustomData customData = itemInHand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (!customData.isEmpty()) {
                if (!level.isClientSide()) {
                    net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                    
                    // Determine the entity type from the tag, default to WOLF for backwards compatibility
                    EntityType<?> entityType = EntityType.WOLF;
                    if (tag.contains("RevivalEntityType")) {
                        net.minecraft.resources.ResourceLocation typeId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("RevivalEntityType"));
                        if (typeId != null) {
                            java.util.Optional<EntityType<?>> optionalType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(typeId);
                            if (optionalType.isPresent()) {
                                entityType = optionalType.get();
                            }
                        }
                    }
                    
                    net.minecraft.world.entity.Entity entity = entityType.create(level);
                    if (entity instanceof net.minecraft.world.entity.TamableAnimal pet) {
                        pet.load(tag);
                        pet.removeAllEffects(); // Ensure no persistent effects from death (like Wither)
                        pet.setRemainingFireTicks(0); // Clear fire if the pet died while burning
                        pet.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
                        // Make sure the UUID is fresh so it doesn't conflict
                        pet.setUUID(java.util.UUID.randomUUID());
                        pet.setHealth(pet.getMaxHealth());
                        
                        level.addFreshEntity(pet);
                        
                        // Firework effect
                        ItemStack firework = new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
                        FireworkRocketEntity fireworkEntity = new FireworkRocketEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, firework);
                        level.addFreshEntity(fireworkEntity);
                        
                        // Consume the Dog Tag
                        if (!player.isCreative()) {
                            itemInHand.shrink(1);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
