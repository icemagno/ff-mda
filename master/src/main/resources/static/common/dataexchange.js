
let dataExchangeImageName = null;
let lastPullMessage = "";

$( document ).ready(function() {

	updateData();
	const ws = new SockJS( "/ws" );
	var stompClient = Stomp.over(ws);
	stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};	 
	 
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/docker/dataexchange/pull', (message) => {
			let payload = JSON.parse( message.body );
			console.log( payload );
		});
		
		stompClient.subscribe('/docker/dataexchange/pull', (message) => {
			let payload = JSON.parse( message.body );
			let status = payload.status;
			if( payload.progress ) updateFixedLog(payload.progress);
			if( status ) log( status );
			if( payload.errorIndicated == true ) log('Finish with ERROR.' );
			if( payload.pullSuccessIndicated == true ) {
				log( 'Finish with SUCCESS.' );
				updateFixedLog("");
				updateData();
				lastPullMessage = "";
			}
		});
	});	
	
	$("#startCont").click( ()=>{
		log('Wait ...');
		$.get("/v1/dataexchange/container/start", function(data, status) {
			console.log( data );
		});
	});
	
	$("#pullImage").click( ()=>{
		$("#containerLog").empty();
		updateFixedLog("");
		log('Wait ...');
		$.get("/v1/dataexchange/image/pull", function(data, status) {
			console.log( data );
		});
	});	
	 
});

function updateFixedLog( what ){
	$("#containerLogFixed").html( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}

function log( what ){
	if( lastPullMessage == what ) return;
	lastPullMessage = what;
	if( $("#containerLog p").length > 20 ) $("#containerLog p").first().remove();
	$("#containerLog").append( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}

function updateData(){

	$.get("/v1/dataexchange/config/get", function(data, status) {
		$("#componentConfig").text( JSON.stringify( data ) );
		if( data.certAndKeysExists ) $("#peerCertDlCont").show();
		
		if( data.image.exists ){
			$("#componentTips").text( "The image is ready to launch a container. You can pull it again if you want to update to a new version.");
			$("#startCont").removeClass('disabled');
			
			dataExchangeImageName = data.image.imageName;
			$("#imageName").text( dataExchangeImageName );
			
			console.log( data.container );
			
		} else $("#componentTips").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
		
	});



	
}

