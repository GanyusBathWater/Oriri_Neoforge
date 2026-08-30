package net.ganyusbathwater.oririmod.client.render.entity.template;

import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.EntityType;

public abstract class AbstractPlayerCosmeticAnimatable implements GeoReplacedEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
