package net.ganyusbathwater.oririmod.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class ModArrowDispenseBehavior extends DefaultDispenseItemBehavior {

    /**
     * Demonstrates how to instantiate and return the custom AbstractArrow entity.
     * Replaces the old 1.20 getProjectile method.
     */
    protected abstract AbstractArrow getProjectile(Level level, Position position, ItemStack stack);

    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
        Level level = blockSource.level();
        Position position = DispenserBlock.getDispensePosition(blockSource);
        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
        
        AbstractArrow arrow = this.getProjectile(level, position, itemStack);
        arrow.shoot(direction.getStepX(), (float)direction.getStepY() + 0.1F, direction.getStepZ(), 1.1F, 6.0F);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        level.addFreshEntity(arrow);
        
        itemStack.shrink(1);
        return itemStack;
    }
    
    @Override
    protected void playSound(BlockSource blockSource) {
        blockSource.level().levelEvent(1002, blockSource.pos(), 0);
    }
}
