package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.client.render.item.ForcefieldEmitterItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class ForcefieldEmitterBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ForcefieldEmitterBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ForcefieldEmitterItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ForcefieldEmitterItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("forcefield_emitter_idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        String name = this.getDescriptionId();
        if (name.contains("repellent")) net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.repellent.desc");
        else if (name.contains("attracting")) net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.attracting.desc");
        else if (name.contains("protection")) net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.protection.desc");
        else if (name.contains("modifier")) net.ganyusbathwater.oririmod.util.TooltipHelper.addAbility(tooltipComponents, "tooltip.oririmod.modifier.desc");
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
