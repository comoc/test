String.prototype.hashCode = function(){
	var hash = 0;
	if (this.length == 0) return hash;
	for (i = 0; i < this.length; i++) {
		char = this.charCodeAt(i);
		hash = ((hash<<5)-hash)+char;
		hash = hash & hash; // Convert to 32bit integer
	}
	return hash;
}

$(function(){

	var id = uuid().hashCode();
	var host = location.host;
	var conn = new WebSocket('ws://' + host + '/');

	conn.onmessage = function(e){
		var data = JSON.parse(e.data);
		if (data[0] == id)
			return;

		console.log(data);
		$('p#box').css({
			"left" : data[1]['left'],
			"top"  : data[1]['top']
		});
	}

	$('p#box').draggable({
		"cursor" : "pointer",
		"drag"   : function(e, ui){
			if(conn){
				var a = JSON.stringify([id].concat(ui.position));
				conn.send(a);
			}
		}
	});
	
	function uuid() {
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
			var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
			return v.toString(16);
		});
	}

});
