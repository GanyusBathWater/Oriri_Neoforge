package net.ganyusbathwater.oririmod.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class GodsTrialData extends SavedData {
    private boolean isActive = false;

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("GodsTrialActive", isActive);
        return tag;
    }

    public static GodsTrialData load(CompoundTag tag, HolderLookup.Provider provider) {
        GodsTrialData data = new GodsTrialData();
        data.isActive = tag.getBoolean("GodsTrialActive");
        return data;
    }

    public static GodsTrialData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            return overworld.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(GodsTrialData::new, GodsTrialData::load),
                    "oririmod_gods_trial"
            );
        }
        return new GodsTrialData();
    }
}
