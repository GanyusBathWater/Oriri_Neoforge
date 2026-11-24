package net.ganyusbathwater.oririmod.dungeon.data;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.dungeon.DungeonDefinition;
import net.ganyusbathwater.oririmod.dungeon.DungeonDefinitionRegistry;
import net.ganyusbathwater.oririmod.item.custom.WishUponAStarItem;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-side manager for applying, swapping, persisting, and awarding
 * per-dungeon "Wish Upon a Star" permanent attribute blessings.
 */
public final class StarWishManager {

    private StarWishManager() {}

    public static String resolveDungeonDisplayName(String dungeonId) {
        DungeonDefinition def = DungeonDefinitionRegistry.get(dungeonId);
        if (def != null && def.displayName() != null && !def.displayName().isBlank()) {
            return def.displayName();
        }
        if (dungeonId == null || dungeonId.isBlank()) {
            return "Unknown Dungeon";
        }
        String[] parts = dungeonId.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    /**
     * Re-synchronizes all permanent Wish Upon a Star attribute modifiers and mana regen
     * stacks on the given player from Overworld SavedData. Safe across death, respawn,
     * dimension change, and relog.
     */
    public static void reapplyAllWishes(ServerPlayer player) {
        PlayerDungeonData data = PlayerDungeonData.get(player.serverLevel());
        Map<String, String> allWishes = data.getAllStarWishes(player.getUUID());

        // 1. Remove all existing star_wish_* modifiers from every supported vanilla attribute
        for (StarWishAttribute attr : StarWishAttribute.values()) {
            if (attr.getVanillaAttribute() == null) continue;
            AttributeInstance inst = player.getAttribute(attr.getVanillaAttribute());
            if (inst == null) continue;

            List<ResourceLocation> toRemove = new ArrayList<>();
            for (AttributeModifier mod : inst.getModifiers()) {
                ResourceLocation id = mod.id();
                if (OririMod.MOD_ID.equals(id.getNamespace())
                        && id.getPath().startsWith(StarWishAttribute.MODIFIER_PREFIX)) {
                    toRemove.add(id);
                }
            }
            for (ResourceLocation id : toRemove) {
                inst.removeModifier(id);
            }
        }

        // 2. Re-apply every active dungeon's chosen blessing
        int manaRegenStacks = 0;
        for (Map.Entry<String, String> entry : allWishes.entrySet()) {
            String dungeonId = entry.getKey();
            StarWishAttribute attr = StarWishAttribute.byId(entry.getValue());
            if (attr == null) continue;

            if (attr == StarWishAttribute.MANA_REGEN) {
                manaRegenStacks++;
            } else if (attr.getVanillaAttribute() != null) {
                AttributeInstance inst = player.getAttribute(attr.getVanillaAttribute());
                if (inst != null) {
                    inst.addOrReplacePermanentModifier(attr.createModifier(dungeonId));
                }
            }
        }

        ModManaUtil.setWishManaRegenStacks(player, manaRegenStacks);

        // Clamp health cleanly if max health decreased after swapping away from MAX_HEALTH
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Validates the player's Wish Upon a Star selection packet, consumes 1 matching item,
     * updates that dungeon's permanent blessing, and plays visual/audio feedback.
     */
    public static void handleWishSelection(ServerPlayer player, String dungeonId, String chosenAttrId, boolean preferOffhand) {
        StarWishAttribute chosen = StarWishAttribute.byId(chosenAttrId);
        if (chosen == null || dungeonId == null || dungeonId.isBlank()) {
            return;
        }

        // Find matching Wish Upon a Star in the player's hands
        InteractionHand primary = preferOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        InteractionHand secondary = preferOffhand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(primary);
        if (!(stack.getItem() instanceof WishUponAStarItem)
                || !dungeonId.equals(WishUponAStarItem.getDungeonId(stack))) {
            stack = player.getItemInHand(secondary);
            if (!(stack.getItem() instanceof WishUponAStarItem)
                    || !dungeonId.equals(WishUponAStarItem.getDungeonId(stack))) {
                return;
            }
        }

        PlayerDungeonData data = PlayerDungeonData.get(player.serverLevel());
        String previousId = data.getStarWish(player.getUUID(), dungeonId);
        StarWishAttribute previous = StarWishAttribute.byId(previousId);

        // Prevent accidentally wasting the item on the exact same attribute already active for this dungeon
        if (previous == chosen) {
            return;
        }

        float prevMaxHealth = player.getMaxHealth();

        // Persist the new choice for this specific dungeon and refresh all modifiers
        data.setStarWish(player.getUUID(), dungeonId, chosen.getId());
        reapplyAllWishes(player);

        // If the player gained max health from this wish, heal them by the gained amount
        float newMaxHealth = player.getMaxHealth();
        if (newMaxHealth > prevMaxHealth) {
            player.heal(newMaxHealth - prevMaxHealth);
        }

        // Consume 1 item (respects Creative Mode infinite materials)
        stack.consume(1, player);

        // Celebratory starlight feedback
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.25F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.1F);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.1D, player.getZ(),
                36, 0.55D, 0.75D, 0.55D, 0.08D);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                24, 0.5D, 0.65D, 0.5D, 0.2D);

        String dungeonName = resolveDungeonDisplayName(dungeonId);
        Component attrName = Component.translatable(chosen.getNameKey());
        Component bonusText = Component.translatable(chosen.getBonusKey());
        if (previous != null) {
            player.sendSystemMessage(Component.translatable(
                    "message.oririmod.wish.changed",
                    dungeonName,
                    Component.translatable(previous.getNameKey()),
                    attrName,
                    bonusText
            ).withStyle(ChatFormatting.GOLD));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.oririmod.wish.granted",
                    dungeonName,
                    attrName,
                    bonusText
            ).withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * Grants 1 dungeon-bound Wish Upon a Star directly to a player (dropping at their feet if inventory is full).
     */
    public static void awardWishItem(ServerPlayer player, String dungeonId) {
        ItemStack wish = WishUponAStarItem.createForDungeon(dungeonId);
        if (!player.getInventory().add(wish)) {
            player.drop(wish, false);
        }
    }
}
