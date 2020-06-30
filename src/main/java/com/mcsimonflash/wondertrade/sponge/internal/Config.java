package com.mcsimonflash.wondertrade.sponge.internal;

import com.google.common.reflect.TypeToken;
import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.mcsimonflash.sponge.teslalibs.configuration.ConfigHolder;
import com.mcsimonflash.wondertrade.sponge.data.BroadcastTypes;
import com.mcsimonflash.wondertrade.sponge.data.TradeEntry;
import com.pixelmonmod.pixelmon.Pixelmon;

import com.pixelmonmod.pixelmon.entities.pixelmon.stats.Gender;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import com.pixelmonmod.pixelmon.enums.EnumNature;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.enums.items.EnumPokeballs;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Config {

    private static final Path DIRECTORY = WonderTrade.getDirectory(), STORAGE = DIRECTORY.resolve("storage");
    private static ConfigHolder config, cooldowns;

    public static boolean allowEggs, notifyCooldowns, allowUntradable, unbreedablePool, undexablePool, undexablePlayerTrades, modifyOriginalTrainerOnTrade, broadcastTrades, regenOnRestart, regenOverwritePlayers, validLevelPokemon, useLevelRange;
    public static int poolSize, minLvl, maxLvl, shinyRate, legendRate, announceInt, entryStorageSaveInterval;
    public static long defCooldown;
    public static String prefix, primaryColor, secondaryColor, highlightColor, customOriginalTrainerName;
    public static List<BroadcastTypes> broadcastTypes = new ArrayList<>();
    public static void load() {
        try {
            config = getLoader(DIRECTORY, "wondertrades.conf", true);
            cooldowns = getLoader(STORAGE, "cooldowns.conf", false);

            broadcastTrades = config.getNode("broadcast-trades").getBoolean(true);
            regenOnRestart = config.getNode("regen-on-restart").getBoolean(false);
            regenOverwritePlayers = config.getNode("regen-overwrite-players").getBoolean(false);
            poolSize = config.getNode("pool-size").getInt(250);
            defCooldown = config.getNode("default-cooldown").getLong(1200000);
            minLvl = config.getNode("min-level").getInt(5);
            maxLvl = config.getNode("max-level").getInt(65);
            shinyRate = config.getNode("shiny-rate").getInt(2048);
            legendRate = config.getNode("legendary-rate").getInt(4096);
            announceInt = config.getNode("announcement-interval").getInt(1800000);
            entryStorageSaveInterval = config.getNode("entry-storage-save-interval").getInt(300000);
            prefix = config.getNode("message-prefix").getString("&8[&3WonderTrade&8] ");
            primaryColor = config.getNode("primary-color").getString("&3");
            secondaryColor = config.getNode("secondary-color").getString("&9");
            highlightColor = config.getNode("highlight-color").getString("&e");
            validLevelPokemon = config.getNode("valid-level-pokemon").getBoolean(true);

            useLevelRange = config.getNode("use-level-ranges").getBoolean(false);
            LevelRangeContainer.totalChance = 0;
            LevelRangeContainer.rangeList.clear();
            for(ConfigurationNode node : config.getNode("return-pokemon-level-ranges").getChildrenList()){
                LevelRangeContainer.rangeList.add(new LevelRangeContainer(node));
            }

            broadcastTypes.clear();
            for(String string : config.getNode("broadcastTypes").getList(TypeToken.of(String.class))){
                broadcastTypes.add(BroadcastTypes.valueOf(string));
            }

            allowEggs = config.getNode("allow-eggs").getBoolean(false);
            allowUntradable = config.getNode("allow-untradeable").getBoolean(false);
            unbreedablePool = config.getNode("unbreedable-pool").getBoolean(false);
            undexablePool = config.getNode("undexable-pool").getBoolean(false);
            undexablePlayerTrades = config.getNode("undexable-player-trades").getBoolean(false);
            modifyOriginalTrainerOnTrade = config.getNode("modifyOriginalTrainer").getBoolean(false);
            customOriginalTrainerName = config.getNode("customOriginalTrainerName").getString("WonderTrade");
            notifyCooldowns = config.getNode("notify-player-cooldown").getBoolean(true);

            boolean startup = Manager.trades == null;
            Manager.trades = new TradeEntry[poolSize];

            TradeConfig.configFile = WonderTrade.getDirectory().resolve("storage").resolve("trades.pool").toFile();
            if(!TradeConfig.configFile.exists()){
                Manager.fillPool(startup && regenOnRestart, regenOverwritePlayers);
                TradeConfig.loadConfig();
            }
            else{
                TradeConfig.loadConfig();
                Manager.fillPool(startup && regenOnRestart, regenOverwritePlayers);
            }
            Inventory.reloadMenuElements();
        } catch (IOException | IllegalArgumentException | ObjectMappingException e) {
            WonderTrade.getLogger().error("Error loading config: " + e.getMessage());
        }
    }

    private static ConfigHolder getLoader(Path dir, String name, boolean asset) throws IOException {
        try {
            Path path = dir.resolve(name);
            if (Files.notExists(path)) {
                Files.createDirectories(dir);
                if (asset) {
                    WonderTrade.getContainer().getAsset(name).get().copyToFile(path);
                } else {
                    Files.createFile(path);
                }
            }
            return ConfigHolder.of(HoconConfigurationLoader.builder().setPath(path).build());
        } catch (IOException e) {
            WonderTrade.getLogger().error("Unable to load config file " + name + ".");
            throw e;
        }
    }

    public static long getCooldown(UUID uuid) {
        return cooldowns.getNode(uuid.toString()).getLong(0);
    }

    public static boolean resetCooldown(UUID uuid) {
        cooldowns.getNode(uuid.toString()).setValue(System.currentTimeMillis());
        return cooldowns.save();
    }
}