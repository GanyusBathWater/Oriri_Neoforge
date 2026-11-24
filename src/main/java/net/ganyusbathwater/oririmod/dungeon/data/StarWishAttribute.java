package net.ganyusbathwater.oririmod.dungeon.data;

import net.ganyusbathwater.oririmod.OririMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The permanent attribute blessings selectable through "Wish Upon a Star".
 * Each main dungeon stores its own independent choice per player, so multiple
 * dungeons stack cleanly and reusing the same dungeon's Wish replaces only
 * that dungeon's blessing.
 */
public enum StarWishAttribute {
    MAX_HEALTH(
            "max_health",
            Attributes.MAX_HEALTH,
            4.0D,
            AttributeModifier.Operation.ADD_VALUE,
            0xFFFF6E7A
    ),
    ATTACK_DAMAGE(
            "attack_damage",
            Attributes.ATTACK_DAMAGE,
            0.05D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            0xFFFF9A4A
    ),
    ARMOR(
            "armor",
            Attributes.ARMOR,
            2.0D,
            AttributeModifier.Operation.ADD_VALUE,
            0xFF8FB8FF
    ),
    MOVEMENT_SPEED(
            "movement_speed",
            Attributes.MOVEMENT_SPEED,
            0.08D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            0xFF6EF0A8
    ),
    MANA_REGEN(
            "mana_regen",
            null, // Handled via ModManaUtil (+50% mana regeneration speed per dungeon wish)
            0.50D,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
            0xFF5CE1F5
    ),
    LUCK(
            "luck",
            Attributes.LUCK,
            2.0D,
            AttributeModifier.Operation.ADD_VALUE,
            0xFFFFD859
    );

    public static final String MODIFIER_PREFIX = "star_wish_";

    private final String id;
    @Nullable
    private final Holder<Attribute> vanillaAttribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final int accentColor;

    StarWishAttribute(String id, @Nullable Holder<Attribute> vanillaAttribute, double amount,
                      AttributeModifier.Operation operation, int accentColor) {
        this.id = id;
        this.vanillaAttribute = vanillaAttribute;
        this.amount = amount;
        this.operation = operation;
        this.accentColor = accentColor;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public Holder<Attribute> getVanillaAttribute() {
        return vanillaAttribute;
    }

    public double getAmount() {
        return amount;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public String getNameKey() {
        return "wish.oririmod.attr." + id + ".name";
    }

    public String getBonusKey() {
        return "wish.oririmod.attr." + id + ".bonus";
    }

    public ResourceLocation createModifierId(String dungeonId) {
        String safeDungeon = sanitizeId(dungeonId);
        return ResourceLocation.fromNamespaceAndPath(OririMod.MOD_ID, MODIFIER_PREFIX + safeDungeon + "_" + this.id);
    }

    public AttributeModifier createModifier(String dungeonId) {
        return new AttributeModifier(createModifierId(dungeonId), this.amount, this.operation);
    }

    @Nullable
    public static StarWishAttribute byId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (StarWishAttribute attr : values()) {
            if (attr.id.equals(id)) {
                return attr;
            }
        }
        return null;
    }

    public static String sanitizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }
}
