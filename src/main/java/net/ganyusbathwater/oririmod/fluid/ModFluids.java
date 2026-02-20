package net.ganyusbathwater.oririmod.fluid;

import net.ganyusbathwater.oririmod.OririMod;
import net.ganyusbathwater.oririmod.block.custom.AetherLiquidBlock;
import net.ganyusbathwater.oririmod.fluid.custom.AetherFlowingFluid;
import net.ganyusbathwater.oririmod.fluid.custom.BloodWaterBlock;
import net.ganyusbathwater.oririmod.fluid.custom.BloodWaterFlowingFluid;
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
        private ModFluids() {
        }

        public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, OririMod.MOD_ID);

        public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, OririMod.MOD_ID);

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OririMod.MOD_ID);

        public static final DeferredHolder<Fluid, BaseFlowingFluid> AETHER_SOURCE = FLUIDS.register("aether",
                        () -> new AetherFlowingFluid.Source(makeProperties()));

        public static final DeferredHolder<Fluid, BaseFlowingFluid> AETHER_FLOWING = FLUIDS.register("aether_flowing",
                        () -> new AetherFlowingFluid.Flowing(makeProperties()));

        public static final DeferredHolder<Block, LiquidBlock> AETHER_BLOCK = BLOCKS.register("aether_block",
                        () -> new AetherLiquidBlock(
                                        AETHER_SOURCE.get(),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)));

        public static final DeferredHolder<Item, BucketItem> AETHER_BUCKET = ITEMS.register("aether_bucket",
                        () -> new BucketItem(
                                        AETHER_SOURCE.get(),
                                        new Item.Properties()
                                                        .stacksTo(1)
                                                        .craftRemainder(Items.BUCKET)));

        private static BaseFlowingFluid.Properties makeProperties() {
                return new BaseFlowingFluid.Properties(
                                ModFluidTypes.AETHER_TYPE,
                                AETHER_SOURCE,
                                AETHER_FLOWING)
                                .block(AETHER_BLOCK)
                                .bucket(AETHER_BUCKET)
                                .tickRate(10)
                                .levelDecreasePerBlock(2)
                                .slopeFindDistance(4);
        }

        // ===== BLOOD WATER FLUID =====
        public static final DeferredHolder<Fluid, BaseFlowingFluid> BLOOD_WATER_SOURCE = FLUIDS.register("blood_water",
                        () -> new BloodWaterFlowingFluid.Source(makeBloodWaterProperties()));

        public static final DeferredHolder<Fluid, BaseFlowingFluid> BLOOD_WATER_FLOWING = FLUIDS.register(
                        "blood_water_flowing", () -> new BloodWaterFlowingFluid.Flowing(makeBloodWaterProperties()));

        public static final DeferredHolder<Block, LiquidBlock> BLOOD_WATER_BLOCK = BLOCKS.register("blood_water_block",
                        () -> new BloodWaterBlock(
                                        BLOOD_WATER_SOURCE.get(),
                                        BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                                                        .noLootTable()));

        public static final DeferredHolder<Item, BucketItem> BLOOD_WATER_BUCKET = ITEMS.register("blood_water_bucket",
                        () -> new BucketItem(
                                        BLOOD_WATER_SOURCE.get(),
                                        new Item.Properties()
                                                        .stacksTo(1)
                                                        .craftRemainder(Items.BUCKET)));

        private static BaseFlowingFluid.Properties makeBloodWaterProperties() {
                return new BaseFlowingFluid.Properties(
                                ModFluidTypes.BLOOD_WATER_TYPE,
                                BLOOD_WATER_SOURCE,
                                BLOOD_WATER_FLOWING)
                                .block(BLOOD_WATER_BLOCK)
                                .bucket(BLOOD_WATER_BUCKET)
                                .tickRate(30) // Slower tick (lava-like)
                                .levelDecreasePerBlock(2) // Flows 4 blocks like water
                                .slopeFindDistance(4);
        }

        public static void register(IEventBus bus) {
                FLUIDS.register(bus);
                BLOCKS.register(bus);
                ITEMS.register(bus);
        }
}
