import { Collection, MongoClient } from 'mongodb';


class MongoManager {

    client: MongoClient;

    constructor(url: string) {
        this.client = new MongoClient(url);
    }

    connect() {
        return this.client.connect();
    }

    collection() {
        type Conversation = {
            userid: string,
            guildid: string,
            channelid: string,
            startTime: Date,
            endTime: null | Date,
            mutes: Date[],
            deafs: Date[]
        }

        const Conversations: Collection<Conversation> = this.client.db("TestBot").collection('conversations');
        return Conversations;
    }

    createConversation(userid: string, guildid: string, channelid: string, startTime: Date) {
        return this.collection().insertOne({
            userid: userid,
            guildid: guildid,
            channelid: channelid,
            startTime: startTime,
            endTime: null,
            mutes: [],
            deafs: []
        });
    }

    getLastConversation(userid: string, guildid: string) {
        return this.collection().findOne({
            userid: userid,
            guildid: guildid
        }, {
            sort: { startTime: -1 }
        });
    }

    setEndtime(userid: string, guildid: string, endTime: Date) {
        return this.collection().findOneAndUpdate({
            userid: userid,
            guildid: guildid
        }, {
            $set: { endTime: endTime }
        }, {
            sort: { startTime: -1 }
        });
    }

    addMuteToConversation(userid: string, guildid: string, muteTime: Date) {
        return this.collection().findOneAndUpdate({
            userid: userid,
            guildid: guildid
        }, {
            $push: {
                mutes: muteTime
            }
        }, {
            sort: { startTime: -1 }
        });
    }

    addDeafToConversation(userid: string, guildid: string, deafTime: Date) {
        return this.collection().findOneAndUpdate({
            userid: userid,
            guildid: guildid
        }, {
            $push: {
                deafs: deafTime
            }
        }, {
            sort: { startTime: -1 }
        });
    }

}

export default MongoManager;