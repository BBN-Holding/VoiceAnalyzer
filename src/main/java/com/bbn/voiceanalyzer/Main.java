package com.bbn.voiceanalyzer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        File configfile = new File("config.json");
        if (!configfile.exists()) {
            try {
                configfile.createNewFile();
                generateConfig(configfile, prepareConfig(new JSONObject(), false, configfile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(new String(Files.readAllBytes(Paths.get(configfile.getPath()))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        prepareConfig(jsonObject, true, configfile);
        Rethink rethink = new Rethink(jsonObject);
        rethink.connect();
        try {
            JDA jda = JDABuilder.createDefault(jsonObject.getString("BOT_TOKEN"), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .addEventListeners(new Voicelistener(rethink), new CommandListener(rethink)).build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject prepareConfig(JSONObject config, boolean exists, File configfile) {
        String[] names = new String[]{
                "BOT_TOKEN", "DB_IP", "DB_NAME", "DB_PORT", "DB_USER", "DB_PASSWORD"
        };
        if (!exists) {
            for (String name : names) {
                config.put(name, "");
            }
            return config;
        } else {
            for (String name : names) {
                if (!config.has(name)) {
                    generateConfig(configfile, prepareConfig(new JSONObject(), false, configfile));
                    return null;
                }
            }
        }
        return null;
    }

    public static void generateConfig(File configfile, JSONObject config) {
        try {
            Files.writeString(configfile.toPath(), config.toString());
            System.out.println("Config updated. Please fill the remaining Fields.");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
