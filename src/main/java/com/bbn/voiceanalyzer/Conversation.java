package com.bbn.voiceanalyzer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;

public class Conversation {

    private String userid;
    private String guildid;
    private String voicechannel;
    private String starttime;
    private String endtime;
    private String[] mutetimes;
    private String[] deaftimes;
    private String[] idletimes;

    public Conversation(String userid, String guildid, String voicechannel, String starttime) {
        this.userid = userid;
        this.guildid = guildid;
        this.voicechannel = voicechannel;
        this.starttime = starttime;
    }

    public Conversation(JSONObject jsonObject) {
        Class c = this.getClass();
        try {
            for (Field field : c.getDeclaredFields()) {
                if (jsonObject.has(field.getName()))
                    if (!(jsonObject.get(field.getName()) instanceof JSONArray))
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

    public String getUserid() {
        return userid;
    }

    public String getGuildid() {
        return guildid;
    }

    public String getVoicechannel() {
        return voicechannel;
    }

    public String getStarttime() {
        return starttime;
    }

    public String getEndtime() {
        return endtime == null ? String.valueOf(System.currentTimeMillis()) : endtime;
    }

    public void setEndtime(String endtime) {
        this.endtime = endtime;
    }

    public String[] getMutetimes() {
        return mutetimes;
    }

    public void setMutetimes(String[] mutetimes) {
        this.mutetimes = mutetimes;
    }

    public String[] getDeaftimes() {
        return deaftimes;
    }

    public void setDeaftimes(String[] deaftimes) {
        this.deaftimes = deaftimes;
    }

    public String[] getIdletimes() {
        return idletimes;
    }

    public void setIdletimes(String[] idletimes) {
        this.idletimes = idletimes;
    }
}
