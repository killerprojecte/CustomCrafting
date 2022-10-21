/*
 *       ____ _  _ ____ ___ ____ _  _ ____ ____ ____ ____ ___ _ _  _ ____
 *       |    |  | [__   |  |  | |\/| |    |__/ |__| |___  |  | |\ | | __
 *       |___ |__| ___]  |  |__| |  | |___ |  \ |  | |     |  | | \| |__]
 *
 *       CustomCrafting Recipe creation and management tool for Minecraft
 *                      Copyright (C) 2021  WolfyScript
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.wolfyscript.customcrafting.recipes;

import com.wolfyscript.utilities.bukkit.nms.item.crafting.FunctionalRecipeBuilderShaped;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.recipes.conditions.Conditions;
import me.wolfyscript.customcrafting.recipes.data.CraftingData;
import me.wolfyscript.customcrafting.recipes.data.IngredientData;
import me.wolfyscript.customcrafting.recipes.settings.AdvancedRecipeSettings;
import me.wolfyscript.customcrafting.utils.CraftManager;
import me.wolfyscript.lib.com.fasterxml.jackson.annotation.JacksonInject;
import me.wolfyscript.lib.com.fasterxml.jackson.annotation.JsonCreator;
import me.wolfyscript.lib.com.fasterxml.jackson.annotation.JsonProperty;
import me.wolfyscript.lib.com.fasterxml.jackson.databind.JsonNode;
import me.wolfyscript.utilities.api.inventory.custom_items.CustomItem;
import me.wolfyscript.utilities.util.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public class CraftingRecipeShaped extends AbstractRecipeShaped<CraftingRecipeShaped, AdvancedRecipeSettings> implements ICustomVanillaRecipe<org.bukkit.inventory.ShapedRecipe> {

    public CraftingRecipeShaped(NamespacedKey namespacedKey, JsonNode node) {
        super(namespacedKey, node, 3, AdvancedRecipeSettings.class);
    }

    @JsonCreator
    public CraftingRecipeShaped(@JsonProperty("key") @JacksonInject("key") NamespacedKey key, @JacksonInject("customcrafting") CustomCrafting customCrafting, @JsonProperty("symmetry") Symmetry symmetry, @JsonProperty("shape") String[] shape) {
        super(key, customCrafting, symmetry, shape, 3, new AdvancedRecipeSettings());
    }

    @Deprecated
    public CraftingRecipeShaped(NamespacedKey key, Symmetry symmetry, String[] shape) {
        this(key, CustomCrafting.inst(), symmetry, shape);
    }

    @Deprecated
    public CraftingRecipeShaped(NamespacedKey key) {
        super(key, CustomCrafting.inst(), new Symmetry(), 3, new AdvancedRecipeSettings());
    }

    private CraftingRecipeShaped(CraftingRecipeShaped craftingRecipe) {
        super(craftingRecipe);
    }

    @Override
    public CraftingRecipeShaped clone() {
        return new CraftingRecipeShaped(this);
    }

    @Override
    public org.bukkit.inventory.ShapedRecipe getVanillaRecipe() {
        if (!getResult().isEmpty() && !ingredients.isEmpty()) {
            if (customCrafting.getConfigHandler().getConfig().isNMSBasedCrafting()) {
                FunctionalRecipeBuilderShaped builder = new FunctionalRecipeBuilderShaped(getNamespacedKey(), getResult().getItemStack(), getInternalShape().getWidth(), getInternalShape().getHeight());
                final CraftManager craftManager = CustomCrafting.inst().getCraftManager();
                builder.setRecipeMatcher((inventory, world) -> {
                    if (!isDisabled() && inventory.getHolder() instanceof Player player) {
                        if (checkConditions(Conditions.Data.of(player, inventory.getLocation() != null ? inventory.getLocation().getBlock() : player.getLocation().getBlock(), player.getOpenInventory()))) {
                            CraftingData craftingData = check(craftManager.getMatrixData(player.getOpenInventory(), inventory));
                            if (craftingData != null) {
                                craftManager.put(player.getUniqueId(), craftingData);
                                return true;
                            }
                        }
                        craftManager.remove(player.getUniqueId());
                    }
                    return false;
                });
                builder.setRecipeAssembler(inventory -> {
                    if (inventory.getHolder() instanceof Player player)
                        return Optional.ofNullable(getResult().getItem(player).orElse(new CustomItem(Material.AIR)).create());
                    return Optional.of(new ItemStack(Material.AIR));
                });
                builder.setRemainingItemsFunction(inventory -> {
                    if (!isDisabled() && inventory.getHolder() instanceof Player player) {
                        craftManager.get(player.getUniqueId()).ifPresent(craftingData -> {
                            for (int i = 0; i < inventory.getMatrix().length; i++) {
                                IngredientData ingredientData = craftingData.getIndexedBySlot().get(i);
                                if (ingredientData != null) {
                                    inventory.setItem(i+1, ingredientData.customItem().shrink(inventory.getMatrix()[i], 1, ingredientData.ingredient().isReplaceWithRemains(), inventory, player, player.getLocation()));
                                }
                            }
                        });
                    }
                    return Optional.of(new ArrayList<>());
                });
                builder.setGroup(group);
                builder.setChoices(getIngredients().stream().map(ingredient -> ingredient.isEmpty() ? null : new RecipeChoice.ExactChoice(ingredient.getBukkitChoices())).collect(Collectors.toCollection(ArrayList::new)));
                builder.createAndRegister();
            } else {
                var recipe = new org.bukkit.inventory.ShapedRecipe(new org.bukkit.NamespacedKey(getNamespacedKey().getNamespace(), getNamespacedKey().getKey()), getResult().getItemStack());
                recipe.shape(getShape());
                mappedIngredients.forEach((character, items) -> recipe.setIngredient(character, new RecipeChoice.ExactChoice(items.getChoices().stream().map(CustomItem::getItemStack).distinct().toList())));
                recipe.setGroup(getGroup());
                return recipe;
            }
        }
        return null;
    }

    @Override
    public boolean isVisibleVanillaBook() {
        return vanillaBook;
    }

    @Override
    public void setVisibleVanillaBook(boolean vanillaBook) {
        this.vanillaBook = vanillaBook;
    }

    @Override
    public boolean isAutoDiscover() {
        return autoDiscover;
    }

    @Override
    public void setAutoDiscover(boolean autoDiscover) {
        this.autoDiscover = autoDiscover;
    }
}