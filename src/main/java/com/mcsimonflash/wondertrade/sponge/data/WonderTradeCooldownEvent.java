package com.mcsimonflash.wondertrade.sponge.data;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;

public class WonderTradeCooldownEvent extends AbstractEvent implements TargetPlayerEvent {

    private final Cause cause;
    private final Player player;

    public WonderTradeCooldownEvent(Player player, Cause cause) {
        this.player = player;
        this.cause = cause;
    }

    @Override
    public Player getTargetEntity() {
        return this.player;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    public Player getPlayer() {
        return player;
    }
}
