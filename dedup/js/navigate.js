
function upNext(bigimg, lefturl, righturl){
	var imgurl		= righturl;
	var width	= bigimg.width;
	var height	= bigimg.height;
	bigimg.onmousemove=function(){
		if(event.offsetX<width/2){
			bigimg.style.cursor	= 'url(/js/arr_left.cur),auto';
			imgurl				= lefturl;
		}else{
			bigimg.style.cursor	= 'url(/js/arr_right.cur),auto';
			imgurl				= righturl;
		}
	}
	bigimg.onclick=function(){
		top.location=imgurl;
	}
}
