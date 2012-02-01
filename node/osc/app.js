
/**
 * Module dependencies.
 */

var express = require('express')
  , routes = require('./routes')
  , ws = require('websocket').server;

var osc = require('osc4node');

var app = module.exports = express.createServer();
var server = new ws({"httpServer" : app});

var oscserver = new osc.Server(12000, '127.0.0.1')
  , oscclient = new osc.Client('127.0.0.1', 11000);

// Configuration

app.configure(function(){
//  app.set('views', __dirname + '/views');
//  app.set('view engine', 'jade');
//  app.use(express.bodyParser());
//  app.use(express.methodOverride());
//  app.use(app.router);
  app.use(express.static(__dirname + '/public'));
});

app.configure('development', function(){
  app.use(express.errorHandler({ dumpExceptions: true, showStack: true })); 
});

app.configure('production', function(){
  app.use(express.errorHandler()); 
});

// WebSocket server
server.on('request', function(request) {
	var connection = request.accept(null, request.origin);
	console.log((new Date()) + ' Connection accepted');
	connection.on('message', function(message) {
		if (message.type === 'utf8') {
			console.log('Received Message: ' + message.utf8Data);
			server.broadcastUTF(message.utf8Data);
			var json = JSON.parse(message.utf8Data);
			console.log(json[0]);
			console.log(json[1][0]);
			console.log(json[1][1]);
			var oscmessage = new osc.Message('/mouse');
			oscmessage.add(json[0]);
			oscmessage.add(json[1][0]);
			oscmessage.add(json[1][1]);
			oscserver.send(oscmessage, oscclient);
		}
		else if (message.type === 'binary') {
			console.log('Received Binary Message of ' + message.binaryData.length + ' bytes');
			connection.broadcastBytes(message.binaryData);
		}
	});
	connection.on('close', function(reasonCode, description) {
		console.log((new Date()) + ' Peer ' + connection.remoteAddress + ' disconnected.');
	});
});


// OSC server
//
oscserver.on('oscmessage', function(msg, rinfo) {
	console.log('oscmessage:address:' + msg.address);
	console.log('oscmessage:typetag:' + msg.typetag);
	for (var i = 0; i < msg.typetag.length; i++) {
		if (msg.typetag[i] == 'i')
			console.log('i:' + i + ":" + parseInt(msg.arguments[i].value));
		else if (msg.typetag[i] == 'f')
			console.log('f:' + i + ":" + parseFloat(msg.arguments[i].value));
		else if (msg.typetag[i] == 's')
			console.log('s:' + i + ":" + msg.arguments[i].value.toString());
		else if (msg.typetag[i] == 'b')
			console.log('b:' + i + ':length:' + msg.arguments[i].value.length);
	}
});

// Routes

app.get('/', routes.index);

app.listen(3000);
console.log("Express server listening on port %d in %s mode", app.address().port, app.settings.env);
