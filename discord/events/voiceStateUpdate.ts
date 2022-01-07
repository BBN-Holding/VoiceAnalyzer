import { VoiceState } from "discord.js";
import { MongoClient } from "mongodb";

module.exports = {
    name: 'voiceStateUpdate',
    once: false,
    execute(oldState: VoiceState, newState: VoiceState, mclient: MongoClient) {
        const collection = mclient.db("VoiceAnalyzer").collection("members-dev")
        console.log(oldState.member?.id)
        console.log(newState.member?.id)
        if (oldState.channel && newState.channel) {
            collection.insertOne({ "userID": newState.member?.id })
            console.log("move")
        } else if (oldState.channel) {
            collection.insertOne({ "userID": oldState.member?.id })
            console.log("leave")
        } else {
            collection.insertOne({ "userID": newState.member?.id })
            console.log("join")
        }
    },
};