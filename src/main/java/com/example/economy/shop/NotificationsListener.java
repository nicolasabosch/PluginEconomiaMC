package com.example.economy.shop;

import java.sql.SQLException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NotificationsListener implements Listener {
    private final MarketplaceService marketplaceService;

    public NotificationsListener(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            for (String msg : marketplaceService.pullNotifications(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage(msg);
            }
            int returned = marketplaceService.deliverPendingReturns(event.getPlayer());
            if (returned > 0) {
                event.getPlayer().sendMessage("Returned " + returned + " marketplace item(s) to your inventory.");
            }
        } catch (SQLException ignored) {
        }
    }
}
