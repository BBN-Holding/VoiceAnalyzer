package one.bbn.voiceanalyzer;

import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Mongo {

    MongoClient client;
    JSONObject config;

    public Mongo(JSONObject config) {
        this.config = config;
    }

    public void connect() {
        client = MongoClients.create("mongodb://" + config.getString("username") + ":" + config.getString("password") + "@localhost:" + config.get("port") + "/?authSource=admin&authMechanism=MONGODB-CR");
    }

    public void createMember(String userid, String guildid) {
        Document doc = new Document("userid", userid)
                .append("guildid", guildid)
                .append("conversations", "[]");
        client.getDatabase("VoiceAnalyzer").getCollection("members").insertOne(doc);
    }

    public JSONObject getMember(String userid, String guildid) {

        MongoCollection<Document> collection = client.getDatabase("VoiceAnalyzer").getCollection("members");

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("userid", userid);
        FindIterable<Document> it = collection.find(whereQuery);
        if (it.cursor().hasNext()) {
            return new JSONObject(it.cursor().next().toJson());
        } else {
            createMember(userid, guildid);
            return getMember(userid, guildid);
        }
    }

    public JSONObject getLastConversation(String userid, String guildid) {
        JSONArray arr = new JSONArray(getMember(userid, guildid).getString("conversations"));
        return arr.getJSONObject(arr.length() - 1);
    }

    public void startConversation(String userid, String guildid, String channel, String starttime) {
        JSONObject jsonObject = getMember(userid, guildid);
        JSONObject conversation = new Conversation(userid, guildid, channel, starttime).toJson();
        jsonObject.put("conversations", new JSONArray(jsonObject.getString("conversations")).put(conversation).toString());

        MongoCollection<Document> collection = client.getDatabase("VoiceAnalyzer").getCollection("members");

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("conversations", jsonObject.getString("conversations"));

        BasicDBObject updateObject = new BasicDBObject();
        updateObject.put("$set", updateFields);

        collection.updateOne(Filters.eq("userid", userid), updateObject);

    }

    public void setLastConversation(String userid, String guildid, Conversation conversation) {
        JSONObject jsonObject = getMember(userid, guildid);
        JSONArray arr = new JSONArray(jsonObject.getString("conversations"));
        arr.remove(arr.length() - 1);
        arr.put(conversation.toJson());
        jsonObject.put("conversations", arr.toString());

        MongoCollection<Document> collection = client.getDatabase("VoiceAnalyzer").getCollection("members");

        BasicDBObject updateFields = new BasicDBObject();
        updateFields.append("conversations", jsonObject.getString("conversations"));

        BasicDBObject updateObject = new BasicDBObject();
        updateObject.put("$set", updateFields);

        collection.updateOne(Filters.eq("userid", userid), updateObject);
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
            if (!conversation.getMuteTimes()[conversation.getMuteTimes().length - 1].endsWith("-")) {
                list.add(timestamp + "-");
            }
            mutes = list.toArray(String[]::new);
        } else
            mutes = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setMuteTimes(mutes);
        setLastConversation(userid, guildid, conversation);
    }

    public void setUnmuted(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getMuteTimes() != null) {
            if (conversation.getMuteTimes()[conversation.getMuteTimes().length - 1].endsWith("-")) {
                conversation.getMuteTimes()[conversation.getMuteTimes().length - 1] = conversation.getMuteTimes()[conversation.getMuteTimes().length - 1] + timestamp;
                setLastConversation(userid, guildid, conversation);
            }
        }
    }

    public void setDeafed(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] deafes;
        if (conversation.getDeafTimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getDeafTimes()));
            if (!conversation.getDeafTimes()[conversation.getDeafTimes().length - 1].endsWith("-")) {
                list.add(timestamp + "-");
            }
            deafes = list.toArray(String[]::new);
        } else
            deafes = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setDeafTimes(deafes);
        setLastConversation(userid, guildid, conversation);
    }

    public void setUndeafed(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getDeafTimes() != null) {
            if (conversation.getDeafTimes()[conversation.getDeafTimes().length - 1].endsWith("-")) {
                conversation.getDeafTimes()[conversation.getDeafTimes().length - 1] = conversation.getDeafTimes()[conversation.getDeafTimes().length - 1] + timestamp;
                setLastConversation(userid, guildid, conversation);
            }
        }
    }

    public void setAfk(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] afk;
        if (conversation.getIdleTimes() != null) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversation.getIdleTimes()));
            if (!conversation.getIdleTimes()[conversation.getIdleTimes().length - 1].endsWith("-")) {
                list.add(timestamp + "-");
            }
            afk = list.toArray(String[]::new);
        } else
            afk = Arrays.asList(timestamp + "-").toArray(String[]::new);

        conversation.setIdleTimes(afk);
        setLastConversation(userid, guildid, conversation);
    }

    public void setOnline(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));
        if (conversation.getIdleTimes() != null) {
            if (conversation.getIdleTimes()[conversation.getIdleTimes().length - 1].endsWith("-")) {
                conversation.getIdleTimes()[conversation.getIdleTimes().length - 1] = conversation.getIdleTimes()[conversation.getIdleTimes().length - 1] + timestamp;
                setLastConversation(userid, guildid, conversation);
            }
        }
    }

    public void setSleep(String userid, String guildid, String timestamp) {
        Conversation conversation = new Conversation(getLastConversation(userid, guildid));

        String[] sleep;
        if (conversation.getSleepTimes() != null) {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(conversation.getSleepTimes()));
            if (!conversation.getSleepTimes()[conversation.getSleepTimes().length - 1].endsWith("-")) {
                list.add(timestamp + "-");
            }
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
            if (conversation.getSleepTimes()[conversation.getSleepTimes().length - 1].endsWith("-")) {
                conversation.getSleepTimes()[conversation.getSleepTimes().length - 1] = conversation.getSleepTimes()[conversation.getSleepTimes().length - 1] + timestamp;
                setLastConversation(userid, guildid, conversation);
            }
        }
    }
}

