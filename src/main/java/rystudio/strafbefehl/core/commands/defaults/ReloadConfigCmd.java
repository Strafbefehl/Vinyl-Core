package rystudio.strafbefehl.core.commands.defaults;

import rystudio.strafbefehl.core.Core;
import rystudio.strafbefehl.core.commands.EventData;
import rystudio.strafbefehl.core.commands.slash.SlashExecutor;
import rystudio.strafbefehl.core.database.MySQL;

import java.io.IOException;

public class ReloadConfigCmd extends SlashExecutor {

    private Core core;

    public ReloadConfigCmd(Core core) {
        this.core = core;
    }

    @Override
    public String getName() {
        return "reloadconfig";
    }

    @Override
    public String getDescription() {
        return "Reloads the EasyCommands config.";
    }

    @Override
    public boolean isOwnerOnly() {
        return true;
    }

    @Override
    public void execute(EventData data, MySQL mySQL) {
        try {
            this.core.getConfig().loadConfig();
            data.reply("Config has been reload successfully.", true).queue();
        } catch (IOException e) {
            data.reply("Error while reloading the config file. Try restarting the bot. If the problem persist contact support on Discord.", true).queue();
            throw new RuntimeException(e);
        }
    }

}
