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

        board.updateLines(
                Component.text("Gesamtzeit", TAAColors.LIGHT_GRAY),
                Component.text().content(" » ").color(TAAColors.LIGHT_GRAY)
                        .append(Component.text(Util.formatTime(timePlayed), TAAColors.LIGHT_GRAY))
                        .build(),
                Component.text(""),
                Component.text("Achievements", TAAColors.LIGHT_GRAY),
                Component.text().content(" » ").color(TAAColors.LIGHT_GRAY)
                        .append(Component.text(currentAchievementAmount, TAAColors.YELLOW))
                        .append(Component.text(" / ", TAAColors.YELLOW))
                        .append(Component.text(maxAchievementAmount, TAAColors.YELLOW))
                        .build()
        );
    }

    public void createBoard(Player... players) {
        for (Player player : players) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(Component.text("Trilluxe Achievement Hunt").style(Style.style(TAAColors.RED, TextDecoration.BOLD)));
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
            fileManager.loadGameState(player.getUniqueId());

            plugin.getGameManager().startTimer();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        FileManager fileManager = new FileManager(plugin);
        fileManager.saveGameState(player.getUniqueId());

        plugin.getGameManager().stopTimer();

        FastBoard board = this.boards.remove(player.getUniqueId());

        if (board != null) {
            board.delete();
        }
    }

}