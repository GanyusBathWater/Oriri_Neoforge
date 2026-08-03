package net.ganyusbathwater.oririmod.client;

import net.ganyusbathwater.oririmod.network.packet.PlayDungeonMusicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side handler for playing/stopping dungeon music.
 */
@OnlyIn(Dist.CLIENT)
public class DungeonMusicHandler {

    private static final List<SoundInstance> ACTIVE_TRACKS = new ArrayList<>();

    public static void handle(PlayDungeonMusicPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        SoundManager soundManager = mc.getSoundManager();

        if (payload.stopAll()) {
            for (SoundInstance track : ACTIVE_TRACKS) {
                soundManager.stop(track);
            }
            ACTIVE_TRACKS.clear();
        }

        if (payload.track() != null) {
            // Create a custom sound instance
            SoundEvent event = SoundEvent.createVariableRangeEvent(payload.track());
            
            SimpleSoundInstance sound = new SimpleSoundInstance(
                    payload.track(),
                    SoundSource.MUSIC,
                    1.0F, // Volume
                    1.0F, // Pitch
                    net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom(),
                    payload.looping(),
                    0,    // Delay
                    SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0, // x, y, z
                    true  // Relative
            );

            soundManager.play(sound);
            ACTIVE_TRACKS.add(sound);
        }
    }
}
