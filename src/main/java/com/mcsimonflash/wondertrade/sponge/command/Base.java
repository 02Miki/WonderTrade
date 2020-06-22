package com.mcsimonflash.wondertrade.sponge.command;

import com.mcsimonflash.wondertrade.sponge.WonderTrade;
import com.google.inject.Inject;
import com.mcsimonflash.sponge.teslalibs.command.Aliases;
import com.mcsimonflash.sponge.teslalibs.command.Children;
import com.mcsimonflash.sponge.teslalibs.command.Command;
import com.mcsimonflash.sponge.teslalibs.command.Permission;
import com.mcsimonflash.wondertrade.sponge.internal.Utils;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Aliases({"wondertrade", "wtrade"})
@Permission("wondertrade.command.base")
@Children({Menu.class, Pool.class, Regen.class, Take.class, Trade.class})
public class Base extends Command {

    private static final Text LINKS = Text.of("                                      ", CmdUtils.link("View Plugin Wiki", Utils.parseURL("https://github.com/FriendlyRainbowAnimal/WonderTrade/wiki")));

    @Inject
    protected Base(Settings settings) {
        super(settings.usage(CmdUtils.usage("/wondertrade", "The base command for WonderTrade.")));
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        PaginationList.builder()
                .title(WonderTrade.getPrefix())
                .padding(Utils.toText("&7="))
                .contents(Stream.concat(Stream.of(getUsage()), ((Command) this).getChildren().stream().filter(c -> c.getSpec().testPermission(src)).map(Command::getUsage)).collect(Collectors.toList()))
                .footer(LINKS)
                .sendTo(src);
        return CommandResult.success();
    }

}