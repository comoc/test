var KEY_ENTER = 13;
var KEY_LEFT = 37;
var KEY_UP = 38;
var KEY_RIGHT = 39;
var KEY_DOWN = 40;
var AUTOPLAY = true;
var player;
$(function () { 

    var page = 1;
    var pageSize = 18;  

//  var cursor;

    $(document).ready(function() {
//      $("#general").focus();
//      createCursor();
    });


	$('a[href=#]').click(function(){
		return false;
	});

	$('#plitem').click(function () {
		var param = $(this).data('data-url');
		console.log("param: " + param);
//		loadVideo(param, AUTOPLAY);
	});

    function addItemToGridContent(i, item) {
        if (i == 0)
            $("#grid-content").empty();

        $("#grid-content").append(item);    
	}

    function searchVideo(query)  {
        if (!query)
            return;

        var time = 'all_time';
        var url = 'http://gdata.youtube.com/feeds/api/videos?start-index={page}&max-results={pageSize}&callback=?';  
        var url2 = url.replace('{page}', page).replace('{pageSize}', pageSize);  
        var params = {  
            v: '2',  
            q: query,
            alt: 'json-in-script',  // atom, rss, json, json-in-script(jsonp)  
            format: '5'             // 1-mobile, 5-swf, 6-mobile rtsp  
        }

        $.getJSON(url2, params, function (data) {
			console.log('getJSON');
			if (!data) {
				console.log('getJSON data is null');
				return;
			}

            var feed = data.feed;  
            var entries = feed.entry || [];  
            $.each(data.feed.entry, function (i, item) {  
                var vid = item.media$group.yt$videoid.$t;  
                var title = item.title.$t;  
                var url = item.content.src;  
                var thumbnailUrl = item.media$group.media$thumbnail[0].url;  
                var playerUrl = item.media$group.media$content[0].url;
                console.log("getJson" + i + ", " + thumbnailUrl);

				var scrpt = '';

				scrpt += '<a href="javascript:loadVideo(\'';
                scrpt += playerUrl;
                scrpt += '\',' + AUTOPLAY + ');"';
                scrpt += ' title="' + title;
                scrpt += '"><img class="my_content" src="'
                        + thumbnailUrl
                        + '" /><br>'
                        + title
						+ '</a>';

//				scrpt += '<a href="#" id="plitem" data-url="' + playerUrl + '" title="' + title + '">'
//					+ '<img class="my_content" src="' + thumbnailUrl + '" /><br />'
//					+ title
//					+ '</a>';

                console.log('script: ' + scrpt);
                addItemToGridContent(
                    i,
                    '<li>'
                    + scrpt
                    + '</li>'
					);


            });

            if (entries && entries.length > 0) {
                $("#grid-content").vgrid();

                loadVideo(entries[0].media$group.media$content[0].url, AUTOPLAY);  
                $('#playerContainer').show();  
            }
		});
    }


    $('#btnSearch').click(function () {  
        var query = $('#txtQuery').val();
        searchVideo(query);
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
            searchVideo(query);
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


    function playPauseVideo() {
        player = document.getElementById('player');
        if (player) {
            var s = player.getPlayerState();
            if (s == 1) {
                player.pauseVideo();
            } else if (s == 2 || s == 0) {
                player.playVideo();
            }
        }
    }
    
    function skipVideo(secFromNow) {
        player = document.getElementById('player');
        if (player) {
            var dur = player.getDuration();
            var to = secFromNow + player.getCurrentTime();
            if (to < 0)
                to = 0;
            else if (to > dur)
                to = dur;
            player.seekTo(to, true);
        }
    }
    
    function playVideo() {
        if (player) {
            player.playVideo();
        }
    }
    
    function pauseVideo() {
        if (player) {
            player.pauseVideo();
        }
    }
    
    function onytplayerStateChange(newState) {
        console.log("Player's new state: " + newState);
    }
    
    function onYouTubePlayerReady(playerId) {
        if (playerId == 'player') {
            player = document.getElementById('player');
			player.addEventListener("onStateChange", "onytplayerStateChange");
        }
    }

//  function createCursor() {
//      cursor = $('#cursor');
//      if (!cursor.attr('id')) {
//        cursor = $('<img>');
//        cursor.attr('class', 'cursor');
//        cursor.attr('id', 'cursor');
//        cursor.attr('src', 'cursor.png');
//        cursor.css('position', 'fixed');
//        cursor.css('width', '24px');
//        cursor.css('height', '24px');
//        $('#wrapper').append(cursor);
//    }
//
//      cursor.css('left', '0px');
//      cursor.css('top', '0px');
//      cursor.show();
//      //setTimeout(function() {
//      //  cursor.hide();
//      //}, 10000);
//  }
//  function moveCursor(dx, dy) {
//      if (cursor) {
//          var pos = cursor.position();
//          var l = parseInt(pos.left) + dx;
//          var t = parseInt(pos.top) + dy;
//          var lmin = -cursor.width() / 3;
//          var tmin = -cursor.height() / 5;
//          var lmax = $(document).width() + lmin;
//          var tmax = $(document).height() + tmin;
//          if (l < lmin) {
//              l = lmin;
//          } else if (l > lmax) {
//              l = lmax;
//          }
//          if (t < tmin) {
//              t = tmin;
//          } else if (t > tmax) {
//              t = tmax;
//          }
//          console.log('l:' + l);
//          console.log('t:' + t);
//          cursor.css('left', l + 'px');
//          cursor.css('top', t + 'px');
//      }
//  }
    
});

function getScrollPosition() {
	var obj = new Object();
	obj.x = document.documentElement.scrollLeft || document.body.scrollLeft;
	obj.y = document.documentElement.scrollTop || document.body.scrollTop;
	return obj;
}

function loadVideo(playerUrl, autoplay) {
    var url = playerUrl + '&enablejsapi=1&playerapiid=player&rel=0&border=0&fs=1&autohide=1&hd=1&autoplay=' + (autoplay ? 1 : 0);
    console.log("loadVideo:" + url);
    swfobject.embedSWF(
        url,
        'player', '640', '505', '9.0.0', false, false,
        { allowfullscreen: 'true', allowScriptAccess: 'always'}  // 290 x 250  
	);

	scrollTo(0,0);
} 

