package de.mikefox2k.taa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager implements Listener {

    private final TAAPlugin plugin;
    private Map<UUID, PlayerData> players;
    private Map<UUID, Long> lastAchievementTime = new HashMap<>();
    private Map<UUID, Long> timePlayed = new HashMap<>();
    private BukkitTask timeTracker;
    private boolean isGameRunning;

    public GameManager(TAAPlugin plugin) {
        this.plugin = plugin;
        this.players = new HashMap<>();
        this.isGameRunning = false;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void registerPlayer(Player player) {
        if (this.isGameRunning) {
            player.sendMessage("Das Spiel ist bereits gestartet.");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (this.players.containsKey(uuid)) {
            player.sendMessage("Du bist bereits registriert.");
        } else {
            this.players.put(uuid, new PlayerData(uuid, player.getName()));
            player.sendMessage("Du hast dich registriert.");
        }
    }

    public boolean startGame() {
        if (this.isGameRunning) {
            return false;
        }

        Bukkit.broadcast(Component.text("Die Trilluxe Achievement Hunt startet bald!"));
        this.isGameRunning = true;

        World overworld = plugin.getServer().getWorlds().getFirst();

        Difficulty difficulty = Difficulty.valueOf(plugin.getConfig().getString("difficulty"));
        for(World world : plugin.getServer().getWorlds()) {
            world.setDifficulty(difficulty);
        }

        overworld.setTime(0);

        new BukkitRunnable() {
            private int countdown = 10;

            @Override
            public void run() {
                if (countdown > 0) {
                    for (PlayerData playerData : players.values()) {
                        Player player = plugin.getServer().getPlayer(playerData.getUuid());

                        if (player != null) {
                            Title title = Title.title(Component.text(String.valueOf(countdown)).color(TAAColors.RED), Component.empty());
                            player.showTitle(title);
                        }
                    }
                    countdown--;
                } else {

                    removeAllAchievements();

                    for (PlayerData playerData : players.values()) {
                        Player player = plugin.getServer().getPlayer(playerData.getUuid());

                        if (player != null) {
                            plugin.getGUIPanel().createBoard(player);
                            player.getInventory().clear();
                            player.updateInventory();
                            player.setGameMode(GameMode.SURVIVAL);
                            player.setHealth(20D);
                            player.setFoodLevel(20);

                            startTimer();
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        handleAdvancement(event.getPlayer(), event.getAdvancement());
    }

    public void handleAdvancement(Player player, Advancement advancement) {

        PlayerData playerData = this.players.get(player.getUniqueId());
        NamespacedKey achievementKey = advancement.getKey();

        if (isRecipe(advancement)) {
            return;
        }

        if (!this.isGameRunning || playerData == null) {
            return;
        }

        playerData.getEarnedAchievements().add(achievementKey);
        plugin.getGUIPanel().updateBoards();
    }

    private void removeAllAchievements() {
        for (PlayerData playerData : players.values()) {
            Player player = plugin.getServer().getPlayer(playerData.getUuid());

            if (player != null) {

                Iterator<Advancement> it = Bukkit.getServer().advancementIterator();

                while (it.hasNext()) {
                    Advancement advancement = it.next();
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);

                    for (String criteria : advancement.getCriteria()) {
                        progress.revokeCriteria(criteria);
                    }
                }
            }
        }
    }

    private boolean isRecipe(Advancement advancement) {
        String key = advancement.getKey().getKey();
        return key.split("/")[0].equals("recipes");
    }

    public void startTimer() {
        timeTracker = new BukkitRunnable() {
            @Override
            public void run() {
                   for ( PlayerData playerData : players.values()) {
                       Player player = plugin.getServer().getPlayer(playerData.getUuid());
                       long lastPlayTime = timePlayed.getOrDefault(playerData.getUuid(), 0L);
                       timePlayed.put(playerData.getUuid(), lastPlayTime + 1);

                       plugin.getGUIPanel().updateBoards();
                   }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public long getTimePlayed(UUID uuid) {
        return this.timePlayed.getOrDefault(uuid, 0L);
    }

    public void setTimePlayed(UUID uuid, long time) {
        this.timePlayed.clear();
        this.timePlayed.put(uuid, time);
    }

    public void stopTimer() {
        if (this.timeTracker != null) {
            this.timeTracker.cancel();
        }
    }

    public BukkitTask getTimeTracker() {
        return this.timeTracker;
    }

    public Set<UUID> getRegisteredUUIDs() {
        return this.players.keySet();
    }

    public Map<UUID, PlayerData> getPlayers() {
        return this.players;
    }

    public boolean isGameRunning() {
        return this.isGameRunning;
    }

    public void setGameRunning(boolean gameRunning) {
        this.isGameRunning = gameRunning;
    }

    public void setPlayers(Map<UUID, PlayerData> players) {
        this.players = players;
    }

    public int getCurrentAchievementAmount() {
        int res = 0;

        for (PlayerData playerData : players.values()) {
            res += playerData.getEarnedAchievements().size();
        }

        return res;
    }

    public int getMaxAchievementAmount() {
        return 122*players.size();
    }

}
