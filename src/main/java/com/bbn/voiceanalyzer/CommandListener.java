package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CommandListener extends ListenerAdapter {

    Rethink rethink;

    public CommandListener(Rethink rethink) {
        this.rethink = rethink;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("-stats")) return;

        Member member = event.getMember();
        if (event.getMessage().getMentionedMembers().size()==1) member = event.getMessage().getMentionedMembers().get(0);

        Date lastlefttime = new Date();
        long lastconnectedtime = Long.parseLong(rethink.getLastConnectedTime(member));

        String connected = rethink.getConnected(member);
        long connectednew = 0;
        if (lastconnectedtime!=0) {
            connectednew = lastlefttime.getTime() - lastconnectedtime;
        }

        JSONObject jsonObject = rethink.get(member);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(Long.parseLong(connected)+connectednew));

        calendar.set(Calendar.HOUR, calendar.get(Calendar.HOUR)-1);
        event.getChannel().sendMessage(
                new EmbedBuilder()
                        .setTitle("Voicestats")
                        .setAuthor(member.getUser().getAsTag(), member.getUser().getEffectiveAvatarUrl(), member.getUser().getEffectiveAvatarUrl())
                        .addField("Connected Times", jsonObject.getString("connectedTimes"), true)
                        .addField("Time Connected", DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.GERMAN).format(calendar.getTime()), true)
                        .build()).queue();
    }

}
