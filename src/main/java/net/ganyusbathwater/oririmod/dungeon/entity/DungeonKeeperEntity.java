package net.ganyusbathwater.oririmod.dungeon.entity;

import net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry;
import net.ganyusbathwater.oririmod.dungeon.party.DungeonParty;
import net.ganyusbathwater.oririmod.dungeon.party.DungeonPartyManager;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.OpenDungeonSelectionPayload;
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
 * The Dungeon Keeper NPC. When right-clicked, it opens either the DungeonSelectionScreen
 * or the Party Management Screen.
 */
public class DungeonKeeperEntity extends PathfinderMob {

    public DungeonKeeperEntity(EntityType<? extends DungeonKeeperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0); // Stands still
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS; // client side

        DungeonPartyManager partyManager = DungeonPartyManager.get(sp.serverLevel());
        DungeonParty party = partyManager.getPartyForPlayer(sp.getUUID());

        if (party == null) {
            // Player is not in a party. Send Dungeon Selection Screen.
            sendSelectionScreen(sp);
        } else {
            // Player is in a party. Open Party Management Screen.
            sendOpenScreen(sp, party, party.getDungeonId());
        }

        return InteractionResult.CONSUME;
    }
    
    private void sendSelectionScreen(ServerPlayer sp) {
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> descs = new ArrayList<>();
        List<String> lores = new ArrayList<>();
        List<String> previews = new ArrayList<>();
        
        for (var def : DungeonDefinitionRegistry.all()) {
            ids.add(def.id());
            names.add(def.displayName());
            descs.add(def.description());
            lores.add(def.loreText());
            previews.add(def.previewTexture() != null ? def.previewTexture().toString() : "");
        }
        
        PacketDistributor.sendToPlayer(sp, new OpenDungeonSelectionPayload(ids, names, descs, lores, previews));
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
