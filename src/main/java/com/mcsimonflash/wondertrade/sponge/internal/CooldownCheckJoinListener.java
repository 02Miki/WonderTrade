package com.mcsimonflash.wondertrade.sponge.internal;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;

public class CooldownCheckJoinListener {

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @First Player player){
        long time = Utils.getCooldown(player) - (System.currentTimeMillis() - Config.getCooldown(player.getUniqueId()));
        if(time > 0){
            Task cooldownTask = Utils.createCooldownTaskForPlayer(player, time);
            Utils.playerCooldownTasks.put(player.getUniqueId(), cooldownTask);
        }
    }
}
