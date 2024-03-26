
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
	});


	$.get("/v1/dataexchange/image/pulled", function(data, status) {
  		if ( data.exists ) { 
			$("#imageName").text( "Image ready to start a container. You can pull it again if you want to update to a new version.");
			$("#startCont").removeClass('disabled');
			
			dataExchangeImageName = data.imageName;
			
			$.get("/v1/dataexchange/container/get", function(data, status) {
				console.log( data );
			});
		} else $("#imageName").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
	});

	
}

