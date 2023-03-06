import { ActivityType, Client, REST, Routes } from 'discord.js'
import { token } from './config.json';
import MongoManager from './mongo';
import { voiceStateUpdate } from './voiceStateUpdate';

const client = new Client({ intents: [ 3244031 ] });

const mongo = new MongoManager('mongodb://localhost:6776');

client.on("ready", async () => {
    console.log(`Logged in as ${client.user!.tag}!`);
    client.user!.setActivity('bbn.one', { type: ActivityType.Listening });

    const rest = new REST({ version: '10' }).setToken(token);

    (async () => {
        try {
            console.log('Started refreshing application (/) commands.');

            await rest.put(Routes.applicationCommands(client.user!.id), {
                body:
                    [
                        {
                            name: 'ping',
                            description: 'Just some command to check if the bot is up and running',
                        },
                    ]
            });

            console.log('Successfully reloaded application (/) commands.');
        } catch (error) {
            console.error(error);
        }
    })();
});

client.on('voiceStateUpdate', (...args) => voiceStateUpdate(...args, mongo));

client.on('interactionCreate', (interaction) => {
    if (interaction.isCommand() && interaction.commandName == "ping") {
        interaction.reply("Online dies das")
    }
});


client.login(token);
