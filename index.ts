import { REST } from '@discordjs/rest';
import { Routes } from 'discord-api-types/v9';
import { token } from "./config.json";
import fs from "fs";

const commands = [ {
    name: 'ping',
    description: 'Replies with Pong!'
} ];

const rest = new REST({ version: '9' }).setToken(token);

(async () => {
    try {
        console.log('Started refreshing application (/) commands.');

        await rest.put(
            Routes.applicationCommands("715897534874386473"),
            { body: commands },
        );

        console.log('Successfully reloaded application (/) commands.');
    } catch (error) {
        console.error(error);
    }
})();

import { MongoClient } from 'mongodb';
import { Client, Intents } from 'discord.js';

const client = new Client({ intents: [ Intents.FLAGS.GUILDS, Intents.FLAGS.GUILD_VOICE_STATES ] });

const url = 'mongodb://localhost:6776';
const mclient = new MongoClient(url);

async function connect() {
    await mclient.connect();
    console.log('Connected successfully to Mongo server');
}

const eventFiles = fs.readdirSync('./events');

for (const file of eventFiles) {
    const event = require(`./events/${file}`);
    if (event.once) {
        client.once(event.name, (...args) => event.execute(...args));
    } else {
        client.on(event.name, (...args) => event.execute(...args, mclient));
    }
}

client.on('interactionCreate', async (interaction: any) => {
    if (!interaction.isCommand()) return;

    if (interaction.commandName === 'ping') {
        await interaction.reply('Pong!');
    }
});

connect();
client.login(token);
