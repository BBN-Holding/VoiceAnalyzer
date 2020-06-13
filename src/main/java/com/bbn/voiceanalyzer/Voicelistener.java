package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.Date;

public class Voicelistener extends ListenerAdapter {

    Rethink rethink;

    public Voicelistener(Rethink rethink) {
        this.rethink = rethink;
    }

    @Override
    public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent event) {
        if (event instanceof GuildVoiceJoinEvent) {
            rethink.setLastConnectedTime(((GuildVoiceJoinEvent) event).getMember(), String.valueOf(System.currentTimeMillis()));
            rethink.setConnectedTimes(((GuildVoiceJoinEvent) event).getMember(), String.valueOf(Long.parseLong(
                    rethink.getConnectedTimes(((GuildVoiceJoinEvent) event).getMember()))+1));
        } else if (event instanceof GuildVoiceLeaveEvent) {
            Date lastlefttime = new Date();
            long lastconnectedtime = Long.parseLong(rethink.getLastConnectedTime(((GuildVoiceLeaveEvent) event).getMember()));

            if (lastconnectedtime==0) return;

            String connected = rethink.getConnected(((GuildVoiceLeaveEvent) event).getMember());
            long connectednew = lastlefttime.getTime() - lastconnectedtime;

            rethink.setConnected(((GuildVoiceLeaveEvent) event).getMember(), String.valueOf(Long.parseLong(connected)+connectednew));
            rethink.setLastConnectedTime(((GuildVoiceLeaveEvent) event).getMember(), String.valueOf(0));
        } else if (event instanceof GuildVoiceMuteEvent) {

        } else if (event instanceof GuildVoiceDeafenEvent) {

        }
    }
}
