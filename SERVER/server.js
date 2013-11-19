var express = require('express'),
    peer = require('./routes/peers');
 
var app = express();
 
app.configure(function () {
    app.use(express.logger('dev'));     /* 'default', 'short', 'tiny', 'dev' */
    app.use(express.bodyParser());
});
app.get('/',function(req,res) {
	res.send('hello yulric');
});
app.get('/peers', peer.findAll);
app.get('/peers/:id', peer.findById);
app.post('/peers', peer.addPeer);
app.put('/peers/:id', peer.updatePeer);
app.delete('/peers/:id', peer.deletePeer);
 

var port = process.env.OPENSHIFT_INTERNAL_PORT || 8080
    , ip = process.env.OPENSHIFT_INTERNAL_IP || "127.0.0.1";

app.listen(ip,port);
console.log('Listening on port 3001...');