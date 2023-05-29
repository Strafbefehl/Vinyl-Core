package rystudio.strafbefehl.vinyl;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
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

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Core {

    public JDA jda;

    private DefaultShardManagerBuilder builder;
    private static ShardManager shardManager;

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

    private Long millisStart;

    private static Config config;

    private TextChannel logChannel;

    private Logger logger;

    public Core(DefaultShardManagerBuilder builder) throws IOException {
        config = new Config();
        this.logger = new Logger(this);
        this.builder = builder;
        loadIntents();

        this.slashCommands = new SlashCommands(this);

        if(this.config.getUsePrefixCommands()) {
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

        if(usePrefixCommands) {
            this.prefixCommands = new PrefixCommands(this);
            getGatewayIntents().add(GatewayIntent.MESSAGE_CONTENT);
        }

        builder = DefaultShardManagerBuilder.create(token, gatewayIntents);
        builder.addEventListeners(slashCommands);
    }

    public JDA buildJDA() throws InterruptedException {


        builder.setEnabledIntents(gatewayIntents);
        builder.enableCache(enabledCacheFlags);
        builder.disableCache(disabledCacheFlags);

        shardManager = builder.build(); // Build the ShardManager instance
        jda = shardManager.getShardById(0); // Assign the JDA instance

        millisStart = System.currentTimeMillis();

        Logger.log(LogType.LISTENERS, jda.getRegisteredListeners().toString());

        if(this.config != null && this.config.getUseMysql()) {
            try {
                this.mysqlInit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        if(this.config != null && this.config.getUsePrefixCommands()) {
            this.jda.addEventListener(prefixCommands);
        }

        this.jda.addEventListener(slashCommands);

        updateCommands();
        logCurrentExecutors();
        Logger.log(LogType.OK, "Core (RyStudio) finished loading in " + ConsoleColors.GREEN_BOLD + (System.currentTimeMillis() - millisStart) + "ms" + ConsoleColors.GREEN + ".");
        return jda;
    }

    public static void initShardManager(ShardManager manager) {
        shardManager = manager;
    }

    public static ShardManager getShardManager() {
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

        if(mySQL.checkConnection(0)) {
            DatabaseMetaData dbm = mySQL.getConnection().getMetaData();
            ResultSet tables = dbm.getTables(null, null, "guilds", null);
            if(tables.next()) {
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

        if(jda.getGuilds().isEmpty()) {
            return;
        }

        PreparedStatement preparedStatement = mySQL.getConnection().prepareStatement("SELECT * FROM guilds");
        ResultSet rs = preparedStatement.executeQuery();
        while(rs.next()) {
            if((rs.getString(1) == null || rs.getString(1).isEmpty()) || (rs.getString(2) == null || rs.getString(2).isEmpty())) {
                continue;
            }
            if(jda.getGuilds().contains(jda.getGuildById(rs.getString(1)))) {
                Guild guild = jda.getGuildById(rs.getString(1));
                guildsMusicChannel.put(guild, guild.getTextChannelById(rs.getString(2)));
            }
        }
    }

    public MySQL getMySQL() {
        return mySQL;
    }

    public Map<String, IExecutor> getExecutors() { return executorMap; }

    public Core addExecutor(IExecutor... executors) {
        for (IExecutor executor : executors) {
            if(executor.getName() == null || executor.getName().isEmpty()) {
                Logger.log(LogType.WARNING, "Command: '" + executor.getClass().getSimpleName() + "' doesn't have a name and could cause errors.");
            }
            if(executor.getDescription() == null || executor.getDescription().isEmpty()) {
                Logger.log(LogType.WARNING, "Command: '" + executor.getClass().getName() + "' doesn't have a description.");
            }
            this.executorMap.put(executor.getName(), executor);
            if(executor.getAliases() != null && !executor.getAliases().isEmpty()) {
                for (String alias : executor.getAliases()) {
                    if(alias.isEmpty()) {
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

        List<Command> commands = jda.retrieveCommands().complete();
        Logger.log(LogType.EXECUTORS, ConsoleColors.BLUE_BOLD + "- Logging registered Executors");
        Logger.logNoType(ConsoleColors.BLUE_BOLD + "- [Slash]");
        for (Command command : commands) {
            Logger.logNoType("/" + command.getName() + ConsoleColors.RESET + ":" + ConsoleColors.CYAN + command.getId());
        }
        Logger.logNoType(ConsoleColors.BLUE_BOLD + "- [Prefix]");
        getExecutors().forEach((s, iExecutor) -> {
            if(iExecutor instanceof PrefixExecutor) {
                if(!iExecutor.getAliases().contains(s)) {
                    Logger.logNoType(getPrefixCommands().getPrefix() + s);
                }
            }
        });

    }

    /**
     * Updates all executors/commands to Discord Guild.
     */
    private void updateCommands() {
        List<CommandData> commands = new ArrayList<>();
        getExecutors().forEach((name, executor) -> {
            if(executor instanceof SlashExecutor) {
                SlashExecutor executor1 = (SlashExecutor) executor;
                commands.add(Commands.slash(name, executor1.getDescription()).addOptions(executor1.getOptions()));
            }
        });
        jda.updateCommands().addCommands(commands).queue();
    }

    public Core registerListeners(ListenerAdapter... listeners) {
        if (List.of(listeners).isEmpty()) {
            return this;
        } else {
            if (this.builder == null) {
                this.builder = DefaultShardManagerBuilder.createDefault(config.getToken());
                // Configure any other settings for the builder if needed
            }

            ListenerAdapter[] var2 = listeners;
            int var3 = listeners.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                ListenerAdapter listener = var2[var4];
                this.builder.addEventListeners(listener);
            }

            return this;
        }
    }




    public Map<Guild, Channel> getGuildsMusicChannel() {
        return guildsMusicChannel;
    }

    public static Config getConfig() {
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
