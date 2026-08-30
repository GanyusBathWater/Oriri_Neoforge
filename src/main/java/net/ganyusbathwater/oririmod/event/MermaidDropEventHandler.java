package net.ganyusbathwater.oririmod.event;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Random;

@EventBusSubscriber(modid = OririMod.MOD_ID)
public class MermaidDropEventHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().getType() == ModEntities.MERMAID.get()) {
            double chance = 0.0005; // Base chance 0.05%
            if (RANDOM.nextDouble() <= chance) {
                ItemStack scale = new ItemStack(ModItems.MERMAID_SCALE.get());
                scale.set(DataComponents.DYED_COLOR, new DyedItemColor(RANDOM.nextInt(0xFFFFFF), true));
                
                ItemEntity itemEntity = new ItemEntity(
                        event.getEntity().level(),
                        event.getEntity().getX(),
                        event.getEntity().getY(),
                        event.getEntity().getZ(),
                        scale
                );
                event.getDrops().add(itemEntity);
            }
        }
    }
}
