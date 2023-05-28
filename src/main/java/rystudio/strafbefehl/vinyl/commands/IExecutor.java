package rystudio.strafbefehl.vinyl.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;

import java.util.List;

public interface IExecutor {

    String getName();

    List<String> getAliases();

    String getDescription();

    boolean isOwnerOnly();

    boolean isDJOnly();

    boolean isBotOwner();

    List<Role> getAuthorizedRoles(JDA jda);

}
