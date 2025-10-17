package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MagicStaffItem extends Item implements ModRarityCarrier {

    private final ModRarity rarity;

    public enum StaffAction {
        GROW, REGEN, HASTE
    }

    private final StaffAction action;
    private final int durationTicks;   // für Effekte
    private final int amplifier;       // für Effekte
    private final int cooldownTicks;

    public MagicStaffItem(Properties properties, StaffAction action, int durationTicks, int amplifier, int cooldownTicks, ModRarity rarity) {
        super(properties);
        this.action = action;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.cooldownTicks = cooldownTicks;
        this.rarity = rarity;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        switch (action) {
            case GROW -> {
                BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hit.getBlockPos();
                    boolean grown = false;
                    if (!level.isClientSide && level instanceof ServerLevel server) {
                        // Versuche Block am Treffer zu düngen, sonst den angrenzenden Block in Blickrichtung
                        grown = tryBonemeal(server, pos) || tryBonemeal(server, pos.relative(hit.getDirection()));
                        if (grown) {
                            // Partikelevent wie Knochenmehl
                            server.levelEvent(1505, pos, 0);
                        }
                    }
                    if (grown) {
                        player.getCooldowns().addCooldown(this, Math.max(5, cooldownTicks));
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                    }
                }
                return InteractionResultHolder.pass(stack);
            }
            case REGEN -> {
                if (!level.isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, amplifier, false, true));
                    player.getCooldowns().addCooldown(this, cooldownTicks);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case HASTE -> {
                if (!level.isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, durationTicks, amplifier, false, true));
                    player.getCooldowns().addCooldown(this, cooldownTicks);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            default -> {
                return InteractionResultHolder.pass(stack);
            }
        }
    }

    private boolean tryBonemeal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BonemealableBlock growable) {
            if (growable.isValidBonemealTarget(level, pos, state)) {
                if (growable.isBonemealSuccess(level, level.random, pos, state)) {
                    growable.performBonemeal(level, level.random, pos, state);
                }
                return true;
            }
        }
        return false;
    }
}
