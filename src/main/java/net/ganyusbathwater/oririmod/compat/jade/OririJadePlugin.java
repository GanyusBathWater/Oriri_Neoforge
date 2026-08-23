package net.ganyusbathwater.oririmod.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class OririJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(EntityElementComponentProvider.INSTANCE, net.minecraft.world.entity.Entity.class);
    }
}
