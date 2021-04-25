package one.bbn.voiceanalyzer;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class VoiceListener extends ListenerAdapter {

    Mongo mongo;
    JSONObject config;

    public VoiceListener(Mongo mongo, JSONObject config) {
        this.mongo = mongo;
        this.config = config;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        mongo.getMember(event.getAuthor().getId(), event.getGuild().getId());
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        // Start conversation
        mongo.startConversation(event.getMember().getId(), event.getGuild().getId(), event.getChannelJoined().getId(), String.valueOf(System.currentTimeMillis()));
        if (event.getChannelJoined().getMembers().size() == 2) {
            for (Member member : event.getChannelJoined().getMembers()) {
                mongo.setOnline(member.getId(), member.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
            }
        }
        if (event.getChannelJoined().getMembers().size() == 1) {
            mongo.setAfk(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        // Stop conversation => Conversation Time, Members in Conversation
        mongo.stopConversation(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        if (event.getChannelLeft().getMembers().size() == 1) {
            mongo.setAfk(event.getChannelLeft().getMembers().get(0).getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    public void onGuildVoiceMute(@NotNull GuildVoiceMuteEvent event) {
        // Start/Stop mute on conversation => Mute Time
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (event.isMuted()) {
                    mongo.setMuted(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                } else {
                    mongo.setUnmuted(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
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
                    mongo.setDeafed(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                } else {
                    mongo.setUndeafed(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                }
            }
        }, 1000);
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        mongo.switchChannel(event.getMember().getId(), event.getGuild().getId(), event.getChannelJoined().getId());
    }

    @Override
    public void onUserUpdateOnlineStatus(@NotNull UserUpdateOnlineStatusEvent event) {
        // Start/Stop afk on conversation => AFK Time
        if (event.getMember().getVoiceState() != null) {
            if (event.getMember().getVoiceState().inVoiceChannel()) {
                if (event.getMember().getVoiceState().getChannel().getMembers().size() != 1) {
                    if (event.getNewOnlineStatus().equals(OnlineStatus.IDLE)) {
                        // Start afk
                        mongo.setAfk(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    } else if (event.getNewOnlineStatus().equals(OnlineStatus.ONLINE)) {
                        // Stop afk
                        mongo.setOnline(event.getMember().getId(), event.getGuild().getId(), String.valueOf(System.currentTimeMillis()));
                    }
                }
            }
        }
    }
}
