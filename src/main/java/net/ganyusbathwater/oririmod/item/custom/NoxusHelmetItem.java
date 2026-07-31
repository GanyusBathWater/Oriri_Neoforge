package net.ganyusbathwater.oririmod.item.custom;

import net.ganyusbathwater.oririmod.client.render.NoxusHelmetRenderer;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class NoxusHelmetItem extends ModArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String variant;

    public NoxusHelmetItem(Holder<ArmorMaterial> material, Properties properties, ModRarity rarity, String variant) {
        super(material, Type.HELMET, properties, rarity);
        this.variant = variant;
    }

    public String getVariant() {
        return this.variant;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<software.bernie.geckolib.animatable.client.GeoRenderProvider> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.GeoRenderProvider() {
            private NoxusHelmetRenderer renderer;

            @Override
            public @org.jetbrains.annotations.NotNull <T extends LivingEntity> net.minecraft.client.model.HumanoidModel<?> getGeoArmorRenderer(@org.jetbrains.annotations.Nullable T livingEntity, @org.jetbrains.annotations.NotNull ItemStack itemStack, @org.jetbrains.annotations.NotNull EquipmentSlot equipmentSlot, @org.jetbrains.annotations.NotNull net.minecraft.client.model.HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = new NoxusHelmetRenderer();
                }
                return this.renderer;
            }
        });
    }
}
