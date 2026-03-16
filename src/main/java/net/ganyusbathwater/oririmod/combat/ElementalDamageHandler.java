package net.ganyusbathwater.oririmod.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class ElementalDamageHandler {

    private ElementalDamageHandler() {
    }

    private static final String NBT_ROOT = "OririCombat";
    private static final String NBT_RESISTS = "ElementResists"; // map: elementName -> float
    private static final String NBT_IN_MULT = "IncomingMult"; // float
    private static final String NBT_OUT_MULT = "OutgoingMult"; // float

    public static void register() {
        NeoForge.EVENT_BUS.register(ElementalDamageHandler.class);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        Element defenderElement = EntityElementRegistry.getElement(target);
        Element attackerElement = Element.PHYSICAL;

        Entity direct = event.getSource().getDirectEntity();
        Entity owner = event.getSource().getEntity();

        if (direct != null) {
            attackerElement = EntityElementRegistry.getElement(direct);
        }

        if (attackerElement == Element.PHYSICAL && owner instanceof LivingEntity attacker) {
            ItemStack mainHand = attacker.getMainHandItem();
            attackerElement = ItemElementRegistry.getElement(mainHand);

            if (attackerElement == Element.PHYSICAL) {
                // --- ELEMENTAL ENCHANTMENT OVERRIDE ---
                if (!mainHand.isEmpty()) {
                    net.minecraft.core.RegistryAccess registryAccess = attacker.level().registryAccess();
                    net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> enchantmentRegistry = registryAccess
                            .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

                    if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                            enchantmentRegistry
                                    .getOrThrow(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_FIRE),
                            mainHand) > 0) {
                        attackerElement = Element.FIRE;
                    } else if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                            enchantmentRegistry
                                    .getOrThrow(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_WATER),
                            mainHand) > 0) {
                        attackerElement = Element.WATER;
                    } else if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                            enchantmentRegistry.getOrThrow(
                                    net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_NATURE),
                            mainHand) > 0) {
                        attackerElement = Element.NATURE;
                    } else if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                            enchantmentRegistry
                                    .getOrThrow(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_EARTH),
                            mainHand) > 0) {
                        attackerElement = Element.EARTH;
                    } else if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                            enchantmentRegistry
                                    .getOrThrow(net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_LIGHT),
                            mainHand) > 0) {
                        attackerElement = Element.LIGHT;
                    } else if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                            enchantmentRegistry.getOrThrow(
                                    net.ganyusbathwater.oririmod.enchantment.ModEnchantments.ELEMENT_DARKNESS),
                            mainHand) > 0) {
                        attackerElement = Element.DARKNESS;
                    }
                }

                if (attackerElement == Element.PHYSICAL) {
                    attackerElement = EntityElementRegistry.getElement(attacker);
                }
            }
        }

        // --- CUSTOM DAMAGE SOURCE OVERRIDE FOR ELEMENTS ---
        if (attackerElement != Element.PHYSICAL && attackerElement != Element.TRUE_DAMAGE) {
            net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> typeToUse = null;
            if (attackerElement == Element.FIRE)
                typeToUse = net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_FIRE;
            else if (attackerElement == Element.WATER)
                typeToUse = net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_WATER;
            else if (attackerElement == Element.NATURE)
                typeToUse = net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_NATURE;
            else if (attackerElement == Element.EARTH)
                typeToUse = net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_EARTH;
            else if (attackerElement == Element.LIGHT)
                typeToUse = net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_LIGHT;
            else if (attackerElement == Element.DARKNESS)
                typeToUse = net.ganyusbathwater.oririmod.damage.ModDamageTypes.ELEMENT_DARKNESS;

            if (typeToUse != null && !event.getSource().is(typeToUse)) {
                // To apply the actual damage source type into the event, we basically can't
                // change the Event's source directly
                // but we CAN cancel it and fire hurt with new source
                event.setCanceled(true);
                target.hurt(net.ganyusbathwater.oririmod.damage.ModDamageTypes.getElementalDamage(target.level(),
                        owner != null ? owner : direct, typeToUse), event.getAmount());
                return;
            }
        }

        // --- TRUE DAMAGE OVERRIDE ---
        if (attackerElement == Element.TRUE_DAMAGE) {
            // Check if the source is already TRUE_DAMAGE to prevent infinite recurrsion
            if (!event.getSource().is(net.ganyusbathwater.oririmod.damage.ModDamageTypes.TRUE_DAMAGE)) {
                event.setCanceled(true);
                // Re-apply damage using the custom True Damage type
                target.hurt(net.ganyusbathwater.oririmod.damage.ModDamageTypes.getTrueDamage(target.level(),
                        owner != null ? owner : direct), event.getAmount());
                return;
            }
        }

        float baseDamage = event.getAmount();

        // 1) Element\-Multiplier
        double elemMult = ElementEffectiveness.getMultiplier(attackerElement, defenderElement);
        float damage = (float) (baseDamage * elemMult);

        // 2) Spieler\-Resistenz (nur wenn Ziel Spieler)
        if (target instanceof Player playerTarget) {
            float resist = getPlayerResistance(playerTarget, attackerElement); // z.B. 0.25 => 25%
            damage = damage * (1.0f - clamp01(resist));

            // 3) Defense\-Multiplikator (eingehend)
            damage = damage * getPlayerIncomingMultiplier(playerTarget);
        }

        // 4) Outgoing\-Multiplikator (wenn Angreifer Spieler ist)
        if (owner instanceof Player playerAttacker) {
            damage = damage * getPlayerOutgoingMultiplier(playerAttacker);
        }

        if (damage < 0.0f)
            damage = 0.0f;
        event.setAmount(damage);
    }

    public static void setPlayerResistance(Player player, Element element, float resistFraction) {
        if (player == null || element == null)
            return;
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag res = root.getCompound(NBT_RESISTS);
        res.putFloat(element.name(), clamp01(resistFraction));
        root.put(NBT_RESISTS, res);
        player.getPersistentData().put(NBT_ROOT, root);
    }

    public static float getPlayerResistance(Player player, Element element) {
        if (player == null || element == null)
            return 0.0f;
        CompoundTag root = player.getPersistentData().getCompound(NBT_ROOT);
        CompoundTag res = root.getCompound(NBT_RESISTS);
        return clamp01(res.getFloat(element.name()));
    }

    public static void setPlayerIncomingMultiplier(Player player, float multiplier) {
        if (player == null)
            return;
        CompoundTag root = getOrCreateRoot(player);
        root.putFloat(NBT_IN_MULT, Math.max(0.0f, multiplier));
        player.getPersistentData().put(NBT_ROOT, root);
    }

    public static float getPlayerIncomingMultiplier(Player player) {
        if (player == null)
            return 1.0f;
        CompoundTag root = player.getPersistentData().getCompound(NBT_ROOT);
        float v = root.contains(NBT_IN_MULT) ? root.getFloat(NBT_IN_MULT) : 1.0f;
        return Math.max(0.0f, v);
    }

    public static void setPlayerOutgoingMultiplier(Player player, float multiplier) {
        if (player == null)
            return;
        CompoundTag root = getOrCreateRoot(player);
        root.putFloat(NBT_OUT_MULT, Math.max(0.0f, multiplier));
        player.getPersistentData().put(NBT_ROOT, root);
    }

    public static float getPlayerOutgoingMultiplier(Player player) {
        if (player == null)
            return 1.0f;
        CompoundTag root = player.getPersistentData().getCompound(NBT_ROOT);
        float v = root.contains(NBT_OUT_MULT) ? root.getFloat(NBT_OUT_MULT) : 1.0f;
        return Math.max(0.0f, v);
    }

    private static CompoundTag getOrCreateRoot(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(NBT_ROOT) ? data.getCompound(NBT_ROOT) : new CompoundTag();
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(1.0f, v);
    }
}