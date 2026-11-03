package com.carrot123.yixiangshouronglu.gene.recipe;

import com.carrot123.yixiangshouronglu.gene.*;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.google.gson.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

public final class GeneRecipe implements Recipe<Container> {
    public record Input(int slot, Ingredient ingredient, int count, ResourceLocation geneId, GeneTypes.Quality quality) {
        boolean matches(ItemStack stack) {
            if (stack.getCount() < count || !ingredient.test(stack)) { return false; }
            if ((geneId != null || quality != null) && GeneModuleItem.definition(stack) == null) { return false; }
            var data = GeneModuleItem.read(stack);
            return (geneId == null || data != null && data.id().equals(geneId)) && (quality == null || data != null && data.quality() == quality);
        }
    }
    private final ResourceLocation id;
    private final List<Input> inputs;
    private final GeneTypes.Mode mode;
    private final ResourceLocation output;
    private final GeneTypes.Quality quality;
    private final int duration;
    private final JsonObject json;
    private final boolean shapeless;
    public GeneRecipe(ResourceLocation id, List<Input> inputs, GeneTypes.Mode mode, ResourceLocation output, GeneTypes.Quality quality, int duration, JsonObject json) {
        this.id = id; this.inputs = List.copyOf(inputs); this.mode = mode; this.output = output; this.quality = quality; this.duration = duration; this.json = json.deepCopy(); this.shapeless = GsonHelper.getAsBoolean(json, "shapeless", false);
    }
    public List<Input> inputs() { return inputs; }
    public GeneTypes.Mode mode() { return mode; }
    public int duration() { return duration; }
    public String signature() { return json.toString(); }
    public boolean shapeless() { return shapeless; }
    public int[] matchSlots(Container container, GeneTypes.Mode selectedMode) {
        if (mode != selectedMode || GeneDefinitions.get(output) == null) { return null; }
        int[] slots = new int[inputs.size()];
        boolean[] occupied = new boolean[4];
        if (shapeless) {
            if (!assign(container, 0, slots, occupied)) { return null; }
        } else {
            for (int index = 0; index < inputs.size(); index++) {
                Input input = inputs.get(index);
                if (!input.matches(container.getItem(input.slot()))) { return null; }
                slots[index] = input.slot(); occupied[input.slot()] = true;
            }
        }
        for (int slot = 0; slot < 4; slot++) { if (!occupied[slot] && !container.getItem(slot).isEmpty()) { return null; } }
        if (mode == GeneTypes.Mode.UPGRADE) {
            var data = GeneModuleItem.read(container.getItem(0));
            if (data == null || GeneModuleItem.definition(container.getItem(0)) == null || !data.id().equals(output) || data.quality().next() != quality) { return null; }
        }
        return slots;
    }
    private boolean assign(Container container, int index, int[] slots, boolean[] occupied) {
        if (index == inputs.size()) {
            for (int slot = 0; slot < 4; slot++) { if (!occupied[slot] && !container.getItem(slot).isEmpty()) { return false; } }
            return true;
        }
        for (int slot = 0; slot < 4; slot++) {
            if (!occupied[slot] && inputs.get(index).matches(container.getItem(slot))) {
                occupied[slot] = true; slots[index] = slot;
                if (assign(container, index + 1, slots, occupied)) { return true; }
                occupied[slot] = false;
            }
        }
        return false;
    }
    public boolean matchesInputs(Container container, GeneTypes.Mode selectedMode) { return matchSlots(container, selectedMode) != null; }
    @Override public boolean matches(Container container, Level level) { return matchesInputs(container, mode); }
    @Override public ItemStack assemble(Container container, RegistryAccess access) { return mode == GeneTypes.Mode.UPGRADE ? GeneModuleItem.upgrade(container.getItem(0), quality) : getResultItem(access); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return GeneModuleItem.create(output, quality); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 4; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModGeneRecipes.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModGeneRecipes.TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.withSize(4, Ingredient.EMPTY);
        inputs.forEach(input -> result.set(input.slot(), input.ingredient())); return result;
    }
    public static final class Serializer implements RecipeSerializer<GeneRecipe> {
        @Override public GeneRecipe fromJson(ResourceLocation id, JsonObject json) {
            try {
                if (json.toString().length() > 32767) { throw new JsonSyntaxException("Gene recipe exceeds network limit"); }
                GeneTypes.Mode mode = GeneTypes.Mode.valueOf(GsonHelper.getAsString(json, "mode"));
                if (mode == GeneTypes.Mode.UPGRADE && GsonHelper.getAsBoolean(json, "shapeless", false)) { throw new JsonSyntaxException("Enhancement requires its main slot"); }
                int duration = GsonHelper.getAsInt(json, "time");
                if (duration <= 0) { throw new JsonSyntaxException("Recipe time must be positive"); }
                List<Input> inputs = new ArrayList<>(); Set<Integer> used = new HashSet<>();
                for (JsonElement element : GsonHelper.getAsJsonArray(json, "inputs")) {
                    JsonObject input = element.getAsJsonObject(); int slot = GsonHelper.getAsInt(input, "slot"); int count = GsonHelper.getAsInt(input, "count", 1);
                    if (slot < 0 || slot >= 4 || !used.add(slot) || count < 1 || count > 64) { throw new JsonSyntaxException("Invalid input slot or count"); }
                    Ingredient ingredient = Ingredient.fromJson(input.get("ingredient"));
                    if (ingredient.isEmpty()) { throw new JsonSyntaxException("Empty input ingredient"); }
                    inputs.add(new Input(slot, ingredient, count, input.has("gene") ? new ResourceLocation(GsonHelper.getAsString(input, "gene")) : null,
                            input.has("quality") ? GeneTypes.Quality.valueOf(GsonHelper.getAsString(input, "quality")) : null));
                }
                if (inputs.isEmpty() || mode == GeneTypes.Mode.UPGRADE && inputs.stream().noneMatch(input -> input.slot() == 0 && input.count() == 1)) { throw new JsonSyntaxException("Missing required inputs"); }
                JsonObject result = GsonHelper.getAsJsonObject(json, "result");
                return new GeneRecipe(id, inputs, mode, new ResourceLocation(GsonHelper.getAsString(result, "gene")), GeneTypes.Quality.valueOf(GsonHelper.getAsString(result, "quality", "ALPHA")), duration, json);
            } catch (IllegalArgumentException exception) { throw new JsonSyntaxException("Invalid gene recipe " + id, exception); }
        }
        @Override public GeneRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) { return fromJson(id, JsonParser.parseString(buffer.readUtf(32767)).getAsJsonObject()); }
        @Override public void toNetwork(FriendlyByteBuf buffer, GeneRecipe recipe) { buffer.writeUtf(recipe.json.toString(), 32767); }
    }
}
