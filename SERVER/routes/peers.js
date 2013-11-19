var mongo = require('mongodb');
 
var Server = mongo.Server,
    Db = mongo.Db,
    BSON = mongo.BSONPure;
 
var server = new Server('localhost', 27017, {auto_reconnect: true});
db = new Db('peerdb', server);
//var db = new Db('winedb',new Server 'localhost',27017, ) 

db.open(function(err, db) {
    if(!err) {
        console.log("Connected to 'peerdb' database");
        db.collection('peers', {strict:true}, function(err, collection) {
            if (err) {
                console.log("The 'peers' collection doesn't exist. Creating it with sample data...");
                populateDB();
            }
        });
    }
});
 
exports.findById = function(req, res) {
    var id = req.params.id;
    console.log('Retrieving peer: ' + id);
    db.collection('peers', function(err, collection) {
        try 
        {
            var findThisID = new BSON.ObjectID(id);
        }
        catch (err)
        {
            console.log(err);
        }
        
        collection.findOne({'_id':findThisID}, function(err, item) {
            if(!item){
                console.log("peer doesn't exist");
            }
            res.send(item);
        });
    });
};
 
exports.findAll = function(req, res) {
    db.collection('peers', function(err, collection) {
        collection.find().toArray(function(err, items) {
            
            res.send(items);
        });
    });
};
 
exports.addPeer = function(req, res) {
    var peer = req.body;
    console.log('Adding peer: ' + JSON.stringify(peer));
    db.collection('peers', function(err, collection) {
        collection.insert(peer, {safe:true}, function(err, result) {
            if (err) {
                res.send({'error':'An error has occurred'});
            } else {
                console.log('Success: ' + JSON.stringify(result[0]));
                res.send(result[0]);
            }
        });
    });
}
 
exports.updatePeer = function(req, res) {
    var id = req.params.id;
    var peer = req.body;
    console.log('Updating peer: ' + id);
    console.log(JSON.stringify(peer));
    db.collection('peers', function(err, collection) {
        collection.update({'_id':new BSON.ObjectID(id)}, peer, {safe:true}, function(err, result) {
            if (err) {
                console.log('Error updating peer: ' + err);
                res.send({'error':'An error has occurred'});
            } else {
                console.log('' + result + ' document(s) updated');
                res.send(peer);
            }
        });
    });
}
 
exports.deletePeer = function(req, res) {
    var id = req.params.id;
    console.log('Deleting peer: ' + id);
    db.collection('peers', function(err, collection) {
        collection.remove({'_id':new BSON.ObjectID(id)}, {safe:true}, function(err, result) {
            if (err) {
                res.send({'error':'An error has occurred - ' + err});
            } else {
                console.log('' + result + ' document(s) deleted');
                res.send(req.body);
            }
        });
    });
}
 
/*--------------------------------------------------------------------------------------------------------------------*/
// Populate database with sample data -- Only used once: the first time the application is started.
// You'd typically not find this code in a real-life app, since the database would already exist.
var populateDB = function() {
 
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
 
    db.collection('peers', function(err, collection) {
        collection.insert(peers, {safe:true}, function(err, result) {});
    });
 
};