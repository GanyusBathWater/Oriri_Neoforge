package net.ganyusbathwater.oririmod.fluid;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.AetherLiquidBlock;
import net.ganyusbathwater.oririmod.fluid.custom.AetherFlowingFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, OririMod.MOD_ID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, OririMod.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, OririMod.MOD_ID);

    public static final DeferredHolder<Fluid, BaseFlowingFluid> AETHER_SOURCE =
            FLUIDS.register("aether", () -> new AetherFlowingFluid.Source(makeProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid> AETHER_FLOWING =
            FLUIDS.register("aether_flowing", () -> new AetherFlowingFluid.Flowing(makeProperties()));

    public static final DeferredHolder<Block, LiquidBlock> AETHER_BLOCK =
            BLOCKS.register("aether_block", () ->
                    new AetherLiquidBlock(
                            AETHER_SOURCE.get(),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)
                    )
            );

    public static final DeferredHolder<Item, BucketItem> AETHER_BUCKET =
            ITEMS.register("aether_bucket", () ->
                    new BucketItem(
                            AETHER_SOURCE.get(),
                            new Item.Properties()
                                    .stacksTo(1)
                                    .craftRemainder(Items.BUCKET)
                    )
            );

    private static BaseFlowingFluid.Properties makeProperties() {
        return new BaseFlowingFluid.Properties(
                ModFluidTypes.AETHER_TYPE,
                AETHER_SOURCE,
                AETHER_FLOWING
        )
                .block(AETHER_BLOCK)
                .bucket(AETHER_BUCKET)
                .tickRate(10)
                .levelDecreasePerBlock(2)
                .slopeFindDistance(4);
    }

    public static void register(IEventBus bus) {
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
