package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;

public class CommandListener extends ListenerAdapter {

    Rethink rethink;

    public CommandListener(Rethink rethink) {
        this.rethink = rethink;
    }

    public String getTime(Long ms) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(ms);
        String str = String.format("%02d Days %02d:%02d:%02d",
                calendar.get(Calendar.DAY_OF_MONTH) - 1, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
        return str;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equals("-statstop")) {
            HashMap<JSONObject, Long> times = new HashMap<>();
            event.getGuild().loadMembers().onSuccess(
                    members -> {
                        for (JSONObject jsonObject : rethink.getAll(event.getGuild().getIdLong())) {
                            Date lastlefttime = new Date();

                            long lastconnectedtime = Long.parseLong(jsonObject.getString("lastConnectedTime"));

                            String connected = jsonObject.getString("connected");
                            long connectednew = 0;
                            if (lastconnectedtime != 0) {
                                connectednew = lastlefttime.getTime() - lastconnectedtime;
                            }

                            long elapsedTime = Long.parseLong(connected) + connectednew;

                            times.put(jsonObject, elapsedTime);
                        }

                        Set<Map.Entry<JSONObject, Long>> set = times.entrySet();
                        List<Map.Entry<JSONObject, Long>> list = new ArrayList<>(set);
                        list.sort((Map.Entry.comparingByValue()));
                        Collections.reverse(list);

                        ArrayList<String> strings = new ArrayList<>();
                        for (Map.Entry<JSONObject, Long> entry : list) {
                            if (list.indexOf(entry) > 10) break;
                            JSONObject json = entry.getKey();

                            User user = event.getJDA().getUserById(json.getString("memberid"));
                            if (user == null) {
                                user = event.getJDA().retrieveUserById(json.getString("memberid")).complete();
                            }

                            strings.add(list.indexOf(entry), list.indexOf(entry)+". "+user.getAsTag() + " - " + getTime(entry.getValue()));
                        }

                        event.getChannel().sendMessage(new EmbedBuilder()
                                .setTitle("Voice Toplist")
                                .setDescription(String.join("\n", strings.toArray(String[]::new)))
                                .setFooter("Provided by BBN", "https://bigbotnetwork.com/images/avatar.png")
                                .setTimestamp(Instant.now())
                                .build()).queue();
                    }
            );

        } else if (event.getMessage().getContentRaw().startsWith("-stats")) {

            Member member = event.getMember();
            if (event.getMessage().getMentionedMembers().size() == 1)
                member = event.getMessage().getMentionedMembers().get(0);

            Date lastlefttime = new Date();
            long lastconnectedtime = Long.parseLong(rethink.getLastConnectedTime(member));

            String connected = rethink.getConnected(member);
            long connectednew = 0;
            if (lastconnectedtime != 0) {
                connectednew = lastlefttime.getTime() - lastconnectedtime;
            }

            long lastmutedtime = Long.parseLong(rethink.getLastMutedTime(member));

            String muted = rethink.getMuted(event.getMember());
            long mutednew = 0;
            if (event.getMember().getVoiceState() != null)
                if (event.getMember().getVoiceState().isMuted()) {
                    if (lastmutedtime != 0) {
                        mutednew = new Date().getTime() - lastmutedtime;
                    }
                }

            JSONObject jsonObject = rethink.get(member);

            event.getChannel().sendMessage(
                    new EmbedBuilder()
                            .setTitle("Voicestats")
                            .setAuthor(member.getUser().getAsTag(), member.getUser().getEffectiveAvatarUrl(),
                                    member.getUser().getEffectiveAvatarUrl())
                            .addField("Connected Times", jsonObject.getString("connectedTimes"), true)
                            .addField("Time Connected", getTime(Long.parseLong(connected) + connectednew), true)
                            .addField("Time Muted", getTime(Long.parseLong(muted) + mutednew), true)
                            .setFooter("Provided by BBN", "https://bigbotnetwork.com/images/avatar.png")
                            .setTimestamp(Instant.now())
                            .build()).queue();
        }
    }
}
