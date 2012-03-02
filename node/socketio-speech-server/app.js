var express = require('express');
var app     = express.createServer();
var io      = require('socket.io').listen(app);

app.configure(function(){
	app.use(express.static(__dirname + '/public'));
});

app.listen(8001, function() {
	//console.log("Express server listening on port %d in %s mode", app.address().port, app.settings.env);
});

io.sockets.on('connection', function (socket) {
	socket.on('my_event', function (data) {
    	console.log(data);
  	});
});

