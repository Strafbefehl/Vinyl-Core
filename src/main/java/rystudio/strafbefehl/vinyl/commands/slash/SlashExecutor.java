package rystudio.strafbefehl.vinyl.commands.slash;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.sharding.ShardManager;
import rystudio.strafbefehl.vinyl.Core;
import rystudio.strafbefehl.vinyl.commands.EventData;
import rystudio.strafbefehl.vinyl.commands.IExecutor;
import rystudio.strafbefehl.vinyl.database.MySQL;

import java.util.ArrayList;
import java.util.List;

public abstract class SlashExecutor implements IExecutor {
    ShardManager shardManager = Core.getShardManager();
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

    @Override
    public boolean isBotOwner() {
        return true;
    }

    public List<OptionData> getOptions() {
        return options;
    }

    @Override
    public List<Role> getAuthorizedRoles(JDA jda) {
        return new ArrayList<>();
    }

    public void execute(EventData event, MySQL mySQL) {
        if (shardManager != null) {
            // Iteriere über alle Shards im ShardManager
            for (JDA shard : shardManager.getShards()) {
                int shardId = shard.getShardInfo().getShardId();
                int shardTotal = shardManager.getShardsTotal();

                System.out.println("Shard ID: " + shardId + " / " + shardTotal);
                System.out.println("Guilds: " + shard.getGuildCache().size());
                System.out.println("Users: " + shard.getUserCache().size());
                System.out.println("-------------------------");
            }
        } else {
            System.out.println("ShardManager ist nicht initialisiert.");
        }
    }

}
