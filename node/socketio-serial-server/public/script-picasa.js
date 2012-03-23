var KEY_ENTER = 13;
var KEY_LEFT = 37;
var KEY_UP = 38;
var KEY_RIGHT = 39;
var KEY_DOWN = 40;
$(function () { 

	var page = 1;
	var pageSize = 18;  

	//  var cursor;

	$(document).ready(function() {
		//      $("#general").focus();
		//      createCursor();
	});
	

	$( "#dialog" ).dialog({
		autoOpen: false,
		show: "fade",
		hide: "fade"
	});


	$('a[href=#]').click(function(){
		return false;
	});

	$('#plitem').click(function () {
		var param = $(this).data('data-url');
		console.log("param: " + param);
	});

	function addItemToGridContent(i, item) {
		if (i == 0)
			$("#grid-content").empty();

		$("#grid-content").append(item);    
	}

	function searchPhoto(query)  {
		if (!query)
			return;
		var opts = { position: 'center', hide: true };
		$('#spinner').spinner(opts);

		var perpage = 20 + 20; // for 1920x1080

		$.ajax({
			url:"http://picasaweb.google.com/data/feed/base/all?alt=json-in-script&kind=photo&q="+query+"&filter=1&access=public&max-results="+perpage+"&imgmax=300",
			dataType:"jsonp",
			success:function(data){
				
				console.log("hit: " + data.feed.openSearch$totalResults.$t); 

				for(var i in data.feed.entry){
					var item=data.feed.entry[i];
					var nn = '<div class="thumb-div"><a href="' + item.link[1].href + '">'
						+ '<img class="thumb" src="' +item.content.src+'" /><br />'
						+ item.title.$t.substr(0, 20) + '</a></div>';

					addItemToGridContent(
						i,
						'<li>' + nn + '</li>'
						);
				}

				$("#grid-content").vgrid();
				
//				var el = $('.list-item'),
//				newone = el.clone(true);
//				el.css("-webkit-animation-name", "fadethrough");
//				el.css("opacity", "1");
//				el.before(newone);
//				$("." + el.attr("class") + ":last").remove();

			},
			complete:function(xhr, status, err){
				$('#spinner').spinner('remove');

			}
		});
	}


	$('#btnSearch').click(function () {  
		var query = $('#txtQuery').val();
		searchPhoto(query);
	});

	$('#txtQuery').keydown(function(e) { 
		if (e.keyCode == KEY_ENTER) {
			var query = $('#txtQuery').val();
			if (query)
		$('#btnSearch').trigger('click');  
		}
		console.log(e.keyCode);
	});

	var host = location.host;
	console.log(host);
	var socket = io.connect('http://' + host);
	socket.on('from_serial', function (data) {
		if (data.my.indexOf('DIRL') == 0) {
			//            sendKeyEvent(true, KEY_RIGHT);
			//            sendKeyEvent(false, KEY_RIGHT);
			//            skipVideo(-5);
		} else if (data.my.indexOf('DIRR') == 0) {
			//            sendKeyEvent(true, KEY_LEFT);
			//            sendKeyEvent(false, KEY_LEFT);
			//            skipVideo(5);
		} else if (data.my.indexOf('DIRU') == 0) {
			//          sendKeyEvent(true, KEY_UP);
			//          sendKeyEvent(false, KEY_UP);
			//playVideo();
			//            scrollDown();
		} else if (data.my.indexOf('DIRD') == 0) {
			//pauseVideo();
			//          sendKeyEvent(true, KEY_DOWN);
			//          sendKeyEvent(false, KEY_DOWN);
			//            scrollUp();
			//      } else if (data.my.indexOf('TAP_') == 0) {
			//              playPauseVideo();
	} else if (data.my.indexOf('SEAR,0,') == 0) {
		var query = data.my.substring(7);
		searchPhoto(query);
	} else if (data.my.indexOf('BGNS') == 0) {
		$( "#dialog" ).dialog('open');
	} else if (data.my.indexOf('ENDS') == 0) {
		$( "#dialog" ).dialog('close');
	}

	console.log(data.my);
	});

	$('input').keydown(function(e){
		if (e.keyCode == 38) {
			$('.my_content').prev().focus();   
		}    
		if (e.keyCode == 40) {
			$('.my_content').next().focus();
		}
	});

	function sendKeyEvent(isPress, keyCode) {
		if (isPress) {
			var e = jQuery.Event("keydown");
			e.which = keyCode;
			$('body').trigger(e);
		}
		else {
			var e = jQuery.Event("keydup");
			e.which = keyCode;
			$('body').trigger(e);
		}
	}

	function scrollUp() {
		scroll('prev');
	}

	function scrollDown() {
		scroll('next');
	}

	// scroll
	function scroll(direction) {

		var scroll, i,
			positions = [],
			here = $(window).scrollTop(),
			collection = $('.post');

		collection.each(function() {
			positions.push(parseInt($(this).offset()['top'],10));
		});

		for(i = 0; i < positions.length; i++) {
			if (direction == 'next' && positions[i] > here) { scroll = collection.get(i); break; }
			if (direction == 'prev' && i > 0 && positions[i] >= here) { scroll = collection.get(i-1); break; }
		}

		if (scroll) {
			$.scrollTo(scroll, {
				duration: 500       
			});
		}

		return false;
	}

	$("#next,#prev").click(function() {        
		return scroll($(this).attr('id'));        
	});

	$(".scrolltoanchor").click(function() {
		$.scrollTo($($(this).attr("href")), {
			duration: 750
		});
		return false;
	});



});

function getScrollPosition() {
	var obj = new Object();
	obj.x = document.documentElement.scrollLeft || document.body.scrollLeft;
	obj.y = document.documentElement.scrollTop || document.body.scrollTop;
	return obj;
}

