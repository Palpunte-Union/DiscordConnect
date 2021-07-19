package com.github.eighty88.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;

public final class DiscordConnect extends JavaPlugin {

    public static DiscordConnect instance;

    public String Token;

    public HashMap<UUID, User> PlayerHash = new HashMap<>();

    public HashMap<UUID, String> RegisterHash = new HashMap<>();

    DiscordClient client;

    GatewayDiscordClient gateway;

    String dbPath = getDataFolder() + File.separator + "players.db";

    @Override
    public void onEnable() {

        instance = this;

        Bukkit.getPluginManager().registerEvents(new EventListener(this), this);

        saveDefaultConfig();
        String confFilePath=getDataFolder() + File.separator + "config.yml";
        try(Reader reader=new InputStreamReader(new FileInputStream(confFilePath), StandardCharsets.UTF_8)){
            FileConfiguration conf=new YamlConfiguration();
            conf.load(reader);

            Token = conf.getString("discord.token");

            if (Token == null || Objects.equals(Token, "")) {
                throw new Exception("Token not found.");
            }

            client = DiscordClient.create(Token);
            gateway = client.login().block();

            List<String> temp = conf.getStringList("players");
            for(String str : temp) {
                String[] string = str.split(":");
                UUID uuid = UUID.fromString(string[0]);
                User User = gateway.getUserById(Snowflake.of(string[1])).block();

                PlayerHash.put(uuid, User);
            }

            Connection dbc = DriverManager.getConnection("jdbc:sqlite:" + dbPath);



            dbc.close();

            gateway.on(MessageCreateEvent.class).subscribe(e -> {
                final Message message = e.getMessage();
                if (message.getContent().startsWith("!register") && !e.getMessage().getAuthor().get().isBot()) {
                    final MessageChannel channel = message.getChannel().block();
                    String key = message.getContent().replace("!register ", "").replace(" ", "");
                    for (UUID uuid : RegisterHash.keySet()) {
                        if(RegisterHash.get(uuid).equals(key)) {
                            Objects.requireNonNull(channel).createEmbed(spec ->
                                    spec.setColor(Color.RED)
                                            .setAuthor(e.getMessage().getAuthor().get().getUsername(), "https://github.com/eighty88", e.getMessage().getAuthor().get().getAvatarUrl())
                                            .setImage("https://mc-heads.net/avatar/" + uuid.toString().replace("-", "") + "/200/helm.png")
                                            .setTitle(Utils.getName(uuid))
                                            .setDescription("アカウントの関連付けに成功しました")
                                            .setTimestamp(Instant.now())
                            ).block();
                            PlayerHash.put(uuid, e.getMessage().getAuthor().get());
                            RegisterHash.remove(uuid);
                            try {
                                Thread.sleep(500L);
                                e.getMessage().delete().block();
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                            DiscordConnect.getInstance().updateConfig();
                        }
                    }
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        } finally {

        }

    }

    @Override
    public void onDisable() {
        updateConfig();
    }

    public void updateConfig() {
        FileConfiguration conf = getConfig();
        List<String> PlayerList = new ArrayList<>();
        for(UUID uuid: PlayerHash.keySet()) {
            PlayerList.add(uuid.toString() + ":" + PlayerHash.get(uuid).getId().asString());
        }
        conf.set("players", PlayerList);
        saveConfig();
    }

    public static DiscordConnect getInstance() {
        return instance;
    }
}
