package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.damage.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class SplinterSpiderEntity extends Spider {

    public SplinterSpiderEntity(EntityType<? extends Spider> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void die(DamageSource pDamageSource) {
        super.die(pDamageSource);

        if (!this.level().isClientSide) {
            boolean isFire = pDamageSource.is(DamageTypeTags.IS_FIRE) || pDamageSource.is(ModDamageTypes.ELEMENT_FIRE);

            if (!isFire) {
                int count = 2 + this.random.nextInt(3); // 2 to 4
                for (int i = 0; i < count; i++) {
                    CaveSpider caveSpider = EntityType.CAVE_SPIDER.create(this.level());
                    if (caveSpider != null) {
                        caveSpider.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                        caveSpider.finalizeSpawn((ServerLevelAccessor) this.level(),
                                this.level().getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.EVENT, null);
                        this.level().addFreshEntity(caveSpider);
                    }
                }
            }
        }
    }
}
