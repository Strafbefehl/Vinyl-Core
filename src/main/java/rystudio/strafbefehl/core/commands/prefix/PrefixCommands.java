package rystudio.strafbefehl.core.commands.prefix;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import rystudio.strafbefehl.core.Core;
import rystudio.strafbefehl.core.utils.LogType;

import java.util.Objects;

public class PrefixCommands extends ListenerAdapter {

    private final Core core;
    private String prefix = "!";

    public PrefixCommands(Core core) {
        this.core = core;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String[] args = event.getMessage().getContentRaw().split(" ");

        if(!args[0].contains(prefix) || Objects.requireNonNull(event.getMember()).getUser().isBot()) {
            return;
        }

        if(!args[0].startsWith(prefix)) {
            return;
        }

        String cmdName = args[0].replaceFirst(prefix, "");

        if(core.getExecutors().containsKey(cmdName) && core.getExecutors().get(cmdName) instanceof PrefixExecutor) {
            PrefixExecutor executor = (PrefixExecutor) core.getExecutors().get(cmdName);
            String[] options = event.getMessage().getContentRaw().replace(prefix + cmdName + " ", "").split(" ");
            if(options.length > 0 && executor.getOptions().size() > 0) {
                for (int i = 0; i < options.length; i++) {
                    executor.getOptions().get(i).setStringValue(options[i]);
                }
            }

            core.getLogger().logBoth(LogType.PREFIXCMD, "'" + cmdName + "' has been triggered.", event.getMember());
            if(!executor.getAuthorizedChannels(core.jda).isEmpty() && !executor.getAuthorizedChannels(core.jda).contains(event.getChannel())) {
                core.getLogger().logBoth(LogType.WARNING, "PrefixCommand: '" + cmdName + "' has been triggered but the channel it was executed in isn't authorized.", event.getMember());
                return;
            }

            if(executor.getAuthorizedRoles(core.jda) != null && !executor.getAuthorizedRoles(core.jda).isEmpty()) {
                for (Role authorizedRole : executor.getAuthorizedRoles(core.jda)) {
                    if(Objects.requireNonNull(event.getMember()).getRoles().contains(authorizedRole)) {
                        executor.execute(event, core.getMySQL());
                        event.getMessage().delete().queue();
                        break;
                    }
                }
                return;
            }

            executor.execute(event, core.getMySQL());
            event.getMessage().delete().queue();
        }
    }

}
