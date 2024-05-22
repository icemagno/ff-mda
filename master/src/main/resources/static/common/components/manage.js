

$( document ).ready(function() {

	const ws = new SockJS( "/ws" );
	var stompClient = Stomp.over(ws);
	stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};
	
	var agentId = $("#agentId").text();
	console.log( agentId )
	
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/agent/data/' + agentId, (message) => {
			let payload = JSON.parse( message.body );
			processData( payload );
		});
		
		stompClient.subscribe('/agent/log/' + agentId, (message) => {
			let payload = JSON.parse( message.body );
			console.log( payload );
		});
		
		
	});
	
});	

function processData( data ){
	console.log( data );
}

function doSomething( what ){
	console.log( what );
}