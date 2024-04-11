


$( document ).ready(function() {
	
	const ws = new SockJS( "/ws" );
	var stompClient = Stomp.over(ws);
	stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};	 
	 
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/shell/ff-mda', (message) => {
			let payload = JSON.parse( message.body );
			console.log( payload );
		});
		
	});		
	
	$.get("/v1/config/get", function(data, status) {
  		console.log( data );
  		$("#orgName").val( data.orgName );
  		$("#nodeName").val( data.nodeName);
  		$("#ipAddress").val( data.ipAddress );
  		$("#hostName").val( data.hostName);  		
		checkConfig( data );
	});
	
	setInterval( ()=>{
		$.get("/v1/config/get", function(data, status) {
			checkConfig( data );
		});
	}, 4000 ); 	
  
	// Download CA certificate file
	$("#dlCACert").click( ()=>{
		window.open("/v1/org/certificate");
	});

  
	// Save Organization and Node names to configuration
	$("#updateOrgName").click( ()=>{
		let orgName = $("#orgName").val();
		let nodeName = $("#nodeName").val();
		let hostName = $("#hostName").val();
		let ipAddress = $("#ipAddress").val();

		let data = { orgName: orgName, nodeName: nodeName, ipAddress: ipAddress, hostName: hostName }
		$.ajax ({
		    url: "/v1/org/save",
		    type: "POST",
		    data: JSON.stringify( {data : data } ),
		    dataType: "json",
		    contentType: "application/json; charset=utf-8",
		    success: function( data ){
		        location.reload();
		    }
		});		
	});
	 
	 
});

// This will set the components tag at right side
// to show the containers state
function setButtonState( component, button, buttonLabel ){
	// set to default / undefined state first
	$(button).removeClass("label-success");
	$(button).removeClass("label-danger");
	$(button).addClass("label-default");
	$(button).text("ABSENT");
	

	// Then set to the actual state. I made this way because 
	// we may ( but shouldn't ) have a not listed state. Just to be safe. ;)
	if( component ) {
		$(buttonLabel).text( component.Status );
		if( component.State == 'running' ) {
			$(button).removeClass("label-default");
			$(button).addClass("label-success");
			$(button).text("RUNNING");
		}
		if( component.State == 'exited' ) {
			$(button).removeClass("label-default");
			$(button).addClass("label-danger");
			$(button).text("EXITED");
		}
	}
}

// Check configuration to interface state
function checkConfig( data ){
	$("#mainTip").text( "Configure the components" );
	$("#mainSubTip").text( "Use the tabs below." );
	$(".btn-manage").removeClass("disabled");
	if( ( data.orgName.length < 3 ) && ( data.nodeName.length < 3 ) ){
		  $("#mainTip").text( "Set Supernode Configuration" );
		  $("#mainSubTip").text( "You must set Organization and Node name before proceed." );
		  $(".btn-manage").addClass("disabled");
	}
	stackStatus = data.stackStatus;
	setButtonState( stackStatus.dataExchange, "#dxStatus", "#dxLabel" );
	setButtonState( stackStatus.postgresql, "#postgreStatus", "#postgreLabel" );
	setButtonState( stackStatus.ipfs, "#ipfsStatus", "#ipfsLabel" );
	setButtonState( stackStatus.besu, "#besuStatus", "#besuLabel" );
	setButtonState( stackStatus.tokens, "#tokensStatus", "#tokensLabel" );
	setButtonState( stackStatus.signer, "#signerStatus", "#signerLabel" ); 
	setButtonState( stackStatus.evmConn, "#connectorStatus", "#connectorLabel" );
	setButtonState( stackStatus.sandbox, "#sandboxStatus", "#sandboxLabel" );
	setButtonState( stackStatus.core, "#coreStatus", "#coreLabel" );
}

// Reload configuration from file. Nothing to do for now.
function reloadCOnfig(){
  $.get("/v1/config/reload", function(data, status) {
  		console.log( data );
  });	
}