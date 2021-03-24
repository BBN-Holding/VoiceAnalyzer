package one.bbn.voiceanalyzer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class CommandListener extends ListenerAdapter {

    Mongo mongo;
    JSONObject config;

    public CommandListener(Mongo mongo, JSONObject config) {
        this.mongo = mongo;
        this.config = config;
    }

    public String getTime(Long ms) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(ms);
        return String.format("%02d Days %02d:%02d:%02d",
                calendar.get(Calendar.DAY_OF_MONTH) - 1, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    public long getSum(String[] data, String endtime) {
        long sum = 0;
        if (data == null) return 0;
        if (endtime == null) endtime = String.valueOf(System.currentTimeMillis());
        for (String dat : data) {
            if (dat.endsWith("-")) dat += endtime;
            sum += Long.parseLong(dat.split("-")[1]) - Long.parseLong(dat.split("-")[0]);
        }
        return sum;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equals("+help")) {
            event.getTextChannel().sendMessage(
                    new EmbedBuilder()
                            .setTitle("Help")
                            .addField("+stats", "Shows your own voice stats", true)
                            .addField("+statstop", "Shows the guild voice leaderboard", true)
                            .setFooter("Provided by BBN", "https://bbn.one/images/avatar.png")
                            .setTimestamp(Instant.now())
                            .build()).queue();
        } else if (event.getMessage().getContentRaw().startsWith("+statstop")) {
            String[] split = event.getMessage().getContentRaw().split(" ");
            int count = 10;
            if (split.length == 2) {
                int temp = Integer.parseInt(split[1]);
                if (temp > 0 && temp < 100) {
                    count = temp;
                }
            }
            int finalCount = count;
            event.getGuild().loadMembers().onSuccess(members -> {
                try {
                    JSONArray data = new JSONArray();
                    HashMap<Long, String> timetoid = new HashMap<>();

                    // Get all voice times
                    for (Member member : members) {
                        JSONObject memberjson = mongo.getMember(member.getId(), member.getGuild().getId());
                        if (memberjson.getJSONArray("conversations").length() != 0) {
                            JSONArray conversations = memberjson.getJSONArray("conversations");
                            long time = 0L;
                            for (int i = 0; i < conversations.length(); i++) {
                                JSONObject conversationobj = conversations.getJSONObject(i);
                                Conversation conversation = new Conversation(conversationobj);
                                if (conversationobj.has("startTime") && (conversationobj.has("endTime") || i == conversations.length() - 1)) {
                                    time += (Long.parseLong(conversation.getEndTime()) - Long.parseLong(conversation.getStartTime()));
                                    time -= getSum(conversation.getMuteTimes(), conversation.getEndTime());
                                    time -= getSum(conversation.getIdleTimes(), conversation.getEndTime());
                                    time -= getSum(conversation.getDeafTimes(), conversation.getEndTime());
                                    time -= getSum(conversation.getSleepTimes(), conversation.getEndTime());
                                }
                            }
                            timetoid.put(time, member.getId());
                        }
                    }
                    if (!timetoid.isEmpty()) {
                        // Sort and reverse the list
                        Set<Map.Entry<Long, String>> set = timetoid.entrySet();
                        List<Map.Entry<Long, String>> list = new ArrayList<>(set);
                        list.sort((Map.Entry.comparingByKey()));
                        Collections.reverse(list);

                        // Build outputstring, Build data object
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<Long, String> entry : list) {
                            if (list.indexOf(entry) < finalCount) {
                                Member member = event.getGuild().getMemberById(entry.getValue());
                                JSONObject memberjson = mongo.getMember(member.getId(), member.getGuild().getId());
                                data.put(memberjson.put("Tag", member.getUser().getAsTag()));
                                sb.append((list.indexOf(entry) + 1)).append(". ").append(member.getUser().getAsTag()).append(" - ").append(getTime(entry.getKey())).append("\n");
                            }
                        }

                        // Send final message
                        event.getTextChannel().sendMessage(
                                new EmbedBuilder()
                                        .setTitle("Statstop")
                                        .setDescription(sb.toString())
                                        .setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl(), event.getAuthor().getEffectiveAvatarUrl())
                                        .setImage("attachment://chart.png")
                                        .setFooter("Provided by BBN", "https://bbn.one/images/avatar.png")
                                        .setTimestamp(Instant.now())
                                        .build()
                        ).addFile(new PlotCreator().createStatstop(data), "chart.png").queue();
                    } else {
                        event.getTextChannel().sendMessage(
                                new EmbedBuilder()
                                        .setTitle("Statstop")
                                        .setDescription("There aren't any voice stats in this guild. Join a voice channel to start recording your stats.")
                                        .setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl(), event.getAuthor().getEffectiveAvatarUrl())
                                        .setFooter("Provided by BBN", "https://bbn.one/images/avatar.png")
                                        .setTimestamp(Instant.now())
                                        .setColor(Color.RED)
                                        .build()).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else if (event.getMessage().getContentRaw().startsWith("+stats")) {
            // Get Member
            Member member = event.getMember();
            if (event.getMessage().getMentionedMembers().size() == 1)
                member = event.getMessage().getMentionedMembers().get(0);
            else if (event.getMessage().getContentRaw().split(" ").length == 2) {
                try {
                    member = Objects.requireNonNull(event.getGuild().getMemberById(event.getMessage().getContentRaw().split(" ")[1]));
                } catch (Exception e) {
                    event.getTextChannel().sendMessage(
                            new EmbedBuilder()
                                    .setTitle("Statstop")
                                    .setDescription("I am not able to find the specified user.")
                                    .setAuthor(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl(), event.getAuthor().getEffectiveAvatarUrl())
                                    .setFooter("Provided by BBN", "https://bbn.one/images/avatar.png")
                                    .setTimestamp(Instant.now())
                                    .setColor(Color.RED)
                                    .build()).queue();
                    return;
                }
            }

            // Get Conversation Object
            JSONArray conversations = new JSONArray(mongo.getMember(member.getId(), event.getGuild().getId()).getJSONArray("conversations"));
            if (conversations.length() != 0) {
                // Get Field Values
                long connected = 0;
                long muted = 0;
                long deafed = 0;
                long idle = 0;
                long sleep = 0;
                for (int i = 0; i < conversations.length(); i++) {
                    JSONObject conversationobj = conversations.getJSONObject(i);
                    Conversation conversation = new Conversation(conversationobj);
                    if (conversationobj.has("startTime") && (conversationobj.has("endTime") || i == conversations.length() - 1)) {
                        connected += (double) (Long.parseLong(conversation.getEndTime()) - Long.parseLong(conversation.getStartTime()));
                        muted += getSum(conversation.getMuteTimes(), conversation.getEndTime());
                        deafed += getSum(conversation.getDeafTimes(), conversation.getEndTime());
                        idle += getSum(conversation.getIdleTimes(), conversation.getEndTime());
                        sleep += getSum(conversation.getSleepTimes(), conversation.getEndTime());
                    }
                }

                long total = connected - muted - deafed - idle - sleep;

                // Send final message
                long finalConnected = connected;
                long finalMuted = muted;
                long finalIdle = idle;
                long finalDeafed = deafed;
                long finalSleep = sleep;
                event.getTextChannel().sendMessage(
                        new EmbedBuilder()
                                .setTitle("Stats")
                                .setAuthor(member.getUser().getAsTag(), member.getUser().getEffectiveAvatarUrl(), member.getUser().getEffectiveAvatarUrl())
                                .addField("Conversations", String.valueOf(conversations.length()), true)
                                .addField("Time", getTime(finalConnected), true)
                                .addField("Muted", getTime(finalMuted), true)
                                .addField("Deafened", getTime(finalDeafed), true)
                                .addField("Idle", getTime(finalIdle), true)
                                .addField("Sleep", getTime(finalSleep), true)
                                .addField("Total", getTime(total), true)
                                .setImage("attachment://chart.png")
                                .setFooter("Provided by BBN", "https://bbn.one/images/avatar.png")
                                .setTimestamp(Instant.now())
                                .build()
                ).addFile(new PlotCreator().createStat(conversations), "chart.png").queue();
            } else {
                event.getTextChannel().sendMessage(
                        new EmbedBuilder()
                                .setTitle("Error")
                                .setColor(Color.RED)
                                .setDescription("There aren't any stats available at the moment. Please check back later.")
                                .setFooter("Provided by BBN", "https://bbn.one/images/avatar.png")
                                .setTimestamp(Instant.now())
                                .build()).queue();
            }
        }
    }
}
