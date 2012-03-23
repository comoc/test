/* Author: Akihiro Komori
 @see <a href="http://css-tricks.com/restart-css-animation/">Restart CSS Animation</a> 
 */
$(function() {	
	$("#fadethrough .incoming").click(function () {
/*
		$(this).toggleClass('active');
				
		if ($(this).hasClass('active')) {
			$('#message').html('Click the image to reset');
//			$(this).bind("webkitAnimationEnd",function(){
//				console.log('webkitAnimationEnd');
//			});			
		} else {
			$('#message').html('Click the image to start transition');			
		}
*/		
		$(this).css("opacity", "1");
		$(this).css("-webkit-animation-name", "fadethrough");
		var el = $(this),
		newone = el.clone(true);
		el.before(newone);
		$("." + el.attr("class") + ":last").remove();
	});
});
