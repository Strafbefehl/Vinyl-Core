package rystudio.strafbefehl.vinyl.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CreateShard extends ListenerAdapter {

    private static final String WEBHOOK_ID = "846411941035376660";
    private static final String WEBHOOK_TOKEN = "9MywTcbt3jFjFNZl8vDK3q4k7gCQ8sOpBBhGL4NCCsFsCInNob3X9e1WuaEec_NaC4rh";

    @Override
    public void onReady(ReadyEvent event) {


        int shardId = event.getJDA().getShardInfo().getShardId();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = now.format(formatter);

        TextChannel webhookChannel = event.getJDA().getTextChannelById(WEBHOOK_ID);
        if (webhookChannel != null) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Shard eingeloggt")
                    .setColor(0xEC1AF8)
                    .addField("Version", "1.0.0-Java", true)
                    .addField("ShardID", String.valueOf(shardId), true)
                    .addField("Zeitpunkt", formattedTime, true)
                    .setFooter("Vinyl by Strafbefehl", null);

            MessageEmbed embed = embedBuilder.build();

            webhookChannel.sendMessage((CharSequence) embed).queue();
        }
    }

}