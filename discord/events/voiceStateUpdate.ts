import { VoiceState } from "discord.js";
import MongoManager from "../../mongo";

module.exports = {
    name: 'voiceStateUpdate',
    once: false,
    execute(oldState: VoiceState, newState: VoiceState, mongomanager: MongoManager) {
        console.log(oldState.member?.id)
        console.log(newState.member?.id)
        if (oldState.channel && newState.channel) {
            if (oldState.channel.id !== newState.channel.id) {
                // Remove if
                if (newState.member) {
                    mongomanager.setEndtime(newState.member.id, newState.guild.id, new Date())
                    mongomanager.createConversation(newState.member.id, newState.guild.id, newState.channel.id, new Date())
                    console.log("move")
                }
            } 
            if (oldState.mute !== newState.mute) {
                // Remove if
                if (newState.member)
                mongomanager.addMuteToConversation(newState.member.id, newState.guild.id, new Date())
                console.log("mute")
            }  
            if (oldState.deaf !== newState.deaf) {
                // Remove if
                if (newState.member)
                mongomanager.addDeafToConversation(newState.member.id, newState.guild.id, new Date())
                console.log("deaf")
            }
        } else if (oldState.channel) {
            // Remove if
            if (newState.member)
                mongomanager.setEndtime(newState.member.id, newState.guild.id, new Date())
            console.log("leave")
        } else if (newState.channel) {
            // Remove if
            if (newState.member)
                mongomanager.createConversation(newState.member.id, newState.guild.id, newState.channel.id, new Date())
            console.log("join")
        }
    },
};