package com.mcsimonflash.wondertrade.sponge.internal;

import ninja.leaping.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LevelRangeContainer {
    public static List<LevelRangeContainer> rangeList = new ArrayList<>();
    public static int totalChance = 0;

    public int chance;
    public int levelRange;

    public LevelRangeContainer(ConfigurationNode node) {
        chance = node.getNode("chance").getInt();
        levelRange = node.getNode("range").getInt();
        totalChance += chance;
    }

    public static LevelRangeContainer getRandomWeightedLevelRange(){
        int cumulative = 0;
        int randomIndex = new Random().nextInt(totalChance);
        for (LevelRangeContainer levelRangeContainer : rangeList) {
            cumulative += levelRangeContainer.chance;
            if (cumulative > randomIndex) {
                return levelRangeContainer;
            }
        }
        return null;
    }
}
