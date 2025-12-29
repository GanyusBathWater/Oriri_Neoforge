package net.ganyusbathwater.oririmod.effect.vestiges;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class MobSenseEffect implements VestigeEffect {

    private final double radius;

    public MobSenseEffect(double radius) {
        this.radius = radius;
    }

    @Override
    public void tick(VestigeContext ctx) {
        if (ctx == null) return;

        Player player = ctx.player();
        if (player == null) return;

        if (player.tickCount % 10 != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        int r = Math.max(1, (int) Math.ceil(radius));
        AABB box = player.getBoundingBox().inflate(r);

        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, false));
        }
    }
}

