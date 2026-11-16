package com.carrot123.yixiangshouronglu.gene.recipe;

import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.carrot123.yixiangshouronglu.registry.ModItems;
import com.carrot123.yixiangshouronglu.util.SoulEntityUtil;
import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public final class ChimeraRecipe implements Recipe<Container> {
    public static final int DURATION = 60;
    private static final int INPUT_SLOTS = 4;
    public record Input(Ingredient ingredient, int count, @Nullable ResourceLocation extractEntity) {
        boolean matches(ItemStack stack) {
            if (extractEntity != null) {
                return stack.getCount() >= count && stack.is(ModItems.CONTAINMENT_EXTRACT.get())
                        && extractEntity.toString().equals(ContainmentExtractItem.getEntityId(stack));
            }
            return stack.getCount() >= count && ingredient.test(ContainmentExtractItem.normalizedCopy(stack));
        }

        public List<ItemStack> displayStacks() {
            if (extractEntity != null) {
                ItemStack stack = ContainmentExtractItem.create(extractEntity.toString());
                return stack.isEmpty() ? List.of() : List.of(stack.copyWithCount(count));
            }
            return Arrays.stream(ingredient.getItems())
                    .map(stack -> stack.copyWithCount(count))
                    .toList();
        }
    }

    private final ResourceLocation id;
    private final List<Input> inputs;
    private final ItemStack output;
    private final String definition;

    private ChimeraRecipe(ResourceLocation id, List<Input> inputs, ItemStack output, String definition) {
        this.id = id;
        this.inputs = List.copyOf(inputs);
        this.output = output.copy();
        this.definition = definition;
    }

    public List<Input> inputs() { return inputs; }
    public String signature() { return definition; }
    public int[] matchSlots(Container container) {
        if (container.getContainerSize() != INPUT_SLOTS) {
            return null;
        }
        int occupied = 0;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            if (!container.getItem(slot).isEmpty()) {
                occupied++;
            }
        }
        if (occupied != inputs.size()) {
            return null;
        }
        int[] slots = new int[inputs.size()];
        return assign(container, slots, 0, 0) ? slots : null;
    }

    private boolean assign(Container container, int[] slots, int input, int used) {
        if (input == inputs.size()) {
            return true;
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            int bit = 1 << slot;
            if ((used & bit) == 0 && inputs.get(input).matches(container.getItem(slot))) {
                slots[input] = slot;
                if (assign(container, slots, input + 1, used | bit)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override public boolean matches(Container container, Level level) { return matchSlots(container) != null; }
    @Override public ItemStack assemble(Container container, RegistryAccess access) { return output.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= INPUT_SLOTS; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModGeneRecipes.CHIMERA_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModGeneRecipes.CHIMERA_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<ChimeraRecipe> {
        @Override public ChimeraRecipe fromJson(ResourceLocation id, JsonObject json) {
            String definition = json.toString();
            if (definition.length() > 32767) {
                throw new JsonSyntaxException("Chimera recipe exceeds network limit");
            }
            JsonArray materials = GsonHelper.getAsJsonArray(json, "inputs");
            if (materials.size() < 2 || materials.size() > INPUT_SLOTS) {
                throw new JsonSyntaxException("Chimera recipes require two to four inputs");
            }
            List<Input> inputs = new ArrayList<>();
            for (JsonElement element : materials) {
                JsonObject input = GsonHelper.convertToJsonObject(element, "input");
                int count = count(input);
                if (input.has("extract_entity")) {
                    ResourceLocation entity = ResourceLocation.tryParse(
                            SoulEntityUtil.normalizeExtractEntityId(GsonHelper.getAsString(input, "extract_entity")));
                    if (input.has("ingredient") || entity == null || !ForgeRegistries.ENTITY_TYPES.containsKey(entity)) {
                        throw new JsonSyntaxException("Invalid or unavailable chimera extract entity: " + input.get("extract_entity"));
                    }
                    inputs.add(new Input(Ingredient.of(ModItems.CONTAINMENT_EXTRACT.get()), count, entity));
                    continue;
                }
                Ingredient ingredient = Ingredient.fromJson(input.get("ingredient"));
                if (ingredient == Ingredient.EMPTY) {
                    throw new JsonSyntaxException("Empty chimera ingredient");
                }
                inputs.add(new Input(ingredient, count, null));
            }
            JsonObject result = GsonHelper.getAsJsonObject(json, "result");
            ResourceLocation item = ResourceLocation.tryParse(GsonHelper.getAsString(result, "item"));
            if (item == null || !ForgeRegistries.ITEMS.containsKey(item)) {
                throw new JsonSyntaxException("Unknown chimera result item");
            }
            int count = count(result);
            ItemStack output = ShapedRecipe.itemStackFromJson(result);
            if (output.isEmpty() || count > output.getMaxStackSize()) {
                throw new JsonSyntaxException("Invalid chimera result stack");
            }
            return new ChimeraRecipe(id, inputs, output, definition);
        }

        private static int count(JsonObject object) {
            try {
                JsonElement value = object.get("count");
                int count = value == null ? 1 : value.getAsBigDecimal().intValueExact();
                if (count >= 1 && count <= 64) {
                    return count;
                }
            } catch (ArithmeticException | IllegalStateException | UnsupportedOperationException | NumberFormatException ignored) {
                throw new JsonSyntaxException("Chimera count must be an integer from 1 to 64");
            }
            throw new JsonSyntaxException("Chimera count must be an integer from 1 to 64");
        }

        @Override public ChimeraRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return fromJson(id, JsonParser.parseString(buffer.readUtf(32767)).getAsJsonObject());
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, ChimeraRecipe recipe) {
            buffer.writeUtf(recipe.definition, 32767);
        }
    }
}
