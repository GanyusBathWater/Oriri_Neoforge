package net.ganyusbathwater.oririmod.block.custom;

import net.ganyusbathwater.oririmod.block.ModBlocks;
import net.ganyusbathwater.oririmod.block.entity.FogSourceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Creative invisible technical block modeled after vanilla's {@link net.minecraft.world.level.block.LightBlock}.
 * Emits terrain-draped atmospheric floor haze inside structures, rooms, and dungeons.
 * <p>
 * Only shows its selection box &amp; marker overlay while the player holds the Fog Source item.
 * Right-click to cycle density/radius (1-5); Shift + Right-click to cycle color preset.
 */
public class FogSourceBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {

    public static final int MIN_DENSITY = 1;
    public static final int MAX_DENSITY = 5;

    public static final IntegerProperty DENSITY = IntegerProperty.create("density", MIN_DENSITY, MAX_DENSITY);
    public static final EnumProperty<FogColor> COLOR = EnumProperty.create("color", FogColor.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public enum FogColor implements StringRepresentable {
        MIST("mist", 0.85F, 0.90F, 0.95F, ChatFormatting.WHITE),
        SCARLET("scarlet", 0.64F, 0.16F, 0.20F, ChatFormatting.RED),
        VILE("vile", 0.44F, 0.24F, 0.56F, ChatFormatting.DARK_PURPLE),
        ELDER("elder", 0.58F, 0.76F, 0.88F, ChatFormatting.AQUA),
        VALLEY("valley", 0.82F, 0.91F, 0.87F, ChatFormatting.GREEN),
        GOLDEN("golden", 0.88F, 0.77F, 0.54F, ChatFormatting.GOLD),
        ABYSS("abyss", 0.18F, 0.27F, 0.44F, ChatFormatting.DARK_BLUE),
        ASH("ash", 0.48F, 0.50F, 0.54F, ChatFormatting.GRAY);

        private final String serializedName;
        public final float r;
        public final float g;
        public final float b;
        public final ChatFormatting chatColor;

        FogColor(String serializedName, float r, float g, float b, ChatFormatting chatColor) {
            this.serializedName = serializedName;
            this.r = r;
            this.g = g;
            this.b = b;
            this.chatColor = chatColor;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

        public FogColor next() {
            FogColor[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    public FogSourceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DENSITY, 3)
                .setValue(COLOR, FogColor.MIST)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DENSITY, COLOR, WATERLOGGED);
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
                int radius = Math.round(getRadiusForDensity(nextDensity));
                player.displayClientMessage(
                        Component.translatable("message.oririmod.fog_source.density", nextDensity, MAX_DENSITY, radius),
                        true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static float getRadiusForDensity(int density) {
        return 7.0F + density * 2.5F; // 9.5 .. 19.5 blocks
    }

    public static float getAlphaForDensity(int density) {
        return 0.34F + density * 0.11F; // 0.45 .. 0.89 (light mist -> thick dense fog)
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return context.isHoldingItem(ModBlocks.FOG_SOURCE.asItem()) ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return !useContext.getItemInHand().is(ModBlocks.FOG_SOURCE.asItem());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FogSourceBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.oririmod.fog_source.line1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.oririmod.fog_source.line2").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
