package net.neednot;


import de.tr7zw.nbtapi.NBTCompound;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import io.javalin.Javalin;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;

import net.neednot.JsonData;
import net.neednot.JsonPlayer;
import net.neednot.JsonServer;
import net.neednot.JsonArmor;
import net.neednot.JsonHelmet;
import net.neednot.JsonChestplate;
import net.neednot.JsonLeggings;
import net.neednot.JsonBoots;
import net.neednot.JsonError;
import net.neednot.JsonBlocks;
import net.neednot.JsonItems;
import net.neednot.JsonMobs;
import net.neednot.JsonStats;
import net.neednot.PlayerOnline;

import java.io.File;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.*;

public final class StatApi extends JavaPlugin {

    private StatApi plugin;
    private static Javalin app;
    public UUID uuid;

    @Override
    public void onEnable() {

        plugin = this;

        FileConfiguration config = this.getConfig();
        config.addDefault("Port", 7000);
        config.options().copyDefaults(true);
        saveConfig();

        int port = config.getInt("Port");

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        app = Javalin.create((javalinConfig) -> {
            javalinConfig.showJavalinBanner = false;
        }).start(port);

        app.get("/player/:uuid", ctx -> {
            //classes
            JsonPlayer JP = new JsonPlayer();
            JsonData JD = new JsonData();
            JsonArmor JA = new JsonArmor();
            JsonHelmet JH = new JsonHelmet();
            JsonChestplate JC = new JsonChestplate();
            JsonLeggings JL = new JsonLeggings();
            JsonBoots JB = new JsonBoots();
            JsonError JE = new JsonError();
            boolean error = false;



            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                JE.setSuccess(false);
                JE.setError("Invalid UUID! A UUID should look like this: " + UUID.randomUUID().toString());
                ctx.json(JE);
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                if (player.hasPlayedBefore()) {

                    if (player.isOnline()) {
                        PlayerOnline PO = new PlayerOnline();
                        PO.setUuid(uuid);
                        JP = PO.getPlayer();
                    }
                    if (!player.isOnline()) {
                        PlayerOffline PO = new PlayerOffline();
                        PO.setUuid(uuid);
                        JP = PO.getPlayer();
                    }

                    //writing them down
                    JD.setPlayer(JP);
                    JD.setSuccess(true);
                    ctx.json(JD);
                } else {
                    JE.setSuccess(false);
                    JE.setError("Player not found!");
                    ctx.json(JE);
                }
            }
        });
        app.get("/server", ctx -> {
            JsonServer JS = new JsonServer();
            int players = Bukkit.getOnlinePlayers().size();
            JS.setPlayers(players);
            String playerslistSTR = Bukkit.getOnlinePlayers().toString().replace("CraftPlayer{name=","").replace("}", "").replace("[", "").replace("]", "").replace(",", "");
            String[] playerslist = playerslistSTR.split(" ");

            JS.setPlayerslist(playerslist);

            ctx.json(JS);
        });
        app.get("/player/:uuid/mobs", ctx -> {

            JsonError JE = new JsonError();
            boolean error = false;

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                JE.setSuccess(false);
                JE.setError("Invalid UUID! A UUID should look like this: " + UUID.randomUUID().toString());
                ctx.json(JE);
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                if (player.hasPlayedBefore()) {

                    ArrayList<JsonMobs> mobs = new ArrayList<JsonMobs>();

                    for (EntityType e : EntityType.values()) {

                        JsonMobs JM = new JsonMobs();

                        String mob = e.name();
                        String name = e.name().replace("_", " ").toLowerCase();
                        int kills;
                        int mdeaths;
                        try {
                            if (e.name().equalsIgnoreCase("PLAYER")) {
                                kills = player.getStatistic(Statistic.PLAYER_KILLS);
                            } else {
                                kills = player.getStatistic(Statistic.KILL_ENTITY, e);
                            }
                        } catch (IllegalArgumentException e1) {
                            kills = 0;
                        }
                        try {
                            mdeaths = player.getStatistic(Statistic.ENTITY_KILLED_BY, e);
                        } catch (IllegalArgumentException e1) {
                            mdeaths = 0;
                        }

                        JM.setMob(mob);
                        JM.setName(name);
                        JM.setKills(kills);
                        JM.setDeaths(mdeaths);

                        mobs.add(JM);
                    }
                    ctx.json(mobs);
                } else {
                    JE.setSuccess(false);
                    JE.setError("Player not found!");
                    ctx.json(JE);
                }
            }
        });
        //stats
        app.get("/player/:uuid/stats", ctx -> {

            JsonError JE = new JsonError();
            boolean error = false;

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                JE.setSuccess(false);
                JE.setError("Invalid UUID! A UUID should look like this: " + UUID.randomUUID().toString());
                ctx.json(JE);
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                if (player.hasPlayedBefore()) {

                    ArrayList<JsonStats> stats = new ArrayList<JsonStats>();
                    for (Statistic s : Statistic.values()) {

                        JsonStats JS = new JsonStats();


                        if (s.getType() == Statistic.Type.UNTYPED) {
                            String stat = s.name();
                            int value = player.getStatistic(s);

                            JS.setValue(value);
                            JS.setStat(stat);

                            stats.add(JS);
                        }
                    }
                    ctx.json(stats);
                } else {
                    JE.setSuccess(false);
                    JE.setError("Player not found!");
                    ctx.json(JE);
                }
            }
        });
        //items
        app.get("/player/:uuid/items", ctx -> {

            JsonError JE = new JsonError();
            boolean error = false;

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                JE.setSuccess(false);
                JE.setError("Invalid UUID! A UUID should look like this: " + UUID.randomUUID().toString());
                ctx.json(JE);
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                if (player.hasPlayedBefore()) {

                    ArrayList<JsonItems> items = new ArrayList<JsonItems>();

                    for (Material m : Material.values()) {
                        if (m.isItem()) {

                            JsonItems JI = new JsonItems();

                            String type = m.name();
                            String name = type.replace("_", " ").toLowerCase();

                            int broken = player.getStatistic(Statistic.BREAK_ITEM, m);
                            int crafted = player.getStatistic(Statistic.CRAFT_ITEM, m);
                            int used = player.getStatistic(Statistic.USE_ITEM, m);
                            int dropped = player.getStatistic(Statistic.DROP, m);
                            int pickedup = player.getStatistic(Statistic.PICKUP, m);

                            JI.setName(name);
                            JI.setType(type);
                            JI.setBroken(broken);
                            JI.setCrafted(crafted);
                            JI.setUsed(used);
                            JI.setDropped(dropped);
                            JI.setPickedup(pickedup);
                            items.add(JI);
                        }
                    }
                    ctx.json(items);
                } else {
                    JE.setSuccess(false);
                    JE.setError("Player not found!");
                    ctx.json(JE);
                }
            }
        });
        //blocks
        app.get("/player/:uuid/blocks", ctx -> {

            JsonError JE = new JsonError();
            boolean error = false;

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                JE.setSuccess(false);
                JE.setError("Invalid UUID! A UUID should look like this: " + UUID.randomUUID().toString());
                ctx.json(JE);
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                if (player.hasPlayedBefore()) {

                    ArrayList<JsonBlocks> blocks = new ArrayList<JsonBlocks>();

                    for (Material m : Material.values()) {
                        if (m.isBlock()) {

                            JsonBlocks JBL = new JsonBlocks();

                            String type = m.name();
                            String name = type.replace("_", " ").toLowerCase();

                            int placed = player.getStatistic(Statistic.USE_ITEM, m);
                            int mined = player.getStatistic(Statistic.MINE_BLOCK, m);
                            int crafted = player.getStatistic(Statistic.CRAFT_ITEM, m);
                            int dropped = player.getStatistic(Statistic.DROP, m);
                            int broken = player.getStatistic(Statistic.BREAK_ITEM, m);
                            int pickedup = player.getStatistic(Statistic.PICKUP, m);

                            JBL.setName(name);
                            JBL.setType(type);
                            JBL.setPlaced(placed);
                            JBL.setMined(mined);
                            JBL.setCrafted(crafted);
                            JBL.setDropped(dropped);
                            JBL.setBroken(broken);
                            JBL.setPickedup(pickedup);
                            blocks.add(JBL);
                        }
                    }
                    ctx.json(blocks);
                } else {
                    JE.setSuccess(false);
                    JE.setError("Player not found!");
                    ctx.json(JE);
                }
            }
        });
        app.get("/whitelist/remove/:name", ctx -> {

            String name =  ctx.pathParam("name");

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin , new Runnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + name);
                }
            }, 20L);

            ctx.json("unwhitelisted " + name);

        });
        app.get("/whitelist/add/:name", ctx -> {
            String name =  ctx.pathParam("name");

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin , new Runnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist add " + name);
                }
            }, 20L);

            ctx.json("whitelisted " + name);
        });
        app.get("/kick/:uuid", ctx -> {
            boolean error = false;
            UUID uuid = UUID.randomUUID();

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                ctx.json("No player found");
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                if (player.isOnline() && player.hasPlayedBefore()) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin , new Runnable() {
                        @Override
                        public void run() {
                            player.getPlayer().kickPlayer("Kicked by discord admin");
                        }
                    }, 20L);
                }

                ctx.json("kicked " + player.getName());
            }
        });
        app.get("/ban/:uuid", ctx -> {
            boolean error = false;
            UUID uuid = UUID.randomUUID();

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                ctx.json("No player found");
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "Banned", null, "Admin on discord");
                if (player.isOnline() && player.hasPlayedBefore()) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin , new Runnable() {
                        @Override
                        public void run() {
                            player.getPlayer().kickPlayer("Banned");
                        }
                    }, 20L);
                }

                ctx.json("Banned " + player.getName());
            }
        });
        app.get("/unban/:uuid", ctx -> {
            boolean error = false;
            UUID uuid = UUID.randomUUID();

            try {
                uuid = UUID.fromString(ctx.pathParam("uuid"));
            }
            catch (IllegalArgumentException e1) {
                ctx.json("No player found");
                error = true;
            }
            if (!error) {
                //uuid to player
                OfflinePlayer player = (OfflinePlayer) Bukkit.getOfflinePlayer(uuid);

                getServer().getBanList(BanList.Type.NAME).pardon(player.getName());

                ctx.json("Unbanned " + player.getName());
            }
        });

        Thread.currentThread().setContextClassLoader(classLoader);

    }

    @Override
    public void onDisable() {
        app.stop();
    }
}