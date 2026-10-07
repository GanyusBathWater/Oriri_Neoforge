package net.ganyusbathwater.oririmod.block.entity;

import net.ganyusbathwater.oririmod.block.custom.SoulHarvesterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.ganyusbathwater.oririmod.block.menu.SoulHarvesterMenu;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.ganyusbathwater.oririmod.item.ModItems;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

public class SoulHarvesterBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer inventory = new SimpleContainer(33) {
        @Override
        public void setChanged() {
            super.setChanged();
            SoulHarvesterBlockEntity.this.setChanged();
        }
    };

    protected final ContainerData data;

    private final IItemHandler itemHandler = new InvWrapper(inventory);

    private int progress = 0;
    private int maxProgress = 200; // Base ticks per generation (10 seconds)

    public SoulHarvesterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SOUL_HARVESTER_BE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> SoulHarvesterBlockEntity.this.progress;
                    case 1 -> SoulHarvesterBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> SoulHarvesterBlockEntity.this.progress = value;
                    case 1 -> SoulHarvesterBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.oririmod.soul_harvester");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SoulHarvesterMenu(containerId, playerInventory, this, this.data);
    }

    public ContainerData getContainerData() {
        return this.data;
    }

    public void drops() {
        SimpleContainer inventoryToDrop = new SimpleContainer(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventoryToDrop.setItem(i, inventory.getItem(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventoryToDrop);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.createTag(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag.getList("Inventory", 10), registries);
        if (tag.contains("Progress")) {
            progress = tag.getInt("Progress");
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        ItemStack shardStack = inventory.getItem(0);
        if (shardStack.isEmpty() || !shardStack.is(ModItems.SOUL_SHARD.get())) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        net.minecraft.nbt.CompoundTag tag = shardStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (!tag.contains("captured_entity")) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        String entityId = tag.getString("captured_entity");
        Optional<EntityType<?>> optionalType = EntityType.byString(entityId);
        if (optionalType.isEmpty()) {
            if (progress > 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        EntityType<?> entityType = optionalType.get();

        int speedLevel = getUpgradeLevel(ModItems.SOUL_HARVESTER_SPEED_UPGRADE.get());
        maxProgress = 200 - (speedLevel * 20); // up to level 3: 200 -> 140 ticks
        if (maxProgress < 20) maxProgress = 20;

        progress++;
        if (progress >= maxProgress) {
            progress = 0;
            generateLoot((ServerLevel) level, entityType, pos);
        }
        setChanged();
    }

    private int getUpgradeLevel(Item upgradeItem) {
        int slotToCheck = -1;
        if (upgradeItem == ModItems.SOUL_HARVESTER_LOOTING_UPGRADE.get()) slotToCheck = 1;
        else if (upgradeItem == ModItems.SOUL_HARVESTER_XP_UPGRADE.get()) slotToCheck = 2;
        else if (upgradeItem == ModItems.SOUL_HARVESTER_SPEED_UPGRADE.get()) slotToCheck = 3;
        else if (upgradeItem == ModItems.SOUL_HARVESTER_FIRE_ASPECT_UPGRADE.get()) slotToCheck = 4;
        else if (upgradeItem == ModItems.SOUL_HARVESTER_PLAYER_KILL_UPGRADE.get()) slotToCheck = 5;

        if (slotToCheck != -1) {
            ItemStack stack = inventory.getItem(slotToCheck);
            if (stack.is(upgradeItem)) {
                net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                return tag.contains("oriri_level") ? tag.getInt("oriri_level") : 1;
            }
        }
        return 0;
    }

    private void generateLoot(ServerLevel level, EntityType<?> entityType, BlockPos pos) {
        net.minecraft.resources.ResourceKey<LootTable> lootTableId = entityType.getDefaultLootTable();
        if (lootTableId == null) return;
        
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableId);
        
        int lootingLevel = getUpgradeLevel(ModItems.SOUL_HARVESTER_LOOTING_UPGRADE.get());
        boolean isPlayerKill = getUpgradeLevel(ModItems.SOUL_HARVESTER_PLAYER_KILL_UPGRADE.get()) > 0;
        boolean hasFireAspect = getUpgradeLevel(ModItems.SOUL_HARVESTER_FIRE_ASPECT_UPGRADE.get()) > 0;
        int xpLevel = getUpgradeLevel(ModItems.SOUL_HARVESTER_XP_UPGRADE.get());

        net.neoforged.neoforge.common.util.FakePlayer fakePlayer = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        ItemStack weapon = new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD);
        if (lootingLevel > 0) {
            weapon.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), lootingLevel);
        }
        if (hasFireAspect) {
            weapon.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT), 1);
        }
        fakePlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);

        DamageSource source = isPlayerKill ? level.damageSources().playerAttack(fakePlayer) : level.damageSources().generic();

        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.DAMAGE_SOURCE, source);
        
        if (isPlayerKill) {
            builder.withParameter(LootContextParams.ATTACKING_ENTITY, fakePlayer);
        }

        net.minecraft.world.entity.Entity dummyEntity = entityType.create(level);
        if (dummyEntity != null) {
            builder.withParameter(LootContextParams.THIS_ENTITY, dummyEntity);
        }

        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack> loot = lootTable.getRandomItems(lootParams);

        if (xpLevel > 0) {
            loot.add(new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, xpLevel * 2)); // Give bottles of enchanting for XP
        }

        for (ItemStack drop : loot) {
            ItemStack remainder = drop.copy();
            for (int i = 6; i < 33; i++) {
                if (remainder.isEmpty()) break;
                ItemStack slotStack = inventory.getItem(i);
                if (slotStack.isEmpty()) {
                    inventory.setItem(i, remainder.split(remainder.getCount()));
                } else if (ItemStack.isSameItemSameComponents(slotStack, remainder)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    if (space > 0) {
                        int amount = Math.min(space, remainder.getCount());
                        slotStack.grow(amount);
                        remainder.shrink(amount);
                    }
                }
            }
        }
    }
}
