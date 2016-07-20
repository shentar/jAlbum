var rightnum = 0; 
$("#rightrotate").click(function(){ 
rightnum++; 
$("#singlephoto").rotate(90*rightnum); 
}); 

var leftnum = 0;
$("#leftrotate").click(function(){ 
leftnum++; 
$("#singlephoto").rotate(90*leftnum); 
}); 