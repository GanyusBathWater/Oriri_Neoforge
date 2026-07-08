package net.ganyusbathwater.oririmod.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TallGrassBlock;

public class ElderFernBlock extends TallGrassBlock {
    public static final MapCodec<ElderFernBlock> CODEC = simpleCodec(ElderFernBlock::new);

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<TallGrassBlock> codec() {
        return (MapCodec<TallGrassBlock>) (Object) CODEC;
    }

    public ElderFernBlock(Properties properties) {
        super(properties);
    }
}
