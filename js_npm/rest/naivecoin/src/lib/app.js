const HttpServer = require('./httpServer');
const Blockchain = require('./blockchain');
const Operator = require('./operator');
const Miner = require('./miner');
const Node = require('./node');

const app = function app(host, port) {
    host = process.env.HOST || host || 'localhost';
    port = process.env.PORT || process.env.HTTP_PORT || port || 3001;
    peers = (process.env.PEERS ? process.env.PEERS.split(',') :  []);
    peers = peers.map((peer) => { return { url: peer }; });
    logLevel = (process.env.LOG_LEVEL ? process.env.LOG_LEVEL : 6);
    name = process.env.NAME  || '1';

    require('./util/consoleWrapper.js')(name, logLevel);

    console.info(`Starting node ${name}`);

    let blockchain = new Blockchain(name);
    let operator = new Operator(name, blockchain);
    let miner = new Miner(blockchain, logLevel);
    let node = new Node(host, port, peers, blockchain);
    let httpServer = new HttpServer(node, blockchain, operator, miner);

    //httpServer.listen(host, port);
    return httpServer;
}

const reset = function app(httpServer) {
    peers = (process.env.PEERS ? process.env.PEERS.split(',') :  []);
    peers = peers.map((peer) => { return { url: peer }; });
    logLevel = (process.env.LOG_LEVEL ? process.env.LOG_LEVEL : 6);
    name = process.env.NAME  || '1';

    console.info(`resetting node ${name}`);

    httpServer.blockchain.reset();
    httpServer.operator.reset();
    httpServer.node.reset();

}

module.exports = {
    reset,
    app
};