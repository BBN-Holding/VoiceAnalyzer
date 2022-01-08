import { REST } from '@discordjs/rest';
import { Routes } from 'discord-api-types/v9';
import fs from "fs";
import { Client, Intents } from 'discord.js';
import MongoManager from '../mongo';

class DiscordManager {

    mongomanager: MongoManager;
    token: string;
    constructor(mongomanager: MongoManager, token: string) {
        this.mongomanager = mongomanager;
        this.token = token;
    }

    init(): Promise<void> {
        return new Promise((resolve, reject) => {
            // TODO: Automatic command detection
            const commands = [{
                name: 'ping',
                description: 'Replies with Pong!'
            }];
            const rest = new REST({ version: '9' }).setToken(this.token);

            console.log('Started refreshing application (/) commands.');

            rest.put(
                Routes.applicationCommands("715897534874386473"),
                { body: commands },
            ).then(() => {
                console.log('Successfully reloaded application (/) commands.');
                resolve();
            }, reject);

        });
    }

    start() {
        const client = new Client({ intents: [Intents.FLAGS.GUILDS, Intents.FLAGS.GUILD_VOICE_STATES] });

        fs.readdir('./discord/events', (err, files) => {
            if (err) return console.error(err);
            files.forEach(file => {
                const event = require(`./events/${file}`);
                if (event.once) {
                    client.once(event.name, (...args) => event.execute(...args, this.mongomanager));
                } else {
                    client.on(event.name, (...args) => event.execute(...args, this.mongomanager));
                }
            });

            client.on('interactionCreate', async (interaction: any) => {
                if (!interaction.isCommand()) return;

                if (interaction.commandName === 'ping') {
                    await interaction.reply('Pong!');
                }
            });

            client.login(this.token);
        })
    }
}

export default DiscordManager;