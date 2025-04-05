package de.mikefox2k.taa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager implements Listener {

    private final TAAPlugin plugin;
    private Map<UUID, PlayerData> players;

    private Map<UUID, Long> timePlayed = new HashMap<>();
    private BukkitTask timeTrackerPlayed;

    private Map<UUID, Long> timeSinceLastAchievement = new HashMap<>();
    private BukkitTask timeTrackerSinceLastAchievement;

    private Map<UUID, String> currentGoal = new HashMap<>();

    private int deathCount;

    private int currentPoints;
    private String lastAchievement;

    private boolean isGameRunning;

    public GameManager(TAAPlugin plugin) {
        this.plugin = plugin;
        this.players = new HashMap<>();
        this.isGameRunning = false;
        this.lastAchievement = "-";

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
        this.deathCount = 0;

        World overworld = plugin.getServer().getWorlds().getFirst();

        // Difficulty difficulty = Difficulty.valueOf(plugin.getConfig().getString("difficulty"));
        for(World world : plugin.getServer().getWorlds()) {
            world.setDifficulty(Difficulty.NORMAL);
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

                            startTimerPlayed();
                            startTimerSinceLastAchievement();
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    public Map<UUID, String> getCurrentGoal() {
        return this.currentGoal;
    }

    public void updateCurrentGoal(String goal) {
        for (PlayerData playerData : players.values()) {
            Player player = plugin.getServer().getPlayer(playerData.getUuid());
            if (player != null) {
                this.currentGoal.clear();
                this.currentGoal.put(playerData.getUuid(), goal);
            }
        }
    }

    public int getDeathCount() {
        return this.deathCount;
    }

    public void setDeathCount(int count) {
        this.deathCount = count;
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

        ConfigurationSection advancementSection = getAdvancementSection(advancement);

        setCurrentPoints(calcCurrentPoints(advancementSection));
        setLastAchievement(getAdvancementName(advancementSection));

        resetTimeSinceLastAchievement();
        plugin.getGUIPanel().updateBoards();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData playerData = players.get(player.getUniqueId());

        // Ignore if player is not registered or game is not running
        if(playerData == null || !isGameRunning) {
            return;
        }

        this.deathCount += 1;
        setDeathCount(this.deathCount);

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

    private ConfigurationSection getAdvancementSection(Advancement advancement) {
        String path = advancement.getKey().getNamespace() + "." + advancement.getKey().getKey().replace('/', '.');
        return plugin.getAdvancementsConfig().getConfigurationSection(path);
    }

    private ConfigurationSection getAdvancementSection(NamespacedKey key) {
        String path = key.getNamespace() + "." + key.getKey().replace('/', '.');
        return plugin.getAdvancementsConfig().getConfigurationSection(path);
    }

    public int getAdvancementPoints(ConfigurationSection advancementSection) {
        return advancementSection.getInt("points", 0);
    }

    public String getAdvancementName(ConfigurationSection advancementSection) {
        return advancementSection.getString("de");
    }

    public void startTimerPlayed() {
        timeTrackerPlayed = new BukkitRunnable() {
            @Override
            public void run() {
                   for ( PlayerData playerData : players.values()) {
                       long lastPlayTime = timePlayed.getOrDefault(playerData.getUuid(), 0L);
                       timePlayed.put(playerData.getUuid(), lastPlayTime + 1);

                       plugin.getGUIPanel().updateBoards();
                   }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void startTimerSinceLastAchievement() {

        // No need to update GUI, since we update every second by the playtime tracker

        timeTrackerSinceLastAchievement = new BukkitRunnable() {
            @Override
            public void run() {
                for ( PlayerData playerData : players.values()) {
                    long lastPlayTime = timeSinceLastAchievement.getOrDefault(playerData.getUuid(), 0L);
                    timeSinceLastAchievement.put(playerData.getUuid(), lastPlayTime + 1);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public long getTimePlayed(UUID uuid) {
        return this.timePlayed.getOrDefault(uuid, 0L);
    }

    public long getTimeSinceLastAchievement(UUID uuid) {
        return this.timeSinceLastAchievement.getOrDefault(uuid, 0L);
    }

    public void setTimePlayed(UUID uuid, long time) {
        this.timePlayed.clear();
        this.timePlayed.put(uuid, time);
    }

    public void setTimeSinceLastAchievement(UUID uuid, long time) {
        this.timeSinceLastAchievement.clear();
        this.timeSinceLastAchievement.put(uuid, time);
    }

    public void resetTimeSinceLastAchievement() {
        for (PlayerData playerData : players.values()) {
            this.timeSinceLastAchievement.put(playerData.getUuid(), 0L);
        }
    }

    public void stopTimerPlayed() {
        if (this.timeTrackerPlayed != null) {
            this.timeTrackerPlayed.cancel();
        }
    }

    public void stopTimerSinceLastAchievement() {
        if (this.timeTrackerSinceLastAchievement != null) {
            this.timeTrackerSinceLastAchievement.cancel();
        }
    }

    public BukkitTask getTimeTrackerPlayed() {
        return this.timeTrackerPlayed;
    }

    public BukkitTask getTimeTrackerSinceLastAchievement() {
        return this.timeTrackerSinceLastAchievement;
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

    public void setCurrentPoints(int points) {
        this.currentPoints = points;
    }

    public int getCurrentPoints() {
        return this.currentPoints;
    }

    public void setLastAchievement(String name) {
        this.lastAchievement = name;
    }

    public String getLastAchievement() {
        return this.lastAchievement;
    }

    public int calcCurrentPoints(ConfigurationSection advancementSection) {
        int res = 0;
        for (PlayerData playerData : players.values()) {
            for (NamespacedKey key : playerData.getEarnedAchievements()) {
                res += getAdvancementPoints(getAdvancementSection(key));
            }
        }
        return res;
    }

    public int getMaxPoints() {
        int res = 0;
        List<String> configKey = plugin.getAdvancementsConfig().getKeys(true).stream().filter(s -> s.endsWith(".points")).toList();

        for (String key : configKey) {
            res += plugin.getAdvancementsConfig().getInt(key);
        }

        return res * players.size();
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
