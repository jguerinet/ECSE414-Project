#!/bin/env node
//  OpenShift sample Node application
var express = require('express');
var fs      = require('fs');
var mongojs = require('mongojs');
//var peer = require('./routes/peers');
var db;
/**
 *  Define the sample application.
 */
var SampleApp = function() {

    //  Scope.
    var self = this;


    /*  ================================================================  */
    /*  Helper functions.                                                 */
    /*  ================================================================  */

    /**
     *  Set up server IP address and port # using env variables/defaults.
     */
    self.setupVariables = function() {
        //  Set the environment variables we need.
        self.ipaddress = process.env.OPENSHIFT_NODEJS_IP;
        self.port      = process.env.OPENSHIFT_NODEJS_PORT || 8080;

        // default to a 'localhost' configuration:
        var connection_string = '127.0.0.1:27017/chat';
        // if OPENSHIFT env variables are present, use the available connection info:
        if(process.env.OPENSHIFT_MONGODB_DB_PASSWORD){
          connection_string = process.env.OPENSHIFT_MONGODB_DB_USERNAME + ":" +
          process.env.OPENSHIFT_MONGODB_DB_PASSWORD + "@" +
          process.env.OPENSHIFT_MONGODB_DB_HOST + ':' +
          process.env.OPENSHIFT_MONGODB_DB_PORT + '/' +
          process.env.OPENSHIFT_APP_NAME;
        }

        db = mongojs(connection_string, ['peers']);
        var peers = db.collection('peers');
        // similar syntax as the Mongo command-line interface
        // log each of the first ten docs in the collection
        db.peers.find({}).limit(10).forEach(function(err, doc) {
          if (err) throw err;
          if (doc) { console.dir(doc); }
        });
        

        if (typeof self.ipaddress === "undefined") {
            //  Log errors on OpenShift but continue w/ 127.0.0.1 - this
            //  allows us to run/test the app locally.
            console.warn('No OPENSHIFT_NODEJS_IP var, using 127.0.0.1');
            self.ipaddress = "127.0.0.1";
        };
    };


    /**
     *  Populate the cache.
     */
    self.populateCache = function() {
        if (typeof self.zcache === "undefined") {
            self.zcache = { 'index.html': '' };
        }

        //  Local cache for static content.
        self.zcache['index.html'] = fs.readFileSync('./index.html');
    };


    /**
     *  Retrieve entry (content) from cache.
     *  @param {string} key  Key identifying content to retrieve from cache.
     */
    self.cache_get = function(key) { return self.zcache[key]; };


    /**
     *  terminator === the termination handler
     *  Terminate server on receipt of the specified signal.
     *  @param {string} sig  Signal to terminate on.
     */
    self.terminator = function(sig){
        if (typeof sig === "string") {
           console.log('%s: Received %s - terminating sample app ...',
                       Date(Date.now()), sig);
           process.exit(1);
        }
        console.log('%s: Node server stopped.', Date(Date.now()) );
    };


    /**
     *  Setup termination handlers (for exit and a list of signals).
     */
    self.setupTerminationHandlers = function(){
        //  Process on exit and signals.
        process.on('exit', function() { self.terminator(); });

        // Removed 'SIGPIPE' from the list - bugz 852598.
        ['SIGHUP', 'SIGINT', 'SIGQUIT', 'SIGILL', 'SIGTRAP', 'SIGABRT',
         'SIGBUS', 'SIGFPE', 'SIGUSR1', 'SIGSEGV', 'SIGUSR2', 'SIGTERM'
        ].forEach(function(element, index, array) {
            process.on(element, function() { self.terminator(element); });
        });
    };


    /*  ================================================================  */
    /*  App server functions (main app logic here).                       */
    /*  ================================================================  */

    /**
     *  Create the routing table entries + handlers for the application.
     */
    self.createRoutes = function() {
        self.routes = { };

        self.routes['/asciimo'] = function(req, res) {
            var link = "http://i.imgur.com/kmbjB.png";
            res.send("<html><body><img src='" + link + "'></body></html>");
        };

        self.routes['/'] = function(req, res) {
            res.setHeader('Content-Type', 'text/html');
            res.send("Hello World like a bau5");
        };
        self.routes
    };


    /**
     *  Initialize the server (express) and create the routes and register
     *  the handlers.
     */
    self.initializeServer = function() {
        self.createRoutes();
        self.app = express();

        //  Add handlers for the app (from the routes).
        for (var r in self.routes) {
            self.app.get(r, self.routes[r]);
        }
        self.app.get('/test',function(req,res){
            console.log("test worked");
            res.send("sup");
        });
        self.app.get('/peers', function(req,res){
            console.log('Display all peers');
            db.peers.find({}).limit(10).forEach(function(err, doc) {
                if (err) throw err;
                if (doc) { console.dir(doc); }
            });
        });

        self.populateDB();
        /*self.app.get('/peers/:id', peer.findById);
        self.app.post('/peers', peer.addPeer);
        self.app.put('/peers/:id', peer.updatePeer);
        self.app.delete('/peers/:id', peer.deletePeer);
*/
    };

    /*self.app.get('/peers',function(){
        console.log("Got this PEERS function");
    });*/
    /**
     *  Initializes the sample application.
     */
    self.initialize = function() {
        self.setupVariables();
        self.populateCache();
        self.setupTerminationHandlers();

        // Create the express server and routes.
        self.initializeServer();
    };

    /*--------------------------------------------------------------------------------------------------------------------*/
    // Populate database with sample data -- Only used once: the first time the application is started.
    // You'd typically not find this code in a real-life app, since the database would already exist.
    self.populateDB = function() {
        console.log("populating db");
        var peers = [
        {
            name: "Cameron Bell",
            externalAddress: "69.143.546.34",
            externalPort: "34583",
            
        },
        {
            name: "Julien Guerinet",
            externalAddress: "69.142.966.234",
            externalPort: "58394",
        },
        {
            name: "Yulric Sequeira",
            externalAddress: "69.143.194.59",
            externalPort: "51283",
        }];

        db.peers.insert(peers, {safe:true}, function(err, result) {
                console.log('Error: ' + err);
                console.log('Result: ' + result);
        });
     
    };


    /**
     *  Start the server (starts up the sample application).
     */
    self.start = function() {
        //  Start the app on the specific interface (and port).
        self.app.listen(self.port, self.ipaddress, function() {
            console.log('%s: Node server started on %s:%d ...',
                        Date(Date.now() ), self.ipaddress, self.port);
        });
    };

};   /*  Sample Application.  */



/**
 *  main():  Main code.
 */
var zapp = new SampleApp();
zapp.initialize();
zapp.start();



