package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.Date;

public class VoiceListener extends ListenerAdapter {

    Rethink rethink;

    public VoiceListener(Rethink rethink) {
        this.rethink = rethink;
    }

    @Override
    public void onGenericGuildVoice(@Nonnull GenericGuildVoiceEvent tmpevent) {
        if (tmpevent instanceof GuildVoiceJoinEvent) {
            GuildVoiceJoinEvent event = ((GuildVoiceJoinEvent) tmpevent);
            rethink.setLastConnectedTime(event.getMember(), String.valueOf(System.currentTimeMillis()));
            rethink.setConnectedTimes(event.getMember(), String.valueOf(Long.parseLong(
                    rethink.getConnectedTimes(event.getMember())) + 1));
        } else if (tmpevent instanceof GuildVoiceLeaveEvent) {
            GuildVoiceLeaveEvent event = (GuildVoiceLeaveEvent) tmpevent;
            Date lastlefttime = new Date();
            long lastconnectedtime = Long.parseLong(rethink.getLastConnectedTime(event.getMember()));

            if (lastconnectedtime == 0) return;

            String connected = rethink.getConnected(event.getMember());
            long connectednew = lastlefttime.getTime() - lastconnectedtime;

            rethink.setConnected(event.getMember(), String.valueOf(Long.parseLong(connected) + connectednew));
            rethink.setLastConnectedTime(event.getMember(), String.valueOf(0));
        } else if (tmpevent instanceof GuildVoiceMuteEvent) {
            GuildVoiceMuteEvent event = ((GuildVoiceMuteEvent) tmpevent);
            if (event.isMuted()) {
                rethink.setLastMutedTime(event.getMember(), String.valueOf(System.currentTimeMillis()));
            } else {
                String muted = rethink.getMuted(event.getMember());
                long lastmutedtime = Long.parseLong(rethink.getLastMutedTime(event.getMember()));

                if (lastmutedtime == 0) return;

                long mutednew = new Date().getTime() - lastmutedtime;

                rethink.setMuted(event.getMember(), String.valueOf(Long.parseLong(muted) + mutednew));
                rethink.setLastMutedTime(event.getMember(), String.valueOf(0));
            }
        }
    }
}
