package rystudio.strafbefehl.core.commands.slash;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import rystudio.strafbefehl.core.Core;
import rystudio.strafbefehl.core.commands.EventData;
import rystudio.strafbefehl.core.commands.slash.SlashExecutor;
import rystudio.strafbefehl.core.utils.LogType;

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

            if(!executor.getAuthorizedChannels(core.jda).isEmpty() && !executor.getAuthorizedChannels(core.jda).contains(event.getChannel())) {
                event.reply("This command cannot be used in this channel.").setEphemeral(true).queue();
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
