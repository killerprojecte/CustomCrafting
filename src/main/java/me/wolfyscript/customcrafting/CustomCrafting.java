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

package me.wolfyscript.customcrafting;

import me.wolfyscript.customcrafting.commands.CommandCC;
import me.wolfyscript.customcrafting.commands.CommandRecipe;
import me.wolfyscript.customcrafting.configs.custom_data.CauldronData;
import me.wolfyscript.customcrafting.configs.custom_data.EliteWorkbenchData;
import me.wolfyscript.customcrafting.configs.custom_data.RecipeBookData;
import me.wolfyscript.customcrafting.data.CCCache;
import me.wolfyscript.customcrafting.data.CCPlayerData;
import me.wolfyscript.customcrafting.data.cauldron.Cauldrons;
import me.wolfyscript.customcrafting.data.patreon.Patreon;
import me.wolfyscript.customcrafting.data.patreon.Patron;
import me.wolfyscript.customcrafting.gui.elite_crafting.EliteCraftingCluster;
import me.wolfyscript.customcrafting.gui.item_creator.ClusterItemCreator;
import me.wolfyscript.customcrafting.gui.item_creator.tabs.*;
import me.wolfyscript.customcrafting.gui.main_gui.ClusterMain;
import me.wolfyscript.customcrafting.gui.potion_creator.ClusterPotionCreator;
import me.wolfyscript.customcrafting.gui.recipe_creator.ClusterRecipeCreator;
import me.wolfyscript.customcrafting.gui.recipebook.ClusterRecipeBook;
import me.wolfyscript.customcrafting.gui.recipebook_editor.ClusterRecipeBookEditor;
import me.wolfyscript.customcrafting.handlers.ConfigHandler;
import me.wolfyscript.customcrafting.handlers.DataHandler;
import me.wolfyscript.customcrafting.handlers.DisableRecipesHandler;
import me.wolfyscript.customcrafting.listeners.*;
import me.wolfyscript.customcrafting.network.NetworkHandler;
import me.wolfyscript.customcrafting.placeholderapi.PlaceHolder;
import me.wolfyscript.customcrafting.recipes.conditions.*;
import me.wolfyscript.customcrafting.recipes.items.extension.*;
import me.wolfyscript.customcrafting.recipes.items.target.MergeAdapter;
import me.wolfyscript.customcrafting.recipes.items.target.adapters.*;
import me.wolfyscript.customcrafting.registry.CCRegistries;
import me.wolfyscript.customcrafting.utils.ChatUtils;
import me.wolfyscript.customcrafting.utils.CraftManager;
import me.wolfyscript.customcrafting.utils.NamespacedKeyUtils;
import me.wolfyscript.customcrafting.utils.UpdateChecker;
import me.wolfyscript.customcrafting.utils.cooking.CookingManager;
import me.wolfyscript.customcrafting.utils.other_plugins.OtherPlugins;
import me.wolfyscript.utilities.api.WolfyUtilCore;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.chat.Chat;
import me.wolfyscript.utilities.api.inventory.gui.InventoryAPI;
import me.wolfyscript.utilities.util.NamespacedKey;
import me.wolfyscript.utilities.util.Reflection;
import me.wolfyscript.utilities.util.entity.CustomPlayerData;
import me.wolfyscript.utilities.util.json.jackson.KeyedTypeIdResolver;
import me.wolfyscript.utilities.util.version.WUVersion;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class CustomCrafting extends JavaPlugin {

    //Only used for displaying which version it is.
    private static final boolean PREMIUM = false;

    public static final NamespacedKey ADVANCED_CRAFTING_TABLE = new NamespacedKey(NamespacedKeyUtils.NAMESPACE, "advanced_crafting_table");
    public static final NamespacedKey INTERNAL_ADVANCED_CRAFTING_TABLE = NamespacedKeyUtils.fromInternal(ADVANCED_CRAFTING_TABLE);
    public static final NamespacedKey ELITE_CRAFTING_TABLE = new NamespacedKey(NamespacedKeyUtils.NAMESPACE, "elite_crafting_table");
    public static final NamespacedKey RECIPE_BOOK = new NamespacedKey(NamespacedKeyUtils.NAMESPACE, "recipe_book");
    public static final NamespacedKey CAULDRON = new NamespacedKey(NamespacedKeyUtils.NAMESPACE, "cauldron");
    //Used for backwards compatibility
    public static final NamespacedKey ADVANCED_WORKBENCH = new NamespacedKey(NamespacedKeyUtils.NAMESPACE, "workbench");

    private static final String CONSOLE_SEPARATOR = "------------------------------------------------------------------------";

    public static final int BUKKIT_VERSION = Bukkit.getUnsafe().getDataVersion();
    public static final int CONFIG_VERSION = 5;

    //Instance Object to use when no Object was passed!
    private static CustomCrafting instance;
    //Utils
    private final String currentVersion;
    private final WUVersion version;
    private final Patreon patreon;
    private final ChatUtils chatUtils;
    //The main WolfyUtilities instance
    private final WolfyUtilities api;
    private final Chat chat;
    private final CCRegistries registries;

    //Recipe Managers / API
    private final CraftManager craftManager;
    private final CookingManager cookingManager;

    //File Handlers to load, save or edit data
    private ConfigHandler configHandler;
    private DataHandler dataHandler;
    private Cauldrons cauldrons = null;

    private final UpdateChecker updateChecker;
    private final NetworkHandler networkHandler;

    private DisableRecipesHandler disableRecipesHandler;

    private final OtherPlugins otherPlugins;
    private final boolean isPaper;

    public CustomCrafting() {
        super();
        instance = this;
        currentVersion = getDescription().getVersion();
        this.version = WUVersion.parse(currentVersion.split("-")[0]);
        this.otherPlugins = new OtherPlugins(this);
        isPaper = WolfyUtilities.hasClass("com.destroystokyo.paper.utils.PaperPluginLogger");
        api = WolfyUtilCore.getInstance().getAPI(this, false);

        this.registries = new CCRegistries(this, api.getCore());

        this.chat = api.getChat();
        this.chat.setInGamePrefix("§7[§3CC§7] ");
        api.setInventoryAPI(new InventoryAPI<>(api.getPlugin(), api, CCCache.class));
        this.chatUtils = new ChatUtils(this);
        this.patreon = new Patreon();
        this.updateChecker = new UpdateChecker(this, 55883);
        this.networkHandler = new NetworkHandler(this, api);

        this.craftManager = new CraftManager(this);
        this.cookingManager = new CookingManager(this);
    }

    public static CustomCrafting inst() {
        return instance;
    }

    /**
     * @deprecated Replaced by {@link #inst()}
     */
    @Deprecated
    public static CustomCrafting getInst() {
        return inst();
    }

    @Override
    public void onLoad() {
        getLogger().info("WolfyUtilities API: " + Bukkit.getPluginManager().getPlugin("WolfyUtilities"));
        getLogger().info("Environment: " + WolfyUtilities.getENVIRONMENT());

        getLogger().info("Registering custom data");
        var customItemData = api.getRegistries().getCustomItemData();
        customItemData.register(new EliteWorkbenchData.Provider());
        customItemData.register(new RecipeBookData.Provider());
        customItemData.register(new CauldronData.Provider());

        getLogger().info("Registering Result Extensions");
        var resultExtensions = getRegistries().getRecipeResultExtensions();
        resultExtensions.register(new CommandResultExtension());
        resultExtensions.register(new MythicMobResultExtension());
        resultExtensions.register(new SoundResultExtension());
        resultExtensions.register(new ResultExtensionAdvancement());

        CustomPlayerData.register(new CCPlayerData.Provider());

        getLogger().info("Registering Result Merge Adapters");
        var resultMergeAdapters = getRegistries().getRecipeMergeAdapters();
        resultMergeAdapters.register(new EnchantMergeAdapter());
        resultMergeAdapters.register(new EnchantedBookMergeAdapter());
        resultMergeAdapters.register(new DisplayNameMergeAdapter());
        resultMergeAdapters.register(new DamageMergeAdapter());
        resultMergeAdapters.register(new PlaceholderAPIMergeAdapter());
        resultMergeAdapters.register(new FireworkRocketMergeAdapter());

        getLogger().info("Registering Recipe Conditions");
        var recipeConditions = getRegistries().getRecipeConditions();
        recipeConditions.register(AdvancedWorkbenchCondition.KEY, AdvancedWorkbenchCondition.class, new AdvancedWorkbenchCondition.GUIComponent());
        recipeConditions.register(CraftDelayCondition.KEY, CraftDelayCondition.class, new CraftDelayCondition.GUIComponent());
        recipeConditions.register(CraftLimitCondition.KEY, CraftLimitCondition.class, new CraftLimitCondition.GUIComponent());
        recipeConditions.register(EliteWorkbenchCondition.KEY, EliteWorkbenchCondition.class, new EliteWorkbenchCondition.GUIComponent());
        recipeConditions.register(ExperienceCondition.KEY, ExperienceCondition.class, new ExperienceCondition.GUIComponent());
        recipeConditions.register(PermissionCondition.KEY, PermissionCondition.class, new PermissionCondition.GUIComponent());
        recipeConditions.register(WeatherCondition.KEY, WeatherCondition.class, new WeatherCondition.GUIComponent());
        recipeConditions.register(WorldBiomeCondition.KEY, WorldBiomeCondition.class, new WorldBiomeCondition.GUIComponent());
        recipeConditions.register(WorldNameCondition.KEY, WorldNameCondition.class, new WorldNameCondition.GUIComponent());
        recipeConditions.register(WorldTimeCondition.KEY, WorldTimeCondition.class, new WorldTimeCondition.GUIComponent());
        recipeConditions.register(ConditionAdvancement.KEY, ConditionAdvancement.class, new ConditionAdvancement.GUIComponent());

        KeyedTypeIdResolver.registerTypeRegistry(ResultExtension.class, resultExtensions);
        KeyedTypeIdResolver.registerTypeRegistry(MergeAdapter.class, resultMergeAdapters);
        KeyedTypeIdResolver.registerTypeRegistry((Class<Condition<?>>) (Object) Condition.class, recipeConditions);
    }

    @Override
    public void onEnable() {
        this.api.initialize();
        writeBanner();
        writePatreonCredits();
        writeSeparator();

        this.otherPlugins.init();

        configHandler = new ConfigHandler(this);
        configHandler.loadRecipeBookConfig();
        configHandler.renameOldRecipesFolder();
        dataHandler = new DataHandler(this);
        configHandler.loadDefaults();
        disableRecipesHandler = new DisableRecipesHandler(this);

        writeSeparator();
        registerListeners();
        registerCommands();
        registerInventories();
        if (WolfyUtilities.isDevEnv()) {
            this.networkHandler.registerPackets();
        }

        cauldrons = new Cauldrons(this);
        if (WolfyUtilities.hasPlugin("PlaceholderAPI")) {
            api.getConsole().info("$msg.startup.placeholder$");
            new PlaceHolder(this).register();
        }
        if (api.getCore().getCompatibilityManager().getPlugins().isDoneLoading()) {
            dataHandler.loadRecipesAndItems();
        }
        updateChecker.run(null);

        //Load Metrics
        var metrics = new Metrics(this, 3211);
        metrics.addCustomChart(new SimplePie("used_language", () -> getConfigHandler().getConfig().getString("language")));
        metrics.addCustomChart(new SimplePie("advanced_workbench", () -> configHandler.getConfig().isAdvancedWorkbenchEnabled() ? "enabled" : "disabled"));
        writeSeparator();
    }

    @Override
    public void onDisable() {
        try {
            configHandler.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cauldrons.endAutoSaveTask();
        cauldrons.save();
    }

    private void writeBanner() {
        getLogger().info("____ _  _ ____ ___ ____ _  _ ____ ____ ____ ____ ___ _ _  _ ____ ");
        getLogger().info("|    |  | [__   |  |  | |\\/| |    |__/ |__| |___  |  | |\\ | | __ ");
        getLogger().info("|___ |__| ___]  |  |__| |  | |___ |  \\ |  | |     |  | | \\| |__]");
        getLogger().info(() -> "    v" + currentVersion + " " + (PREMIUM ? "Premium" : "Free"));
        getLogger().info(" ");
    }

    public void writeSeparator() {
        getLogger().info(CONSOLE_SEPARATOR);
    }

    private void writePatreonCredits() {
        patreon.initialize();
        getLogger().info("");
        getLogger().info("Special thanks to my Patrons for supporting this project: ");
        List<Patron> patronList = patreon.getPatronList();
        int lengthColumn = 20;
        int size = patronList.size();
        for (int i = 0; i <= size; i += 2) {
            if (i < size) {
                var sB = new StringBuilder();
                String name = patronList.get(i).getName();
                sB.append("| ").append(name);
                sB.append(" ".repeat(Math.max(0, lengthColumn - name.length())));
                if (i + 1 < patronList.size()) {
                    sB.append("| ").append(patronList.get(i + 1).getName());
                }
                getLogger().log(Level.INFO, "     {0}", sB);
            }
        }
    }

    private void registerListeners() {
        var pM = Bukkit.getPluginManager();
        pM.registerEvents(new PlayerListener(this), this);
        pM.registerEvents(new CraftListener(this), this);
        pM.registerEvents(new FurnaceListener(this, cookingManager), this);
        pM.registerEvents(new AnvilListener(this), this);
        pM.registerEvents(new CauldronListener(this), this);
        pM.registerEvents(new EliteWorkbenchListener(api), this);
        pM.registerEvents(new GrindStoneListener(this), this);
        pM.registerEvents(new BrewingStandListener(api, this), this);
        pM.registerEvents(new RecipeBookListener(), this);
        pM.registerEvents(new SmithingListener(this), this);
    }

    private void registerCommands() {
        final var serverCommandMap = Reflection.getDeclaredField(Bukkit.getServer().getClass(), "commandMap");
        serverCommandMap.setAccessible(true);
        try {
            var commandMap = (CommandMap) serverCommandMap.get(Bukkit.getServer());
            commandMap.register("customcrafting", new CommandCC(this));
            commandMap.register("recipes", "customcrafting", new CommandRecipe(this));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void registerInventories() {
        api.getConsole().info("$msg.startup.inventories$");
        InventoryAPI<CCCache> invAPI = this.api.getInventoryAPI(CCCache.class);
        var registry = getRegistries().getItemCreatorTabs();
        //Register tabs for the item creator
        registry.register(new TabArmorSlots());
        registry.register(new TabAttributes());
        registry.register(new TabConsume());
        registry.register(new TabCustomDurability());
        registry.register(new TabCustomModelData());
        registry.register(new TabDamage());
        registry.register(new TabDisplayName());
        registry.register(new TabEliteCraftingTable());
        registry.register(new TabEnchants());
        registry.register(new TabFlags());
        registry.register(new TabFuel());
        registry.register(new TabLocalizedName());
        registry.register(new TabLore());
        registry.register(new TabParticleEffects());
        registry.register(new TabPermission());
        registry.register(new TabPlayerHead());
        registry.register(new TabPotion());
        registry.register(new TabRarity());
        registry.register(new TabRecipeBook());
        registry.register(new TabRepairCost());
        registry.register(new TabVanilla());
        registry.register(new TabUnbreakable());
        //Register the GUIs
        invAPI.registerCluster(new ClusterMain(invAPI, this));
        invAPI.registerCluster(new ClusterRecipeCreator(invAPI, this));
        invAPI.registerCluster(new ClusterRecipeBook(invAPI, this));
        invAPI.registerCluster(new EliteCraftingCluster(invAPI, this));
        invAPI.registerCluster(new ClusterItemCreator(invAPI, this));
        invAPI.registerCluster(new ClusterPotionCreator(invAPI, this));
        invAPI.registerCluster(new ClusterRecipeBookEditor(invAPI, this));
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public WolfyUtilities getApi() {
        return api;
    }

    public void onPlayerDisconnect(Player player) {
        this.networkHandler.disconnectPlayer(player);
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public CraftManager getCraftManager() {
        return craftManager;
    }

    public CookingManager getCookingManager() {
        return cookingManager;
    }

    public ChatUtils getChatUtils() {
        return chatUtils;
    }

    public Cauldrons getCauldrons() {
        return cauldrons;
    }

    public Patreon getPatreon() {
        return patreon;
    }

    public boolean isPaper() {
        return isPaper;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public WUVersion getVersion() {
        return version;
    }

    public DisableRecipesHandler getDisableRecipesHandler() {
        return disableRecipesHandler;
    }

    public CCRegistries getRegistries() {
        return registries;
    }
}
