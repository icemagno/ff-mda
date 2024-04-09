


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
}

// Reload configuration from file. Nothing to do for now.
function reloadCOnfig(){
  $.get("/v1/config/reload", function(data, status) {
  		console.log( data );
  });	
}