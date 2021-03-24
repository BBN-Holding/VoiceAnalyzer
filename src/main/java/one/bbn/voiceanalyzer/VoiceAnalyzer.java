package one.bbn.voiceanalyzer;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class VoiceAnalyzer {

    public static void main(String[] args) {
        try {
            File file = new File("config.json");
            if (!file.exists()) {
                file.createNewFile();
                Files.writeString(file.toPath(), "{\"token\":\"\"}");
                System.err.println("Please fill your config.json!");
                System.exit(0);
            }

            JSONObject config = new JSONObject(new String(Files.readAllBytes(new File("config.json").toPath())));

            Mongo mongo = new Mongo(config);
            mongo.connect();

            JDABuilder.create(config.getString("token"), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .addEventListeners(new VoiceListener(mongo, config), new CommandListener(mongo, config))
                    .setActivity(Activity.listening("Voice Channels"))
                    .build();

        } catch (IOException | LoginException e) {
            e.printStackTrace();
        }
    }

}
