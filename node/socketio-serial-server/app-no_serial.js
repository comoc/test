var express = require('express');
var app     = express.createServer();
var io      = require('socket.io').listen(app);


var active = true;
var sock = null;

app.configure(function(){
	app.use(express.static(__dirname + '/public'));
});

app.listen(8001, function() {
});

io.sockets.on('connection', function (socket) {
	sock = socket;
	socket.on('from_client', function (data) {
    	console.log(data);
  	});
});


//console.log("Express server listening on port %d in %s mode", app.address().port, app.settings.env);

//  fs.write(fd, "\n------------------------------------------------------------\nClosing SerialPort: "+target+" at "+Date.now()+"\n------------------------------------------------------------\n");  

