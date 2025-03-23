package de.mikefox2k.taa;

import org.bukkit.NamespacedKey;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final String playerName;
    private Set<NamespacedKey> earnedAchievements;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.earnedAchievements = new HashSet<>();
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public Set<NamespacedKey> getEarnedAchievements() {
        return this.earnedAchievements;
    }
}
