


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
function setButtonState( component, button){
	// set to default / undefined state first
	$(button).removeClass("label-success");
	$(button).removeClass("label-danger");
	$(button).addClass("label-default");
	$(button).text("ABSENT");

	// Then set to the actual state. I made this way because 
	// we may ( but shouldn't ) have a not listed state. Just to be safe. ;)
	if( component != false && component ) {
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
	setButtonState( stackStatus.dataExchange, "#dxStatus" );
	setButtonState( stackStatus.postgres, "#postgreStatus" );
	setButtonState( stackStatus.ipfs, "#ipfsStatus" );
	setButtonState( stackStatus.besu, "#besuStatus" );
	setButtonState( stackStatus.tokens, "#tokensStatus" );
	setButtonState( stackStatus.signer, "#signerStatus" ); 
	setButtonState( stackStatus.evmConn, "#connectorStatus" );
	setButtonState( stackStatus.sandbox, "#sandboxStatus" );
	setButtonState( stackStatus.core, "#coreStatus" );
}

// Reload configuration from file. Nothing to do for now.
function reloadCOnfig(){
  $.get("/v1/config/reload", function(data, status) {
  		console.log( data );
  });	
}