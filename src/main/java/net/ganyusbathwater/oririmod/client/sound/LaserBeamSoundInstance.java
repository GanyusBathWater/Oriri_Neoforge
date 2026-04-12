package net.ganyusbathwater.oririmod.client.sound;

import net.ganyusbathwater.oririmod.entity.LaserBeamEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class LaserBeamSoundInstance extends AbstractTickableSoundInstance {
    private final LaserBeamEntity beam;
    private int lifeTime = 0;

    public LaserBeamSoundInstance(LaserBeamEntity beam) {
        super(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.beam = beam;
        this.looping = false;
        this.delay = 0;
        this.volume = 0.01f; // Must be strictly > 0.0f or the SoundEngine immediately deletes the sound!
        this.pitch = 0.5f; // Deep demonic rumble
        this.x = beam.getX();
        this.y = beam.getY();
        this.z = beam.getZ();
    }

    public static void play(LaserBeamEntity beam) {
        Minecraft.getInstance().getSoundManager().play(new LaserBeamSoundInstance(beam));
    }

    @Override
    public void tick() {
        this.lifeTime++;
        
        // If the beam is abruptly dead, kill the sound.
        if (this.beam.isRemoved()) {
            this.stop();
            return;
        }

        // Calculate 1-second (20 ticks) fade in and fade out
        float maxVolume = 1.5f;
        float fadeTicks = 20.0f;
        int age = this.beam.getAgeTicks();
        int duration = this.beam.getDurationTicks();
        
        if (this.lifeTime <= fadeTicks) {
            // Fade in over the first 20 ticks
            this.volume = maxVolume * (this.lifeTime / fadeTicks);
        } else if (age >= duration - fadeTicks) {
            // Fade out over the last 20 ticks
            float remaining = duration - age;
            this.volume = maxVolume * (Math.max(0.0f, remaining) / fadeTicks);
            if (this.volume <= 0.0f) {
                this.stop();
                return;
            }
        } else {
            this.volume = maxVolume;
        }

        // Keep the sound localized to the beam's midpoint
        this.x = this.beam.getX();
        this.y = this.beam.getY();
        this.z = this.beam.getZ();

        // At 0.5 pitch, the wither sound lasts ~17 seconds (340 ticks). 
        // We manually loop it right before it fades completely so it's seamless.
        if (this.lifeTime >= 280) {
            // CRITICAL: Schedule the new sound to start on the next frame to avoid 
            // ConcurrentModificationException in the SoundEngine's tick loop!
            Minecraft.getInstance().execute(() -> play(this.beam));
            this.stop(); // stop this instance since a new one took over
        }
    }
}
