package one.bbn.voiceanalyzer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.botblock.javabotblockapi.core.BotBlockAPI;
import org.botblock.javabotblockapi.requests.PostAction;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;

public class GuildListener extends ListenerAdapter {

    JSONObject config;
    BotBlockAPI api;

    public GuildListener(JSONObject config, BotBlockAPI api) {
        this.config = config;
        this.api = api;
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        push(event.getJDA());
        super.onGuildJoin(event);
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        push(event.getJDA());
        super.onGuildLeave(event);
    }

    public void push(JDA jda) {
        PostAction postAction = new PostAction("757998776286838845");
        try {
            postAction.postGuilds("757998776286838845", jda.getGuilds().size(), api);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
