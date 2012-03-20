/* Author: Akihiro Komori
 @see <a href="http://css-tricks.com/restart-css-animation/">Restart CSS Animation</a> 
 */
$(function() {	
	$("#fadethrough .incoming").click(function () {
		$(this).css("opacity", "1");
		$(this).css("-webkit-animation-name", "fadethrough");
		var el = $(this),
		newone = el.clone(true);
		el.before(newone);
		$("." + el.attr("class") + ":last").remove();
	});
});
