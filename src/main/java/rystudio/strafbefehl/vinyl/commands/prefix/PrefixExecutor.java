package rystudio.strafbefehl.vinyl.commands.prefix;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import rystudio.strafbefehl.vinyl.commands.IExecutor;
import rystudio.strafbefehl.vinyl.database.MySQL;

import java.util.ArrayList;
import java.util.List;

public abstract class PrefixExecutor implements IExecutor {

    public List<PrefixOptions> options = new ArrayList<>();

    @Override
    public String getName() {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return new ArrayList<>();
    }

    @Override
    public String getDescription() {
        return null;
    }

    public List<PrefixOptions> getOptions() {
        return options;
    }

    @Override
    public boolean isOwnerOnly() {
        return false;
    }

    @Override
    public List<Channel> getAuthorizedChannels(JDA jda) {
        return new ArrayList<>();
    }

    @Override
    public List<Role> getAuthorizedRoles(JDA jda) {
        return new ArrayList<>();
    }

    public void execute(MessageReceivedEvent event, MySQL mySQL) { }

}
