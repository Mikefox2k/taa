package de.mikefox2k.taa;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.UUID;

public class TAAPlugin extends JavaPlugin implements Listener {

    private FileConfiguration advancementsConfig;

    private GUIPanel guiPanel;
    private GameManager gameManager;
    private FileManager fileManager;

    LiteralCommandNode<CommandSourceStack> taaCommand = Commands.literal("taa")
            .then(Commands.literal("enter")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    Entity executor = ctx.getSource().getExecutor();

                    if (!(executor instanceof Player player)) {
                        sender.sendMessage("You must be a player to use this command!");
                        return Command.SINGLE_SUCCESS;
                    }

                    gameManager.registerPlayer(player);
                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("start")
                .executes(ctx -> {
                    if (gameManager.getPlayers().isEmpty()) {
                        ctx.getSource().getSender().sendPlainMessage("Du musst dich registrieren, bevor du die Hunt startest!");
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!gameManager.startGame()) {
                        ctx.getSource().getSender().sendPlainMessage("Die Hunt läuft bereits!");
                        return Command.SINGLE_SUCCESS;
                    }

                    return Command.SINGLE_SUCCESS;
                }))
            .then(Commands.literal("add")
                    .then(Commands.argument("string", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String string = StringArgumentType.getString(ctx, "string");

                                gameManager.updateCurrentGoal(string);

                                return Command.SINGLE_SUCCESS;
                            })
                    )
            )
            .then(Commands.literal("clear")
                    .executes(ctx -> {
                        gameManager.updateCurrentGoal(" - ");

                        return Command.SINGLE_SUCCESS;
                    }))
            .build();

    @Override
    public void onEnable() {
        this.gameManager = new GameManager(this);
        this.guiPanel = new GUIPanel(this);
        this.fileManager = new FileManager(this);

        saveDefaultConfig();

        loadConfig();
        fileManager.loadGameState();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(taaCommand);
        });
    }

    @Override
    public void onDisable() {

        UUID uuid = gameManager.getRegisteredUUIDs().iterator().next();

        fileManager.saveGameState();
    }

    private void loadConfig() {
        File advancementsConfigFile = new File(getDataFolder(), "advancements.yml");
        if (!advancementsConfigFile.exists()) {
            advancementsConfigFile.getParentFile().mkdirs();
            saveResource("advancements.yml", false);
        }

        this.advancementsConfig = YamlConfiguration.loadConfiguration(advancementsConfigFile);
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }

    public GUIPanel getGUIPanel() {
        return this.guiPanel;
    }

    public FileConfiguration getAdvancementsConfig() {
        return this.advancementsConfig;
    }
}
