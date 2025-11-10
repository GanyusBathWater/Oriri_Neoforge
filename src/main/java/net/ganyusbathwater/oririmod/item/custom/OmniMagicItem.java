package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.MagicBoltEntity;
import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class OmniMagicItem extends Item implements ModRarityCarrier {

    private static final ResourceLocation TEX_ARCANE_OUTER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/blood_outer.png");
    private static final ResourceLocation TEX_ARCANE_MID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/blood_mid.png");
    private static final ResourceLocation TEX_ARCANE_INNER =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "textures/effect/magic_circles/blood_inner.png");

    public enum OmniAbility {
        BOLT_NORMAL, BOLT_SONIC, BOLT_BLAZE, BOLT_ENDER, BOLT_EXPLOSIVE, BOLT_METEOR,
        STAFF_GROW, STAFF_REGEN, STAFF_HASTE;

        public boolean isBolt() { return this.name().startsWith("BOLT_"); }
        public boolean isStaff() { return this.name().startsWith("STAFF_"); }

        public MagicBoltAbility toBolt() {
            return switch (this) {
                case BOLT_SONIC -> MagicBoltAbility.SONIC;
                case BOLT_BLAZE -> MagicBoltAbility.BLAZE;
                case BOLT_ENDER -> MagicBoltAbility.ENDER;
                case BOLT_EXPLOSIVE -> MagicBoltAbility.EXPLOSIVE;
                case BOLT_METEOR -> MagicBoltAbility.METEOR;
                default -> MagicBoltAbility.NORMAL;
            };
        }

        public MagicStaffAction toStaff() {
            return switch (this) {
                case STAFF_GROW -> MagicStaffAction.GROW;
                case STAFF_REGEN -> MagicStaffAction.REGEN;
                case STAFF_HASTE -> MagicStaffAction.HASTE;
                default -> null;
            };
        }

        public int getManaCost() {
            return switch (this) {
                case BOLT_SONIC -> 12;
                case BOLT_BLAZE -> 8;
                case BOLT_ENDER -> 18;
                case BOLT_EXPLOSIVE -> 20;
                case BOLT_METEOR -> 40;
                case BOLT_NORMAL -> 5;
                case STAFF_GROW -> 6;
                case STAFF_REGEN -> 20;
                case STAFF_HASTE -> 12;
            };
        }
    }

    public enum MagicStaffAction { GROW, REGEN, HASTE }

    private static final String NBT_SELECTED = "OmniAbility";

    private final ModRarity rarity;
    private final int cooldown;
    private final int effectDuration;
    private final int effectAmplifier;

    private static final float METEOR_OUTER_RADIUS_PLAYER = 1.2f;
    private static final float METEOR_OUTER_DISTANCE_PLAYER = 1.6f;
    private static final float METEOR_MID_RADIUS_GROUND = 7.0f;
    private static final float METEOR_INNER_RADIUS_GROUND = 5.0f;

    public OmniMagicItem(Properties props, ModRarity rarity, int cooldown, int effectDuration, int effectAmplifier) {
        super(props);
        this.rarity = rarity;
        this.cooldown = cooldown;
        this.effectDuration = effectDuration;
        this.effectAmplifier = effectAmplifier;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        OmniAbility ab = getSelected(stack);
        return (ab.isBolt() ? UseAnim.BOW : UseAnim.NONE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        OmniAbility ab = getSelected(stack);

        // Fähigkeit durchschalten mit Shift
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                OmniAbility next = nextAbility(ab);
                setSelected(stack, next);
                player.displayClientMessage(Component.literal("Fähigkeit: " + prettyName(next)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Bolt-Fähigkeiten werden "aufgeladen"
        if (ab.isBolt()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Staff-Fähigkeiten wirken sofort
        MagicStaffAction sa = ab.toStaff();
        if (sa == null) return InteractionResultHolder.pass(stack);

        switch (sa) {
            case GROW -> {
                BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
                if (!level.isClientSide && hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hit.getBlockPos();
                    boolean ok = tryBonemeal((ServerLevel) level, pos) || tryBonemeal((ServerLevel) level, pos.above());
                    if (ok && ModManaUtil.tryConsumeMana(player, ab.getManaCost())) {
                        player.getCooldowns().addCooldown(this, cooldown);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case REGEN -> {
                if (!level.isClientSide) {
                    if (ModManaUtil.tryConsumeMana(player, ab.getManaCost())) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, effectDuration, effectAmplifier));
                        player.getCooldowns().addCooldown(this, cooldown);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            case HASTE -> {
                if (!level.isClientSide) {
                    if (ModManaUtil.tryConsumeMana(player, ab.getManaCost())) {
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, effectDuration, effectAmplifier));
                        player.getCooldowns().addCooldown(this, cooldown);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            default -> {
                return InteractionResultHolder.pass(stack);
            }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        OmniAbility ab = getSelected(stack);
        if (!level.isClientSide || !ab.isBolt()) return;

        // Nur alle ~30 Ticks Reset, damit der Renderer persistenten Indikator nicht nach 40 Ticks entfernt.
        var prev = MagicIndicatorClientState.INSTANCE.getActive().get(living.getId());
        if (prev != null) {
            long age = level.getGameTime() - prev.startGameTime();
            if (age > 30) {
                MagicIndicatorClientState.stopFor(living);
            }
        }

        if (ab == OmniAbility.BOLT_METEOR) {
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

            MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                    .duration(0)
                    .distance(METEOR_OUTER_DISTANCE_PLAYER)
                    .spin(4.0f)
                    .worldAnchor(groundCenter)
                    .persistentUntilMeteorImpact(true); // wichtig: nicht vom MagicBoltItem abhängig

            MagicIndicatorClientState.Indicator.Layer outer = new MagicIndicatorClientState.Indicator.Layer(
                    TEX_ARCANE_OUTER, METEOR_OUTER_RADIUS_PLAYER, 10f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.PLAYER
            );
            MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                    TEX_ARCANE_MID, METEOR_MID_RADIUS_GROUND, -6f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.WORLD
            );
            MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                    TEX_ARCANE_INNER, METEOR_INNER_RADIUS_GROUND, 6f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.WORLD
            );

            MagicIndicatorClientState.startFor(living, b.addLayer(outer).addLayer(mid).addLayer(inner).build());
        } else {
            MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                    .duration(0)
                    .distance(1.6f)
                    .spin(6f)
                    .persistentUntilMeteorImpact(true); // damit Renderer nicht sofort entfernt

            MagicIndicatorClientState.Indicator.Layer layer = new MagicIndicatorClientState.Indicator.Layer(
                    null, 1.2f, 0f, 0xFFFFFFFF, 0f,
                    MagicIndicatorClientState.Anchor.PLAYER
            );

            MagicIndicatorClientState.startFor(living, b.addLayer(layer).build());
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        OmniAbility ab = getSelected(stack);

        // Client: Indikatoren verwalten
        if (level.isClientSide) {
            if (ab == OmniAbility.BOLT_METEOR) {
                BlockHitResult hit = raycastToGround(level, living, 96.0);
                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos ground = findGround(level, hit.getBlockPos().above(), 12);
                    if (ground != null) {
                        double topY = getTopSurfaceY(level, ground);
                        Vec3 groundCenter = new Vec3(ground.getX() + 0.5, topY, ground.getZ() + 0.5);

                        // Start für „Boden“-Kreise bis Meteorit einschlägt
                        MagicIndicatorClientState.stopFor(living);
                        MagicIndicatorClientState.Indicator.Builder b = MagicIndicatorClientState.Indicator.builder()
                                .duration(0)
                                .spin(6f)
                                .worldAnchor(groundCenter)
                                .persistentUntilMeteorImpact(true);

                        MagicIndicatorClientState.Indicator.Layer mid = new MagicIndicatorClientState.Indicator.Layer(
                                TEX_ARCANE_MID, METEOR_MID_RADIUS_GROUND, -6f, 0xFFFFFFFF, 0f,
                                MagicIndicatorClientState.Anchor.WORLD
                        );
                        MagicIndicatorClientState.Indicator.Layer inner = new MagicIndicatorClientState.Indicator.Layer(
                                TEX_ARCANE_INNER, METEOR_INNER_RADIUS_GROUND, 6f, 0xFFFFFFFF, 0f,
                                MagicIndicatorClientState.Anchor.WORLD
                        );

                        MagicIndicatorClientState.startFor(living, b.addLayer(mid).addLayer(inner).build());
                    }
                }
                return;
            }
            // andere Bolts: Anzeige beenden
            MagicIndicatorClientState.stopFor(living);
            return;
        }

        // Server: Aktion ausführen
        int usedTicks = getUseDuration(stack, living) - timeLeft;
        int minCharge = 10;

        if (ab.isBolt()) {
            if (usedTicks < minCharge) return;

            if (living instanceof Player p) {
                if (!ModManaUtil.tryConsumeMana(p, ab.getManaCost())) return;
            }

            if (ab == OmniAbility.BOLT_METEOR) {
                BlockHitResult hit = raycastToGround(level, living, 96.0);
                if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

                BlockPos ground = findGround(level, hit.getBlockPos().above(), 12);
                if (ground == null) return;

                MeteorEntity meteor = (MeteorEntity) ModEntities.METEOR.get().create(level);
                if (meteor == null) return;

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

            MagicBoltEntity bolt = new MagicBoltEntity(level, living);
            bolt.setAbility(ab.toBolt());

            float speed = switch (ab) {
                case BOLT_SONIC -> 5.0F;
                case BOLT_BLAZE -> 2.2F;
                case BOLT_ENDER -> 1.3F;
                case BOLT_NORMAL -> 1.6F;
                case BOLT_EXPLOSIVE -> 2.0F;
                case BOLT_METEOR -> 1.0F;
                default -> 1.6F;
            };

            bolt.launchStraight(living, speed);
            level.addFreshEntity(bolt);

            if (living instanceof Player p) {
                p.getCooldowns().addCooldown(this, cooldown);
            }
        }
    }

    // ---------- Data Components: Auswahl speichern ----------

    private static OmniAbility getSelected(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);

        String name = "";
        if (data != null) {
            net.minecraft.nbt.CompoundTag tag = data.copyTag();
            name = tag.getString(NBT_SELECTED);
        }

        try {
            return name.isEmpty() ? OmniAbility.BOLT_NORMAL : OmniAbility.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return OmniAbility.BOLT_NORMAL;
        }
    }

    private static void setSelected(ItemStack stack, OmniAbility ab) {
        net.minecraft.world.item.component.CustomData data =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);

        net.minecraft.nbt.CompoundTag tag =
                (data != null) ? data.copyTag() : new net.minecraft.nbt.CompoundTag();

        tag.putString(NBT_SELECTED, ab.name());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static OmniAbility nextAbility(OmniAbility current) {
        OmniAbility[] vals = OmniAbility.values();
        int idx = (current.ordinal() + 1) % vals.length;
        return vals[idx];
    }

    private static String prettyName(OmniAbility ab) {
        return switch (ab) {
            case BOLT_NORMAL -> "Bolt: Normal";
            case BOLT_SONIC -> "Bolt: Sonic";
            case BOLT_BLAZE -> "Bolt: Blaze";
            case BOLT_ENDER -> "Bolt: Ender";
            case BOLT_EXPLOSIVE -> "Bolt: Explosiv";
            case BOLT_METEOR -> "Bolt: Meteor";
            case STAFF_GROW -> "Stab: Wachsen";
            case STAFF_REGEN -> "Stab: Regeneration";
            case STAFF_HASTE -> "Stab: Eile";
        };
    }

    // ---------- Utils (Raycast / Bonemeal) ----------

    private static BlockHitResult raycastToGround(Level level, LivingEntity living, double range) {
        Vec3 eye = living.getEyePosition(1.0f);
        Vec3 look = living.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        return level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                living
        ));
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

    private static double getTopSurfaceY(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return pos.getY() + 1.0;
        return pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
    }

    private static boolean tryBonemeal(ServerLevel level, BlockPos pos) {
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