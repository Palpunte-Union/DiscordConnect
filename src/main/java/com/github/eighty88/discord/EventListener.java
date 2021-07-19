package com.github.eighty88.discord;


import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class EventListener implements Listener {
    public String keyMessage = "あなたは認証されていません。\n以下のコマンドをDiscordで入力してください。\n!register %key%";

    public DiscordConnect discordConnect;

    public EventListener(DiscordConnect discordConnect) {
        this.discordConnect = discordConnect;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        if(!discordConnect.PlayerHash.containsKey(e.getUniqueId())) {
            String key;
            if(discordConnect.RegisterHash.containsKey(e.getUniqueId())) {
                key = discordConnect.RegisterHash.get(e.getUniqueId());
            } else {
                do {
                    key = RandomStringUtils.randomAlphanumeric(7);
                } while (discordConnect.RegisterHash.containsValue(key));
                discordConnect.RegisterHash.put(e.getUniqueId(), key);
            }
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    keyMessage.replace("%key%", key));
        }
    }
}
