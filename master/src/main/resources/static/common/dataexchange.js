


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
		
		stompClient.subscribe('/docker', (message) => {
			 console.log( message.body );
		});
	});	 
	 
});

function pullImage(){
	
	$.get("/v1/dataexchange/image/pull", function(data, status) {
		console.log( data );
	});
}


function updateData(){

	$.get("/v1/dataexchange/image/pulled", function(data, status) {
  		if ( data.exists ) { 
			$("#imageName").text( "Image ready to start a container. You can pull it again if you want to update to a new version." /*data.imageName*/ );
			$("#startCont").removeClass('disabled');
			
			$.get("/v1/dataexchange/container/get", function(data, status) {
				console.log( data );
			});
			
		} else $("#imageName").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
	});

	
}

