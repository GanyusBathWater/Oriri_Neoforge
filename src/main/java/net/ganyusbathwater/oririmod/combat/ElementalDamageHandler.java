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

        // --- TRUE DAMAGE OVERRIDE ---
        if (attackerElement == Element.TRUE_DAMAGE) {
            // Bypass armor, enchantments, and resistances natively via NeoForge's reduction modifiers
            // without canceling the event (canceling aborts Player.attack and prevents weapon durability loss)
            event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, (c, r) -> 0.0F);
            event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ENCHANTMENTS, (c, r) -> 0.0F);
            event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.INNATE_RESISTANCE, (c, r) -> 0.0F);
            event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.MOB_EFFECTS, (c, r) -> 0.0F);
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

        if (owner instanceof LivingEntity attacker && direct == attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.getItem() instanceof net.ganyusbathwater.oririmod.item.custom.CustomScytheItem scythe) {
                scythe.applyScytheEffects(weapon, target, attacker);
            }
        }
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
        float baseResistance = clamp01(res.getFloat(element.name()));

        // Add Armor Bonuses
        if (player.getInventory().getArmor(0).getItem() instanceof net.ganyusbathwater.oririmod.item.custom.ModArmorItem armorItem) {
            if (armorItem.hasPlayerCorrectArmorOn(armorItem.getMaterial(), player)) {
                if (element == Element.NATURE && armorItem.getMaterial() == net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.ANCIENT_ARMOR_MATERIAL) {
                    baseResistance += 0.25f;
                } else if (element == Element.DARKNESS && armorItem.getMaterial() == net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL) {
                    baseResistance += 0.25f;
                } else if (element == Element.LIGHT && armorItem.getMaterial() == net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL) {
                    baseResistance += 0.25f;
                } else if (element == Element.EARTH && armorItem.getMaterial() == net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL) {
                    baseResistance += 0.25f;
                } else if (element == Element.FIRE && armorItem.getMaterial() == net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.MOLTEN_ARMOR_MATERIAL) {
                    baseResistance += 0.25f;
                } else if (element == Element.WATER && armorItem.getMaterial() == net.ganyusbathwater.oririmod.item.custom.ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL) {
                    baseResistance += 0.25f;
                }
            }
        }

        return clamp01(baseResistance);
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