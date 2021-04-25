package one.bbn.voiceanalyzer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class Conversation {

    private String userID;
    private String guildID;
    private String[] voiceChannel;
    private String startTime;
    private String endTime;
    private String[] muteTimes;
    private String[] deafTimes;
    private String[] idleTimes;

    public Conversation(String userid, String guildID, String voiceChannel, String startTime) {
        this.userID = userid;
        this.guildID = guildID;
        this.voiceChannel = Arrays.asList(voiceChannel).toArray(String[]::new);
        this.startTime = startTime;
    }

    public Conversation(JSONObject jsonObject) {
        Class c = this.getClass();
        try {
            for (Field field : c.getDeclaredFields()) {
                if (jsonObject.has(field.getName()))
                    if (field.getName().equals("voiceChannel")) {
                        String[] data;
                        if (jsonObject.get(field.getName()) instanceof String) {
                            data = Arrays.asList(jsonObject.get(field.getName())).toArray(String[]::new);
                        } else {
                            data = (String[]) jsonObject.get(field.getName());
                        }
                        field.set(this, data);
                    }
                    else if (!(jsonObject.get(field.getName()) instanceof JSONArray))
                        field.set(this, jsonObject.get(field.getName()));
                    else
                        field.set(this, jsonObject.getJSONArray(field.getName()).toList().toArray(String[]::new));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJson() {
        Class c = this.getClass();
        JSONObject jsonObject = new JSONObject();
        try {
            for (Field field : c.getDeclaredFields()) {
                jsonObject.put(field.getName(), field.get(this));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public void addVoiceChannel(String voicechannel) {
        ArrayList<String> vclist = new ArrayList(Arrays.asList(this.voiceChannel));
        vclist.add(voicechannel);
        this.voiceChannel = vclist.toArray(String[]::new);
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime == null ? String.valueOf(System.currentTimeMillis()) : endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String[] getMuteTimes() {
        return muteTimes;
    }

    public void setMuteTimes(String[] muteTimes) {
        this.muteTimes = muteTimes;
    }

    public String[] getDeafTimes() {
        return deafTimes;
    }

    public void setDeafTimes(String[] deafTimes) {
        this.deafTimes = deafTimes;
    }

    public String[] getIdleTimes() {
        return idleTimes;
    }

    public void setIdleTimes(String[] idleTimes) {
        this.idleTimes = idleTimes;
    }

}
