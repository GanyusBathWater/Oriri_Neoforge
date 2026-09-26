package net.ganyusbathwater.oririmod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ModSignBlockEntity extends SignBlockEntity {
    public ModSignBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(pPos, pBlockState);
        try {
            // In 1.20+, signs have front and back text
            this.setText(this.getFrontText().setColor(net.minecraft.world.item.DyeColor.WHITE).setHasGlowingText(true), true);
            this.setText(this.getBackText().setColor(net.minecraft.world.item.DyeColor.WHITE).setHasGlowingText(true), false);
        } catch (Exception e) {
            // Ignore if mapping changed
        }
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.MOD_SIGN.get();
    }
}
