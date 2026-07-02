package net.ganyusbathwater.oririmod.entity.custom;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ModBoatEntity extends Boat {
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE = SynchedEntityData.defineId(ModBoatEntity.class, EntityDataSerializers.INT);

    public ModBoatEntity(EntityType<? extends Boat> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ModBoatEntity(Level pLevel, double pX, double pY, double pZ) {
        this(ModEntities.MOD_BOAT.get(), pLevel);
        this.setPos(pX, pY, pZ);
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_ID_TYPE, 0);
    }

    public void setModVariant(ModBoatType pVariant) {
        this.entityData.set(DATA_ID_TYPE, pVariant.ordinal());
    }

    public ModBoatType getModVariant() {
        return ModBoatType.byId(this.entityData.get(DATA_ID_TYPE));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString("Type", this.getModVariant().getName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("Type", 8)) {
            this.setModVariant(ModBoatType.byName(pCompound.getString("Type")));
        }
    }

    @Override
    public Item getDropItem() {
        return switch (getModVariant()) {
            case SCARLET -> ModItems.SCARLET_BOAT.get();
            case ABYSS_CROWN -> ModItems.ABYSS_CROWN_BOAT.get();
            default -> ModItems.ELDER_BOAT.get();
        };
    }

    public enum ModBoatType {
        ELDER("elder"),
        SCARLET("scarlet"),
        ABYSS_CROWN("abyss_crown");

        private final String name;

        ModBoatType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static ModBoatType byId(int id) {
            ModBoatType[] types = values();
            if (id < 0 || id >= types.length) {
                id = 0;
            }
            return types[id];
        }

        public static ModBoatType byName(String name) {
            ModBoatType[] types = values();
            for (ModBoatType type : types) {
                if (type.getName().equals(name)) {
                    return type;
                }
            }
            return types[0];
        }
    }
}
