import os

base_dir = r"c:\Users\denni\IdeaProjects\Oriri_Neoforge\src\main\java\net\ganyusbathwater\oririmod"
entity_dir = os.path.join(base_dir, "entity", "custom", "arrow")
item_dir = os.path.join(base_dir, "item", "custom", "arrow")

os.makedirs(entity_dir, exist_ok=True)
os.makedirs(item_dir, exist_ok=True)

arrows = [
    {
        "name": "Tnt",
        "lower": "tnt",
        "imports": """
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
""",
        "body": """
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, Level.ExplosionInteraction.TNT);
            this.discard();
        }
    }
"""
    },
    {
        "name": "EventHorizon",
        "lower": "event_horizon",
        "imports": "",
        "body": ""
    },
    {
        "name": "DragonIron",
        "lower": "dragon_iron",
        "imports": """
import net.minecraft.world.phys.EntityHitResult;
import net.ganyusbathwater.oririmod.damage.ModDamageTypes;
import net.minecraft.world.entity.LivingEntity;
""",
        "body": """
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide() && result.getEntity() instanceof LivingEntity living) {
            living.hurt(ModDamageTypes.getTrueDamage(this.level(), this.getOwner()), (float)this.getBaseDamage() + 2.0F);
        }
        super.onHitEntity(result);
    }
"""
    },
    {
        "name": "Frost",
        "lower": "frost",
        "imports": """
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
""",
        "body": """
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide() && result.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
            living.setTicksFrozen(living.getTicksFrozen() + 200);
        }
    }
"""
    },
    {
        "name": "Copper",
        "lower": "copper",
        "imports": """
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.core.BlockPos;
""",
        "body": """
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            if (this.level().isThundering()) { // Dynamic check as requested
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
                if (lightning != null) {
                    lightning.moveTo(Vec3.atBottomCenterOf(BlockPos.containing(result.getLocation())));
                    this.level().addFreshEntity(lightning);
                }
            }
        }
    }
"""
    },
    {
        "name": "Sonic",
        "lower": "sonic",
        "imports": "",
        "body": """
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount == 1) {
            // Speed up projectile immediately
            this.setDeltaMovement(this.getDeltaMovement().scale(3.0D));
        }
    }
"""
    }
]

entity_template = """package net.ganyusbathwater.oririmod.entity.custom.arrow;

import net.ganyusbathwater.oririmod.entity.ModEntities;
import net.ganyusbathwater.oririmod.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
{imports}

public class {name}ArrowEntity extends AbstractArrow {{
    public {name}ArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {{
        super(entityType, level);
    }}

    public {name}ArrowEntity(Level level, double x, double y, double z, ItemStack pickupItem, @Nullable ItemStack weapon) {{
        super(ModEntities.{upper}_ARROW.get(), x, y, z, level, pickupItem, weapon);
    }}

    public {name}ArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItem, @Nullable ItemStack weapon) {{
        super(ModEntities.{upper}_ARROW.get(), shooter, level, pickupItem, weapon);
    }}

    @Override
    protected ItemStack getDefaultPickupItem() {{
        return new ItemStack(ModItems.{upper}_ARROW.get());
    }}
{body}
}}
"""

item_template = """package net.ganyusbathwater.oririmod.item.custom.arrow;

import net.ganyusbathwater.oririmod.entity.custom.arrow.{name}ArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class {name}ArrowItem extends ArrowItem {{
    public {name}ArrowItem(Item.Properties properties) {{
        super(properties);
    }}

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {{
        return new {name}ArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
    }}
}}
"""

for arrow in arrows:
    name = arrow["name"]
    upper = arrow["lower"].upper()
    entity_code = entity_template.format(name=name, upper=upper, imports=arrow["imports"], body=arrow["body"])
    item_code = item_template.format(name=name)
    
    with open(os.path.join(entity_dir, f"{name}ArrowEntity.java"), "w") as f:
        f.write(entity_code)
        
    with open(os.path.join(item_dir, f"{name}ArrowItem.java"), "w") as f:
        f.write(item_code)

print("Generated arrow classes")
