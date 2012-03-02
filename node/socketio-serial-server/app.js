var express = require('express');
var app     = express.createServer();
var io      = require('socket.io').listen(app);

var serialport = require('serialport');
var SerialPort = serialport.SerialPort; // localize object constructor

var active = true;
var sock = null;

var sp = new SerialPort('/dev/tty.NexusOne-SpeechServer', { 
	parser: serialport.parsers.readline('\n')
});

sp.on('data', function (data) {
	if (sock)
		sock.emit('from_serial', {my: data.toString()});
  	console.log(data.toString());
});

sp.on('close', function (data) {
  active = false;
});

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

