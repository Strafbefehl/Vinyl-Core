package rystudio.strafbefehl.core.commands.slash;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import rystudio.strafbefehl.core.commands.EventData;
import rystudio.strafbefehl.core.commands.IExecutor;
import rystudio.strafbefehl.core.database.MySQL;

import java.util.ArrayList;
import java.util.List;

public abstract class SlashExecutor implements IExecutor {

    public List<OptionData> options = new ArrayList<>();

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

    @Override
    public boolean isOwnerOnly() {
        return false;
    }

    public List<OptionData> getOptions() {
        return options;
    }

    @Override
    public List<Channel> getAuthorizedChannels(JDA jda) {
        return new ArrayList<>();
    }

    @Override
    public List<Role> getAuthorizedRoles(JDA jda) {
        return new ArrayList<>();
    }

    public void execute(EventData event, MySQL mySQL) {

    }

}
