package com.example.economy.jobs;

import java.sql.SQLException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class JobsListener implements Listener {
    private final JobsService jobsService;

    public JobsListener(JobsService jobsService) {
        this.jobsService = jobsService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            jobsService.rewardBlock(event.getPlayer().getUniqueId(), event.getBlock().getType());
        } catch (SQLException ignored) {
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }
        try {
            jobsService.rewardMob(killer.getUniqueId(), event.getEntityType());
        } catch (SQLException ignored) {
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        try {
            jobsService.rewardFish(event.getPlayer().getUniqueId());
        } catch (SQLException ignored) {
        }
    }
}
