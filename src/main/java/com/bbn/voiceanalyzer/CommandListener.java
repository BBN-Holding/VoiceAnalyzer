package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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
                        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
                        ArrayList<String> strings = new ArrayList<>();
                        for (Map.Entry<JSONObject, Long> entry : list) {
                            if (list.indexOf(entry)>10) break;
                            JSONObject json = entry.getKey();

                            long elapsedSeconds = entry.getValue() / 1000;
                            long secondsDisplay = elapsedSeconds % 60;
                            long elapsedMinutes = elapsedSeconds / 60;
                            long minutesDisplay = elapsedMinutes % 60;
                            long elapsedHours = elapsedMinutes / 60;
                            long hoursDisplay = elapsedHours % 24;
                            long elapsedDays = elapsedHours / 24;

                            event.getJDA().retrieveUserById(json.getString("memberid")).queue(
                                    user -> {
                                        strings.add(user.getAsTag() + " - " + String.format("%02d Days %02d:%02d:%02d", elapsedDays, hoursDisplay, minutesDisplay, secondsDisplay));
                                        if (strings.size()==list.size() || list.size()>10 && strings.size()==10) {
                                            event.getChannel().sendMessage(new EmbedBuilder()
                                                    .setTitle("Voice Toplist")
                                                    .setDescription(String.join("\n", strings.toArray(String[]::new)))
                                                    .setFooter("Provided by BBN", "https://bigbotnetwork.com/images/avatar.png")
                                                    .setTimestamp(Instant.now())
                                                    .build()).queue();
                                        }
                                    }
                            );
                        }
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

            JSONObject jsonObject = rethink.get(member);
            long elapsedTime = Long.parseLong(connected) + connectednew;
            long elapsedSeconds = elapsedTime / 1000;
            long secondsDisplay = elapsedSeconds % 60;
            long elapsedMinutes = elapsedSeconds / 60;
            long minutesDisplay = elapsedMinutes % 60;
            long elapsedHours = elapsedMinutes / 60;
            long hoursDisplay = elapsedHours % 24;
            long elapsedDays = elapsedHours / 24;

            event.getChannel().sendMessage(
                    new EmbedBuilder()
                            .setTitle("Voicestats")
                            .setAuthor(member.getUser().getAsTag(), member.getUser().getEffectiveAvatarUrl(), member.getUser().getEffectiveAvatarUrl())
                            .addField("Connected Times", jsonObject.getString("connectedTimes"), true)
                            .addField("Time Connected", String.format("%02d Days %02d:%02d:%02d", elapsedDays, hoursDisplay, minutesDisplay, secondsDisplay), true)
                            .setFooter("Provided by BBN", "https://bigbotnetwork.com/images/avatar.png")
                            .setTimestamp(Instant.now())
                            .build()).queue();
        }
    }
}
