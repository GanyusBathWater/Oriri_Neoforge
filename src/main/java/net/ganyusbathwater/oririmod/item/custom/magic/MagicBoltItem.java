package net.ganyusbathwater.oririmod.item.custom.magic;

import net.ganyusbathwater.oririmod.entity.FireballProjectileEntity;
import net.ganyusbathwater.oririmod.entity.MagicBoltEntity;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MagicBoltItem extends Item implements ModRarityCarrier {
    private final MagicBoltAbility ability;
    private final int cooldown;
    private final ModRarity rarity;
    private final int manaCost;

    private static final float METEOR_OUTER_RADIUS_PLAYER = 1.2f;
    private static final float METEOR_OUTER_DISTANCE_PLAYER = 1.6f;
    private static final float METEOR_MID_RADIUS_GROUND = 7.0f;
    private static final float METEOR_INNER_RADIUS_GROUND = 5.0f;

    public MagicBoltItem(Properties props, MagicBoltAbility ability, int cooldown, int manaCost, ModRarity rarity) {
        super(props);
        this.ability = ability;
        this.cooldown = cooldown;
        this.rarity = rarity;
        this.manaCost = manaCost;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide)
            return;

        if (this.ability == MagicBoltAbility.METEOR) {
            BlockHitResult hit = raycastToGround(level, living, 96.0);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                MagicIndicatorClientState.stopFor(living);
                return;
            }

            BlockPos ground = findGround(level, hit.getBlockPos().above(), 12);
            if (ground == null) {
                MagicIndicatorClientState.stopFor(living);
                return;
            }
            double topY = getTopSurfaceY(level, ground);
            Vec3 groundCenter = new Vec3(ground.getX() + 0.5, topY, ground.getZ() + 0.5);

            ResourceLocation TEX_OUTER = ResourceLocation.fromNamespaceAndPath("oririmod",
                    "textures/effect/magic_circles/arcane_outer.png");
            ResourceLocation TEX_MID = ResourceLocation.fromNamespaceAndPath("oririmod",
                    "textures/effect/magic_circles/arcane_mid.png");
            ResourceLocation TEX_INNER = ResourceLocation.fromNamespaceAndPath("oririmod",
                    "textures/effect/magic_circles/arcane_inner.png");

            MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                    .duration(0)
                    .distance(METEOR_OUTER_DISTANCE_PLAYER)
                    .spin(4.0f)
                    .worldAnchor(groundCenter);

            MagicIndicatorClientState.Indicator.Layer outer = new MagicIndicatorClientState.Indicator.Layer(
                    TEX_OUTER, METEOR_OUTER_RADIUS_PLAYER, 10f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.PLAYER);
            MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                    TEX_MID, METEOR_MID_RADIUS_GROUND, -6f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.WORLD);
            MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                    TEX_INNER, METEOR_INNER_RADIUS_GROUND, 6f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.WORLD);

            MagicIndicatorClientState.startFor(living, b.addLayer(outer).addLayer(mid).addLayer(inner).build());
        } else {
            MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                    .duration(0)
                    .distance(1.6f)
                    .spin(6f);

            MagicIndicatorClientState.Indicator.Layer layer = new MagicIndicatorClientState.Indicator.Layer(
                    null, 1.2f, 0f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.PLAYER);

            MagicIndicatorClientState.startFor(living, b.addLayer(layer).build());
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide) {
            if (this.ability == MagicBoltAbility.METEOR) {
                BlockHitResult hit = raycastToGround(level, living, 96.0);
                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos ground = findGround(level, hit.getBlockPos().above(), 12);
                    if (ground != null) {
                        double topY = getTopSurfaceY(level, ground);
                        Vec3 groundCenter = new Vec3(ground.getX() + 0.5, topY, ground.getZ() + 0.5);

                        ResourceLocation TEX_MID = ResourceLocation.fromNamespaceAndPath("oririmod",
                                "textures/effect/magic_circles/arcane_mid.png");
                        ResourceLocation TEX_INNER = ResourceLocation.fromNamespaceAndPath("oririmod",
                                "textures/effect/magic_circles/arcane_inner.png");

                        MagicIndicatorClientState.stopFor(living);

                        MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                                .duration(0)
                                .spin(6f)
                                .worldAnchor(groundCenter)
                                .persistentUntilMeteorImpact(true);

                        MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                                TEX_MID, METEOR_MID_RADIUS_GROUND, -6f, 0xFFFFFFFF, 0f,
                                MagicIndicatorClientState.Anchor.WORLD);
                        MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                                TEX_INNER, METEOR_INNER_RADIUS_GROUND, 6f, 0xFFFFFFFF, 0f,
                                MagicIndicatorClientState.Anchor.WORLD);

                        MagicIndicatorClientState.startFor(living, b.addLayer(mid).addLayer(inner).build());
                    }
                }
                return;
            }

            MagicIndicatorClientState.stopFor(living);
            return;
        }

        int usedTicks = getUseDuration(stack, living) - timeLeft;
        int minCharge = 10;
        if (usedTicks < minCharge)
            return;

        if (living instanceof Player p) {
            // Spieler muss Mana besitzen
            if (!ModManaUtil.tryConsumeMana(p, manaCost))
                return;
        }

        if (this.ability == MagicBoltAbility.METEOR) {
            BlockHitResult hit = raycastToGround(level, living, 96.0);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK)
                return;

            BlockPos ground = findGround(level, hit.getBlockPos().above(), 12);
            if (ground == null)
                return;

            MeteorEntity meteor = (MeteorEntity) ModEntities.METEOR.get().create(level);
            if (meteor == null)
                return;

            meteor.configure(ground.immutable(), 12.0f, 7);
            meteor.setOwnerId(living.getId());

            double spawnX = ground.getX() + 0.5;
            double spawnZ = ground.getZ() + 0.5;
            double spawnY = ground.getY() + 90.0;
            meteor.setPos(spawnX, spawnY, spawnZ);
            meteor.setDeltaMovement(0.0, -0.01, 0.0);

            level.addFreshEntity(meteor);

            if (living instanceof Player p) {
                p.getCooldowns().addCooldown(this, cooldown);
            }
            return;
        }

        if (this.ability == MagicBoltAbility.AMATEUR_FIREBALL ||
                this.ability == MagicBoltAbility.APPRENTICE_FIREBALL ||
                this.ability == MagicBoltAbility.JOURNEYMAN_FIREBALL ||
                this.ability == MagicBoltAbility.WISE_FIREBALL) {

            FireballProjectileEntity fireball = new FireballProjectileEntity(level, living);
            fireball.configureForGrade(this.ability);
            fireball.launchStraight(living, 1.6F); // Base speed
            level.addFreshEntity(fireball);

            if (living instanceof Player p) {
                p.getCooldowns().addCooldown(this, cooldown);
            }
            return;
        }

        MagicBoltEntity bolt = new MagicBoltEntity(level, living);
        bolt.setAbility(this.ability);

        float speed = switch (this.ability) {
            case SONIC -> 5.0F;
            case BLAZE -> 2.2F;
            case ENDER -> 1.3F;
            case NORMAL -> 1.6F;
            case EXPLOSIVE -> 2.0F;
            case METEOR -> 1.0F;
            default -> 1.6F; // Should not be reached for fireballs due to check above
        };

        bolt.launchStraight(living, speed);
        level.addFreshEntity(bolt);

        if (living instanceof Player p) {
            p.getCooldowns().addCooldown(this, cooldown);
        }
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    private static BlockHitResult raycastToGround(Level level, LivingEntity living, double range) {
        Vec3 eye = living.getEyePosition(1.0f);
        Vec3 look = living.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        return level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                living));
    }

    private static BlockPos findGround(Level level, BlockPos start, int maxDrop) {
        BlockPos p = start;
        for (int i = 0; i < maxDrop; i++) {
            if (!level.isEmptyBlock(p) && level.isEmptyBlock(p.above())) {
                return p;
            }
            p = p.below();
        }
        return null;
    }

    // Oberkante des Blocks (inkl. Slabs/Stufen) als Welt-Y ermitteln
    private static double getTopSurfaceY(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty())
            return pos.getY() + 1.0; // Fallback: Blockoberseite
        return pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
    }
}