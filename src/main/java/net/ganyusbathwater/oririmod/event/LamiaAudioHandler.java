package net.ganyusbathwater.oririmod.event;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.custom.LamiaEntity;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * Handles audio dampening for Lamia entities and players equipped with Lamia Scale.
 * Enforces the "Silent Predator" profile: completely mutes two-legged bipedal footstep
 * and landing stomps on both server and client sound engines.
 */
@EventBusSubscriber(modid = OririMod.MOD_ID)
public class LamiaAudioHandler {

    public static boolean isLamiaActive(Player player) {
        if (player == null) return false;
        boolean hasCurio = CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(ModItems.LAMIA_SCALE.get()).isPresent())
                .orElse(false);
        if (hasCurio) return true;
        return player.getMainHandItem().is(ModItems.LAMIA_SCALE.get()) ||
               player.getOffhandItem().is(ModItems.LAMIA_SCALE.get());
    }

    public static boolean isFootstepSound(ResourceLocation loc) {
        if (loc == null) return false;
        String path = loc.getPath();
        return path.endsWith(".step") ||
               path.endsWith(".fall") ||
               path.equals("entity.player.step") ||
               path.contains(".step.") ||
               path.contains(".fall.");
    }

    @SubscribeEvent
    public static void onPlaySoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        Holder<SoundEvent> soundHolder = event.getSound();
        if (soundHolder == null) return;
        if (isFootstepSound(soundHolder.value().getLocation())) {
            Entity entity = event.getEntity();
            if (entity instanceof LamiaEntity) {
                event.setCanceled(true);
            } else if (entity instanceof Player player && isLamiaActive(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlaySoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        Holder<SoundEvent> soundHolder = event.getSound();
        if (soundHolder == null) return;
        if (isFootstepSound(soundHolder.value().getLocation())) {
            Vec3 pos = event.getPosition();
            Level level = event.getLevel();
            if (level != null && pos != null) {
                AABB box = new AABB(pos.x - 1.5D, pos.y - 1.5D, pos.z - 1.5D,
                                    pos.x + 1.5D, pos.y + 1.5D, pos.z + 1.5D);
                List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, box, LamiaAudioHandler::isLamiaActive);
                if (!nearbyPlayers.isEmpty()) {
                    event.setCanceled(true);
                    return;
                }
                List<LamiaEntity> nearbyLamias = level.getEntitiesOfClass(LamiaEntity.class, box);
                if (!nearbyLamias.isEmpty()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @EventBusSubscriber(modid = OririMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientSoundHandler {
        @SubscribeEvent
        public static void onClientPlaySound(PlaySoundEvent event) {
            SoundInstance sound = event.getSound();
            if (sound == null || sound.getLocation() == null) return;
            if (isFootstepSound(sound.getLocation())) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && isLamiaActive(mc.player)) {
                    double distSq = mc.player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ());
                    if (distSq < 4.0D) {
                        event.setSound(null);
                        return;
                    }
                }
                if (mc.level != null) {
                    AABB box = new AABB(sound.getX() - 1.5D, sound.getY() - 1.5D, sound.getZ() - 1.5D,
                                        sound.getX() + 1.5D, sound.getY() + 1.5D, sound.getZ() + 1.5D);
                    List<Player> nearbyLamias = mc.level.getEntitiesOfClass(Player.class, box, LamiaAudioHandler::isLamiaActive);
                    if (!nearbyLamias.isEmpty()) {
                        event.setSound(null);
                        return;
                    }
                    List<LamiaEntity> lamiaMobs = mc.level.getEntitiesOfClass(LamiaEntity.class, box);
                    if (!lamiaMobs.isEmpty()) {
                        event.setSound(null);
                    }
                }
            }
        }
    }
}
