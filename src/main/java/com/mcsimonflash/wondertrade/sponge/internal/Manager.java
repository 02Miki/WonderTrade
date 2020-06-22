package com.mcsimonflash.wondertrade.sponge.internal;

import com.mcsimonflash.wondertrade.sponge.data.TradeEntry;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Manager {

    static TradeEntry[] trades;
    private static final Random RANDOM = new Random();

    public static TradeEntry trade(TradeEntry entry) {
        //initial random check to check if it's a legendary
        //(equal legend chance regardless of selection logic later on)
        int index = RANDOM.nextInt(trades.length);
        TradeEntry tradeEntry = trades[index];


        if(!tradeEntry.getPokemon().isLegendary()){
            //If not a legendary, perform additional logic checks
            if(Config.useLevelRange){
                LevelRangeContainer levelRangeContainer = LevelRangeContainer.getRandomWeightedLevelRange();

                int low = entry.getPokemon().getLevel() - levelRangeContainer.levelRange;
                int high = entry.getPokemon().getLevel() + levelRangeContainer.levelRange;

                List<TradeEntry> filteredTrades = filterTrades(low, high, false);
                //we filter legendarys out as we check your chance to get one earlier,
                //with level based logic, you could exploit the system by only using pokemon within a legendary's level range

                index = RANDOM.nextInt(filteredTrades.size());
                TradeEntry ret = filteredTrades.get(index);

                int originalIndex = Arrays.asList(trades).indexOf(ret);

                trades[originalIndex] = entry;
                //Config.saveTrade(originalIndex);
                return ret;
            }
        }
        trades[index] = entry;
        //Config.saveTrade(index);
        return tradeEntry;

    }

    public static TradeEntry take(int index) {
        TradeEntry entry = trades[index];
        trades[index] = new TradeEntry(genRandomPixelmon(), Utils.ZERO_UUID, LocalDateTime.now());
        //Config.saveTrade(index);
        return entry;
    }

    public static void fillPool(boolean overwrite, boolean overwritePlayers) {
        for (int i = 0; i < trades.length; i++) {
            if (trades[i] == null || overwrite && (overwritePlayers || trades[i].getOwner().equals(Utils.ZERO_UUID))) {
                trades[i] = new TradeEntry(genRandomPixelmon(), Utils.ZERO_UUID, LocalDateTime.now());
            }
        }
        TradeConfig.saveConfig();
        //Config.saveAll();
    }

    private static Pokemon genRandomPixelmon() {
        if(Config.validLevelPokemon){
            for(int i = 1; i > 0; i++){
                EnumSpecies type = Config.legendRate != 0 && RANDOM.nextInt(Config.legendRate) == 0 ? EnumSpecies.LEGENDARY_ENUMS[RANDOM.nextInt(EnumSpecies.LEGENDARY_ENUMS.length)] : EnumSpecies.randomPoke(false);
                Pokemon poke = Pixelmon.pokemonFactory.create(type);
                poke.getBaseStats().calculateMinMaxLevels();

                int levelRange = poke.getBaseStats().spawnLevelRange;
                //Sanity Check to work around negative levelranges(Meinshao...)
                if(levelRange < 0){
                    levelRange = poke.getBaseStats().spawnLevelRange * -1;
                }

                int pokeMinLevel = poke.getBaseStats().minLevel;
                int pokeMaxLevel = poke.getBaseStats().spawnLevel + levelRange;

                if(pokeMinLevel > pokeMaxLevel){
                    pokeMaxLevel = pokeMinLevel;
                }


                int ourMinLevel = 0;
                if(pokeMinLevel <= Config.minLvl && Config.minLvl <= pokeMaxLevel){
                    ourMinLevel = Config.minLvl;
                }
                if(pokeMinLevel >= Config.minLvl && pokeMinLevel <= Config.maxLvl){
                    ourMinLevel = pokeMinLevel;
                }


                int ourMaxLevel = 0;
                if(pokeMaxLevel >=  Config.maxLvl && pokeMaxLevel >= Config.minLvl){
                    ourMaxLevel = Config.maxLvl;
                }
                if(pokeMaxLevel <= Config.maxLvl && pokeMaxLevel >= Config.minLvl){
                    ourMaxLevel = pokeMaxLevel;
                }

                if(pokeMaxLevel <= Config.minLvl){
                    ourMinLevel = Config.minLvl;
                    ourMaxLevel = Config.minLvl;
                }

                if(poke.isLegendary()){
                    ourMinLevel = poke.getBaseStats().spawnLevel;
                    ourMaxLevel = poke.getBaseStats().spawnLevel;
                }

                if(ourMinLevel >= Config.minLvl && ourMaxLevel <= Config.maxLvl){
                    int level = ourMinLevel == ourMaxLevel ? ourMaxLevel : RANDOM.nextInt(ourMaxLevel - ourMinLevel) + ourMinLevel;
                    poke.setLevel(level);
                    poke.setShiny(Config.shinyRate != 0 && RANDOM.nextInt(Config.shinyRate) == 0);
                    poke.setOriginalTrainer(Utils.ZERO_UUID, Config.customOriginalTrainerName);
                    if(Config.unbreedablePool){
                        poke.addSpecFlag("unbreedable");
                    }
                    if(Config.undexablePool){
                        poke.addSpecFlag("undexable");
                    }
                    return poke;
                }
            }
        }
        EnumSpecies type = Config.legendRate != 0 && RANDOM.nextInt(Config.legendRate) == 0 ? EnumSpecies.LEGENDARY_ENUMS[RANDOM.nextInt(EnumSpecies.LEGENDARY_ENUMS.length)] : EnumSpecies.randomPoke(false);
        PokemonSpec spec = PokemonSpec.from(type.name);
        spec.level = RANDOM.nextInt(Config.maxLvl - Config.minLvl) + Config.minLvl;
        spec.shiny = Config.shinyRate != 0 && RANDOM.nextInt(Config.shinyRate) == 0;
        Pokemon poke = spec.create();
        poke.setOriginalTrainer(Utils.ZERO_UUID, Config.customOriginalTrainerName);
        if(Config.unbreedablePool){
            poke.addSpecFlag("unbreedable");
        }
        if(Config.undexablePool){
            poke.addSpecFlag("undexable");
        }
        return poke;

    }


    private static List<TradeEntry> filterTrades(int lowBounds, int highBounds, boolean legendarys){
        List<TradeEntry> filteredTrades = Arrays.stream(trades).filter(tradeEntry -> tradeEntry.getPokemon().getLevel() >= lowBounds && tradeEntry.getPokemon().getLevel() <= highBounds).collect(Collectors.toList());
        if(filteredTrades.isEmpty()){
            for(int i = 0; i > -1; i++){
                int low = lowBounds - i;
                int high = highBounds + i;
                List<TradeEntry> filteredTrades1 = Arrays.stream(trades).filter(tradeEntry ->
                        tradeEntry.getPokemon().getLevel() >= low //if level above or equal to our lower bound
                        && tradeEntry.getPokemon().getLevel() <= high //if level below or equal to our upper bound
                        && tradeEntry.getPokemon().isLegendary() == legendarys //filter legendarys as per the parameter's specifications
                )
                        .collect(Collectors.toList());
                if(!filteredTrades1.isEmpty()){
                    return filteredTrades1;
                }
            }
        }
        return filteredTrades;
    }

}