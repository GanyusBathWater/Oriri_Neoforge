package net.ganyusbathwater.oririmod.events.vestiges;

import net.ganyusbathwater.oririmod.effect.vestiges.PhoenixFeatherEffect;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = "oririmod")
public final class PhoenixFeatherEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        if (player.level().isClientSide) return;

        float incoming = event.getOriginalDamage();
        if (incoming <= 0.0f) return;

        // Nur bei tödlichem Treffer versuchen (vor Schadensanwendung)
        if (player.getHealth() - incoming > 0.0f) return;

        // Nur wenn PhoenixFeather als Curio ausgerüstet
        if (!hasCurioEquipped(player, ModItems.PHOENIX_FEATHER.get())) return;

        // Chance kommt aus dem Vestige-Level via NBT (gesetzt durch PhoenixFeatherEffect.tick/onEquip)
        float chancePercent = PhoenixFeatherEffect.getNegatingChancePercent(player);
        if (chancePercent <= 0.0f) return;

        boolean saved = PhoenixFeatherEffect.tryNegateFatalDamageLikeTotem(player, event.getSource(), chancePercent);
        if (saved) {
            event.setNewDamage(0.0f);
        }
    }

    private static boolean hasCurioEquipped(Player player, Item item) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(item).isPresent())
                .orElse(false);
    }
}