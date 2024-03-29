package rystudio.strafbefehl.vinyl;


import com.mysql.cj.log.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManager;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import rystudio.strafbefehl.vinyl.commands.IExecutor;
import rystudio.strafbefehl.vinyl.commands.prefix.PrefixCommands;
import rystudio.strafbefehl.vinyl.commands.prefix.PrefixExecutor;
import rystudio.strafbefehl.vinyl.commands.slash.SlashCommands;
import rystudio.strafbefehl.vinyl.commands.slash.SlashExecutor;
import rystudio.strafbefehl.vinyl.database.MySQL;
import rystudio.strafbefehl.vinyl.utils.Config;
import rystudio.strafbefehl.vinyl.utils.ConsoleColors;
import rystudio.strafbefehl.vinyl.utils.LogType;
import rystudio.strafbefehl.vinyl.utils.Logger;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class Core extends ListenerAdapter {

    public JDA jda;
    private boolean running;
    private DefaultShardManager shardManager;

    private DefaultShardManagerBuilder builder;

    private MySQL mySQL;

    private static final String WEBHOOK_ID = "846411941035376660";
    private static final String WEBHOOK_TOKEN = "9MywTcbt3jFjFNZl8vDK3q4k7gCQ8sOpBBhGL4NCCsFsCInNob3X9e1WuaEec_NaC4rh";


    private final Map<String, IExecutor> executorMap = new HashMap<>();

    private final List<GatewayIntent> gatewayIntents = new ArrayList<>();
    private final List<CacheFlag> enabledCacheFlags = new ArrayList<>();
    private final List<CacheFlag> disabledCacheFlags = new ArrayList<>();

    private PrefixCommands prefixCommands;
    private final SlashCommands slashCommands;

    private final Map<Guild, Channel> guildsMusicChannel = new HashMap<>();

    private List<JDA> jdas = new ArrayList<>();

    private Long millisStart;

    private Config config;

    private TextChannel logChannel;

    private Logger logger;

    public Core(DefaultShardManagerBuilder builder) throws IOException {
        config = new Config();
        this.logger = new Logger(this);
        this.builder = builder;
        loadIntents();

        this.slashCommands = new SlashCommands(this);

        if (this.config.getUsePrefixCommands()) {
            this.prefixCommands = new PrefixCommands(this);
            getGatewayIntents().add(GatewayIntent.MESSAGE_CONTENT);
        }

        builder = DefaultShardManagerBuilder.create(this.config.getToken(), gatewayIntents);
        builder.addEventListeners(slashCommands);
    }

    @Deprecated
    public Core(String token, boolean usePrefixCommands, DefaultShardManagerBuilder builder) throws IOException {
        this.builder = builder;

        millisStart = System.currentTimeMillis();

        loadIntents();

        this.slashCommands = new SlashCommands(this);

        if (usePrefixCommands) {
            this.prefixCommands = new PrefixCommands(this);
            getGatewayIntents().add(GatewayIntent.MESSAGE_CONTENT);
        }

    }

    public JDA buildJDA(int shardCount) throws InterruptedException {
        if (running && shardManager != null) {
            stopBot();
        }

        for (int i = 0; i < shardCount; i++) {
            Thread.sleep(5000);
            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(getConfig().getToken(), gatewayIntents);
            builder.setEnabledIntents(gatewayIntents);
            builder.enableCache(enabledCacheFlags);
            builder.disableCache(disabledCacheFlags);

            builder.setShardsTotal(shardCount);
            builder.setShards(i, shardCount - 1);

            ShardManager shardManager = builder.build(); // Build the ShardManager instance
            jdas.add(shardManager.getShardById(i)); // Add the JDA instance to the list
            jdas.get(i).awaitReady().getPresence().setActivity(Activity.listening("Shard: " + String.valueOf(i)));

            jdas.get(i).addEventListener(this);

            Logger.log(LogType.LISTENERS, jdas.get(i).getRegisteredListeners().toString());

            jdas.get(i).addEventListener(slashCommands);


            if (this.config != null && this.config.getUsePrefixCommands()) {
                jdas.get(i).addEventListener(prefixCommands);
            }
        }


        if (this.config != null && this.config.getUseMysql()) {
            try {
                this.mysqlInit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        millisStart = System.currentTimeMillis();


        updateCommands();
        logCurrentExecutors();
        Logger.log(LogType.OK, "Core (RyStudio) finished loading in " + ConsoleColors.GREEN_BOLD + (System.currentTimeMillis() - millisStart) + "ms" + ConsoleColors.GREEN + ".");

        updateStats();


        return jdas.get(0);
    }

    public void stopBot() {
        if (!running || shardManager == null) {
            return;
        }
        shardManager.shutdown();
        running = false;
        shardManager = null;
    }


    private void updateStats() {
        DiscordBotListAPI api = new DiscordBotListAPI.Builder().token(getConfig().getToken()).botId(getConfig().getBotID()).build();

        try {
            //Top.gg
            int guildsShards = 0;


            for (JDA shard : jdas) {
                guildsShards += shard.getGuilds().size();
            }


            api.setStats(guildsShards);
            Logger.log(LogType.OK, "Updated bot stats on top.gg");
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error while updating bot stats on top.gg: " + e.getMessage());
        }


        try {
            int guildsShards = 0;
            int amountShards = 0;

            for (JDA shard : jdas) {
                guildsShards += shard.getGuilds().size();
            }

            for (JDA shard : jdas) {
                amountShards = shard.getShardManager().getShardsTotal();
                break;
            }

            String token = "VOID_V3imKsNSyhsDEWoJJTTgRFKL5cYPtrQlWO6aYLuMUTtjdIkG";
            String botId = getConfig().getBotID();

            String url = "https://api.voidbots.net/bot/stats/" + botId;
            String jsonData = "{\"server_count\": " + guildsShards + ", \"shard_count\": " + amountShards + "}";

            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(jsonData.getBytes());
            outputStream.flush();
            outputStream.close();

            BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = responseReader.readLine()) != null) {
                response.append(line);
            }
            responseReader.close();

            Logger.log(LogType.OK, "Updated bot stats on VoidBots");

            connection.disconnect();
        } catch (IOException e) {
            Logger.log(LogType.ERROR, "Error while updating bot stats on VoidBots: " + e.getMessage());
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        try {
            updateStats();
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error while updating bot stats onGuildJoin: " + e.getMessage());
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        try {
            updateStats();
        } catch (Exception e) {
            Logger.log(LogType.ERROR, "Error while updating bot stats onGuildLeave: " + e.getMessage());
        }
    }


    public ShardManager getShardManager() {
        return shardManager;
    }

    private void loadIntents() {
        gatewayIntents.addAll(List.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.SCHEDULED_EVENTS));
    }

    private void loadCacheFlags() {
        enabledCacheFlags.addAll(List.of(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE));
    }

    public List<GatewayIntent> getGatewayIntents() {
        return gatewayIntents;
    }

    public Core addGatewayIntents(GatewayIntent... intents) {
        this.getGatewayIntents().addAll(List.of(intents));
        return this;
    }

    public List<CacheFlag> getEnabledCacheFlags() {
        return enabledCacheFlags;
    }

    public Core addEnabledCacheFlags(CacheFlag... flags) {
        this.getEnabledCacheFlags().addAll(List.of(flags));
        return this;
    }

    public List<CacheFlag> getDisabledCacheFlags() {
        return disabledCacheFlags;
    }

    public void addDisabledCacheFlags(CacheFlag... flags) {
        this.getDisabledCacheFlags().addAll(List.of(flags));
    }

    public PrefixCommands getPrefixCommands() {
        return prefixCommands;
    }

    /**
     * Connects a MySQL database to Core.
     */
    private void mysqlInit() throws SQLException {
        mySQL = new MySQL(this.config.getDB_Host(), this.config.getDB_Port(), this.config.getDB_Database(), this.config.getDB_User(), this.config.getDB_Password());
        try {
            mySQL.connect();
            Logger.log(LogType.OK, "Database connection successful.");
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.log(LogType.ERROR, "Error while trying to connect to database. Try reloading maven project.");
            return;
        }

        if (mySQL.checkConnection(0)) {
            DatabaseMetaData dbm = mySQL.getConnection().getMetaData();
            ResultSet tables = dbm.getTables(null, null, "guilds", null);
            if (tables.next()) {
                loadMySQLGuilds();
                return;
            }
            String table = "CREATE TABLE guilds ( guild_id varchar(255) primary key, music_channel varchar(255) )";
            PreparedStatement preparedStatement = mySQL.getConnection().prepareStatement(table);
            preparedStatement.execute();
            loadMySQLGuilds();
        }
    }

    private void loadMySQLGuilds() throws SQLException {

        for (JDA shard : jdas) {
            shard.getGuilds().forEach(guild -> {
                try {
                    PreparedStatement preparedStatement = mySQL.getConnection().prepareStatement("SELECT * FROM guilds WHERE guild_id = ?");
                    preparedStatement.setString(1, guild.getId());
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        if ((rs.getString(1) == null || rs.getString(1).isEmpty()) || (rs.getString(2) == null || rs.getString(2).isEmpty())) {
                            return;
                        }
                        guildsMusicChannel.put(guild, guild.getTextChannelById(rs.getString(2)));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public MySQL getMySQL() {
        return mySQL;
    }

    public Map<String, IExecutor> getExecutors() {
        return executorMap;
    }

    public Core addExecutor(IExecutor... executors) {
        for (IExecutor executor : executors) {
            if (executor.getName() == null || executor.getName().isEmpty()) {
                Logger.log(LogType.WARNING, "Command: '" + executor.getClass().getSimpleName() + "' doesn't have a name and could cause errors.");
            }
            if (executor.getDescription() == null || executor.getDescription().isEmpty()) {
                Logger.log(LogType.WARNING, "Command: '" + executor.getClass().getName() + "' doesn't have a description.");
            }
            this.executorMap.put(executor.getName(), executor);
            if (executor.getAliases() != null && !executor.getAliases().isEmpty()) {
                for (String alias : executor.getAliases()) {
                    if (alias.isEmpty()) {
                        Logger.log(LogType.WARNING, "Alias: '" + executor.getClass().getSimpleName() + "' doesn't have a name and could cause errors.");
                    }
                    this.executorMap.put(alias, executor);
                }
            }
        }
        return this;
    }

    public Core clearExecutors() {
        this.executorMap.clear();
        return this;
    }

    /**
     * Used to debug executors. Serve to identify if the commands are registered to Discord correctly.
     */
    private void logCurrentExecutors() {

        for (JDA shard : jdas) {
            shard.retrieveCommands().queue(commands -> {
                Logger.log(LogType.EXECUTORS, ConsoleColors.BLUE_BOLD + "- Logging registered Executors");
                Logger.logNoType(ConsoleColors.BLUE_BOLD + "- [Slash]");
                for (Command command : commands) {
                    Logger.logNoType("/" + command.getName() + ConsoleColors.RESET + ":" + ConsoleColors.CYAN + command.getId());
                }
                //Logger.logNoType(ConsoleColors.BLUE_BOLD + "- [Prefix]");
                getExecutors().forEach((s, iExecutor) -> {
                    if (iExecutor instanceof PrefixExecutor) {
                        if (!iExecutor.getAliases().contains(s)) {
                            Logger.logNoType(getPrefixCommands().getPrefix() + s);
                        }
                    }
                });
            });
            break;
        }

    }

    /**
     * Updates all executors/commands to Discord Guild.
     */
    private void updateCommands() {


        List<CommandData> commands = new ArrayList<>();
        getExecutors().forEach((name, executor) -> {
            if (executor instanceof SlashExecutor) {
                SlashExecutor executor1 = (SlashExecutor) executor;
                commands.add(Commands.slash(name, executor1.getDescription()).addOptions(executor1.getOptions()));
            }
        });

        for (JDA shard : jdas) {
            shard.updateCommands().addCommands(commands).queue();
        }
    }

    public Core registerListeners(ListenerAdapter... listeners) {
        if (List.of(listeners).isEmpty()) {
            return this;
        } else {
            if (this.shardManager == null) {
                this.shardManager = (DefaultShardManager) DefaultShardManagerBuilder.createDefault(this.config.getToken()).build();
                // Configure any other settings for the shardManager if needed
            }

            ListenerAdapter[] var2 = listeners;
            int var3 = listeners.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                ListenerAdapter listener = var2[var4];
                this.shardManager.addEventListener(listener);
                Logger.log(LogType.LISTENERS, "Registered listener: " + listener.getClass().getSimpleName());
            }

            return this;
        }
    }


    public Map<Guild, Channel> getGuildsMusicChannel() {
        return guildsMusicChannel;
    }

    public Config getConfig() {
        return config;
    }

    public JDA getJDA() {
        return jda;
    }

    public TextChannel getLogChannel() {
        return logChannel;
    }

    public Logger getLogger() {
        return logger;
    }
}
