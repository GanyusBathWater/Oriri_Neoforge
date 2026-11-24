package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.mana.ModManaUtil;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class ArcanaItem extends ModRarityItem {

    public static final ResourceLocation ROOT_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "arcana/root");
    public static final ResourceLocation MASTERY_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "arcana/all_arcana");
    public static final String CRITERION_CONSUMED = "consumed";
    public static final String CRITERION_ALL_CONSUMED = "all_consumed";
    public static final int DUPLICATE_XP_LEVELS = 5;

    public enum ArcanaType {
        THE_FOOL("the_fool", "0", "The Fool", () -> ModItems.THE_FOOL.get()),
        THE_MAGICIAN("the_magician", "I", "The Magician", () -> ModItems.THE_MAGICIAN.get()),
        THE_HIGH_PRIESTESS("the_high_priestess", "II", "The High Priestess", () -> ModItems.THE_HIGH_PRIESTESS.get()),
        THE_EMPRESS("the_empress", "III", "The Empress", () -> ModItems.THE_EMPRESS.get()),
        THE_EMPEROR("the_emperor", "IV", "The Emperor", () -> ModItems.THE_EMPEROR.get()),
        THE_HIEROPHANT("the_hierophant", "V", "The Hierophant", () -> ModItems.THE_HIEROPHANT.get()),
        THE_LOVERS("the_lovers", "VI", "The Lovers", () -> ModItems.THE_LOVERS.get()),
        THE_CHARIOT("the_chariot", "VII", "The Chariot", () -> ModItems.THE_CHARIOT.get()),
        STRENGTH("strength", "VIII", "Strength", () -> ModItems.STRENGTH.get()),
        THE_HERMIT("the_hermit", "IX", "The Hermit", () -> ModItems.THE_HERMIT.get()),
        WHEEL_OF_FORTUNE("wheel_of_fortune", "X", "Wheel of Fortune", () -> ModItems.WHEEL_OF_FORTUNE.get()),
        JUSTICE("justice", "XI", "Justice", () -> ModItems.JUSTICE.get()),
        THE_HANGED_MAN("the_hanged_man", "XII", "The Hanged Man", () -> ModItems.THE_HANGED_MAN.get()),
        DEATH("death", "XIII", "Death", () -> ModItems.DEATH.get()),
        TEMPERANCE("temperance", "XIV", "Temperance", () -> ModItems.TEMPERANCE.get()),
        THE_DEVIL("the_devil", "XV", "The Devil", () -> ModItems.THE_DEVIL.get()),
        THE_TOWER("the_tower", "XVI", "The Tower", () -> ModItems.THE_TOWER.get()),
        THE_STAR("the_star", "XVII", "The Star", () -> ModItems.THE_STAR.get()),
        THE_MOON("the_moon", "XVIII", "The Moon", () -> ModItems.THE_MOON.get()),
        THE_SUN("the_sun", "XIX", "The Sun", () -> ModItems.THE_SUN.get()),
        JUDGEMENT("judgement", "XX", "Judgement", () -> ModItems.JUDGEMENT.get()),
        THE_WORLD("the_world", "XXI", "The World", () -> ModItems.THE_WORLD.get());

        private final String id;
        private final String numeral;
        private final String displayName;
        private final Supplier<? extends Item> itemSupplier;
        private final ResourceLocation advancementId;

        ArcanaType(String id, String numeral, String displayName, Supplier<? extends Item> itemSupplier) {
            this.id = id;
            this.numeral = numeral;
            this.displayName = displayName;
            this.itemSupplier = itemSupplier;
            this.advancementId = ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "arcana/" + id);
        }

        public String getId() {
            return id;
        }

        public String getNumeral() {
            return numeral;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Item getItem() {
            return itemSupplier.get();
        }

        public ResourceLocation getAdvancementId() {
            return advancementId;
        }

        public String getTitleKey() {
            return "advancement.oririmod.arcana." + id + ".title";
        }

        public String getDescriptionKey() {
            return "advancement.oririmod.arcana." + id + ".desc";
        }
    }

    private final ArcanaType arcanaType;

    public ArcanaItem(Properties properties, ModRarity rarity, ArcanaType arcanaType) {
        super(properties, rarity);
        this.arcanaType = arcanaType;
    }

    public ArcanaType getArcanaType() {
        return arcanaType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            grantRootAdvancement(sp);

            boolean alreadyUnlocked = hasUnlocked(sp, this.arcanaType);
            if (alreadyUnlocked) {
                sp.giveExperienceLevels(DUPLICATE_XP_LEVELS);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.85F, 1.15F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                            sp.getX(), sp.getY() + 1.0D, sp.getZ(),
                            18, 0.45D, 0.6D, 0.45D, 0.15D);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            sp.getX(), sp.getY() + 1.0D, sp.getZ(),
                            8, 0.4D, 0.5D, 0.4D, 0.05D);
                }
                sp.displayClientMessage(
                        Component.translatable("message.oririmod.arcana.duplicate_xp",
                                Component.translatable(this.getDescriptionId()), DUPLICATE_XP_LEVELS)
                                .withStyle(ChatFormatting.GREEN),
                        true);
            } else {
                awardArcanaAdvancement(sp, this.arcanaType);
                int unlockedCount = countUnlocked(sp);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.05F);
                level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.85F, 1.2F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANT,
                            sp.getX(), sp.getY() + 1.1D, sp.getZ(),
                            30, 0.5D, 0.7D, 0.5D, 0.25D);
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            sp.getX(), sp.getY() + 1.0D, sp.getZ(),
                            12, 0.35D, 0.5D, 0.35D, 0.06D);
                }
                sp.displayClientMessage(
                        Component.translatable("message.oririmod.arcana.attuned",
                                Component.translatable(this.getDescriptionId()), unlockedCount, ArcanaType.values().length)
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        true);

                checkAndGrantMastery(sp, true);
            }

            stack.consume(1, sp);
            sp.getCooldowns().addCooldown(this, 5);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.arcana.numeral", this.arcanaType.getNumeral())
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.oririmod.arcana.use")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static void grantRootAdvancement(ServerPlayer player) {
        AdvancementHolder root = player.server.getAdvancements().get(ROOT_ADVANCEMENT_ID);
        if (root != null) {
            var progress = player.getAdvancements().getOrStartProgress(root);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(root, criterion);
                }
            }
        }
    }

    public static boolean hasUnlocked(ServerPlayer player, ArcanaType type) {
        AdvancementHolder adv = player.server.getAdvancements().get(type.getAdvancementId());
        if (adv != null) {
            return player.getAdvancements().getOrStartProgress(adv).isDone();
        }
        return false;
    }

    private static void awardArcanaAdvancement(ServerPlayer player, ArcanaType type) {
        AdvancementHolder adv = player.server.getAdvancements().get(type.getAdvancementId());
        if (adv != null) {
            player.getAdvancements().award(adv, CRITERION_CONSUMED);
        }
    }

    public static int countUnlocked(ServerPlayer player) {
        int count = 0;
        for (ArcanaType type : ArcanaType.values()) {
            if (hasUnlocked(player, type)) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasAllArcana(ServerPlayer player) {
        return countUnlocked(player) >= ArcanaType.values().length;
    }

    /**
     * Checks whether the player has unlocked all 22 Major Arcana advancements and activates
     * the permanent +200 Max Mana and 2x Mana Regeneration mastery bonus.
     */
    public static void checkAndGrantMastery(ServerPlayer player, boolean announceIfNew) {
        if (!hasAllArcana(player)) {
            return;
        }

        boolean wasAlreadyMaster = ModManaUtil.hasArcanaMastery(player);
        AdvancementHolder masteryAdv = player.server.getAdvancements().get(MASTERY_ADVANCEMENT_ID);
        boolean masteryAdvNewlyDone = false;
        if (masteryAdv != null && !player.getAdvancements().getOrStartProgress(masteryAdv).isDone()) {
            player.getAdvancements().award(masteryAdv, CRITERION_ALL_CONSUMED);
            masteryAdvNewlyDone = true;
        }

        if (!wasAlreadyMaster) {
            ModManaUtil.setArcanaMastery(player, true);
            ModManaUtil.addMana(player, ModManaUtil.ARCANA_MASTERY_MAX_MANA_BONUS);
        }

        if (announceIfNew && (!wasAlreadyMaster || masteryAdvNewlyDone)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1.2D, player.getZ(),
                        40, 0.6D, 0.8D, 0.6D, 0.25D);
            }
            player.sendSystemMessage(Component.translatable("message.oririmod.arcana.mastery_unlocked")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    /**
     * Re-synchronizes the player's permanent Arcana Mastery bonus from their persistent
     * server advancements (called on login, respawn, and dimension change).
     */
    public static void syncMasteryFromAdvancements(ServerPlayer player) {
        AdvancementHolder masteryAdv = player.server.getAdvancements().get(MASTERY_ADVANCEMENT_ID);
        boolean hasMasteryAdv = masteryAdv != null && player.getAdvancements().getOrStartProgress(masteryAdv).isDone();
        boolean all22 = hasAllArcana(player);

        if (all22 && !hasMasteryAdv && masteryAdv != null) {
            player.getAdvancements().award(masteryAdv, CRITERION_ALL_CONSUMED);
            hasMasteryAdv = true;
        }

        boolean shouldHaveMastery = all22 || hasMasteryAdv;
        if (ModManaUtil.hasArcanaMastery(player) != shouldHaveMastery) {
            ModManaUtil.setArcanaMastery(player, shouldHaveMastery);
        }
    }
}
