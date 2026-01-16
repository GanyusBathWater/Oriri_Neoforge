package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.events.world.WorldEventManager;
import net.ganyusbathwater.oririmod.events.world.WorldEventType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(LootTable.class)
public class LuckyLootTableMixin {

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V", at = @At("TAIL"))
    private void oriri_onGenerateLoot(LootContext context, Consumer<ItemStack> consumer, CallbackInfo ci) {
        ServerLevel level = context.getLevel();
        if (level != null) {
            WorldEventManager manager = WorldEventManager.get(level);
            if (manager.isEventActive(WorldEventType.GREEN_MOON)) {
                if (level.random.nextFloat() < 0.25F) { // 25% Chance auf doppelten Loot
                    LootTable table = (LootTable) (Object) this;
                    table.getRandomItems(context, consumer); // Führt die Loot-Generierung einfach nochmal aus
                }
            }
        }
    }
}