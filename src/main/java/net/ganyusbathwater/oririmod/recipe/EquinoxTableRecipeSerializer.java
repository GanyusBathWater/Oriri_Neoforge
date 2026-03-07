package net.ganyusbathwater.oririmod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class EquinoxTableRecipeSerializer implements RecipeSerializer<EquinoxTableRecipe> {

    public static final MapCodec<EquinoxTableRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("top").forGetter(EquinoxTableRecipe::getTop),
            Ingredient.CODEC_NONEMPTY.fieldOf("left").forGetter(EquinoxTableRecipe::getLeft),
            Ingredient.CODEC_NONEMPTY.fieldOf("center").forGetter(EquinoxTableRecipe::getCenter),
            Ingredient.CODEC_NONEMPTY.fieldOf("right").forGetter(EquinoxTableRecipe::getRight),
            Ingredient.CODEC_NONEMPTY.fieldOf("bottom").forGetter(EquinoxTableRecipe::getBottom),
            Ingredient.CODEC_NONEMPTY.fieldOf("template").forGetter(EquinoxTableRecipe::getTemplate),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(EquinoxTableRecipe::getResult),
            Codec.INT.fieldOf("mana_cost").forGetter(EquinoxTableRecipe::getManaCost))
            .apply(instance, EquinoxTableRecipe::new));

    // Manual StreamCodec since composite() only supports up to 6 fields
    public static final StreamCodec<RegistryFriendlyByteBuf, EquinoxTableRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EquinoxTableRecipe decode(RegistryFriendlyByteBuf buf) {
            Ingredient top = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient left = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient center = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient right = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient bottom = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int manaCost = buf.readInt();
            return new EquinoxTableRecipe(top, left, center, right, bottom, template, result, manaCost);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EquinoxTableRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getTop());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getLeft());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getCenter());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getRight());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getBottom());
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.getTemplate());
            ItemStack.STREAM_CODEC.encode(buf, recipe.getResult());
            buf.writeInt(recipe.getManaCost());
        }
    };

    @Override
    public MapCodec<EquinoxTableRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EquinoxTableRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
