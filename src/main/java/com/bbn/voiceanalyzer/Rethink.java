package com.bbn.voiceanalyzer;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlOpFailedError;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Result;
import net.dv8tion.jda.api.entities.Member;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Rethink {

    private final RethinkDB r = RethinkDB.r;
    private Connection conn;
    private final JSONObject config;

    public Rethink(JSONObject config) {
        this.config = config;
    }

    public void connect() {
        try {
            conn = r.connection()
                    .hostname(config.getString("DB_IP"))
                    .db(config.getString("DB_NAME"))
                    .port(config.getInt("DB_PORT"))
                    .user(config.getString("DB_USER"), config.getString("DB_PASSWORD"))
                    .connect();
            System.out.println("DB CONNECTED");
            this.createTables();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DB CONNECTION FAILED");
        }
    }

    public void createTables() {
        try {
            r.dbCreate(config.getString("DB_NAME")).run(conn);
        } catch (ReqlOpFailedError e) {
            System.out.println(e.getMessage());
        }
        try {
            r.tableCreate("member").run(conn);
        } catch (ReqlOpFailedError e) {
            System.out.println(e.getMessage());
        }
    }

    public void createMember(Member member) {
        Result result = r.table("member").getAll(member.getUser().getId()).run(conn);
        boolean contained = false;
        while (result.hasNext()) {
            JSONObject jsonObject = new JSONObject((LinkedHashMap) result.next());
            if (jsonObject.getString("guildid").equals(member.getGuild().getId())) {
                if (jsonObject.getString("memberid").equals(member.getUser().getId())) {
                    contained = true;
                }
            }
        }
        if (!contained)
            r.table("member").insert(
                    r.hashMap("memberid", member.getUser().getId())
                            .with("guildid", member.getGuild().getId())
                            .with("lastConnectedTime", "0")
                            .with("connected", "0")
                            .with("connectedTimes", "0")
            ).run(conn);
    }

    public void setConnected(Member member, String connected) {
        setToMember("connected", connected, member);
    }

    public String getConnected(Member member) {
        return get(member).getString("connected");
    }

    public void setConnectedTimes(Member member, String connectedTimes) {
        setToMember("connectedTimes", connectedTimes, member);
    }

    public String getConnectedTimes(Member member) {
        return get(member).getString("connectedTimes");
    }

    public void setLastConnectedTime(Member member, String connectedTime) {
        setToMember("lastConnectedTime", connectedTime, member);
    }

    public String getLastConnectedTime(Member member) {
        return get(member).getString("lastConnectedTime");
    }

    public JSONObject get(Member member) {
        Result result = r.table("member").run(conn);
        while (result.hasNext()) {
            JSONObject jsonObject = new JSONObject((LinkedHashMap) result.next());
            if (jsonObject.getString("guildid").equals(member.getGuild().getId())) {
                if (jsonObject.getString("memberid").equals(member.getUser().getId())) {
                    return jsonObject;
                }
            }
        }
        createMember(member);
        return get(member);
    }

    public ArrayList<JSONObject> getAll(long guildid) {
        Result result = r.table("member").run(conn);
        ArrayList<JSONObject> all = new ArrayList<>();
        while (result.hasNext()) {
            JSONObject jsonObject = new JSONObject((LinkedHashMap) result.next());
            if (jsonObject.getString("guildid").equals(String.valueOf(guildid))) {
                all.add(jsonObject);
            }
        }
        return all;
    }

    public void setToMember(String key, String value, Member member) {
        Result result = r.table("member").run(conn);
        String id = "";
        while (result.hasNext()) {
            JSONObject jsonObject = new JSONObject((LinkedHashMap) result.next());
            if (jsonObject.getString("guildid").equals(member.getGuild().getId())) {
                if (jsonObject.getString("memberid").equals(member.getUser().getId())) {
                    id = jsonObject.getString("id");
                }
            }
        }
        if (!id.equals("")) {
            r.table("member").get(id).update(r.hashMap(key, value)).run(conn);
        } else {
            createMember(member);
            setToMember(key, value, member);
        }
    }
}
