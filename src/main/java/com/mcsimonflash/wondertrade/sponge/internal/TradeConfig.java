package com.mcsimonflash.wondertrade.sponge.internal;

import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.mcsimonflash.wondertrade.sponge.data.TradeEntry;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TradeConfig {
    public static File configFile;

    public static void loadConfig(){
        WonderTrade.getLogger().info("Loading WonderTrade Pokemon Pool...");
        try {
            if(!configFile.exists()){
                configFile.createNewFile();
            }
            DataInputStream dataStream = new DataInputStream(new FileInputStream(configFile));
            NBTTagCompound nbt = CompressedStreamTools.read(dataStream);
            for(int i = 0; i < Config.poolSize; i++){
                if(nbt.hasKey("entry" + i)){
                    Manager.trades[i] = new TradeEntry(
                            Pixelmon.pokemonFactory.create(nbt.getCompoundTag("entry" + i)),
                            LocalDateTime.ofEpochSecond(nbt.getLong("entrydate" + i), 0, ZoneOffset.UTC)
                    );
                }
            }
            dataStream.close();
            WonderTrade.getLogger().info("Successfully loaded WonderTrade pool.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void saveConfig(){
        //WonderTrade.getLogger().info("Saving WonderTrade Pokemon Pool...");
        try{
            if(!configFile.exists()){
                configFile.createNewFile();
            }

            NBTTagCompound nbt = new NBTTagCompound();
            for(int i = 0; i < Config.poolSize; i++){
                TradeEntry entry = Manager.trades[i];
                nbt.setTag("entry" + i, entry.getPokemon().writeToNBT(new NBTTagCompound()));
                nbt.setLong("entrydate" + i, entry.getDate().toEpochSecond(ZoneOffset.UTC));
            }

            DataOutputStream dataStream = new DataOutputStream(new FileOutputStream(configFile));
            CompressedStreamTools.write(nbt, dataStream);
            dataStream.close();
            //WonderTrade.getLogger().info("Successfully saved WonderTrade pool.");
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
}
