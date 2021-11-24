package net.coalcube.bansystem.spigot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import net.coalcube.bansystem.core.command.Command;
import net.coalcube.bansystem.spigot.util.SpigotUser;
import net.coalcube.bansystem.spigot.util.TabCompleteUtil;

public class CommandWrapper implements CommandExecutor, TabExecutor {

    private final Command cmd;
    private final boolean tab;

    public CommandWrapper(Command cmd, boolean tab) {
        this.cmd = cmd;
        this.tab = tab;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias,
                                      String[] args) {
        return tab ? TabCompleteUtil.completePlayerNames(sender, args) : new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        cmd.execute(new SpigotUser(sender), args);
        return true;
    }

}