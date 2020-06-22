package com.mcsimonflash.wondertrade.sponge.internal;

import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.mcsimonflash.wondertrade.sponge.data.TradeEntry;
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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.translation.locale.Locales;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Utils {

    private static World world;
    private static Task announcementTask;
    private static Task configSaveTask;
    public static final UUID ZERO_UUID = new UUID(0, 0);

    public static final Pattern MESSAGE = Pattern.compile("\\[(.+?)]\\{((?:.|\\n)+?)}(?=.)");
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
            if (matcher.hitEnd() && index < message.length()) {
                builder.append(toText(message.substring(index)));
            }
        }
        if (index == 0) {
            builder.append(toText(message));
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
            storage.set(slot,null);
            Pixelmon.EVENT_BUS.post(new PixelmonDeletedEvent((EntityPlayerMP) player, pokemon, DeleteType.COMMAND));
            storage.set(slot, entry.getPokemon());
            Pixelmon.EVENT_BUS.post(new PixelmonReceivedEvent((EntityPlayerMP) player, ReceiveType.Command, entry.getPokemon()));
        }
    }

    public static void trade(Player player, int box, int position) {
        PCStorage storage = getPCStorage(player);
        Pokemon pokemon = storage.getBox(box).get(position);

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
            storage.getBox(box).set(position, null);
            Pixelmon.EVENT_BUS.post(new PixelmonDeletedEvent((EntityPlayerMP) player, pokemon, DeleteType.COMMAND));
            storage.getBox(box).set(position, entry.getPokemon());
            Pixelmon.EVENT_BUS.post(new PixelmonReceivedEvent((EntityPlayerMP) player, ReceiveType.Command, entry.getPokemon()));
        }
    }

    private static TradeEntry trade(Player player, Pokemon pokemon) {
        //If incoming pokemon doesn't have the undexable tag, but the config is set to mark new trades as undexable... add it!
        if(Config.undexablePlayerTrades && !pokemon.hasSpecFlag("undexable")){
            pokemon.addSpecFlag("undexable");
        }
        TradeEntry entry = new TradeEntry(pokemon, player.getUniqueId(), LocalDateTime.now());
        logTransaction(player, entry, true);
        entry = Manager.trade(entry).refine(player);
        logTransaction(player, entry, false);
        Object[] args = new Object[] {"player", player.getName(), "traded", getShortDesc(pokemon), "traded-details", getDesc(pokemon), "received", getShortDesc(entry.getPokemon()), "received-details", getDesc(entry.getPokemon())};
        if (Config.broadcastTrades && (announcementCheck(pokemon) || announcementCheck(entry.getPokemon()))) {
            //Player Input Pokemon
            if(announcementCheck(pokemon) && !announcementCheck(entry.getPokemon())){
                Sponge.getServer().getBroadcastChannel().send(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.broadcast.input", args).toString())));
            }
            //Returned Pokemon
            if(announcementCheck(entry.getPokemon()) && !announcementCheck(pokemon)){
                Sponge.getServer().getBroadcastChannel().send(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.broadcast.received", args).toString())));
            }
            //Pog both were speical
            if(announcementCheck(entry.getPokemon()) && announcementCheck(pokemon)){
                Sponge.getServer().getBroadcastChannel().send(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.broadcast.doublespecial", args).toString())));
            }

        }
        player.sendMessage(WonderTrade.getPrefix().concat(parseText(WonderTrade.getMessage(Locales.DEFAULT, "wondertrade.trade.success.message", args).toString())));
        return entry;
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
        return pokemon.isEgg() ? "Mysterious Egg" : "Level " + pokemon.getLevel() + (pokemon.isShiny() ? " Shiny " : " ") + (EnumSpecies.legendaries.contains(pokemon.getSpecies().name) ? "Legendary " : "") + pokemon.getSpecies().name;
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
            return String.format("%1$sPokemon : %2$sMysterious Egg", Config.primaryColor, Config.secondaryColor);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("%1$sPokemon : ");
        if (pokemon.isShiny()) {
            builder.append("%3$sShiny ");
        }
        if (EnumSpecies.legendaries.contains(pokemon.getSpecies().name)) {
            builder.append("%3$sLegendary ");
        }
        builder.append("%2$s")
                .append(pokemon.getSpecies().name).append(" \n")
                .append(" %1$s- Level : %2$s").append(pokemon.getLevel()).append(" \n")
                .append(" %1$s- OriginalTrainer : %2$s").append(pokemon.getOriginalTrainer()).append(" \n")
                .append(" %1$s- Ability : %2$s").append(pokemon.getAbility().getName());
        if(pokemon.getAbilitySlot() == 2){
            builder.append(" %2$s(%3$sHA%2$s)");
        }
        builder.append(" \n");
        builder.append(" %1$s- Nature : %2$s").append(resolvePokeNatureString(pokemon.getNature())).append("\n");
        if(pokemon.getMintNature() != null){
            builder.append(" %3$s- MintNature : %3$s").append(resolvePokeNatureString(pokemon.getMintNature())).append("\n");
        }
        builder.append(" %1$s- HiddenPower : %2$s").append(resolvePokeHiddenPowerString(pokemon)).append("\n");
        builder.append(" %1$s- Growth : %2$s").append(pokemon.getGrowth().name()).append("\n");
        builder.append(" %1$s- Gender : %2$s").append(resolvePokeGenderString(pokemon)).append("\n");
        if (pokemon.getHeldItem() != net.minecraft.item.ItemStack.EMPTY) {
            builder.append(" %1$s- Held Item : %2$s").append(pokemon.getHeldItem().getDisplayName()).append("\n");
        }
        builder.append(" %1$s- Poke Ball : %2$s").append(pokemon.getCaughtBall().getLocalizedName()).append("\n");
        if (pokemon.getForm() >= 100) {
            builder.append(" %3$s- Special Texture : ").append(getPokemonSpecialTexture(pokemon).name()).append("\n");
        }
        if (!pokemon.getCustomTexture().isEmpty() && pokemon.getCustomTexture() != null ){
            builder.append(" %3$s- Custom Texture : ").append(pokemon.getCustomTexture()).append("\n");
        }


        builder.append(" %1$s- Friendship : %2$s").append(pokemon.getFriendship()).append("\n").append("\n")
                //EVS
                .append("%1$sEVs %1$s:   %2$s").append(EVTotal).append("%1$s/%2$s510 %1$s- %2$s").append(f.format(EVPercent)).append("%1$s%%")
                .append("\n   %2$s")
                .append(evs.hp).append(" HP").append(" %1$s/ %2$s")
                .append(evs.attack).append(" Atk").append(" %1$s/ %2$s")
                .append(evs.defence).append(" Def")
                .append("\n   ")
                .append(evs.specialAttack).append(" SpAtk").append(" %1$s/ %2$s")
                .append(evs.specialDefence).append(" SpDef").append(" %1$s/ %2$s")
                .append(evs.speed).append(" Speed")
                .append("\n")
                //IVS
                .append("\n%1$sIVs %1$s:   %2$s").append(IVTotal).append("%1$s/%2$s186 %1$s- %2$s").append(f.format(IVPercent)).append("%1$s%%");
        if(ivs.isHyperTrained(StatsType.HP) || ivs.isHyperTrained(StatsType.Attack) || ivs.isHyperTrained(StatsType.Defence) || ivs.isHyperTrained(StatsType.SpecialAttack) || ivs.isHyperTrained(StatsType.SpecialDefence) || ivs.isHyperTrained(StatsType.Speed)){
            int hyperIVTotal = 0;
            StatsType[] types = StatsType.getStatValues();
            int statsLength = types.length;

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
            builder.append("\n%3$sHyperTrained IVs %1$s:   %3$s").append(hyperIVTotal).append("%1$s/%3$s186 %1$s- %3$s").append(f.format(hyperIVPercent)).append("%1$s%%");
        }

        builder.append("\n   %2$s");
        StatsType[] types = StatsType.getStatValues();
        int statsLength = types.length;
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
                return "HP";
            case Attack:
                return "Atk";
            case Defence:
                return "Def";
            case SpecialAttack:
                return "SpAtk";
            case SpecialDefence:
                return "SpDef";
            case Speed:
                return "Speed";
            case Accuracy:
                return "Acc";
            case Evasion:
                return "Eva";
            case None:
                return "None";
            default:
                return "";
        }
    }

    public static String resolvePokeGenderString(Pokemon pokemon){
        if(pokemon.getGender().name().equals("Male")){
            return "&bMale ♂";
        }
        if(pokemon.getGender().name().equals("Female")){
            return "&dFemale ♀";
        }
        else{
            return "&7Genderless ø";
        }
    }

    public static String resolvePokeNatureString(EnumNature nature){
        if(nature == EnumNature.Hardy || nature == EnumNature.Serious || nature == EnumNature.Docile || nature == EnumNature.Bashful || nature == EnumNature.Quirky){
            return nature.getLocalizedName() + " &7(Neutral)";
        }
        else{
            return nature.getLocalizedName() + " &7(&a+ " + nature.increasedStat.getLocalizedName() + " &7/ &c- " + nature.decreasedStat.getLocalizedName() + "&7)";
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

    private static boolean announcementCheck(Pokemon pokemon){
        return pokemon.isShiny() || EnumSpecies.ultrabeasts.contains(pokemon.getSpecies().name) || pokemon.isLegendary();
    }
}