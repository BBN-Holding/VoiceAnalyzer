package com.bbn.voiceanalyzer;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.gen.exc.ReqlOpFailedError;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
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
        Cursor cursor = r.table("members").filter(row -> row.getField("userid").eq(userid)).map(ReqlExpr::toJson).run(conn);
        if (!cursor.hasNext()) {
            createMember(userid, guildid);
            return getMember(userid, guildid);
        } else {
            for (Object doc : cursor) {
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
        if (conversation.getDeaftimes() != null && conversation.getDeaftimes().length > 0 && conversation.getDeaftimes()[conversation.getDeaftimes().length - 1].endsWith("-"))
            setUndeafed(userid, guildid, timestamp);
        if (conversation.getMutetimes() != null && conversation.getMutetimes().length > 0 && conversation.getMutetimes()[conversation.getMutetimes().length - 1].endsWith("-"))
            setUnmuted(userid, guildid, timestamp);
        if (conversation.getIdletimes() != null && conversation.getIdletimes().length > 0 && conversation.getIdletimes()[conversation.getIdletimes().length - 1].endsWith("-"))
            setOnline(userid, guildid, timestamp);
        conversation = new Conversation(getLastConversation(userid, guildid));
        conversation.setEndtime(timestamp);
        setLastConversation(userid, guildid, conversation);
    }

    public void setMuted(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] mutes;
        if (conversation.getMutetimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getMutetimes()));
            list.add(timestamp + "-");
            mutes = list.toArray(String[]::new);
        } else
            mutes = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setMutetimes(mutes);
        setLastConversation(userid, guildid, conversation);
    }

    public void setUnmuted(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getMutetimes() != null) {
            conversation.getMutetimes()[conversation.getMutetimes().length - 1] = conversation.getMutetimes()[conversation.getMutetimes().length - 1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }

    public void setDeafed(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] deafes;
        if (conversation.getDeaftimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getDeaftimes()));
            list.add(timestamp + "-");
            deafes = list.toArray(String[]::new);
        } else
            deafes = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setDeaftimes(deafes);
        setLastConversation(userid, guildid, conversation);
    }

    public void setUndeafed(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getDeaftimes() != null) {
            conversation.getDeaftimes()[conversation.getDeaftimes().length - 1] = conversation.getDeaftimes()[conversation.getDeaftimes().length - 1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }

    public void setAfk(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] afk;
        if (conversation.getIdletimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getIdletimes()));
            list.add(timestamp + "-");
            afk = list.toArray(String[]::new);
        } else
            afk = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setIdletimes(afk);
        setLastConversation(userid, guildid, conversation);
    }

    public void setOnline(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getIdletimes() != null) {
            conversation.getIdletimes()[conversation.getIdletimes().length - 1] = conversation.getIdletimes()[conversation.getIdletimes().length - 1] + timestamp;
            setLastConversation(userid, guildid, conversation);
        }
    }
}
