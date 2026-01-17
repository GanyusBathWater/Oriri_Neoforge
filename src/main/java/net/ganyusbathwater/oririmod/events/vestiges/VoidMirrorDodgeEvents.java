package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.MirrorOfTheVoidEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "oririmod")
public final class VoidMirrorDodgeEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoidMirrorDodgeEvents.class);

    private VoidMirrorDodgeEvents() {}

    private static final String NBT_KEY = "Oriri_VoidMirror_Dodge";
    private static final String NBT_COOLDOWN_SECONDS = "CooldownSeconds";

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        final float incoming = event.getOriginalDamage();
        final boolean enabled = MirrorOfTheVoidEffect.isDodgeEnabled(player);
        final int activeCd = getCooldownSecondsRaw(player);
        final int baseCd = MirrorOfTheVoidEffect.getBaseCooldownSeconds(player);

        if (!enabled) return;
        if (incoming <= 0.0f) return;
        if (incoming > player.getMaxHealth()) return;
        if (activeCd > 0) return;

        event.setNewDamage(0.0f);
        setNewDodgeCooldown(player);
    }

    public static void setNewDodgeCooldown(Player player) {
        if (player == null) return;
        if (player.level().isClientSide) return;

        int baseCd = MirrorOfTheVoidEffect.getBaseCooldownSeconds(player);
        if (baseCd < 0) baseCd = 0;

        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);
        tag.putInt(NBT_COOLDOWN_SECONDS, baseCd);
        root.put(NBT_KEY, tag);

        // HUD\-Sync (Client Cache)
        MirrorOfTheVoidEffect.setClientCooldownSeconds(player.getUUID(), baseCd);
    }

    public static void tick(Player player) {}

    private static int getCooldownSecondsRaw(Player player) {
        if (player == null) return 0;
        CompoundTag root = player.getPersistentData();
        CompoundTag tag = root.getCompound(NBT_KEY);
        return tag.getInt(NBT_COOLDOWN_SECONDS);
    }
}