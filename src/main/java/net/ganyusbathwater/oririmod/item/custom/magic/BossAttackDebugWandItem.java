package net.ganyusbathwater.oririmod.item.custom.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import java.util.UUID;

public class BossAttackDebugWandItem extends Item {

    public enum BossAttackType {
        SWORD_PROJECTILE, METEOR_SHOWER, LASERBEAM_NORMAL, LASERBEAM_CYLINDER, LASERBEAM_CIRCLE,
        LASERBEAM_GROUND, LASERBEAM_STARBURST, INSTA_DEATH, ROOT_ATTACK, GROUND_SLAM,
        WAVE_CIRCULAR, WAVE_CONE, WAVE_PLAIN;
    }

    private static final String NBT_SELECTED = "ActiveAttack";
    private static final String NBT_TARGET_UUID = "TargetUUID";

    public BossAttackDebugWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new net.ganyusbathwater.oririmod.client.screen.BossAttackSelectionScreen(stack, hand));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (!level.isClientSide) {
            BossAttackType current = getSelected(stack);
            UUID targetId = getTargetUUID(stack);

            Entity target = null;
            if (targetId != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                target = serverLevel.getEntity(targetId);
            }

            BlockPos targetPos = null;
            if (target != null) {
                targetPos = target.blockPosition();
            } else {
                BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    targetPos = hit.getBlockPos();
                }
            }

            player.displayClientMessage(Component
                    .literal("Triggering " + prettyName(current) + " at "
                            + (target != null ? target.getName().getString()
                                    : (targetPos != null ? targetPos.toShortString() : "nowhere")))
                    .withStyle(net.minecraft.ChatFormatting.GREEN), true);

            // TODO: Actually spawn the attacks here later
            if (current == BossAttackType.SWORD_PROJECTILE
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.ganyusbathwater.oririmod.entity.SwordProjectileEntity sword = new net.ganyusbathwater.oririmod.entity.SwordProjectileEntity(
                        level, player);

                // Spawn the sword somewhere in the air with random offset
                Vec3 look = player.getLookAngle();

                // Random position logic:
                double offsetX = (level.random.nextDouble() - 0.5) * 16.0; // -8 to +8
                double offsetZ = (level.random.nextDouble() - 0.5) * 16.0; // -8 to +8
                double offsetY = 10.0 + (level.random.nextDouble() * 4.0); // 10 to 14

                sword.setPos(player.getX() + look.x * 5 + offsetX, player.getY() + offsetY,
                        player.getZ() + look.z * 5 + offsetZ);

                // Initialize downwards directly inside the entity so it bypasses scope
                // protections
                sword.initializeRotationDown();

                if (targetId != null) {
                    sword.setTargetId(targetId);
                }
                serverLevel.addFreshEntity(sword);
            } else if (current == BossAttackType.METEOR_SHOWER
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockPos meteorTarget = targetPos != null ? targetPos : player.blockPosition();
                net.ganyusbathwater.oririmod.util.MeteorShowerUtil.unleash(serverLevel, meteorTarget, player.getId());
            } else if (current == BossAttackType.ROOT_ATTACK
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                BlockPos rootTarget = targetPos != null ? targetPos : player.blockPosition();
                net.ganyusbathwater.oririmod.util.RootAttackUtil.unleash(serverLevel, rootTarget, player.getId());
            } else if (current == BossAttackType.LASERBEAM_NORMAL
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // Beam origin: player eye position
                Vec3 beamStart = player.getEyePosition();
                Vec3 beamEnd = target != null ? target.getEyePosition() : (targetPos != null ? Vec3.atCenterOf(targetPos) : beamStart.add(player.getLookAngle().scale(100.0))); // Raycast will automatically shorten this!
                net.ganyusbathwater.oririmod.util.LaserBeamUtil.unleash(serverLevel,
                        net.ganyusbathwater.oririmod.util.LaserBeamUtil.LaserBeamConfig.simple(beamStart, beamEnd, 0xFF_00AAFF));
            } else if (current == BossAttackType.LASERBEAM_CYLINDER
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Vec3 center = target != null ? target.position() : player.position();
                // Random start angle so multiple spawns don't overlap perfectly
                float startRot = player.getRandom().nextFloat() * ((float) Math.PI * 2);
                net.ganyusbathwater.oririmod.util.LaserBeamUtil.orbitingCylinder(
                        serverLevel, center, 5.0f, 100.0f, 380.0f / 160.0f, 0xFF_FF0000, 160, startRot);
            } else if (current == BossAttackType.LASERBEAM_CIRCLE
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Vec3 center = target != null ? target.position().add(0, 1, 0) : player.position().add(0, 1, 0); // sweep at waist height
                float startRot = player.getRandom().nextFloat() * ((float) Math.PI * 2);
                net.ganyusbathwater.oririmod.util.LaserBeamUtil.orbitingClockAngle(
                        serverLevel, center, 100.0f, 380.0f / 160.0f, 0xFF_AA00FF, 160, startRot);
            } else if (current == BossAttackType.LASERBEAM_GROUND
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Vec3 base = target != null ? target.position() : player.position();
                BlockPos groundPos = BlockPos.containing(base);
                // Search downwards for solid ground up to typical cavern depth
                while (groundPos.getY() > level.getMinBuildHeight() && !level.getBlockState(groundPos).isSolidRender(level, groundPos)) {
                    groundPos = groundPos.below();
                }
                net.ganyusbathwater.oririmod.util.LaserBeamUtil.unleash(serverLevel,
                        net.ganyusbathwater.oririmod.util.LaserBeamUtil.LaserBeamConfig.upward(Vec3.atBottomCenterOf(groundPos.above()), 100.0f, 0xFF_00FF00));
            } else if (current == BossAttackType.LASERBEAM_STARBURST
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                Vec3 center = target != null ? target.position().add(0, 1.5, 0) : player.position().add(0, 1.5, 0); // chest height
                int numBeams = 20;
                int duration = 300; // 15 seconds perfectly
                float spinSpeedDeg = 720.0f / 300.0f; // 2 full rotations mathematically guaranteed!
                int laserColor = 0xFF_880000; // Bloody red
                
                for (int i = 0; i < numBeams; i++) {
                    float startYaw = player.getRandom().nextFloat() * ((float) Math.PI * 2);
                    // Native spherical constraints: 
                    // Range of exactly -70.0 to +70.0 mathematically guarantees the 40° empty gap zones!
                    float pitchDeg = -70.0f + (player.getRandom().nextFloat() * 140.0f);
                    float staticPitch = (float) Math.toRadians(pitchDeg);
                    boolean isSilent = (i > 0);
                    
                    net.ganyusbathwater.oririmod.util.LaserBeamUtil.orbitSphere(
                            serverLevel, center, 100.0f, staticPitch, spinSpeedDeg, laserColor, duration, startYaw, isSilent);
                }
            } else if (current == BossAttackType.INSTA_DEATH
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.ganyusbathwater.oririmod.entity.DoomClockEntity clock = new net.ganyusbathwater.oririmod.entity.DoomClockEntity(
                        net.ganyusbathwater.oririmod.entity.ModEntities.DOOM_CLOCK.get(), level);
                clock.setPos(player.getX(), player.getY() + 3.0, player.getZ());
                clock.setOwnerId(player.getId());
                serverLevel.addFreshEntity(clock);
            } else if (current == BossAttackType.WAVE_CIRCULAR || current == BossAttackType.WAVE_CONE || current == BossAttackType.WAVE_PLAIN) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    Vec3 origin = target != null ? target.position() : player.position();
                    
                    // Generate a vibrant random color! (Bounding RGB above 100 to prevent ultra-dark invisible waves)
                    int r = 100 + level.random.nextInt(156);
                    int g = 100 + level.random.nextInt(156);
                    int b = 100 + level.random.nextInt(156);
                    int randomColor = (0xFF << 24) | (r << 16) | (g << 8) | b;

                    if (current == BossAttackType.WAVE_CIRCULAR) {
                        net.ganyusbathwater.oririmod.util.MagicWaveUtil.spawnCircular(
                                serverLevel, origin, 32, randomColor, 6f, player.getId());
                    } else if (current == BossAttackType.WAVE_CONE) {
                        float facingYaw = (float) Math.toRadians(-player.getYRot());
                        net.ganyusbathwater.oririmod.util.MagicWaveUtil.spawnCone(
                                serverLevel, origin, facingYaw, randomColor, 6f, player.getId());
                    } else if (current == BossAttackType.WAVE_PLAIN) {
                        float facingYaw = (float) Math.toRadians(-player.getYRot());
                        net.ganyusbathwater.oririmod.util.MagicWaveUtil.spawnPlainWave(
                                serverLevel, origin, facingYaw, randomColor, 6f, player.getId());
                    }
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {
            setTargetUUID(stack, entity.getUUID());
            player.displayClientMessage(Component.literal("Locked target: " + entity.getName().getString())
                    .withStyle(net.minecraft.ChatFormatting.AQUA), true);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        BossAttackType current = getSelected(stack);
        tooltipComponents.add(Component.literal("Selected Attack: " + prettyName(current))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        UUID targetId = getTargetUUID(stack);
        if (targetId != null) {
            tooltipComponents
                    .add(Component.literal("Target Locked: Yes").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        } else {
            tooltipComponents
                    .add(Component.literal("Target Locked: No").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
        tooltipComponents.add(Component.literal("Shift+Right Click: Switch Attack")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltipComponents.add(
                Component.literal("Left Click Entity: Lock target").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static BossAttackType getSelected(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack
                .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) {
            String name = data.copyTag().getString(NBT_SELECTED);
            try {
                return name.isEmpty() ? BossAttackType.SWORD_PROJECTILE : BossAttackType.valueOf(name);
            } catch (IllegalArgumentException e) {
                return BossAttackType.SWORD_PROJECTILE;
            }
        }
        return BossAttackType.SWORD_PROJECTILE;
    }

    public static void setSelected(ItemStack stack, BossAttackType type) {
        net.minecraft.world.item.component.CustomData data = stack
                .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag tag = data != null ? data.copyTag() : new net.minecraft.nbt.CompoundTag();
        tag.putString(NBT_SELECTED, type.name());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static UUID getTargetUUID(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack
                .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().hasUUID(NBT_TARGET_UUID)) {
            return data.copyTag().getUUID(NBT_TARGET_UUID);
        }
        return null;
    }

    private static void setTargetUUID(ItemStack stack, UUID uuid) {
        net.minecraft.world.item.component.CustomData data = stack
                .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag tag = data != null ? data.copyTag() : new net.minecraft.nbt.CompoundTag();
        tag.putUUID(NBT_TARGET_UUID, uuid);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static BossAttackType nextAttack(BossAttackType current) {
        BossAttackType[] vals = BossAttackType.values();
        return vals[(current.ordinal() + 1) % vals.length];
    }

    public static String prettyName(BossAttackType type) {
        return switch (type) {
            case SWORD_PROJECTILE -> "Sword Projectile";
            case METEOR_SHOWER -> "Meteor Shower";
            case LASERBEAM_NORMAL -> "Laserbeam Normal";
            case LASERBEAM_CYLINDER -> "Laserbeam Cylinder";
            case LASERBEAM_CIRCLE -> "Laserbeam Circle";
            case LASERBEAM_GROUND -> "Laserbeam Ground";
            case LASERBEAM_STARBURST -> "Laserbeam Starburst";
            case INSTA_DEATH -> "Insta Death";
            case ROOT_ATTACK -> "Root Attack";
            case GROUND_SLAM -> "Ground Slam";
            case WAVE_CIRCULAR -> "Wave: Circular";
            case WAVE_CONE -> "Wave: Cone";
            case WAVE_PLAIN -> "Wave: Plain";
        };
    }
}
