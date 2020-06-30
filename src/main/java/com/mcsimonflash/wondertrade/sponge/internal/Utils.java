package com.mcsimonflash.wondertrade.sponge.internal;

import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.mcsimonflash.wondertrade.sponge.data.BroadcastTypes;
import com.mcsimonflash.wondertrade.sponge.data.TradeEntry;
import com.mcsimonflash.wondertrade.sponge.data.WonderTradeCooldownEvent;
import com.mcsimonflash.wondertrade.sponge.data.WonderTradeEvent;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.enums.DeleteType;
import com.pixelmonmod.pixelmon.api.enums.ReceiveType;
import com.pixelmonmod.pixelmon.api.events.PixelmonDeletedEvent;
import com.pixelmonmod.pixelmon.api.events.PixelmonReceivedEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HiddenPower;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.EVStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.IVStore;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.enums.EnumNature;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.enums.EnumType;
import com.pixelmonmod.pixelmon.enums.forms.EnumSpecial;
import com.pixelmonmod.pixelmon.enums.forms.RegionalForms;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.translation.locale.Locales;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Utils {

    private static World world;
    private static Task announcementTask;
    private static Task configSaveTask;
    public static HashMap<UUID, Task> playerCooldownTasks = new HashMap<>();
    public static final UUID ZERO_UUID = new UUID(0, 0);

    //public static final Pattern MESSAGE = Pattern.compile("\\[(.+?)]\\{((?:.|\\n)+?)}(?=.)");
    public static final Pattern MESSAGE = Pattern.compile("\\[(.+?)]\\{((?:.|\\n)+?)}");
    //public static final Pattern MESSAGE = Pattern.compile("\\[(.+?)]\\(((?:.|\n)+?)\\)");

    public static void initialize() {
        if (announcementTask != null) {
            announcementTask.cancel();
        }
        if (configSaveTask != null) {
            configSaveTask.cancel();
        }
        world = (net.minecraft.world.World) Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName())
                .orElseThrow(() -> new IllegalStateException("No default world."));
        Config.load();
        if (Config.announceInt > 0) {
            announcementTask = Task.builder()
                    .execute(t -> {
                        int shinies = 0, ultrabeasts = 0, legendaries = 0;
                        for (TradeEntry entry : Manager.trades) {
                            if (entry.getPokemon().isShiny()) {
                                shinies++;
                            }
                            if (EnumSpecies.ultrabeasts.contains(entry.getPokemon().getSpecies().name)) {
                                ultrabeasts++;
                            }
                            if (EnumSpecies.legendaries.contains(entry.getPokemon().getSpecies().name)) {
                                legendaries++;
                            }
                        }
                        //Intentionally don't add prefix to this message to offer servers full configurability of their message. (Hope to see multi-line usage!)
                        Sponge.getServer().getBroadcastChannel().send(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.announcement", "pool-size", Config.poolSize, "shinies", shinies, "ultrabeasts", ultrabeasts, "legendaries", legendaries).toText());
                    })
                    .interval(Config.announceInt, TimeUnit.MILLISECONDS)
                    .submit(WonderTrade.getContainer());
        }
        if(Config.entryStorageSaveInterval > 0){
            configSaveTask = Task.builder()
                    .execute(t -> TradeConfig.saveConfig())
                    .interval(Config.entryStorageSaveInterval, TimeUnit.MILLISECONDS)
                    .submit(WonderTrade.getContainer());
        }
    }

    public static Optional<URL> parseURL(String url) {
        try {
            return Optional.of(new URL(url));
        } catch (MalformedURLException ignored) {
            return Optional.empty();
        }
    }

    public static Text toText(String msg) {
        return TextSerializers.FORMATTING_CODE.deserialize(msg);
    }

    public static Text parseText(String message) {
        Matcher matcher = MESSAGE.matcher(message);
        Text.Builder builder = Text.builder();
        int index = 0;
        while (matcher.find()) {

            if (matcher.start() > index) {
                builder.append(toText(message.substring(index, matcher.start())));
            }
            Text.Builder subtext = toText(matcher.group(1)).toBuilder();
            String group = matcher.group(2);
            try {
                subtext.onClick(group.startsWith("/") ? TextActions.runCommand(group) : TextActions.openUrl(new URL(group)));
                subtext.onHover(TextActions.showText(toText(group)));
            } catch (MalformedURLException e) {
                subtext.onHover(TextActions.showText(toText(group)));
            }
            builder.append(subtext.build());
            index = matcher.end();
        }
        if (index == 0) {
            builder.append(toText(message));
        }
        else{
            builder.append(toText(message.substring(index)));
        }
        return builder.build();
    }


    public static PartyStorage getPartyStorage(Player player) {
        return Pixelmon.storageManager.getParty((EntityPlayerMP) player);
    }

    public static PCStorage getPCStorage(Player player) {
        return Pixelmon.storageManager.getPCForPlayer((EntityPlayerMP) player);
    }

    public static long getCooldown(Player player) {
        try {
            return Integer.parseInt(player.getOption("wondertrade:cooldown").orElse(String.valueOf(Config.defCooldown)));
        } catch (NumberFormatException e) {
            WonderTrade.getLogger().error("Invalid Cooldown Option Set For Player " + player.getName() + ": " + player.getOption("wondertrade:cooldown").orElse(""));
            return Config.defCooldown;
        }
    }

    public static void trade(Player player, int slot) {
        PartyStorage storage = getPartyStorage(player);
        Pokemon pokemon = storage.get(slot);
        //to stop ghost entities
        storage.retrieveAll();

        if (pokemon.isEgg() && !(Config.allowEggs || player.hasPermission("wondertrade.trade.eggbypass")) ){
            player.sendMessage(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-eggs").toString())));
            player.closeInventory();
        }
        if (pokemon.hasSpecFlag("untradeable") && !(Config.allowUntradable || player.hasPermission("wondertrade.trade.untradeablebypass")) ){
            player.sendMessage(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-untradeables").toString())));
            player.closeInventory();
        }
        else{
            TradeEntry entry = trade(player, pokemon);
            // if returns null, likely that the event was cancelled, otherwise could be a bug
            if(entry != null){
                //handle cooldown after successful trade, ensuring the player's cooldown only functions if their event goes through
                if(!player.hasPermission("wondertrade.trade.cooldownbypass")){
                    if (Config.resetCooldown(player.getUniqueId())) {
                        long time = Utils.getCooldown(player) - (System.currentTimeMillis() - Config.getCooldown(player.getUniqueId()));
                        Task cooldownTask = Utils.createCooldownTaskForPlayer(player, time);
                        Utils.playerCooldownTasks.put(player.getUniqueId(), cooldownTask);
                    }
                    else{
                        player.sendMessage(WonderTrade.getMessage(player, "wondertrade.trade.reset-cooldown.failure"));
                    }
                }

                storage.set(slot,null);
                Pixelmon.EVENT_BUS.post(new PixelmonDeletedEvent((EntityPlayerMP) player, pokemon, DeleteType.COMMAND));
                storage.set(slot, entry.getPokemon());
                Pixelmon.EVENT_BUS.post(new PixelmonReceivedEvent((EntityPlayerMP) player, ReceiveType.Command, entry.getPokemon()));
            }
        }
    }

    public static void trade(Player player, int box, int position) {
        PCStorage storage = getPCStorage(player);
        Pokemon pokemon = storage.getBox(box).get(position);
        //to stop ghost entities
        getPartyStorage(player).retrieveAll();

        if (pokemon.isEgg() && !(Config.allowEggs || player.hasPermission("wondertrade.trade.eggbypass")) ){
            player.sendMessage(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-eggs").toString())));
            player.closeInventory();
        }
        if (pokemon.hasSpecFlag("untradeable") && !(Config.allowUntradable || player.hasPermission("wondertrade.trade.untradeablebypass")) ){
            player.sendMessage(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-untradeables").toString())));
            player.closeInventory();
        }
        else{
            TradeEntry entry = trade(player, pokemon);
            // if returns null, likely that the event was cancelled, otherwise could be a bug
            if(entry != null){
                //handle cooldown after successful trade, ensuring the player's cooldown only functions if their event goes through
                if(!player.hasPermission("wondertrade.trade.cooldownbypass")){
                    if (Config.resetCooldown(player.getUniqueId())) {
                        long time = Utils.getCooldown(player) - (System.currentTimeMillis() - Config.getCooldown(player.getUniqueId()));
                        Task cooldownTask = Utils.createCooldownTaskForPlayer(player, time);
                        Utils.playerCooldownTasks.put(player.getUniqueId(), cooldownTask);
                    }
                    else{
                        player.sendMessage(WonderTrade.getMessage(player, "wondertrade.trade.reset-cooldown.failure"));
                    }
                }

                storage.getBox(box).set(position, null);
                Pixelmon.EVENT_BUS.post(new PixelmonDeletedEvent((EntityPlayerMP) player, pokemon, DeleteType.COMMAND));
                storage.getBox(box).set(position, entry.getPokemon());
                Pixelmon.EVENT_BUS.post(new PixelmonReceivedEvent((EntityPlayerMP) player, ReceiveType.Command, entry.getPokemon()));
            }
        }
    }

    private static TradeEntry trade(Player player, Pokemon pokemon) {
        //create TradeEntry object for input pokemon
        TradeEntry inputEntry = new TradeEntry(pokemon, player.getUniqueId(), LocalDateTime.now());
        //get TradeEntry object for what will be the output pokemon
        TradeEntry outputEntry = Manager.getTrade(inputEntry);

        //create event
        EventContext eventContext = EventContext.builder().add(EventContextKeys.PLUGIN, WonderTrade.getContainer()).add(EventContextKeys.PLAYER, player).build();
        WonderTradeEvent event = new WonderTradeEvent(player, inputEntry, outputEntry, Cause.of(eventContext,  WonderTrade.getContainer()));
        //push event
        Sponge.getEventManager().post(event);

        //if event doesn't get cancelled, we move forward
        if (!event.isCancelled()) {
            //log input
            logTransaction(player, inputEntry, true);
            //log output
            logTransaction(player, outputEntry, false);
            //perform change checks on output
            outputEntry.refine(player);
            //trade two pokemon
            Manager.performTradeForEntrys(inputEntry, outputEntry);

            Object[] args = new Object[] {"player", player.getName(), "traded", getShortDesc(pokemon), "traded-details", getDesc(pokemon), "received", getShortDesc(outputEntry.getPokemon()), "received-details", getDesc(outputEntry.getPokemon())};
            if (Config.broadcastTrades && (broadcastCheck(pokemon) || broadcastCheck(outputEntry.getPokemon()))) {
                //Player Input Pokemon
                if(broadcastCheck(pokemon) && !broadcastCheck(outputEntry.getPokemon())){
                    Sponge.getServer().getBroadcastChannel().send(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.broadcast.input", args).toString())));
                }
                //Returned Pokemon
                else if(broadcastCheck(outputEntry.getPokemon()) && !broadcastCheck(pokemon)){
                    Sponge.getServer().getBroadcastChannel().send(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.broadcast.received", args).toString())));
                }
                //Pog both were special
                else if(broadcastCheck(outputEntry.getPokemon()) && broadcastCheck(pokemon)){
                    Sponge.getServer().getBroadcastChannel().send(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.broadcast.doublespecial", args).toString())));
                }

            }
            player.sendMessage(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.message", args).toString())));
            return outputEntry;
        }
        return null;
    }

    public static void take(Player player, int index) {
        PartyStorage storage = getPartyStorage(player);
        storage.retrieveAll();
        TradeEntry entry = Manager.take(index).refine(player);
        logTransaction(player, entry, false);
        storage.add(entry.getPokemon());
    }

    public static void logTransaction(User user, TradeEntry entry, boolean add) {
        WonderTrade.getLogger().info(user.getName() + (add ? " added " : " removed ") + "a " + getShortDesc(entry.getPokemon()) + (add ? "." : " (added by " + entry.getOwnerName() + ")."));
    }

    public static String getShortDesc(Pokemon pokemon) {
        if(pokemon.isEgg()){
            return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.text.egg").toString();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.text.level"))
                .append(" ")
                .append(pokemon.getLevel());
        if(pokemon.isShiny()){
            builder.append(" ").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.text.shiny"));
        }
        if(EnumSpecies.ultrabeasts.contains(pokemon.getSpecies().name)){
            builder.append(" ").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.text.ultrabeast"));
        }
        if(EnumSpecies.legendaries.contains(pokemon.getSpecies().name)){
            builder.append(" ").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.text.legendary"));
        }
        if(pokemon.getForm() >= 100){
            builder.append(" ").append(getPokemonSpecialTexture(pokemon).getTranslatedName().getUnformattedComponentText());
        }
        else if(!pokemon.getCustomTexture().isEmpty() && pokemon.getCustomTexture() != null){
            builder.append(" ").append(capitalizeFirstLetter(pokemon.getCustomTexture()));
        }
        if(pokemon.getPersistentData().hasKey("entity-particles:particle")){
            builder.append(" ").append(capitalizeFirstLetter(pokemon.getPersistentData().getString("entity-particles:particle")));
        }
        if(pokemon.getFormEnum() == RegionalForms.ALOLAN || pokemon.getFormEnum() == RegionalForms.GALARIAN){
            builder.append(" ").append(pokemon.getFormEnum().getTranslatedName().getUnformattedComponentText());
        }
        builder.append(" ").append(pokemon.getSpecies().getTranslatedName().getUnformattedComponentText());
        return builder.toString();
    }


    public static String getDesc(Pokemon pokemon) {
        DecimalFormat f = new DecimalFormat("#.##");
        EVStore evs = pokemon.getEVs();
        IVStore ivs = pokemon.getIVs();
        int EVTotal = ((evs.hp + evs.attack + evs.defence + evs.specialAttack + evs.specialDefence + evs.speed));
        double EVPercent = (100* EVTotal/510.0);
        int IVTotal = ((ivs.hp + ivs.attack + ivs.defence + ivs.specialAttack + ivs.specialDefence + ivs.speed));
        double IVPercent = (100* IVTotal/186.0);

        if (pokemon.isEgg()) {
            return String.format("%1$s" + WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.pokemonlabel") + " : %2$s" + WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.egg"), Config.primaryColor, Config.secondaryColor);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("%1$s").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.pokemonlabel")).append(" : ");
        if (pokemon.isShiny()) {
            builder.append("%3$s").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.shinylabel")).append(" ");
        }
        if (EnumSpecies.ultrabeasts.contains(pokemon.getSpecies().name)) {
            builder.append("%3$s").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.ultrabeastlabel")).append(" ");
        }
        if (EnumSpecies.legendaries.contains(pokemon.getSpecies().name)) {
            builder.append("%3$s").append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.legendarylabel")).append(" ");
        }
        if (pokemon.getFormEnum() == RegionalForms.ALOLAN || pokemon.getFormEnum() == RegionalForms.GALARIAN) {
            builder.append("%3$s").append(pokemon.getFormEnum().getTranslatedName().getUnformattedComponentText()).append(" ");
        }
        builder.append("%2$s")
                .append(pokemon.getSpecies().getTranslatedName().getUnformattedComponentText()).append(" \n")
                .append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.levellabel"))
                .append(" : %2$s").append(pokemon.getLevel()).append(" \n")

                .append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.originaltrainerlabel"))
                .append(" : %2$s").append(pokemon.getOriginalTrainer()).append(" \n")

                .append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.abilitylabel"))
                .append(" : %2$s").append(pokemon.getAbility().getTranslatedName().getUnformattedComponentText());

        if(pokemon.getAbilitySlot() == 2){
            builder.append(" %2$s(%3$s")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.hiddenabilitylabel"))
                    .append("%2$s)");
        }
        builder.append(" \n");

        builder.append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.naturelabel"))
                .append(" : %2$s")
                .append(resolvePokeNatureString(pokemon.getNature())).append("\n");
        if(pokemon.getMintNature() != null){
            builder.append(" %3$s- ")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.mintnaturelabel"))
                    .append(" : %3$s")
                    .append(resolvePokeNatureString(pokemon.getNature())).append("\n");
        }

        builder.append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.hiddenpowerlabel"))
                .append(" : %2$s")
                .append(resolvePokeHiddenPowerString(pokemon)).append("\n");

        builder.append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.growthlabel"))
                .append(" : %2$s")
                .append(pokemon.getGrowth().getTranslatedName().getUnformattedComponentText()).append("\n");

        builder.append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.genderlabel"))
                .append(" : %2$s")
                .append(resolvePokeGenderString(pokemon)).append("\n");

        if (pokemon.getHeldItem() != net.minecraft.item.ItemStack.EMPTY) {
            builder.append(" %1$s- ")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.helditemlabel"))
                    .append(" : %2$s")
                    .append(pokemon.getHeldItem().getDisplayName()).append("\n");
        }

        builder.append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.pokeballlabel"))
                .append(" : %2$s")
                .append(pokemon.getCaughtBall().getTranslatedName().getUnformattedComponentText()).append("\n");

        if (pokemon.getForm() >= 100) {
            builder.append(" %3$s- ")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.specialtexturelabel"))
                    .append(" : %3$s")
                    .append(getPokemonSpecialTexture(pokemon).getTranslatedName().getUnformattedComponentText()).append("\n");
        }
        if (!pokemon.getCustomTexture().isEmpty() && pokemon.getCustomTexture() != null ){
            builder.append(" %3$s- ")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.customtexturelabel"))
                    .append(" : %3$s")
                    .append(capitalizeFirstLetter(pokemon.getCustomTexture())).append("\n");
        }
        if (pokemon.getPersistentData().hasKey("entity-particles:particle")){
            builder.append(" %3$s- ")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.entityparticles"))
                    .append(" : %3$s")
                    .append(capitalizeFirstLetter(pokemon.getPersistentData().getString("entity-particles:particle"))).append("\n");
        }

        builder.append(" %1$s- ")
                .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.friendshiplabel"))
                .append(" : %2$s")
                .append(pokemon.getFriendship()).append("\n").append("\n");

                //EVS
                builder.append("%1$s")
                        .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.evslabel"))
                        .append(" %1$s:   %2$s").append(EVTotal).append("%1$s/%2$s510 %1$s- %2$s").append(f.format(EVPercent)).append("%1$s%%")
                .append("\n   %2$s");

        StatsType[] types = StatsType.getStatValues();
        int statsLength = types.length;
        for(int i = 0; i < statsLength; i++) {
            StatsType type = types[i];

            builder.append(evs.get(type));
            builder.append(" %2$s").append(getShortStatName(type));

            if(i == 2){
                builder.append("\n   ");
            }
            else if(i != 5){
                builder.append(" %1$s/ %2$s");
            }
        }
//                .append(evs.hp).append(getShortStatName(StatsType.HP)).append(" %1$s/ %2$s")
//                .append(evs.attack).append(" Atk").append(" %1$s/ %2$s")
//                .append(evs.defence).append(" Def")
//                .append("\n   ")
//                .append(evs.specialAttack).append(" SpAtk").append(" %1$s/ %2$s")
//                .append(evs.specialDefence).append(" SpDef").append(" %1$s/ %2$s")
//                .append(evs.speed).append(" Speed")
//                .append("\n")

                //IVS
                builder.append("\n%1$s")
                        .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.ivslabel"))
                        .append(" %1$s:   %2$s").append(IVTotal).append("%1$s/%2$s186 %1$s- %2$s").append(f.format(IVPercent)).append("%1$s%%");
        if(ivs.isHyperTrained(StatsType.HP) || ivs.isHyperTrained(StatsType.Attack) || ivs.isHyperTrained(StatsType.Defence) || ivs.isHyperTrained(StatsType.SpecialAttack) || ivs.isHyperTrained(StatsType.SpecialDefence) || ivs.isHyperTrained(StatsType.Speed)){
            int hyperIVTotal = 0;

            for(int i = 0; i < statsLength; ++i) {
                StatsType type = types[i];
                if(ivs.isHyperTrained(type)){
                    hyperIVTotal += 31;
                }
                else{
                    hyperIVTotal += ivs.get(type);
                }
            }
            double hyperIVPercent = (100* hyperIVTotal/186.0);
            builder.append("\n%3$s")
                    .append(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.ui.pokesprite.lore.hypertrainedivslabel"))
                    .append(" %1$s:   %3$s").append(hyperIVTotal).append("%1$s/%3$s186 %1$s- %3$s").append(f.format(hyperIVPercent)).append("%1$s%%");
        }

        builder.append("\n   %2$s");
        for(int i = 0; i < statsLength; i++) {
            StatsType type = types[i];

            builder.append(ivs.get(type));
            if(ivs.isHyperTrained(type)){
                builder.append("%3$s+").append(31-ivs.get(type));
            }
            builder.append(" %2$s").append(getShortStatName(type));

            if(i == 2){
                builder.append("\n   ");
            }
            else if(i != 5){
                builder.append(" %1$s/ %2$s");
            }
        }

//                builder.append(ivs.hp).append( "HP").append("%1$s/%2$s")
//                .append(ivs.attack).append(" Atk").append("%1$s/%2$s")
//                .append(ivs.defence).append(" Def")
//                .append("\n")
//                .append(ivs.specialAttack).append(" SpAtk").append("%1$s/%2$s")
//                .append(ivs.specialDefence).append(" SpDef").append("%1$s/%2$s")
//                .append(ivs.speed).append(" Speed");
        return String.format(builder.toString(), Config.primaryColor, Config.secondaryColor, Config.highlightColor);
    }

    public static String getShortStatName(StatsType type){
        switch (type){
            case HP:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.hp").toString();
            case Attack:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.attack").toString();
            case Defence:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.defence").toString();
            case SpecialAttack:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.specialattack").toString();
            case SpecialDefence:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.specialdefence").toString();
            case Speed:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.speed").toString();
            case Accuracy:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.accuracy").toString();
            case Evasion:
                return WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.stats.short.evasion").toString();
            case None:
                return "None";
            default:
                return "";
        }
    }

    public static String resolvePokeGenderString(Pokemon pokemon){
        if(pokemon.getGender().name().equals("Male")){
            return "&b" + pokemon.getGender().getTranslatedName().getUnformattedComponentText() + " ♂";
        }
        if(pokemon.getGender().name().equals("Female")){
            return "&d" + pokemon.getGender().getTranslatedName().getUnformattedComponentText() + " ♀";
        }
        else{
            return "&7Genderless ø";
        }
    }

    public static String resolvePokeNatureString(EnumNature nature){
        if(nature == EnumNature.Hardy || nature == EnumNature.Serious || nature == EnumNature.Docile || nature == EnumNature.Bashful || nature == EnumNature.Quirky){
            return nature.getTranslatedName().getUnformattedComponentText() + " &7(" + WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.utils.nature.boost.neutral").toString() + ")";
        }
        else{
            return nature.getTranslatedName().getUnformattedComponentText() + " &7(&a+ " + nature.increasedStat.getTranslatedName().getUnformattedComponentText() + " &7/ &c- " + nature.decreasedStat.getTranslatedName().getUnformattedComponentText() + "&7)";
        }
    }

    public static String resolvePokeHiddenPowerString(Pokemon pokemon){
        EnumType type = HiddenPower.getHiddenPowerType(pokemon.getIVs());
        switch (type){
            case Normal:
                return "&f" + type.getLocalizedName();
            case Fire:
                return "&c" + type.getLocalizedName();
            case Water:
                return "&9" + type.getLocalizedName();
            case Electric:
                return "&e" + type.getLocalizedName();
            case Grass:
                return "&2" + type.getLocalizedName();
            case Ice:
                return "&b" + type.getLocalizedName();
            case Fighting:
                return "&6" + type.getLocalizedName();
            case Poison:
                return "&5" + type.getLocalizedName();
            case Ground:
                return "&6" + type.getLocalizedName();
            case Flying:
                return "&b" + type.getLocalizedName();
            case Psychic:
                return "&d" + type.getLocalizedName();
            case Bug:
                return "&a" + type.getLocalizedName();
            case Rock:
                return "&6" + type.getLocalizedName();
            case Ghost:
                return "&5" + type.getLocalizedName();
            case Dragon:
                return "&9" + type.getLocalizedName();
            case Dark:
                return "&8" + type.getLocalizedName();
            case Steel:
                return "&7" + type.getLocalizedName();
            case Fairy:
                return "&d" + type.getLocalizedName();
            case Mystery:
                return "&f" + type.getLocalizedName();
            default:
                return "ERROR - " + type.getLocalizedName();
        }
    }

    public static DyeColor resolveDyeFromAmpersand(String ampersand){
        switch (ampersand){
            case "&0":
                return DyeColors.BLACK;
            case "&1":
                return DyeColors.BLUE;
            case "&2":
                return DyeColors.GREEN;
            case "&3":
                return DyeColors.CYAN;
            case "&4":
                return DyeColors.RED;
            case "&5":
                return DyeColors.PURPLE;
            case "&6":
                return DyeColors.ORANGE;
            case "&7":
                return DyeColors.SILVER;
            case "&8":
                return DyeColors.GRAY;
            case "&9":
                return DyeColors.BLUE;
            case "&a":
                return DyeColors.LIME;
            case "&b":
                return DyeColors.LIGHT_BLUE;
            case "&c":
                return DyeColors.RED;
            case "&d":
                return DyeColors.PINK;
            case "&e":
                return DyeColors.YELLOW;
            case "&f":
                return DyeColors.WHITE;
            default:
                return DyeColors.WHITE;
        }
    }

    public static EnumSpecial getPokemonSpecialTexture(Pokemon pokemon){
        List<EnumSpecial> enumSpecialList = Arrays.stream(EnumSpecial.values()).filter(enumSpecial -> pokemon.getForm() == enumSpecial.getForm()).collect(Collectors.toList());
        if(enumSpecialList.size() > 0){
            return (enumSpecialList.get(0));
        }
        return EnumSpecial.Base;
    }

    private static boolean broadcastCheck(Pokemon pokemon){
        return pokemon.isShiny() && Config.broadcastTypes.contains(BroadcastTypes.SHINY) ||
                EnumSpecies.ultrabeasts.contains(pokemon.getSpecies().name) && Config.broadcastTypes.contains(BroadcastTypes.ULTRABEAST) ||
                pokemon.isLegendary() && Config.broadcastTypes.contains(BroadcastTypes.LEGENDARY) ||
                pokemon.getForm() >= 100 && Config.broadcastTypes.contains(BroadcastTypes.SPECIALTEXTURE) ||
                !pokemon.getCustomTexture().isEmpty() && Config.broadcastTypes.contains(BroadcastTypes.CUSTOMTEXTURE) ||
                pokemon.getPersistentData().hasKey("entity-particles:particle") && Config.broadcastTypes.contains(BroadcastTypes.AURA)
                ;
    }

    public static Task createCooldownTaskForPlayer(Player player, long time){
        return Task.builder()
                .execute(t -> {
                    playerCooldownTasks.remove(player.getUniqueId());
                    //create event
                    EventContext eventContext = EventContext.builder().add(EventContextKeys.PLUGIN, WonderTrade.getContainer()).add(EventContextKeys.PLAYER, player).build();
                    WonderTradeCooldownEvent event = new WonderTradeCooldownEvent(player, Cause.of(eventContext,  WonderTrade.getContainer()));
                    //push event
                    Sponge.getEventManager().post(event);
                    if(player.isOnline() && Config.notifyCooldowns){
                        player.sendMessage(WonderTrade.getMessage(player, "wondertrade.trade.cooldown.expired"));
                    }
                })
                .delay(time, TimeUnit.MILLISECONDS)
                .submit(WonderTrade.getContainer());
    }

    public static String capitalizeFirstLetter(String string){
        return string.substring(0,1).toUpperCase() + string.substring(1);
    }
}