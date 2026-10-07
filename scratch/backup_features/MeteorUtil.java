package net.ganyusbathwater.oririmod.util;

import net.ganyusbathwater.oririmod.entity.MeteorEntity;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class MeteorUtil {
    private MeteorUtil() {}

    public static void callStrike(ServerLevel level, BlockPos target, int spawnHeight, float power, int fireRadius) {
        MeteorEntity meteor = ModEntities.METEOR.get().create(level);
        if (meteor == null) return;

        double sx = target.getX() + 0.5;
        double sy = target.getY() + Math.max(16, spawnHeight);
        double sz = target.getZ() + 0.5;

        meteor.moveTo(sx, sy, sz, 0f, 0f);
        meteor.setDeltaMovement(new Vec3(0, -1.4, 0)); // schneller Anflug
        meteor.configure(target, power, fireRadius);

        level.addFreshEntity(meteor);
    }
}