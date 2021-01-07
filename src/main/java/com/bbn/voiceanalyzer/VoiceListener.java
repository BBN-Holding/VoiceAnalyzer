package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class VoiceListener extends ListenerAdapter {

    Rethink rethink;
    JSONObject config;

    public VoiceListener(Rethink rethink, JSONObject config) {
        this.rethink = rethink;
        this.config = config;
    }

    @Override
    public void onVoiceChannelUpdateName(@NotNull VoiceChannelUpdateNameEvent event) {
        if (!event.getOldName().endsWith(" - Sleep") && event.getNewName().endsWith(" - Sleep")) {
            for (Member member : event.getChannel().getMembers()) {
                rethink.setSleep(member.getId(), member.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set sleep for " + member.getUser().getAsTag()).queue();
            }
        }
        if (event.getOldName().endsWith(" - Sleep") && !event.getNewName().endsWith(" - Sleep")) {
            for (Member member : event.getChannel().getMembers()) {
                rethink.setAwake(member.getId(), member.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set awake for " + member.getUser().getAsTag()).queue();
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        rethink.getMember(event.getAuthor().getId(), event.getGuild().getId());
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        // Start conversation
        rethink.startConversation(event.getMember().getId(), event.getGuild().getId(), event.getChannelJoined().getId(), String.valueOf(System.currentTimeMillis()));
        event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Started Conversation of " + event.getMember().getUser().getAsTag()).queue();
        if (event.getChannelJoined().getMembers().size()==2) {
            for (Member member : event.getChannelJoined().getMembers()) {
                rethink.setOnline(member.getId(), member.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
            }
        }
        if (event.getChannelJoined().getMembers().size()==1) {
            rethink.setAfk(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        // Stop conversation => Conversation Time, Members in Conversation
        event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Stopped Conversation of " + event.getMember().getUser().getAsTag()).queue();
        rethink.stopConversation(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        if (event.getChannelLeft().getMembers().size()==1) {
            rethink.setAfk(event.getChannelLeft().getMembers().get(0).getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    public void onGuildVoiceMute(@NotNull GuildVoiceMuteEvent event) {
        // Start/Stop mute on conversation => Mute Time
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (event.isMuted()) {
                    rethink.setMuted(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set muted of " + event.getMember().getUser().getAsTag()).queue();
                } else {
                    rethink.setUnmuted(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set unmuted of " + event.getMember().getUser().getAsTag()).queue();
                }
            }
        }, 2500);
    }

    @Override
    public void onGuildVoiceDeafen(@NotNull GuildVoiceDeafenEvent event) {
        // Start/Stop deaf on conversation => Deaf Time
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (event.isDeafened()) {
                    rethink.setDeafed(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set deafen of " + event.getMember().getUser().getAsTag()).queue();
                } else {
                    rethink.setUndeafed(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set undeafen of " + event.getMember().getUser().getAsTag()).queue();
                }
            }
        }, 1000);
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        // Stop and Start the conversation
        event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Stopped Conversation of " + event.getMember().getUser().getAsTag()).queue();
        rethink.stopConversation(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        if (event.getChannelLeft().getMembers().size()==1) {
            rethink.setAfk(event.getChannelLeft().getMembers().get(0).getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                rethink.startConversation(event.getMember().getId(), event.getGuild().getId(), event.getChannelJoined().getId(), String.valueOf(System.currentTimeMillis()));
                event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Started Conversation of " + event.getMember().getUser().getAsTag()).queue();
                if (event.getChannelJoined().getMembers().size()==2) {
                    for (Member member : event.getChannelJoined().getMembers()) {
                        rethink.setOnline(member.getId(), member.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    }
                } else if (event.getChannelJoined().getMembers().size()==1) {
                    rethink.setAfk(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                }
            }
        }, 1000);
    }

    @Override
    public void onUserUpdateOnlineStatus(@NotNull UserUpdateOnlineStatusEvent event) {
        // Start/Stop afk on conversation => AFK Time
        if (event.getMember().getVoiceState() != null) {
            if (event.getMember().getVoiceState().inVoiceChannel()) {
                if (event.getNewOnlineStatus().equals(OnlineStatus.IDLE)) {
                    // Start afk
                    event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set afk of " + event.getMember().getUser().getAsTag()).queue();
                    rethink.setAfk(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                } else if (event.getNewOnlineStatus().equals(OnlineStatus.ONLINE)) {
                    // Stop afk
                    rethink.setOnline(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    event.getGuild().getTextChannelById(config.getString("channel")).sendMessage("Set online of " + event.getMember().getUser().getAsTag()).queue();
                }
            }
        }
    }
}
