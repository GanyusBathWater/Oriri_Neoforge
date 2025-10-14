package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.entity.MagicBoltEntity;
import net.ganyusbathwater.oririmod.util.MagicBoltAbility;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagicBoltItem extends Item implements ModRarityCarrier {
    private final MagicBoltAbility ability;
    private final int cooldown;
    private final ModRarity rarity;

    public MagicBoltItem(Properties props, MagicBoltAbility ability, int cooldown, ModRarity rarity) {
        super(props);
        this.ability = ability;
        this.cooldown = cooldown;
        this.rarity = rarity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MagicBoltEntity bolt = new MagicBoltEntity(level, player);
            bolt.setAbility(ability);
            float speed = ability == MagicBoltAbility.SONIC ? 3.0F : 1.6F;
            bolt.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            bolt.launchStraight(player, speed);
            level.addFreshEntity(bolt);
            player.getCooldowns().addCooldown(this, cooldown);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }
}
