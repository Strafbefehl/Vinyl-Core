package rystudio.strafbefehl.vinyl.commands.slash;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import rystudio.strafbefehl.vinyl.Core;
import rystudio.strafbefehl.vinyl.commands.EventData;
import rystudio.strafbefehl.vinyl.database.MySQL;
import rystudio.strafbefehl.vinyl.utils.LogType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class SlashCommands extends ListenerAdapter {

    private final Core core;

    public SlashCommands(Core core) {
        this.core = core;
    }
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(core.getExecutors().containsKey(event.getName()) && core.getExecutors().get(event.getName()) instanceof SlashExecutor) {
            SlashExecutor executor = (SlashExecutor) core.getExecutors().get(event.getName());
            core.getLogger().logBoth(LogType.SLASHCMD, "'" + executor.getName() + "' has been triggered.", event.getMember());
            if(executor.isOwnerOnly() && ! (Objects.requireNonNull(event.getMember())).isOwner()) {
                event.reply("This command can only be used by the server owner.").setEphemeral(true).queue();
                return;
            }

            if (executor.isDJOnly()) {
                try {
                    MySQL mySQL = core.getMySQL();
                    Connection connection = mySQL.getConnection();
                    PreparedStatement ps = connection.prepareStatement("SELECT dj_role FROM guilds WHERE guild_id = ?");
                    ps.setString(1, event.getGuild().getId());
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String djRole = rs.getString("dj_role");
                        // Überprüfe, ob der Benutzer die DJ-Rolle besitzt
                        if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(djRole))) {
                            // Der Benutzer besitzt die DJ-Rolle und der Befehl kann ausgeführt werden
                            // Füge hier den restlichen Code hinzu, um den Befehl auszuführen
                        } else {
                            // Der Benutzer besitzt nicht die DJ-Rolle und der Befehl wird abgebrochen
                            event.reply("You must have the DJ role to use this command.").setEphemeral(true).queue();
                            return;
                        }
                    } else {
                        // Es wurde keine DJ-Rolle festgelegt und der Befehl kann ausgeführt werden
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            if (executor.isBotOwner() || Objects.requireNonNull(event.getMember()).getId().equals("364055339626135572")) {
                // User with ID 364055339626135572 is executing the code or is the bot owner
                // Continue with the rest of the code execution
            } else {
                // User with ID 364055339626135572 is not executing the code
                // Cancel the execution or take appropriate action
                return;
            }

            if(executor.getAuthorizedRoles(core.jda) != null && !executor.getAuthorizedRoles(core.jda).isEmpty()) {
                for (Role authorizedRole : executor.getAuthorizedRoles(core.jda)) {
                    if(Objects.requireNonNull(event.getMember()).getRoles().contains(authorizedRole)) {
                        executor.execute(new EventData(event), core.getMySQL());
                        break;
                    }
                }
                return;
            }

            executor.execute(new EventData(event), core.getMySQL());
        }
    }

}
