var net = require('net');
var sys = require('sys');
var server = net.createServer(function (stream) {
  var array = [];
  stream.setEncoding('utf8');
  stream.on('connect', function () {
    stream.write('connected\r\n');
  });
  stream.on('data', function (data) {
    array.push(data.slice(0,-2));
    stream.write('res: ' + array.join(',') + '\n');
  });
  stream.on('end', function () {
    sys.log('disconnected')
    stream.end();
  });
});
server.listen(8127, 'localhost');

