package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.dungeon.data.PlayerDungeonData;
import net.ganyusbathwater.oririmod.dungeon.data.StarWishManager;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.ganyusbathwater.oririmod.item.component.ModDataComponents;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Secret dungeon reward item ("Wish Upon a Star").
 * Carries a {@link ModDataComponents#WISH_DUNGEON_ID} component tying it to a specific dungeon
 * so every main dungeon grants its own unique permanent attribute upgrade slot, while reusing
 * the same dungeon's Wish lets the player swap that dungeon's chosen attribute.
 */
public class WishUponAStarItem extends ModRarityItem {

    public static final String DEFAULT_DUNGEON_ID = "excavation_site_six";

    public WishUponAStarItem(Properties properties, ModRarity rarity) {
        super(properties, rarity);
    }

    public static ItemStack createForDungeon(String dungeonId) {
        ItemStack stack = new ItemStack(ModItems.WISH_UPON_A_STAR.get());
        String resolved = (dungeonId == null || dungeonId.isBlank()) ? DEFAULT_DUNGEON_ID : dungeonId;
        stack.set(ModDataComponents.WISH_DUNGEON_ID.get(), resolved);
        return stack;
    }

    public static String getDungeonId(ItemStack stack) {
        String id = stack.get(ModDataComponents.WISH_DUNGEON_ID.get());
        return (id == null || id.isBlank()) ? DEFAULT_DUNGEON_ID : id;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            String dungeonId = getDungeonId(stack);
            String dungeonDisplayName = StarWishManager.resolveDungeonDisplayName(dungeonId);
            String currentAttrId = PlayerDungeonData.get(sp.serverLevel()).getStarWish(sp.getUUID(), dungeonId);
            NetworkHandler.sendOpenWishScreen(sp, dungeonId, dungeonDisplayName, currentAttrId, hand == InteractionHand.OFF_HAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);
        String dungeonId = getDungeonId(stack);
        String displayName = StarWishManager.resolveDungeonDisplayName(dungeonId);
        tooltipComponents.add(Component.translatable("tooltip.oririmod.wish_upon_a_star.dungeon", displayName)
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.oririmod.wish_upon_a_star.use")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
