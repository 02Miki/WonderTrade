package com.mcsimonflash.wondertrade.sponge.internal;

import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.mcsimonflash.sponge.teslalibs.configuration.ConfigHolder;
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
    private static ConfigHolder config, cooldowns, trades;

    public static boolean allowEggs, allowUntradable, unbreedablePool, undexablePool, undexablePlayerTrades, modifyOriginalTrainerOnTrade, broadcastTrades, regenOnRestart, regenOverwritePlayers, validLevelPokemon, useLevelRange;
    public static int poolSize, minLvl, maxLvl, shinyRate, legendRate, announceInt, entryStorageSaveInterval;
    public static long defCooldown;
    public static String prefix, primaryColor, secondaryColor, highlightColor, customOriginalTrainerName;
    public static void load() {
        try {
            config = getLoader(DIRECTORY, "wondertrades.conf", true);
            cooldowns = getLoader(STORAGE, "cooldowns.conf", false);
            //trades = getLoader(STORAGE, "trades.conf", false);

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

            allowEggs = config.getNode("allow-eggs").getBoolean(false);
            allowUntradable = config.getNode("allow-untradeable").getBoolean(false);
            unbreedablePool = config.getNode("unbreedable-pool").getBoolean(false);
            undexablePool = config.getNode("undexable-pool").getBoolean(false);
            undexablePlayerTrades = config.getNode("undexable-player-trades").getBoolean(false);
            modifyOriginalTrainerOnTrade = config.getNode("modifyOriginalTrainer").getBoolean(false);
            customOriginalTrainerName = config.getNode("customOriginalTrainerName").getString("WonderTrade");

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
        } catch (IOException | IllegalArgumentException e) {
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

    public static boolean saveTrade(int index) {
        serializeTrade(Manager.trades[index], trades.getNode("trades", index));
        return trades.save();
    }

    public static boolean saveAll() {
        for (int i = 0; i < poolSize; i++) {
            serializeTrade(Manager.trades[i], trades.getNode("trades", i));
        }
        return trades.save();
    }

    public static long getCooldown(UUID uuid) {
        return cooldowns.getNode(uuid.toString()).getLong(0);
    }

    public static boolean resetCooldown(UUID uuid) {
        cooldowns.getNode(uuid.toString()).setValue(System.currentTimeMillis());
        return cooldowns.save();
    }

    private static TradeEntry deserializeTrade(ConfigurationNode node) {
        EnumSpecies poke = EnumSpecies.getFromName(node.getNode("name").getString("")).orElseThrow(() -> new IllegalStateException("Malformed storage - no pokemon named " + node.getNode("name").getString()));

        UUID owner = UUID.fromString(node.getNode("owner").getString(Utils.ZERO_UUID.toString()));
        LocalDateTime date = LocalDateTime.ofEpochSecond(node.getNode("time").getLong(0), 0, ZoneOffset.UTC);
        Pokemon pokemon = Pixelmon.pokemonFactory.create(poke);
        pokemon.setLevel(node.getNode("level").getInt(5));
        pokemon.setGender(Gender.values()[node.getNode("gender").getInt(0)]);
        pokemon.setGrowth(EnumGrowth.getGrowthFromIndex(node.getNode("growth").getInt(3)));
        pokemon.setNature(EnumNature.getNatureFromIndex(node.getNode("nature").getInt(4)));
        pokemon.setAbility(node.getNode("ability").getString(""));
        pokemon.setShiny(node.getNode("shiny").getBoolean(false));
        pokemon.setForm(node.getNode("form").getInt(0));
        pokemon.setCaughtBall(EnumPokeballs.values()[node.getNode("ball").getInt(0)]);
        return new TradeEntry(pokemon, owner, date);
    }

    private static void serializeTrade(TradeEntry entry, ConfigurationNode node) {
        node.getNode("owner").setValue(entry.getOwner().toString());
        node.getNode("time").setValue(entry.getDate().toEpochSecond(ZoneOffset.UTC));
        node.getNode("name").setValue(entry.getPokemon().getSpecies().name);
        node.getNode("level").setValue(entry.getPokemon().getLevel());
        node.getNode("gender").setValue(entry.getPokemon().getGender().ordinal());
        node.getNode("growth").setValue(entry.getPokemon().getGrowth().index);
        node.getNode("nature").setValue(entry.getPokemon().getNature().index);
        node.getNode("ability").setValue(entry.getPokemon().getAbility().getName());
        node.getNode("shiny").setValue(entry.getPokemon().isShiny());
        node.getNode("form").setValue(entry.getPokemon().getForm());
        node.getNode("ball").setValue(entry.getPokemon().getCaughtBall().ordinal());
    }

}