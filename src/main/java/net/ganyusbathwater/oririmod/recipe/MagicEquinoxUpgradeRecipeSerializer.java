package net.ganyusbathwater.oririmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class MagicEquinoxUpgradeRecipeSerializer implements RecipeSerializer<MagicEquinoxUpgradeRecipe> {

    public static final MapCodec<MagicEquinoxUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("top").forGetter(MagicEquinoxUpgradeRecipe::getTop),
                    Ingredient.CODEC_NONEMPTY.fieldOf("left").forGetter(MagicEquinoxUpgradeRecipe::getLeft),
                    Ingredient.CODEC_NONEMPTY.fieldOf("center").forGetter(MagicEquinoxUpgradeRecipe::getCenter),
                    Ingredient.CODEC_NONEMPTY.fieldOf("right").forGetter(MagicEquinoxUpgradeRecipe::getRight),
                    Ingredient.CODEC_NONEMPTY.fieldOf("bottom").forGetter(MagicEquinoxUpgradeRecipe::getBottom),
                    Ingredient.CODEC_NONEMPTY.fieldOf("template").forGetter(MagicEquinoxUpgradeRecipe::getTemplate),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(MagicEquinoxUpgradeRecipe::getResult),
                    Codec.INT.fieldOf("mana_cost").forGetter(MagicEquinoxUpgradeRecipe::getManaCost),
                    Codec.INT.optionalFieldOf("required_level", 1)
                            .forGetter(MagicEquinoxUpgradeRecipe::getRequiredLevel))
            .apply(instance, MagicEquinoxUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagicEquinoxUpgradeRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MagicEquinoxUpgradeRecipe decode(RegistryFriendlyByteBuf buf) {
            Ingredient top = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient left = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient center = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient right = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient bottom = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int manaCost = buf.readInt();
            int requiredLevel = buf.readInt();
            return new MagicEquinoxUpgradeRecipe(top, left, center, right, bottom, template, result, manaCost,
                    requiredLevel);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, MagicEquinoxUpgradeRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getTop());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getLeft());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getCenter());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getRight());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getBottom());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getTemplate());
            ItemStack.STREAM_CODEC.encode(buf, recipe.getResult());
            buf.writeInt(recipe.getManaCost());
            buf.writeInt(recipe.getRequiredLevel());
        }
    };

    @Override
    public MapCodec<MagicEquinoxUpgradeRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MagicEquinoxUpgradeRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
