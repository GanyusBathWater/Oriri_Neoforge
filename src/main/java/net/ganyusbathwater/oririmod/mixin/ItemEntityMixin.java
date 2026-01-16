package net.ganyusbathwater.oririmod.mixin;

import net.ganyusbathwater.oririmod.enchantment.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow public abstract ItemStack getItem();
    @Shadow private int age;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    private boolean hasInvincibleEnchantment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        // Holt die Verzauberungs-Registry
        var enchantmentRegistry = this.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        // Holt den Holder für die Verzauberung und prüft dessen Level auf dem ItemStack über die Datenkomponente
        return enchantmentRegistry.getHolder(ModEnchantments.INVINCIBLE)
                .map(holder -> {
                    ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
                    return enchantments != null && enchantments.getLevel(holder) > 0;
                })
                .orElse(false);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void makeInvulnerable(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (hasInvincibleEnchantment(this.getItem())) {
            cir.setReturnValue(false); // Verhindert Schaden
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void preventDespawnAndVoidDamage(CallbackInfo ci) {
        if (hasInvincibleEnchantment(this.getItem())) {
            this.age = 0;

            if (this.getY() < (double)(this.level().getMinBuildHeight() - 64)) {
                this.setPos(this.getX(), this.level().getMinBuildHeight(), this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0, 1));
            }
        }
    }
}