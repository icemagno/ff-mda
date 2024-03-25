
let dataExchangeImageName = null;

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
			let status = payload.status;
			if( status ) $("#containerLog").append( '<p style="margin:0px;padding:0px">' + status + '</p>' );
			if( payload.errorIndicated == true ) $("#containerLog").append( '<p style="margin:0px;padding:0px">Finish with ERROR.</p>' );
			if( payload.pullSuccessIndicated == true ) {
				$("#containerLog").append( '<p style="margin:0px;padding:0px">Finish with SUCCESS.</p>' );
				updateData();
			}
		});
	});	
	
	$("#pullImage").click( ()=>{
		$("#containerLog").empty();
		$("#containerLog").append( '<p style="margin:0px;padding:0px">Wait ... </p>' );
		$.get("/v1/dataexchange/image/pull", function(data, status) {
			console.log( data );
		});
		
	});	
	 
});

function log( what ){
	$("#containerLog").append( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}

function updateData(){

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

