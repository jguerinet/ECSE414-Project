//This queries the google stun server and then waits for peer info.
//then calls nat-travesal.js

var stun = require('stun');

var dgram = require("dgram"),
    readline = require('readline');
 
//0 and 1 are "node" ans "scriptname"
//s is a simple struct

var s = {
	  HOST     : 0,
	  HOSTPORT : 0,
	  DESTIP   : 0,
	  DESTPORT : 0,
	  ACK_GARBAGE : new Buffer("node-transversal-garbage"),
	  ACK_MAGICK : new Buffer("node-transversal-magick"),
	  server:  dgram.createSocket("udp4"),
	  listening : false
	}

//Stun Server by Google
var port = 19302;
var host = 'stun.l.google.com';

// Connect to STUN Server
var client = stun.connect(port, host);


client.request(function(){
	console.log("STUN CALLBACK");
});

var natPort;
var natHost;

client.on("response",function(socket) {
	console.log("STUN RESPONSE");
	console.log(socket);
	console.log("Peer Address:");
	var i = readline.createInterface(
              process.stdin, 
              process.stdout, null);
	var input = "";
      i.input.on('data', function (data){
        if (data!="\r" && data!="\n") {
	        input+=data;
        }else {
          if(natHost){
			var b = new Buffer(input);
			natPort = input;
			console.log("Calling nat-traversal with Host: " + natHost + " Port: " + natPort);

			var ownPort = socket["attrs"]["1"]["port"];
			var ownHost = socket["attrs"]["1"]["address"];
			console.log(ownPort+ " --- " + ownHost);
			natTRAVERSAL(ownPort,natHost,natPort,ownHost);
          }else{
	          var b = new Buffer(input);
	          //console.log(input);
	          natHost = input;
	          input = "";
	          console.log("Peer Port:");
	      }
        }
      })
});



client.on("error_response",function(){
	console.log("STUN ERROR RESPONSE");
});

client.on("error",function(err){
	console.log("STUN_ERROR");
	console.error(err.stack);
});

client.on("message", function (msg, rinfo) {
  console.log("Received message on client.");
});

 

 
function natTRAVERSAL(hostport,destip,destport,host){

	s.HOST = host;
	s.HOSTPORT = hostport;
	s.DESTIP = destip;
	s.DESTPORT = destport;

	//listen on local port
	s.server.bind(s.HOSTPORT);
	 
	/*Begin the show ! */
	//send some packets to fool outside firewall...
	for (var i=0; i<10000; i++) {
	  setTimeout(function (){sendMSG( s.ACK_GARBAGE); }, 1000*i); 
	}
	//console.log("i:"+ i);
	//and last with the magick message...
	setTimeout(function (){sendMSG( s.ACK_MAGICK); } , 1000*(i++));
	setTimeout(function (){
	  console.log("Ready");
	  //for (var i=0;i<10;i++) {
	  // var msg = new Buffer("Hey Yulric");
	  // console.log("sent msg\n");
	  // sendMSG(msg);
	  // }
	},1000*(i++))
		
}


function sendMSG(msg, indicator) {
  if (indicator==undefined || indicator) process.stdout.write('.');
  client.send(
        msg, 0 , 
        msg.length, 
        s.DESTPORT, s.DESTIP,function(err,bytes){
          console.log("Send Callback");
          if (err) {
            console.log("err existed");
            console.log(err);
          };
        });
}


s.server.on("listening", function () {
  var address = s.server.address();
  console.log("server listening " +
      address.address + ":" + address.port);
});
 
s.server.on("message", function (msg, rinfo) {
  console.log("*");
  if (s.listening) {
    console.log(msg.toString());
  }
  else if (msg.toString() == s.ACK_MAGICK.toString()) {
    console.log("RECEIVED MAGIC");
    //here we must continue to send datas... ans recieve...
    s.listening = true;
 
    //now, we can read from stdin and send datas to outside
    (function (){
      console.log("read stdin");
      //this is encapsulated to not lose locals
      var i = readline.createInterface(
              process.stdin, 
              process.stdout, null);
      var input = "";
      i.input.on('data', function (data){
        if (data!="\r" && data!="\n") {
	        input+=data;
        }else {
          var b = new Buffer(input);
          sendMSG(b, false);
          input = "";
        }
      })
    })();
  }//endif
});



s.server.on("error",function(err){
  console.log("ERROR");
  console.log(err);


});
 
