package net.ganyusbathwater.oririmod.item.custom;

import com.google.common.collect.ImmutableMap;
import net.ganyusbathwater.oririmod.util.ModRarity;
import net.ganyusbathwater.oririmod.util.ModRarityCarrier;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.resources.ResourceLocation;

public class ModArmorItem extends ArmorItem implements ModRarityCarrier {
    private final ModRarity rarity;

    //For fullset armor effects
    private static final Map<Holder<ArmorMaterial>, List<MobEffectInstance>> MATERIAL_TO_EFFECT_MAP =
            (new ImmutableMap.Builder<Holder<ArmorMaterial>, List<MobEffectInstance>>())
                    .put(ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL,
                            List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0, false, false)))
                    .put(ModArmorMaterials.ANCIENT_ARMOR_MATERIAL,
                            List.of(new MobEffectInstance(MobEffects.REGENERATION, 400, 0, false, false)))
                    .put(ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL,
                            List.of(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1, false, false)))
                    .put(ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL,
                            List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false)))
                    .build();

    private static final ResourceLocation CRYSTAL_STEP_HEIGHT_RL = ResourceLocation.fromNamespaceAndPath("oririmod", "crystal_step_height");
    private static final ResourceLocation GILDED_KNOCKBACK_RL = ResourceLocation.fromNamespaceAndPath("oririmod", "gilded_knockback_resist");

    public ModArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, ModRarity rarity) {
        super(material, type, properties);
        this.rarity = rarity;
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer) {
        if (this.getMaterial() == ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL) {
            if (wearer instanceof Player player) {
                return hasFullSuitOfArmorOn(player) && hasPlayerCorrectArmorOn(this.getMaterial(), player);
            }
        }
        return super.makesPiglinsNeutral(stack, wearer);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if(entity instanceof Player player && !level.isClientSide()) {
            boolean hasFullSuit = hasFullSuitOfArmorOn(player);
            if (hasFullSuit) {
                evaluateArmorEffects(player);
            }

            // Crystal Armor Step Height
            AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeight != null) {
                AttributeModifier mod = new AttributeModifier(CRYSTAL_STEP_HEIGHT_RL, 1.0, AttributeModifier.Operation.ADD_VALUE);
                if (hasFullSuit && hasPlayerCorrectArmorOn(ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL, player)) {
                    if (stepHeight.getModifier(CRYSTAL_STEP_HEIGHT_RL) == null) stepHeight.addTransientModifier(mod);
                } else {
                    if (stepHeight.getModifier(CRYSTAL_STEP_HEIGHT_RL) != null) stepHeight.removeModifier(CRYSTAL_STEP_HEIGHT_RL);
                }
            }

            // Gilded Netherite Knockback Resist
            AttributeInstance kbResist = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kbResist != null) {
                AttributeModifier mod = new AttributeModifier(GILDED_KNOCKBACK_RL, 1.0, AttributeModifier.Operation.ADD_VALUE);
                if (hasFullSuit && hasPlayerCorrectArmorOn(ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL, player)) {
                    if (kbResist.getModifier(GILDED_KNOCKBACK_RL) == null) kbResist.addTransientModifier(mod);
                } else {
                    if (kbResist.getModifier(GILDED_KNOCKBACK_RL) != null) kbResist.removeModifier(GILDED_KNOCKBACK_RL);
                }
            }
        }
    }

    private void evaluateArmorEffects(Player player) {
        for(Map.Entry<Holder<ArmorMaterial>, List<MobEffectInstance>> entry : MATERIAL_TO_EFFECT_MAP.entrySet()) {
            Holder<ArmorMaterial> mapArmorMaterial = entry.getKey();
            List<MobEffectInstance> mapEffect = entry.getValue();

            if(hasPlayerCorrectArmorOn(mapArmorMaterial, player)) {
                addEffectToPlayer(player, mapEffect);
            }
        }
    }

    private void addEffectToPlayer(Player player, List<MobEffectInstance> mapEffect) {
        boolean hasPlayerEffect = mapEffect.stream().allMatch(effect -> player.hasEffect(effect.getEffect()));

        if(!hasPlayerEffect) {
            for (MobEffectInstance effect : mapEffect) {
                player.addEffect(new MobEffectInstance(effect.getEffect(),
                        effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible()));
            }
        }
    }

    private boolean hasFullSuitOfArmorOn(Player player) {
        ItemStack boots = player.getInventory().getArmor(0);
        ItemStack leggings = player.getInventory().getArmor(1);
        ItemStack chestplate = player.getInventory().getArmor(2);
        ItemStack helmet = player.getInventory().getArmor(3);

        return !boots.isEmpty() && !leggings.isEmpty() && !chestplate.isEmpty() && !helmet.isEmpty();
    }

    public boolean hasPlayerCorrectArmorOn(Holder<ArmorMaterial> mapArmorMaterial, Player player) {
        for(ItemStack armorStack : player.getArmorSlots()) {
            if(!(armorStack.getItem() instanceof ArmorItem)) {
                return false;
            }
        }

        ArmorItem boots = ((ArmorItem) player.getInventory().getArmor(0).getItem());
        ArmorItem leggings = ((ArmorItem) player.getInventory().getArmor(1).getItem());
        ArmorItem chestplate = ((ArmorItem) player.getInventory().getArmor(2).getItem());
        ArmorItem helmet = ((ArmorItem) player.getInventory().getArmor(3).getItem());

        return boots.getMaterial() == mapArmorMaterial && leggings.getMaterial() == mapArmorMaterial
                && chestplate.getMaterial() == mapArmorMaterial && helmet.getMaterial() == mapArmorMaterial;
    }

    @Override
    public ModRarity getModRarity() {
        return rarity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            ClientTooltipHelper.addArmorTooltip(this, tooltipComponents);
        }
    }

    private static class ClientTooltipHelper {
        public static void addArmorTooltip(ModArmorItem armor, List<Component> tooltipComponents) {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            boolean hasFullSet = player != null && armor.hasFullSuitOfArmorOn(player) && armor.hasPlayerCorrectArmorOn(armor.getMaterial(), player);
            
            if (!hasFullSet) {
                return;
            }

            ChatFormatting color = ChatFormatting.GOLD;

            if (armor.getMaterial() == ModArmorMaterials.CRYSTAL_ARMOR_MATERIAL) {
                tooltipComponents.add(Component.literal("Set Bonus:").withStyle(color));
                tooltipComponents.add(Component.literal(" - +25% Light Resistance").withStyle(color));
                tooltipComponents.add(Component.literal(" - Speed I").withStyle(color));
                tooltipComponents.add(Component.literal(" - +1.5 Step Height").withStyle(color));
            } else if (armor.getMaterial() == ModArmorMaterials.ANCIENT_ARMOR_MATERIAL) {
                tooltipComponents.add(Component.literal("Set Bonus:").withStyle(color));
                tooltipComponents.add(Component.literal(" - +25% Nature Resistance").withStyle(color));
                tooltipComponents.add(Component.literal(" - Regeneration I").withStyle(color));
            } else if (armor.getMaterial() == ModArmorMaterials.BLUE_ICE_ARMOR_MATERIAL) {
                tooltipComponents.add(Component.literal("Set Bonus:").withStyle(color));
                tooltipComponents.add(Component.literal(" - +25% Darkness Resistance").withStyle(color));
                tooltipComponents.add(Component.literal(" - Absorption II").withStyle(color));
            } else if (armor.getMaterial() == ModArmorMaterials.GILDED_NETHERRITE_ARMOR_MATERIAL) {
                tooltipComponents.add(Component.literal("Set Bonus:").withStyle(color));
                tooltipComponents.add(Component.literal(" - +25% Earth Resistance").withStyle(color));
                tooltipComponents.add(Component.literal(" - Pacifies Piglins").withStyle(color));
                tooltipComponents.add(Component.literal(" - Knockback Immunity").withStyle(color));
            } else if (armor.getMaterial() == ModArmorMaterials.MOLTEN_ARMOR_MATERIAL) {
                tooltipComponents.add(Component.literal("Set Bonus:").withStyle(color));
                tooltipComponents.add(Component.literal(" - +25% Fire Resistance").withStyle(color));
                tooltipComponents.add(Component.literal(" - Sets attackers on fire").withStyle(color));
            } else if (armor.getMaterial() == ModArmorMaterials.PRISMARINE_ARMOR_MATERIAL) {
                tooltipComponents.add(Component.literal("Set Bonus:").withStyle(color));
                tooltipComponents.add(Component.literal(" - +25% Water Resistance").withStyle(color));
                tooltipComponents.add(Component.literal(" - Water Breathing").withStyle(color));
            }
        }
    }
}