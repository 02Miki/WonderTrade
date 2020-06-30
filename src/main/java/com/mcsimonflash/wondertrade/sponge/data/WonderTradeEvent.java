package com.mcsimonflash.wondertrade.sponge.data;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

public class WonderTradeEvent extends AbstractEvent implements TargetPlayerEvent, Cancellable {

    private final Cause cause;
    private final Player player;
    private final TradeEntry inputEntry;
    private final TradeEntry outputEntry;

    private boolean cancelled = false;

    public WonderTradeEvent(Player player, TradeEntry input, TradeEntry output, Cause cause) {
        this.player = player;
        this.inputEntry = input;
        this.outputEntry = output;
        this.cause = cause;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public Player getTargetEntity() {
        return this.player;
    }

    public Player getPlayer() {
        return player;
    }

    public TradeEntry getInputEntry() {
        return inputEntry;
    }

    public TradeEntry getOutputEntry() {
        return outputEntry;
    }
}
