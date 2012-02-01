$(function(){

		var id = uuid();

		var conn = new WebSocket("ws://localhost:3000/");

		conn.onmessage = function(e){
			var data = JSON.parse(e.data);
			if (data[0] == id)
				return;
			console.log(data);
		}

		$(document).mousemove(function(e) {
			if (conn){
				var d = [id, [e.pageX, e.pageY]];
				var a = JSON.stringify(d);
				conn.send(a);
				console.log(a);
			}
		});

		function uuid() {
			return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
						var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
						return v.toString(16);
					});
		}
});
