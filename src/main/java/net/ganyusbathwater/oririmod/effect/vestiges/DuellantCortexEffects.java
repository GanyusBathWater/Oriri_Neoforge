// java
package net.ganyusbathwater.oririmod.effect.vestiges;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class DuellantCortexEffects {

    private static final ResourceLocation ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "duellant_cortex_armor");
    private static final ResourceLocation DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, "duellant_cortex_damage");

    private DuellantCortexEffects() {}

    public static VestigeEffect nearbyHostileScaling(int radius) {
        return new VestigeEffect() {
            @Override
            public void tick(ServerPlayer player, ItemStack stack, int level) {
                ServerLevel levelWorld = player.serverLevel();

                if (player.tickCount % 10 != 0) return;

                int r = Math.max(1, radius);
                AABB box = player.getBoundingBox().inflate(r);
                int hostileCount = 0;
                for (Mob mob : levelWorld.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
                    // \-> hier evtl. noch zusätzliche Filter (feindlich etc.) einbauen
                    hostileCount++;
                }

                // Gegneranzahl im Manager speichern
                NetworkHandler.sendDuellantCortexHostileTo(player, hostileCount);

                AttributeInstance armorInst = player.getAttribute(Attributes.ARMOR);
                AttributeInstance attackInst = player.getAttribute(Attributes.ATTACK_DAMAGE);

                AttributeModifier oldArmor = armorInst.getModifier(ARMOR_ID);
                if (oldArmor != null) {
                    armorInst.removeModifier(oldArmor);
                }
                AttributeModifier oldDamage = attackInst.getModifier(DAMAGE_ID);
                if (oldDamage != null) {
                    attackInst.removeModifier(oldDamage);
                }

                double armorPerMob;
                double damagePerMob;

                if (level < 3) {
                    // Beispielwerte annehmen, falls du sie im Originalcode hast, dort übernehmen
                    armorPerMob = 0.10D;
                    damagePerMob = (level >= 2) ? 0.05D : 0.0D;
                } else {
                    armorPerMob = 0.15D;
                    damagePerMob = 0.075D;
                }

                double baseArmor = armorInst.getBaseValue();
                double totalArmorBonus = baseArmor * armorPerMob * hostileCount;
                if (totalArmorBonus != 0.0D) {
                    AttributeModifier armorMod = new AttributeModifier(
                            ARMOR_ID,
                            totalArmorBonus,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    armorInst.addTransientModifier(armorMod);
                }

                double baseDamage = attackInst.getBaseValue();
                double totalDamageBonus = baseDamage * damagePerMob * hostileCount;
                if (totalDamageBonus != 0.0D) {
                    AttributeModifier dmgMod = new AttributeModifier(
                            DAMAGE_ID,
                            totalDamageBonus,
                            AttributeModifier.Operation.ADD_VALUE
                    );
                    attackInst.addTransientModifier(dmgMod);

                }
            }

            @Override
            public void onRemovedFromExtraInventory(ServerPlayer player, ItemStack stack, int level) {
                DuellantCortexManager.setHostileCount(player, 0);
            }
        };
    }
}