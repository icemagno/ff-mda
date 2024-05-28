
// "/files/recreate/{agentId}"  ( will send too )

function getAgentId(){
	return $("#agentId").text();
}

$( document ).ready(function() {

	setButtonState( 'absent', "#stateBesu" );
	setButtonState( 'absent', "#stateDx" );
	setButtonState( 'absent', "#statePsql" );
	setButtonState( 'absent', "#stateIpfs" );
	setButtonState( 'absent', "#stateTokens" );
	setButtonState( 'absent', "#stateSigner" );
	setButtonState( 'absent', "#stateEvm" );
	setButtonState( 'absent', "#stateSandbox" );
	setButtonState( 'absent', "#stateCore" );

	const ws = new SockJS( "/ws" );
	var stompClient = Stomp.over(ws);
	stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};
	
	var agentId = getAgentId();
	
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
		
		stompClient.subscribe('/shell/' + agentId + "/dataexchange", (message) => {
			let payload = JSON.parse( message.body );
			console.log( payload );
		});
		
	});
	
});	

// This will set the components tag at right side
// to show the containers state
function setButtonState( state, button ){
	// set to default / undefined state first
	$(button).removeClass("label-success");
	$(button).removeClass("label-danger");
	$(button).addClass("label-default");
	$(button).text("ABSENT");
	

	// Then set to the actual state. I made this way because 
	if( state == 'running' ) {
		$(button).removeClass("label-default");
		$(button).addClass("label-success");
		$(button).text("RUNNING");
	}
	if( state == 'exited' ) {
		$(button).removeClass("label-default");
		$(button).addClass("label-danger");
		$(button).text("EXITED");
	}
	
}

function processData( data ){
	console.log( data );
	if( data.besu && data.besu.container ){
		var container = data.besu.container;
		var state = data.besu.container.State;
		$("#imageBesu").text( container.Image );
		$("#statusBesu").text( container.Status );
		setButtonState( state, "#stateBesu" );
	}
}

function alertOk(){
	Swal.fire({
	  title: "Good job!",
	  text: "You clicked the button!",
	  icon: "success"
	});	
}

function reconfig( what ){
	console.log( "Reconfig " + what );
	$.get("/v1/files/send/" + what + "/" + getAgentId(), function( data, status) {
		console.log( data );
	});	
}

function log( what ){
	console.log( "Log " + what );
	alertOk();
}

function start( what ){
	$.get("/v1/agent/deploy/" + what + "/" + getAgentId(), function( data, status) {
		console.log( data );
	});	
}

function stop( what ){
	console.log( "Stop " + what );
}