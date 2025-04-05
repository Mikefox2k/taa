package de.mikefox2k.taa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class FileManager {

    private final TAAPlugin plugin;
    private final File saveFile;
    private final Gson gson;

    public FileManager(TAAPlugin plugin) {
        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), "gamestate.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveGameState() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdir();
            }

            UUID uuid = plugin.getGameManager().getPlayers().keySet().iterator().next();

            GameState state = new GameState(
                    plugin.getGameManager().getPlayers(),
                    plugin.getGameManager().isGameRunning(),
                    plugin.getGameManager().getTimePlayed(uuid),
                    plugin.getGameManager().getTimeSinceLastAchievement(uuid),
                    plugin.getGameManager().getCurrentAchievementAmount(),
                    plugin.getGameManager().getCurrentGoal().getOrDefault(uuid, "-"),
                    plugin.getGameManager().getDeathCount(),
                    plugin.getGameManager().getCurrentPoints(),
                    plugin.getGameManager().getLastAchievement()
            );

            FileWriter writer = new FileWriter(saveFile);
            gson.toJson(state, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadGameState() {
        if (!saveFile.exists()) {
            return;
        }

        try {
            FileReader reader = new FileReader(saveFile);
            GameState state = gson.fromJson(reader, GameState.class);
            reader.close();

            plugin.getGameManager().setGameRunning(state.isGameRunning());
            plugin.getGameManager().setPlayers(state.getRegisteredPlayers());

            UUID uuid = plugin.getGameManager().getPlayers().keySet().iterator().next();

            plugin.getGameManager().setTimePlayed(uuid, state.getTimePlayed());
            plugin.getGameManager().setTimeSinceLastAchievement(uuid, state.getTimeSinceAchievement());
            plugin.getGameManager().updateCurrentGoal(state.getCurrentGoal());
            plugin.getGameManager().setDeathCount(state.getDeathCount());
            plugin.getGameManager().setCurrentPoints(state.getCurrentPointsSave());
            plugin.getGameManager().setLastAchievement(state.getLastAchievementName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class GameState {

        private final Map<UUID, PlayerData> registeredPlayers;
        private final boolean isGameRunning;
        private final Long timePlayed;
        private final Long timeSinceAchievement;
        private final int achievementAmount;
        private final String currentGoal;
        private final int deathCount;
        private final int currentPoints;
        private final String lastAchievementName;

        public GameState(
                Map<UUID, PlayerData> registeredPlayers,
                boolean isGameRunning,
                Long timePlayed,
                Long timeSinceAchievement,
                int achievementAmount,
                String currentGoal,
                int deathCount,
                int currentPoints,
                String lastAchievementName) {
            this.registeredPlayers = registeredPlayers;
            this.isGameRunning = isGameRunning;
            this.timePlayed = timePlayed;
            this.timeSinceAchievement = timeSinceAchievement;
            this.achievementAmount = achievementAmount;
            this.currentGoal = currentGoal;
            this.deathCount = deathCount;
            this.currentPoints = currentPoints;
            this.lastAchievementName = lastAchievementName;
        }

        public Map<UUID, PlayerData> getRegisteredPlayers() {
            return registeredPlayers;
        }

        public boolean isGameRunning() {
            return isGameRunning;
        }

        public Long getTimePlayed() {
            return timePlayed;
        }

        public Long getTimeSinceAchievement() {
            return timeSinceAchievement;
        }

        public int getAchievementAmount() {
            return achievementAmount;
        }

        public String getCurrentGoal() {
            return currentGoal;
        }

        public int getDeathCount() {
            return deathCount;
        }

        public int getCurrentPointsSave() {
            return currentPoints;
        }

        public String getLastAchievementName() {
            return lastAchievementName;
        }

    }
}
