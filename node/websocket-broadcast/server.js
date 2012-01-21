var ws      = require('websocket').server;
var express = require('express');
var app     = express.createServer();

app.configure(function(){
	app.use(express.static(__dirname + '/public'));
});

var server = new ws({
	"httpServer" : app,
//	"autoAcceptConnections" : false
});

function originIsAllowed(origin) {
	return true;
}

server.on('request', function(request) {
	if (!originIsAllowed(request.origin)) {
		request.reject();
		console.log((new Date()) + ' Connection from origin ' + request.origin + ' rejected.');
		return;
	}
	console.log("aaa")
	var connection = request.accept(null, request.origin);
	console.log((new Date()) + ' Connection accepted ' + 'from ' + require.orign);
	connection.on('message', function(message) {
		if (message.type === 'utf8') {
			console.log('Received Message: ' + message.utf8Data);
			server.broadcastUTF(message.utf8Data);
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

app.listen(8001, function() {
	console.log("Express server listening on port %d in %s mode", app.address().port, app.settings.env);
});


