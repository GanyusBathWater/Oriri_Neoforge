package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.mana.ModManaUtil;
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
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

public class MagicStaffItem extends Item implements ModRarityCarrier {

    private final ModRarity rarity;

    public enum StaffAction {
        GROW, REGEN, HASTE
    }

    private final StaffAction action;
    private final int durationTicks; // für Effekte
    private final int amplifier; // für Effekte
    private final int cooldownTicks;
    private final int manaCost;

    public MagicStaffItem(Properties properties, StaffAction action, int durationTicks, int amplifier,
            int cooldownTicks, int manaCost, ModRarity rarity) {
        super(properties);
        this.action = action;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.cooldownTicks = cooldownTicks;
        this.rarity = rarity;
        this.manaCost = manaCost;
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
                        grown = tryBonemeal(server, pos) || tryBonemeal(server, pos.relative(hit.getDirection()));
                        if (grown) {
                            // nur wenn Mana verfügbar und erfolgreich verbraucht wird
                            if (ModManaUtil.tryConsumeMana(player, manaCost, stack)) {
                                server.levelEvent(1505, pos, 0);
                                player.getCooldowns().addCooldown(this, Math.max(5, cooldownTicks));
                                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                            }
                        }
                    }
                }
                return InteractionResultHolder.pass(stack);
            }
            case REGEN -> {
                if (!level.isClientSide) {
                    if (ModManaUtil.tryConsumeMana(player, manaCost, stack)) {
                        player.addEffect(
                                new MobEffectInstance(MobEffects.REGENERATION, durationTicks, amplifier, false, true));
                        player.getCooldowns().addCooldown(this, cooldownTicks);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case HASTE -> {
                if (!level.isClientSide) {
                    if (ModManaUtil.tryConsumeMana(player, manaCost, stack)) {
                        player.addEffect(
                                new MobEffectInstance(MobEffects.DIG_SPEED, durationTicks, amplifier, false, true));
                        player.getCooldowns().addCooldown(this, cooldownTicks);
                    }
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        String descriptionId = this.getDescriptionId();
        Language language = Language.getInstance();

        // Element
        String elementKey = descriptionId + ".element";
        if (language.has(elementKey)) {
            tooltipComponents.add(Component.translatable("tooltip.oririmod.element", Component.translatable(elementKey)).withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        // Mana Cost
        tooltipComponents.add(Component.translatable("tooltip.oririmod.mana_cost", this.manaCost).withStyle(net.minecraft.ChatFormatting.BLUE));

        // Damage (None for staffs)

        // Lore
        String loreKey = descriptionId + ".lore";
        if (language.has(loreKey)) {
            tooltipComponents.add(Component.translatable(loreKey).withStyle(net.minecraft.ChatFormatting.DARK_GRAY, net.minecraft.ChatFormatting.ITALIC));
        }

        tooltipComponents.addAll(buildModTooltip(stack, context, tooltipFlag));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}