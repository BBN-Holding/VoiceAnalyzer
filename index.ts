import config from './config.json';

import MongoManager from './mongo';

import DiscordManager from './discord/discord';

const mongomanager = new MongoManager('mongodb://localhost:6776');
mongomanager.connect().then(() => {

    const services = [
        new DiscordManager(mongomanager, config.token)
    ]

    services.forEach(service => {
        new Promise(() => {
            service.init().then(() => {
                service.start();
            });
        });
    });

})
