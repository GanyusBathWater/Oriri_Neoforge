package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.ganyusbathwater.oririmod.block.entity.TeleporterBlockEntity;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.ganyusbathwater.oririmod.world.data.TeleporterSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.context.BlockPlaceContext;

public class TeleporterBlock extends BaseEntityBlock {
    public static final MapCodec<TeleporterBlock> CODEC = simpleCodec(TeleporterBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 48, 16);
    private static final java.util.Map<java.util.UUID, BlockPos> LOCKED_PLAYERS = new java.util.concurrent.ConcurrentHashMap<>();
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            BlockPos locked = LOCKED_PLAYERS.get(player.getUUID());
            
            double dx = Math.abs(player.getX() - (pos.getX() + 0.5));
            double dz = Math.abs(player.getZ() - (pos.getZ() + 0.5));
            
            if (dx < 0.3 && dz < 0.3) {
                if (locked == null || !locked.equals(pos)) {
                    executeTeleport((ServerLevel) level, pos, player);
                }
            } else if (locked != null && locked.equals(pos)) {
                if (dx > 0.6 || dz > 0.6) {
                    LOCKED_PLAYERS.remove(player.getUUID());
                }
            }
        }
    }

    public TeleporterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = net.minecraft.util.Mth.floor((double)((context.getRotation() * 8.0F / 360.0F) + 0.5F)) & 7;
        return this.defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeleporterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, net.ganyusbathwater.oririmod.block.entity.ModBlockEntities.TELEPORTER.get(), TeleporterBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TeleporterBlockEntity teleporter) {
            net.minecraft.world.item.DyeColor color = stack.get(net.minecraft.core.component.DataComponents.BASE_COLOR);
            if (color != null) {
                teleporter.setBaseColor(color.getTextureDiffuseColor());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (level.getBlockEntity(pos) instanceof TeleporterBlockEntity teleporter) {
                boolean inDungeon = level.dimension().location().getPath().startsWith("dungeon_");
                if ((inDungeon || teleporter.isLocked()) && !player.isCreative()) {
                    player.displayClientMessage(Component.translatable("gui.oririmod.teleporter.locked").withStyle(net.minecraft.ChatFormatting.RED), true);
                    return InteractionResult.SUCCESS;
                }
                NetworkHandler.sendOpenTeleporterScreen(sp, pos, teleporter.getTeleporterId());
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void executeTeleport(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (level.getBlockEntity(pos) instanceof TeleporterBlockEntity teleporter) {
            String id = teleporter.getTeleporterId();
            if (id != null && !id.isEmpty()) {
                TeleporterSavedData data = TeleporterSavedData.get(level);
                BlockPos dest = data.getDestination(id, pos);
                if (dest != null) {
                    if (!isClear(level, pos)) {
                        LOCKED_PLAYERS.put(player.getUUID(), pos);
                        player.displayClientMessage(Component.translatable("gui.oririmod.teleporter.origin_obstructed").withStyle(net.minecraft.ChatFormatting.RED), true);
                    } else if (level.isLoaded(dest)) {
                        if (!(level.getBlockState(dest).getBlock() instanceof TeleporterBlock)) {
                            data.removeTeleporter(id, dest);
                            LOCKED_PLAYERS.put(player.getUUID(), pos);
                            player.displayClientMessage(Component.translatable("gui.oririmod.teleporter.not_found").withStyle(net.minecraft.ChatFormatting.RED), true);
                        } else if (!isClear(level, dest)) {
                            LOCKED_PLAYERS.put(player.getUUID(), pos);
                            player.displayClientMessage(Component.translatable("gui.oririmod.teleporter.obstructed").withStyle(net.minecraft.ChatFormatting.RED), true);
                        } else {
                            LOCKED_PLAYERS.put(player.getUUID(), dest);
                            player.teleportTo(level, dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5, player.getYRot(), player.getXRot());
                            level.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
                            level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        }
                    } else {
                        // Destination unloaded, force teleport to load it
                        LOCKED_PLAYERS.put(player.getUUID(), dest);
                        player.teleportTo(level, dest.getX() + 0.5, dest.getY() + 1.0, dest.getZ() + 0.5, player.getYRot(), player.getXRot());
                        level.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                } else {
                    LOCKED_PLAYERS.put(player.getUUID(), pos);
                    player.displayClientMessage(Component.translatable("gui.oririmod.teleporter.not_found").withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
        }
    }

    public static boolean isClear(Level level, BlockPos pos) {
        for (int y = 1; y <= 2; y++) {
            BlockPos checkPos = pos.offset(0, y, 0);
            if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                TeleporterSavedData data = TeleporterSavedData.get((ServerLevel) level);
                data.removeTeleporter(pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
