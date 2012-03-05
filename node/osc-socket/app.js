
/**
 * Module dependencies.
 */

var express = require('express')
  , routes = require('./routes');

var app = module.exports = express.createServer();

var io = require('socket.io').listen(app);

// import module
var osc = require('osc4node');

// create osc server and client
var server = new osc.Server(11000, 'localhost')
  , client = new osc.Client('localhost', 12000);

// Configuration

app.configure(function(){
  app.set('views', __dirname + '/views');
  app.set('view engine', 'jade');
  app.use(express.bodyParser());
  app.use(express.methodOverride());
  app.use(app.router);
  app.use(express.static(__dirname + '/public'));
});

app.configure('development', function(){
  app.use(express.errorHandler({ dumpExceptions: true, showStack: true }));
});

app.configure('production', function(){
  app.use(express.errorHandler());
});

// Routes

app.get('/', routes.index);

app.listen(3000);
console.log("Express server listening on port %d in %s mode", app.address().port, app.settings.env);

io.sockets.on('connection', function (socket) {
	
	socket.on('from_client', function (data) {
    	console.log(data);

		var bundle = new osc.Bundle();
		bundle.add(data);
		server.send(bundle, client);
  	});

	server.on('oscmessage', function(msg) {
		console.log('oscmessage');
		console.log("address   : " + msg.address);
		console.log("typetag   : " + msg.typetag);
		console.log("message   : " + msg.message);
		console.log("arguments : " + msg.arguments);
	});
});


