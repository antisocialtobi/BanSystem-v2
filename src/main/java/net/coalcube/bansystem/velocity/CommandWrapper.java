package net.coalcube.bansystem.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import net.coalcube.bansystem.core.command.Command;
import net.coalcube.bansystem.velocity.util.VelocityUser;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandWrapper implements SimpleCommand {

    private final Command cmd;

    public CommandWrapper(Command cmd) {
        this.cmd = cmd;
    }
    @Override
    public void execute(final Invocation invocation) {
        cmd.execute(new VelocityUser(invocation.source()), invocation.arguments());
    }

    // With this method you can control the suggestions to send
    // to the CommandSource according to the arguments
    // it has already written or other requirements you need
    @Override
    public List<String> suggest(final Invocation invocation) {
        return List.of();
    }

    // Here you can offer argument suggestions in the same way as the previous method,
    // but asynchronously. It is recommended to use this method instead of the previous one
    // especially in cases where you make a more extensive logic to provide the suggestions
    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        List<String> suggests = cmd.suggest(new VelocityUser(invocation.source()), invocation.arguments());
        return CompletableFuture.completedFuture(suggests);
    }
}