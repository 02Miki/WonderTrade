package com.mcsimonflash.wondertrade.sponge.internal;

import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.google.common.collect.Lists;
import com.mcsimonflash.sponge.teslalibs.inventory.Action;
import com.mcsimonflash.sponge.teslalibs.inventory.Element;
import com.mcsimonflash.sponge.teslalibs.inventory.Layout;
import com.mcsimonflash.sponge.teslalibs.inventory.Page;
import com.mcsimonflash.sponge.teslalibs.inventory.View;
import com.mcsimonflash.wondertrade.sponge.data.TradeEntry;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.client.gui.GuiResources;
import com.pixelmonmod.pixelmon.config.PixelmonItems;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Inventory {
    private static Element PRIMARY_BACKGROUND = Element.of(ItemStack.builder()
            .itemType(ItemTypes.STAINED_GLASS_PANE)
            .add(Keys.DISPLAY_NAME, Text.EMPTY)
            .add(Keys.DYE_COLOR, Utils.resolveDyeFromAmpersand(Config.primaryColor))
            .build());
    private static Element SECONDARY_BACKGROUND = Element.of(ItemStack.builder()
            .itemType(ItemTypes.STAINED_GLASS_PANE)
            .add(Keys.DISPLAY_NAME, Text.EMPTY)
            .add(Keys.DYE_COLOR, Utils.resolveDyeFromAmpersand(Config.secondaryColor))
            .build());
    private static Element CLOSE = Element.of(createItem(ItemTypes.BARRIER, "&4Close", "&cClose The Menu"), inTask(a -> a.getPlayer().closeInventory()));
    private static Element MENU = Element.of(createItem((ItemType) PixelmonItems.tradeMonitor, Config.primaryColor + "Menu", Config.secondaryColor + "Return To The Main Menu"), inTask(a -> createMainMenu(a.getPlayer()).open(a.getPlayer())));
    private static Layout MAIN = Layout.builder()
            .set(PRIMARY_BACKGROUND, 0, 2, 4, 6, 8, 18, 20, 22, 24, 26)
            .set(SECONDARY_BACKGROUND, 1, 3, 5, 7, 9, 17, 19, 21, 23, 25)
            .build();
    private static Layout PAGE = Layout.builder()
            .set(PRIMARY_BACKGROUND, 0, 2, 4, 6, 8, 18, 26, 36, 38, 40, 42, 44)
            .set(SECONDARY_BACKGROUND, 1, 3, 5, 7, 9, 17, 27, 35, 37, 39, 41, 43, 45, 53)
            .set(MENU, 46)
            .set(Page.FIRST, 47)
            .set(Page.PREVIOUS, 48)
            .set(Page.CURRENT, 49)
            .set(Page.NEXT, 50)
            .set(Page.LAST, 51)
            .set(CLOSE, 52)
            .build();
    private static Layout PC = Layout.builder()
            .set(PRIMARY_BACKGROUND, 0, 6, 8, 18, 24, 26, 36, 42, 44)
            .set(SECONDARY_BACKGROUND, 9, 15, 17, 27, 33, 35, 45, 51, 53)
            .set(Page.FIRST, 7)
            .set(Page.PREVIOUS, 16)
            .set(Page.CURRENT, 25)
            .set(Page.NEXT, 34)
            .set(Page.LAST, 43)
            .set(MENU, 52)
            .build();

    private static Consumer<Action.Click> inTask(Consumer<Action.Click> consumer) {
        return a -> Task.builder().execute(t -> consumer.accept(a)).submit(WonderTrade.getContainer());
    }

    private static View createView(InventoryArchetype archetype, String name, Layout layout) {
        return View.builder()
                .archetype(archetype)
                .property(InventoryTitle.of(Utils.toText(name)))
                .build(WonderTrade.getContainer())
                .define(layout);
    }

    public static View createMainMenu(Player player) {
        PartyStorage storage = Utils.getPartyStorage(player);
        Element[] party = new Element[7];
        for (int i = 0; i < 6; i++) {
            int slot = i;
            party[i] = createPokemonElement(player, storage.getAll()[i], "Slot " + (i + 1), inTask(a -> createTradeMenu(player, slot).open(player)));
        }
        party[6] = Element.of(createItem(Sponge.getRegistry().getType(ItemType.class, "pixelmon:pc").get(), Config.primaryColor + "PC (Box " + (Utils.getPCStorage(player).getLastBox() + 1) + ")", Config.secondaryColor + "Click to view your PC"), inTask(a -> createPCMenu(player, -1).open(player)));
        return createView(InventoryArchetypes.CHEST, Config.primaryColor + "WonderTrade", Layout.builder()
                .from(MAIN)
                .page(Arrays.asList(party))
                .build());
    }


    public static View createPCMenu(Player player, int boxNum) {
        PCStorage storage = Utils.getPCStorage(player);
        int box = boxNum != -1 ? boxNum : storage.getLastBox();
        Element[] pc = new Element[30];
        for (int i = 0; i < 30; i++) {
            int pos = i;
            pc[i] = createPokemonElement(player, storage.getBox(box).get(i), "Position " + (i + 1), inTask(a -> createTradeMenu(player, box, pos).open(player)));
        }
        return createView(InventoryArchetypes.DOUBLE_CHEST, Config.primaryColor + "WonderTrade PC", Layout.builder()
                .from(PC)
                .replace(Page.FIRST, createPageElement(Config.primaryColor + "First Box ", box, 0))
                .replace(Page.LAST, createPageElement(Config.primaryColor + "Last Box ", box, storage.getBoxes().length - 1))
                .replace(Page.NEXT, createPageElement(Config.primaryColor + "Next Box ", box, box == storage.getBoxes().length - 1 ? box : box + 1))
                .replace(Page.PREVIOUS, createPageElement(Config.primaryColor + "Previous Box", box, box == 0 ? box : box - 1))
                .replace(Page.CURRENT, createPageElement(Config.primaryColor + "Current Box", box, box))
                .page(Arrays.asList(pc))
                .build());
    }


    private static Element createPokemonElement(Player player, @Nullable Pokemon pokemon, String name, Consumer<Action.Click> action) {
        if (pokemon != null) {
            if(pokemon.isEgg() && !(Config.allowEggs || player.hasPermission("wondertrade.trade.eggbypass")) ){
                //If config is false and pokemon is an egg, otherwise check proceeds downwards
                ItemStack item = createPokemonItem(Config.primaryColor + name, pokemon);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-eggs").toText()));
                return Element.of(item);
            }
            if(pokemon.hasSpecFlag("untradeable") && !(Config.allowUntradable || player.hasPermission("wondertrade.trade.untradeablebypass")) ){
                //If config is false and pokemon is untradable, otherwise check proceeds downwards
                ItemStack item = createPokemonItem(Config.primaryColor + name, pokemon);
                item.offer(Keys.ITEM_LORE, Lists.newArrayList(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-untradeables").toText()));
                return Element.of(item);
            }
            return Element.of(createPokemonItem(Config.primaryColor + name, pokemon), action);

//            if (Config.allowEggs || !pokemon.isEgg()) {
//                //If isn't egg, successful regardless of config
//                //If is egg, requires config to allow else fails
//                return Element.of(createPokemonItem("&3" + name, poke), action);
//
//
//            } else {
//                ItemStack item = createPokemonItem("&3" + name, poke);
//                item.offer(Keys.ITEM_LORE, Lists.newArrayList(WonderTrade.getMessage(player.getLocale(), "wondertrade.trade.no-eggs").toText()));
//                return Element.of(item);
//            }
        } else {
            return Element.of(createItem(ItemTypes.BARRIER, "&4Empty!", "&cNo Pokemon Found In " + name));
        }
    }


    private static Element createPageElement(String name, int page, int target) {
        ItemStack item = createItem(page == target ? ItemTypes.MAP : ItemTypes.PAPER, name + " (" + (target + 1) + ")", "");
        return page == target ? Element.of(item) : Element.of(item, inTask(a -> createPCMenu(a.getPlayer(), target).open(a.getPlayer())));
    }

    public static View createTradeMenu(Player player, int slot) {
        PartyStorage storage = Utils.getPartyStorage(player);
        return createTradeMenu(player, storage.getAll()[slot], Config.primaryColor + "Slot " + (slot + 1), a -> Utils.trade(player, slot));
    }

    public static View createTradeMenu(Player player, int box, int slot) {
        PCStorage storage = Utils.getPCStorage(player);
        return createTradeMenu(player, storage.get(box, slot), "Box " + (box + 1) + ", Position " + (slot + 1), a -> Utils.trade(player, box, slot));
    }


    public static View createTradeMenu(Player player, Pokemon poke, String name, Consumer<Action.Click> action) {
        AtomicReference<Task> task = new AtomicReference<>(null);
        View view = View.builder()
                .archetype(InventoryArchetypes.CHEST)
                .property(InventoryTitle.of(Utils.toText(Config.primaryColor + "Are You Sure?")))
                .onClose(a -> {
                    if (task.get() != null) task.get().cancel();
                })
                .build(WonderTrade.getContainer());
        Element confirm;
        if (Config.defCooldown > 0 && !player.hasPermission("wondertrade.trade.cooldownbypass")) {
            long time = Utils.getCooldown(player) - (System.currentTimeMillis() - Config.getCooldown(player.getUniqueId()));
            Consumer<Action.Click> act = inTask(a -> {
                if (Config.resetCooldown(player.getUniqueId())) {
                    action.accept(a);
                    player.closeInventory();
                } else {
                    player.sendMessage(WonderTrade.getMessage(player, "wondertrade.trade.reset-cooldown.failure"));
                }
            });
            if (time > 0) {
                confirm = Element.of(createItem(Sponge.getRegistry().getType(ItemType.class, "pixelmon:hourglass_silver").get(), "&cCooldown", "&4You must wait " + (time / 1000) + " seconds."));
                AtomicLong remaining = new AtomicLong(time / 1000);
                task.set(Task.builder()
                        .execute(t -> view.setElement(10, remaining.get() <= 0 ? Element.of(createItem(ItemTypes.SLIME_BALL, "&aConfirm", "&2WonderTrade your " + Utils.getShortDesc(poke)), act) :
                                Element.of(createItem(Sponge.getRegistry().getType(ItemType.class, "pixelmon:hourglass_silver").get(), "&4Cooldown", "&cYou must wait " + remaining.getAndDecrement() + " seconds."))))
                        .interval(1, TimeUnit.SECONDS)
                        .submit(WonderTrade.getContainer()));
            } else {
                confirm = Element.of(createItem(ItemTypes.SLIME_BALL, "&2Confirm", "&aWonderTrade your " + Utils.getShortDesc(poke)), act);
            }
        } else {
            confirm = Element.of(createItem(ItemTypes.SLIME_BALL, "&2Confirm", "&aWonderTrade your " + Utils.getShortDesc(poke)), inTask(a -> {
                action.accept(a);
                player.closeInventory();
                //createMainMenu(player).open(player);
            }));
        }
        return view.define(Layout.builder()
                .from(MAIN)
                .set(PRIMARY_BACKGROUND, 12, 14)
                .set(SECONDARY_BACKGROUND, 11, 15)
                .set(Element.of(createPokemonItem(name, poke)), 13)
                .set(confirm, 10)
                .set(Element.of(createItem(ItemTypes.MAGMA_CREAM, "&4Cancel", "&cCancel This Trade"), inTask(a -> createMainMenu(player).open(player))), 16)
                .build());
    }

    public static Page createPoolMenu(boolean take) {
        Page page = Page.builder()
                .archetype(InventoryArchetypes.DOUBLE_CHEST)
                .property(InventoryTitle.of(Utils.toText(Config.primaryColor + "WonderTrade Pool")))
                .layout(PAGE)
                .build(WonderTrade.getContainer());
        Element[] pool = new Element[Config.poolSize];
        for (int i = 0; i < Config.poolSize; i++) {
            int index = i;
            pool[i] = take ? Element.of(createPokemonItem(Config.primaryColor + "Position " + (i + 1), Manager.trades[index]), inTask(a -> {
                Utils.take(a.getPlayer(), index);
                createPoolMenu(take).open(a.getPlayer(), index / 21);
            })) : Element.of(createPokemonItem(Config.primaryColor + "Position " + (i + 1), Manager.trades[index]));
        }
        return page.define(Arrays.asList(pool));
    }

    private static ItemStack createItem(ItemType type, String name, String lore) {
        return ItemStack.builder()
                .itemType(type)
                .add(Keys.DISPLAY_NAME, Utils.toText(name))
                .add(Keys.ITEM_LORE, lore.isEmpty() ? Lists.newArrayList() : Lists.newArrayList(Utils.toText(lore)))
                .build();
    }

    private static ItemStack createPokemonItem(String name, TradeEntry entry) {
        return ItemStack.builder().fromContainer(createItem((ItemType) PixelmonItems.itemPixelmonSprite, name, Utils.getDesc(entry.getPokemon())).toContainer()
                .set(DataQuery.of("UnsafeData", "SpriteName"), getSpriteName(entry.getPokemon())))
                .build();
    }

    private static ItemStack createPokemonItem(String name, Pokemon pokemon) {
        return ItemStack.builder().fromContainer(createItem((ItemType) PixelmonItems.itemPixelmonSprite, name, Utils.getDesc(pokemon)).toContainer()
                .set(DataQuery.of("UnsafeData", "SpriteName"), getSpriteName(pokemon)))
                .build();
    }

    private static String getSpriteName(Pokemon pokemon) {
        if (pokemon.isEgg()) {
            return "pixelmon:" + getEggSpritePathForNBT(pokemon.getSpecies(), pokemon.getEggCycles());
        } else {
            return "pixelmon:" + GuiResources.getSpritePath(pokemon.getSpecies(), pokemon.getForm(), pokemon.getGender(), pokemon.getCustomTexture(), pokemon.isShiny());
        }
    }

    public static String getEggSpritePathForNBT(EnumSpecies species, int eggCycles) {
        if (species == EnumSpecies.Togepi) {
            return eggCycles > 10 ? "sprites/eggs/togepi1" : (eggCycles > 5 ? "sprites/eggs/togepi2" : "sprites/eggs/togepi3");
        } else if (species == EnumSpecies.Manaphy) {
            return eggCycles > 10 ? "sprites/eggs/manaphy1" : (eggCycles > 5 ? "sprites/eggs/manaphy2" : "sprites/eggs/manaphy3");
        } else {
            return eggCycles > 10 ? "sprites/eggs/egg1" : (eggCycles > 5 ? "sprites/eggs/egg2" : "sprites/eggs/egg3");
        }
    }

    public static void reloadMenuElements(){
        PRIMARY_BACKGROUND = Element.of(ItemStack.builder()
                .itemType(ItemTypes.STAINED_GLASS_PANE)
                .add(Keys.DISPLAY_NAME, Text.EMPTY)
                .add(Keys.DYE_COLOR, Utils.resolveDyeFromAmpersand(Config.primaryColor))
                .build());
        SECONDARY_BACKGROUND = Element.of(ItemStack.builder()
                .itemType(ItemTypes.STAINED_GLASS_PANE)
                .add(Keys.DISPLAY_NAME, Text.EMPTY)
                .add(Keys.DYE_COLOR, Utils.resolveDyeFromAmpersand(Config.secondaryColor))
                .build());
        CLOSE = Element.of(createItem(ItemTypes.BARRIER, "&4Close", "&cClose The Menu"), inTask(a -> a.getPlayer().closeInventory()));
        MENU = Element.of(createItem((ItemType) PixelmonItems.tradeMonitor, Config.primaryColor + "Menu", Config.secondaryColor + "Return To The Main Menu"), inTask(a -> createMainMenu(a.getPlayer()).open(a.getPlayer())));
        MAIN = Layout.builder()
                .set(PRIMARY_BACKGROUND, 0, 2, 4, 6, 8, 18, 20, 22, 24, 26)
                .set(SECONDARY_BACKGROUND, 1, 3, 5, 7, 9, 17, 19, 21, 23, 25)
                .build();
        PAGE = Layout.builder()
                .set(PRIMARY_BACKGROUND, 0, 2, 4, 6, 8, 18, 26, 36, 38, 40, 42, 44)
                .set(SECONDARY_BACKGROUND, 1, 3, 5, 7, 9, 17, 27, 35, 37, 39, 41, 43, 45, 53)
                .set(MENU, 46)
                .set(Page.FIRST, 47)
                .set(Page.PREVIOUS, 48)
                .set(Page.CURRENT, 49)
                .set(Page.NEXT, 50)
                .set(Page.LAST, 51)
                .set(CLOSE, 52)
                .build();
        PC = Layout.builder()
                .set(PRIMARY_BACKGROUND, 0, 6, 8, 18, 24, 26, 36, 42, 44)
                .set(SECONDARY_BACKGROUND, 9, 15, 17, 27, 33, 35, 45, 51, 53)
                .set(Page.FIRST, 7)
                .set(Page.PREVIOUS, 16)
                .set(Page.CURRENT, 25)
                .set(Page.NEXT, 34)
                .set(Page.LAST, 43)
                .set(MENU, 52)
                .build();
    }
}