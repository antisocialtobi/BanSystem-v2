package net.coalcube.bansystem.bungee;

import java.util.ArrayList;

import net.coalcube.bansystem.bungee.util.BungeeUser;
import net.coalcube.bansystem.core.command.Command;
import net.coalcube.bansystem.bungee.util.TabCompleteUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;

public class CommandWrapper extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

    private Command cmd;
    private boolean tab;

    public CommandWrapper(String name, Command cmd, boolean tab) {
        super(name);
        this.cmd = cmd;
        this.tab = tab;
    }

    @Override
    public void execute(CommandSender arg0, String[] arg1) {
        cmd.execute(new BungeeUser(arg0), arg1);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender arg0, String[] arg1) {
        return tab ? TabCompleteUtil.completePlayerNames(arg0, arg1) : new ArrayList<>();
    }

}