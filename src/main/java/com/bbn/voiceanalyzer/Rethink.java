package com.bbn.voiceanalyzer;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.gen.exc.ReqlOpFailedError;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Result;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Rethink {

    public static final RethinkDB r = RethinkDB.r;

    Connection conn;


    public void connect() {
        conn = r.connection().db("VCA").hostname("localhost").port(28015).connect();
        createTables();
    }

    public void createTables() {
        try {
            r.dbCreate("VCA").run(conn);
            r.tableCreate("members").run(conn);
        } catch (ReqlOpFailedError error) {
            System.out.println(error.getMessage());
        }
    }

    public void createMember(String userid, String guildid) {
        r.table("members").insert(r.hashMap("userid", userid).with("guildid", guildid).with("conversations", "[]")).run(conn);
    }

    public JSONObject getMember(String userid, String guildid) {
        Result result = r.table("members").filter(row -> row.getField("userid").eq(userid)).map(ReqlExpr::toJson).run(conn);
        if (!result.hasNext()) {
            createMember(userid, guildid);
            return getMember(userid, guildid);
        } else {
            for (Object doc : result) {
                return new JSONObject(String.valueOf(doc));
            }
        }
        return null;
    }

    public JSONObject getLastConversation(String userid, String guildid) {
        JSONArray arr = new JSONArray(getMember(userid, guildid).getString("conversations"));
        return arr.getJSONObject(arr.length() - 1);
    }

    public void startConversation(String userid, String guildid, String channel, String starttime) {
        JSONObject jsonObject = getMember(userid, guildid);
        JSONObject conversation = new Conversation(userid, guildid, channel, starttime).toJson();
        jsonObject.put("conversations", new JSONArray(jsonObject.getString("conversations")).put(conversation).toString());
        r.table("members")
                .get(jsonObject.getString("id"))
                .update(r.hashMap("conversations", jsonObject.getString("conversations")))
                .optArg("non_atomic", true).run(conn);
    }

    public void setLastConversation(String userid, String guildid, Conversation conversation) {
        JSONObject jsonObject = getMember(userid, guildid);
        JSONArray arr = new JSONArray(jsonObject.getString("conversations"));
        arr.remove(arr.length() - 1);
        arr.put(conversation.toJson());
        jsonObject.put("conversations", arr.toString());
        r.table("members")
                .get(jsonObject.getString("id"))
                .update(r.hashMap("conversations", jsonObject.getString("conversations")))
                .optArg("non_atomic", true).run(conn);
    }

    public void stopConversation(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getDeafTimes() != null && conversation.getDeafTimes().length > 0 && conversation.getDeafTimes()[conversation.getDeafTimes().length - 1].endsWith("-"))
            setUndeafed(userid, guildid, timestamp);
        if (conversation.getMuteTimes() != null && conversation.getMuteTimes().length > 0 && conversation.getMuteTimes()[conversation.getMuteTimes().length - 1].endsWith("-"))
            setUnmuted(userid, guildid, timestamp);
        if (conversation.getIdleTimes() != null && conversation.getIdleTimes().length > 0 && conversation.getIdleTimes()[conversation.getIdleTimes().length - 1].endsWith("-"))
            setOnline(userid, guildid, timestamp);
        if (conversation.getSleepTimes() != null && conversation.getSleepTimes().length > 0 && conversation.getSleepTimes()[conversation.getSleepTimes().length - 1].endsWith("-"))
            setAwake(userid, guildid, timestamp);
        conversation = new Conversation(getLastConversation(userid, guildid));
        conversation.setEndTime(timestamp);
        setLastConversation(userid, guildid, conversation);
    }

    public void setMuted(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] mutes;
        if (conversation.getMuteTimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getMuteTimes()));
            list.add(timestamp + "-");
            mutes = list.toArray(String[]::new);
        } else
            mutes = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setMuteTimes(mutes);
        setLastConversation(userid, guildid, conversation);
    }

    public void setUnmuted(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getMuteTimes() != null) {
            conversation.getMuteTimes()[conversation.getMuteTimes().length - 1] = conversation.getMuteTimes()[conversation.getMuteTimes().length - 1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }

    public void setDeafed(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] deafes;
        if (conversation.getDeafTimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getDeafTimes()));
            list.add(timestamp + "-");
            deafes = list.toArray(String[]::new);
        } else
            deafes = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setDeafTimes(deafes);
        setLastConversation(userid, guildid, conversation);
    }

    public void setUndeafed(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getDeafTimes() != null) {
            conversation.getDeafTimes()[conversation.getDeafTimes().length - 1] = conversation.getDeafTimes()[conversation.getDeafTimes().length - 1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }

    public void setAfk(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] afk;
        if (conversation.getIdleTimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getIdleTimes()));
            list.add(timestamp + "-");
            afk = list.toArray(String[]::new);
        } else
            afk = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setIdleTimes(afk);
        setLastConversation(userid, guildid, conversation);
    }

    public void setOnline(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getIdleTimes() != null) {
            conversation.getIdleTimes()[conversation.getIdleTimes().length - 1] = conversation.getIdleTimes()[conversation.getIdleTimes().length - 1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }

    public void setSleep(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] sleep;
        if (conversation.getIdleTimes() != null) {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(conversation.getSleepTimes()));
            list.add(timestamp + "-");
            sleep = list.toArray(String[]::new);
        } else {
            sleep = Arrays.asList(timestamp + "-").toArray(String[]::new);
        }
        conversation.setSleepTimes(sleep);
        setLastConversation(userid, guildid, conversation);
    }

    public void setAwake(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getSleepTimes() != null) {
            conversation.getSleepTimes()[conversation.getSleepTimes().length-1] = conversation.getSleepTimes()[conversation.getSleepTimes().length-1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }
}
