import { MongoClient } from 'mongodb';


class MongoManager {

    mclient: MongoClient;

    constructor(url: string) {
        this.mclient = new MongoClient(url);
    }

    connect() {
        return this.mclient.connect();
    }
}

export default MongoManager;