package com.carrot123.yixiangshouronglu.gene.recipe;

import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class ChimeraRecipe implements Recipe<Container> {
    public static final int DURATION = 60;

    public record Input(Ingredient ingredient, int count) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getCount() >= count && ingredient.test(stack);
        }
    }

    private final ResourceLocation id;
    private final List<Input> inputs;
    private final ItemStack output;
    private final JsonObject json;

    public ChimeraRecipe(ResourceLocation id, List<Input> inputs, ItemStack output, JsonObject json) {
        this.id = id;
        this.inputs = List.copyOf(inputs);
        this.output = output.copy();
        this.json = json.deepCopy();
    }

    public List<Input> inputs() {
        return inputs;
    }

    public int duration() {
        return DURATION;
    }

    public String signature() {
        return json.toString();
    }

    public int[] matchSlots(Container container) {
        if (container.getContainerSize() < 4) {
            return null;
        }
        int[] assignment = new int[4];
        boolean[] occupied = new boolean[4];
        return assign(container, 0, assignment, occupied) ? assignment : null;
    }

    private boolean assign(Container container, int index, int[] assignment, boolean[] occupied) {
        if (index == 4) {
            return true;
        }

        for (int slot = 0; slot < 4; slot++) {
            if (occupied[slot] || !inputs.get(index).matches(container.getItem(slot))) {
                continue;
            }
            occupied[slot] = true;
            assignment[index] = slot;
            if (assign(container, index + 1, assignment, occupied)) {
                return true;
            }
            occupied[slot] = false;
        }

        return false;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return matchSlots(container) != null;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return output.copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModGeneRecipes.CHIMERA_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModGeneRecipes.CHIMERA_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.withSize(4, Ingredient.EMPTY);
        for (int i = 0; i < inputs.size(); i++) {
            result.set(i, inputs.get(i).ingredient());
        }
        return result;
    }

    public static final class Serializer implements RecipeSerializer<ChimeraRecipe> {
        @Override
        public ChimeraRecipe fromJson(ResourceLocation id, JsonObject json) {
            if (json.toString().length() > 32767) {
                throw new JsonSyntaxException("Chimera recipe exceeds network limit: " + id);
            }

            JsonArray ingredients = GsonHelper.getAsJsonArray(json, "inputs");
            if (ingredients.size() != 4) {
                throw new JsonSyntaxException("Chimera recipe requires exactly four inputs: " + id);
            }

            List<Input> inputs = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) {
                JsonObject input = ingredients.get(i).getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(input.get("ingredient"));
                int count = GsonHelper.getAsInt(input, "count", 1);
                if (ingredient.isEmpty() || count < 1 || count > 64) {
                    throw new JsonSyntaxException("Invalid chimera ingredient: " + id);
                }
                inputs.add(new Input(ingredient, count));
            }

            JsonObject result = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(result, "item"));
            if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                throw new JsonSyntaxException("Unknown chimera result: " + itemId);
            }
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                throw new JsonSyntaxException("Missing chimera result: " + itemId);
            }
            int count = GsonHelper.getAsInt(result, "count", 1);
            if (count < 1 || count > item.getMaxStackSize()) {
                throw new JsonSyntaxException("Invalid chimera result count: " + id);
            }

            return new ChimeraRecipe(id, inputs, new ItemStack(item, count), json);
        }

        @Override
        public ChimeraRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return fromJson(id, JsonParser.parseString(buffer.readUtf(32767)).getAsJsonObject());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ChimeraRecipe recipe) {
            buffer.writeUtf(recipe.json.toString(), 32767);
        }
    }
}
