var Toast = Swal.mixin({
	toast: true,
	position: 'top-end',
	showConfirmButton: false,
	timer: 10000
});


function alertToast( title, text, icon = 'success' ){
	Toast.fire({
	  	title: title,
	  	text: text,
	  	icon: icon
	});	
}

function setButtons( what ){
	
	if( what == 'play' ){
		enableBtn("#startCont");
		disableBtn("#stopCont");
		disableBtn("#restartCont");
	}
	
	if( what == 'stop-restart' ){
		disableBtn("#startCont");
		enableBtn("#stopCont");
		enableBtn("#restartCont");
	}
}

function enableBtn( btn ){
	$( btn ).removeClass('disabled');
	$( btn ).removeClass('btn-default');
	$( btn ).addClass('btn-primary');
}

function disableBtn( btn ){
	$( btn ).addClass('disabled');
	$( btn ).addClass('btn-default');
	$( btn ).removeClass('btn-primary');
}

function isDisabled( who ){
	return $(who).hasClass( "disabled" )	
}

function updateFixedLog( what ){
	$("#containerLogFixed").html( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}

function log( what ){
	if( lastPullMessage == what ) return;
	lastPullMessage = what;
	if( $("#containerLog p").length > 20 ) $("#containerLog p").first().remove();
	$("#containerLog").append( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}