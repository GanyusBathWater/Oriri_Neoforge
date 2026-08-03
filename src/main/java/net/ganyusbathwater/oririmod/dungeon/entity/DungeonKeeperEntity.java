package net.ganyusbathwater.oririmod.dungeon.entity;

import net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry;
import net.ganyusbathwater.oririmod.dungeon.party.DungeonParty;
import net.ganyusbathwater.oririmod.dungeon.party.DungeonPartyManager;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Dungeon Keeper NPC. When right-clicked, it opens the DungeonKeeperScreen.
 *
 * NBT data:
 *  - "dungeon_id" : String — which DungeonDefinition this keeper manages
 *  - "keeper_name": String — display name (defaults to "Dungeon Keeper")
 */
public class DungeonKeeperEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_DUNGEON_ID =
            SynchedEntityData.defineId(DungeonKeeperEntity.class, EntityDataSerializers.STRING);

    public DungeonKeeperEntity(EntityType<? extends DungeonKeeperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0); // Stands still
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DUNGEON_ID, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public String getDungeonId() { return this.entityData.get(DATA_DUNGEON_ID); }
    public void setDungeonId(String id) { this.entityData.set(DATA_DUNGEON_ID, id); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("dungeon_id", getDungeonId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("dungeon_id")) setDungeonId(tag.getString("dungeon_id"));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS; // client side

        String dungeonId = getDungeonId();
        if (dungeonId.isBlank()) {
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("[OririMod] This Dungeon Keeper has no dungeon_id set!"));
            return InteractionResult.FAIL;
        }

        // Create or retrieve the party led by this player for this dungeon
        DungeonPartyManager partyManager = DungeonPartyManager.get(sp.serverLevel());
        DungeonParty party = partyManager.getPartyForPlayer(sp.getUUID());

        if (party == null || !party.getDungeonId().equals(dungeonId)) {
            // Start a fresh party for this dungeon
            party = partyManager.createParty(sp.getUUID(), dungeonId);
        }

        sendOpenScreen(sp, party, dungeonId);
        return InteractionResult.CONSUME;
    }

    private void sendOpenScreen(ServerPlayer sp, DungeonParty party, String dungeonId) {
        var definition = DungeonDefinitionRegistry.get(dungeonId);
        String displayName = definition != null ? definition.displayName() : dungeonId;
        String description = definition != null ? definition.description() : "";

        List<UUID> memberIds = new ArrayList<>();
        List<String> memberNames = new ArrayList<>();
        List<String> memberStatuses = new ArrayList<>();

        for (var entry : party.getMemberStatuses().entrySet()) {
            memberIds.add(entry.getKey());
            var memberPlayer = sp.getServer().getPlayerList().getPlayer(entry.getKey());
            memberNames.add(memberPlayer != null ? memberPlayer.getName().getString() : "Unknown");
            memberStatuses.add(entry.getValue().name());
        }

        PacketDistributor.sendToPlayer(sp, new OpenDungeonScreenPayload(
                this.getId(),
                dungeonId,
                displayName,
                description,
                party.getPartyId(),
                party.getLeaderId(),
                memberIds,
                memberNames,
                memberStatuses
        ));
    }
}
