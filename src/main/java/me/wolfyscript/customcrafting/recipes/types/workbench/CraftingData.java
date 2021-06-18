package me.wolfyscript.customcrafting.recipes.types.workbench;

import me.wolfyscript.customcrafting.recipes.types.CraftingRecipe;
import me.wolfyscript.customcrafting.utils.geom.Vec2d;
import me.wolfyscript.customcrafting.utils.recipe_item.Ingredient;
import me.wolfyscript.customcrafting.utils.recipe_item.Result;
import me.wolfyscript.utilities.api.inventory.custom_items.CustomItem;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all the data that is cached when a player crafted recipe.
 * This way the recipe doesn't need to be verified again when the player collects the result.
 * <p>
 * It indexes at which place of the grid which CustomItem is used, so it can use the CustomItem consume options, etc. a user might have saved.
 * <p>
 * The indexed Ingredients are used to target specific items and use it inside the Target options.
 */
public class CraftingData {

    CraftingRecipe<?> recipe;
    Result<?> result;
    Map<Vec2d, CustomItem> foundItems;
    Map<Ingredient, Vec2d> mappedIngredients;

    public CraftingData(CraftingRecipe<?> recipe, Map<Vec2d, CustomItem> foundItems, ItemStack[] matrix) {
        this(recipe, foundItems, new HashMap<>(), matrix);
    }

    public CraftingData(CraftingRecipe<?> recipe, Map<Vec2d, CustomItem> foundItems, Map<Ingredient, Vec2d> mappedIngredients, ItemStack[] matrix) {
        this.recipe = recipe;
        this.result = recipe.getResult().get(matrix);
        this.foundItems = foundItems;
        this.mappedIngredients = mappedIngredients;
    }

    public CraftingRecipe<?> getRecipe() {
        return recipe;
    }

    public Map<Vec2d, CustomItem> getFoundItems() {
        return foundItems;
    }

    public Result<?> getResult() {
        return result;
    }

    public void setResult(Result<?> result) {
        this.result = result;
    }
}
