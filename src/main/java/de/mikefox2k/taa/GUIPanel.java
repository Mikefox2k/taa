package de.mikefox2k.taa;

import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIPanel implements Listener {

    private final TAAPlugin plugin;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    public GUIPanel(TAAPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateBoards, 0, 20*30);
    }

    public void updateBoards() {
        for (FastBoard board : this.boards.values()) {
            updateBoard(board);
        }
    }

    private void updateBoard(FastBoard board) {

        int currentAchievementAmount = this.plugin.getGameManager().getCurrentAchievementAmount();
        int maxAchievementAmount = plugin.getGameManager().getMaxAchievementAmount();
        long timePlayed = this.plugin.getGameManager().getTimePlayed(board.getPlayer().getUniqueId());
        long timeSinceLastAchievement = this.plugin.getGameManager().getTimeSinceLastAchievement(board.getPlayer().getUniqueId());
        String lastAchievement = this.plugin.getGameManager().getLastAchievement();
        String currentGoal = this.plugin.getGameManager().getCurrentGoal().getOrDefault(board.getPlayer().getUniqueId(), "-");
        int deathCount = this.plugin.getGameManager().getDeathCount();
        int currentPoints = this.plugin.getGameManager().getCurrentPoints();
        int maxPoints = this.plugin.getGameManager().getMaxPoints();

        board.updateLines(
                Component.text("»Achievement Hunt«")
                        .style(Style.style(TAAColors.RED, TextDecoration.BOLD, TextDecoration.UNDERLINED)),
                Component.text(""),
                Component.text("Gesamtzeit", TAAColors.YELLOW),
                Component.text(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(Util.formatTime(timePlayed), TAAColors.ORANGE)),
                Component.text("Achievements", TAAColors.YELLOW),
                Component.text(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(currentAchievementAmount, TAAColors.ORANGE))
                        .append(Component.text(" / ", TAAColors.ORANGE))
                        .append(Component.text(maxAchievementAmount, TAAColors.ORANGE)),
                Component.text("Letztes Achievement", TAAColors.YELLOW),
                Component.text(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(Util.formatTime(timeSinceLastAchievement), TAAColors.ORANGE)),
                Component.text(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(lastAchievement, TAAColors.GREEN)),
                Component.text("Punkte", TAAColors.YELLOW),
                Component.text(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(currentPoints, TAAColors.ORANGE))
                        .append(Component.text(" / ", TAAColors.ORANGE))
                        .append(Component.text(maxPoints, TAAColors.ORANGE)),
                Component.text("Ziel", TAAColors.YELLOW),
                Component.text(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(currentGoal, TAAColors.GREEN)),
                Component.text("Tode", TAAColors.YELLOW),
                Component.text().content(" » ").color(TAAColors.ORANGE)
                        .append(Component.text(deathCount, TAAColors.RED))
                        .build()
        );
    }

    public void createBoard(Player... players) {
        for (Player player : players) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(Component.text("»TrilluXe«").style(Style.style(TAAColors.RED, TextDecoration.BOLD)));
            updateBoard(board);
            this.boards.put(player.getUniqueId(), board);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().getRegisteredUUIDs().contains(player.getUniqueId())) {
            createBoard(player);
            FileManager fileManager = new FileManager(plugin);
            fileManager.loadGameState();

            plugin.getGameManager().startTimerPlayed();
            plugin.getGameManager().startTimerSinceLastAchievement();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        FileManager fileManager = new FileManager(plugin);
        fileManager.saveGameState();

        plugin.getGameManager().stopTimerPlayed();
        plugin.getGameManager().stopTimerSinceLastAchievement();

        FastBoard board = this.boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }

}