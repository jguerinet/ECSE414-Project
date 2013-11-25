#!/bin/env node
//  OpenShift sample Node application
var express = require('express');
var fs      = require('fs');
var mongojs = require('mongojs');
var mongo = require('mongodb');
var BSON = mongo.BSONPure;
//var peer = require('./routes/peers');
var db;
var callDb;
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

        callDb = mongojs(connection_string,['calls']);
        var calls = callDb.collection('calls');

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

        

        self.routes['/'] = function(req, res) {
            res.setHeader('Content-Type', 'text/html');
            res.send("Hello World");
        };
        //self.routes
    };


    /**
     *  Initialize the server (express) and create the routes and register
     *  the handlers.
     */
    self.initializeServer = function() {
        self.createRoutes();
        self.app = express();

        self.app.configure(function () {
            self.app.use(express.logger('dev'));     /* 'default', 'short', 'tiny', 'dev' */
            self.app.use(express.bodyParser());
        });

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
            /*db.peers.find({}).limit(10).forEach(function(err, doc) {
                if (err) throw err;
                if (doc) { console.dir(doc);
                res }
            });*/
            db.peers.find(function(error,docs) {
                res.send(docs);
            });
        });
        
        self.app.get('/peers/:id',function(req,res){
            var id = req.params.id;
            console.log('Retrieving peer: ' + id);
            var findThisID;
            try 
            {
               findThisID = new BSON.ObjectID(id);
            }
            catch (err)
            {
               console.log(err);
            }
            
            db.peers.findOne({'_id':findThisID}, function(err, item) {
                if(!item){
                    console.log("peer doesn't exist");
                    res.code = 404;
                    res.send(404);
                    //res.send("Peer does not exist.");
                }
                res.send(item);
           
            });
        });
        
        self.app.post('/peers', function(req, res) {
            var peer = req.body;
            console.log('Adding peer: ' + JSON.stringify(peer));
            db.peers.insert(peer, {safe:true}, function(err, result) {
                if (err) {
                    res.send({'error':'An error has occurred'});
                } else {
                    console.log('Success: ' + JSON.stringify(result[0]));
                    res.send(result[0]);
                }
            });
        });
        self.app.put('/peers/:id', function(req, res) {
            var id = req.params.id;
            var peer = req.body;
            console.log('Updating peer: ' + id);
            console.log(JSON.stringify(peer));
            db.peers.update({'_id':new BSON.ObjectID(id)}, peer, {safe:true}, function(err, result) {
                if (err) {
                    console.log('Error updating peer: ' + err);
                    res.send({'error':'An error has occurred'});
                } else {
                    console.log('' + result + ' document(s) updated');
                    res.send(peer);
                }
            });
        });


        self.app.delete('/peers/:id', function(req, res) {
            var id = req.params.id;
            console.log('Deleting peer: ' + id);
            db.peers.remove({'_id':new BSON.ObjectID(id)}, {safe:true}, function(err, result) {
                if (err) {
                    res.send({'error':'An error has occurred - ' + err});
                } else {
                    console.log('' + result + ' document(s) deleted');
                    res.send(req.body);
                }
            });
        });

        self.app.get('/exterminate',function(req,res){
            var id = req.params.id;
            console.log('Deleting all peers');
            /*db.peers.remove({safe:true}, function(err, result) {
                if (err) {
                    res.send({'error':'An error has occurred - ' + err});
                } else {
                    console.log('' + result + ' document(s) deleted');
                    res.send(req.body);
                }
            });*/

            db.peers.remove(function() {
                res.send('Peers exterminated');
            });

        });

        self.app.get('/calls', function(req,res){
            console.log('Display all calls');
            /*db.peers.find({}).limit(10).forEach(function(err, doc) {
                if (err) throw err;
                if (doc) { console.dir(doc);
                res }
            });*/
            callDb.calls.find(function(error,docs) {
                res.send(docs);
            });
        });

        self.app.get('/calls/:id',function(req,res){
            var id = req.params.id;
            console.log('Retrieving call: ' + id);
            var findThisID;
            try 
            {
               findThisID = new BSON.ObjectID(id);
            }
            catch (err)
            {
               console.log(err);
            }
            
            callDb.calls.findOne({'_id':findThisID}, function(err, item) {
                if(!item){
                    console.log("call doesn't exist");
                    res.code = 404;
                    res.send(404);
                    //res.send("Peer does not exist.");
                }
                res.send(item);
           
            });
        });
        
        self.app.post('/calls', function(req, res) {
            var call = req.body;
            console.log('Adding call: ' + JSON.stringify(call));
            callDb.calls.insert(call, {safe:true}, function(err, result) {
                if (err) {
                    res.send({'error':'An error has occurred'});
                } else {
                    console.log('Success: ' + JSON.stringify(result[0]));
                    res.send(result[0]);
                }
            });
        });
        


        self.app.delete('/calls/:id', function(req, res) {
            var id = req.params.id;
            console.log('Deleting call: ' + id);
            callDb.calls.remove({'_id':new BSON.ObjectID(id)}, {safe:true}, function(err, result) {
                if (err) {
                    res.send({'error':'An error has occurred - ' + err});
                } else {
                    console.log('' + result + ' document(s) deleted');
                    res.send(req.body);
                }
            });
        });

        self.app.get('/clearcalltable',function(req,res){
            var id = req.params.id;
            console.log('Deleting all calls');
            /*db.peers.remove({safe:true}, function(err, result) {
                if (err) {
                    res.send({'error':'An error has occurred - ' + err});
                } else {
                    console.log('' + result + ' document(s) deleted');
                    res.send(req.body);
                }
            });*/

            callDb.calls.remove(function() {
                res.send('Deleted All Calls');
            });

        });


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



