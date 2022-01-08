import { MongoClient } from 'mongodb';


class MongoManager {

    mclient: MongoClient;

    constructor(url: string) {
        this.mclient = new MongoClient(url);
    }

    connect() {
        return this.mclient.connect();
    }

    collection() {
        return this.mclient.db('TestBot').collection('conversations');
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
            // @ts-ignore
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
            // @ts-ignore
            $push: {
                deafs: deafTime
            }
        }, {
            sort: { startTime: -1 }
        });
    }

}

export default MongoManager;