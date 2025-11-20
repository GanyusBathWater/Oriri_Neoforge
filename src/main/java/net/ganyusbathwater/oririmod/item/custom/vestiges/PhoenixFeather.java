// java
package net.ganyusbathwater.oririmod.item.custom.vestiges;

import net.ganyusbathwater.oririmod.item.custom.VestigeItem;
import net.ganyusbathwater.oririmod.effect.vestiges.VestigeEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Vestige: Phoenix Feather
 * Level 1: 20 % Wiederbelebungschance
 * Level 2: 35 % Wiederbelebungschance
 * Level 3: 50 % Wiederbelebungschance
 */

public class PhoenixFeather extends VestigeItem {

    public PhoenixFeather(Properties props) {
        super(props, List.of(
                List.of(resurrectionEffect()), // Level 1
                List.of(resurrectionEffect()), // Level 2
                List.of(resurrectionEffect())  // Level 3
        ));
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        // Standardmäßig alle Level freigeschaltet
        this.setUnlockedLevel(stack, this.getMaxLevel());
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        super.inventoryTick(stack, level, entity, slotId, selected);
        if (level.isClientSide) return;
        // Sicherstellen, dass alle Level freigeschaltet bleiben
        if (this.getUnlockedLevel(stack) < this.getMaxLevel()) {
            this.setUnlockedLevel(stack, this.getMaxLevel());
        }
    }

    /**
     * Effekt, der vom Death-/Hurt-Event genutzt wird, um zu prüfen,
     * ob der Spieler wiederbelebt werden soll.
     *
     * Die eigentliche Auslösung passiert in PhoenixFeatherEvents,
     * dort wird dieser Effekt über VestigeItem.applyTick nicht
     * automatisch aufgerufen, sondern explizit ausgewertet.
     */
    private static VestigeEffect resurrectionEffect() {
        return new VestigeEffect() {

            /**
             * Hilfsmethode, die von außen genutzt werden kann,
             * um die Wiederbelebungs-Chance für eine bestimmte
             * Phoenix Feather Instanz zu prüfen.
             */
            public boolean tryResurrect(ServerPlayer player, ItemStack stack, int lvl) {
                if (!(stack.getItem() instanceof PhoenixFeather self)) return false;

                int unlocked = self.getUnlockedLevel(stack);
                if (lvl != unlocked) return false;

                double chance = switch (unlocked) {
                    case 1 -> 0.20D;
                    case 2 -> 0.35D;
                    case 3 -> 0.50D;
                    default -> 0.0D;
                };

                if (chance <= 0.0D) return false;

                double roll = ThreadLocalRandom.current().nextDouble();
                if (roll > chance) return false;

                // "Totem"-ähnliche Wiederbelebung: Leben wiederherstellen,
                // Feuer löschen, evtl. zukünftige Effekte ergänzen.
                float newHealth = Math.max(1.0F, player.getMaxHealth() * 0.5F);
                player.setHealth(newHealth);
                player.clearFire();

                player.level().playSound(player, player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.TOTEM_USE,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        1.0F, 1.0F);

                return true;
            }

            @Override
            public void tick(ServerPlayer player, ItemStack stack, int lvl) {
                // Kein Tick-Effekt nötig; die Wiederbelebung wird über
                // ein Event (z.B. LivingHurtEvent / LivingDeathEvent) ausgelöst.
            }
        };
    }

    @Override
    public String getTranslationKeyBase() {
        return "tooltip.oririmod.vestige.phoenix_feather";
    }
}