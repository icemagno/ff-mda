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

		$.get("/v1/agent/list", function( data ) {
			
			stompClient.subscribe('/agent/status', (message) => {
				let payload = JSON.parse( message.body );
				processAgent( payload );
			});
			
			stompClient.subscribe('/agent/message', (message) => {
				let payload = JSON.parse( message.body );
				console.log( payload );
			});
			
		});

	});		
	
	// trigger the delete button from modal
	$("#btnDeleteAgent").click( ()=>{
		let id = $("#agentUuid").text();
		console.log("delete " + id );
		$.get("/v1/agent/delete/" + id, function(data, status) {
			console.log( data );
		});		
	});

	$("#btnRegisterAgent").click( ()=>{
		let port = $("#agPort").val();
		let ipAddress = $("#agIpAddress").val();
		let data = { ipAddress: ipAddress, port: port }
		if ( !data.ipAddress || !data.port ) return;
		$.ajax ({
		    url: "/v1/agent/add",
		    type: "POST",
		    data: JSON.stringify( {data : data } ),
		    dataType: "json",
		    contentType: "application/json; charset=utf-8",
		    success: function( data ){
		        processAgent( data );
		    }
		});
		$("#agPort").val('');
		$("#agIpAddress").val('');
	});
  
});

function processAgent( agent ){
	let statusColor = getStatusColor( agent.status );
	if ( $( "#" + agent.id ).length ) {
		// Already have this agent card. Update...
		addStatusColor("#ico_" + agent.id, statusColor );
		$("#sts_" + agent.id).text( agent.hostName );
		$("#nn_" + agent.id).text( agent.nodeName );
		$("#on_" + agent.id).text( agent.orgName );
		// Only allow to manage connected (online) agents
		if( agent.status == 'CONNECTED') $("#lk_" + agent.id).show(); else $("#lk_" + agent.id).hide(); 
	} else {
		// Its a new agent.. add the card
		$("#agentContainer").append( getAgentCard(agent) );
		// Hide the "Manage" button since we don't know its status yet
		$("#lk_" + agent.id).hide();
	}
}

// Show a modal dialog asking to be sure
function deleteAgent( id ){
	$("#agentUuid").text( id );
	$('#delete-agent').modal('show');
}

function getAgentCard( agent ){
	let statusColor = getStatusColor( agent.status );
	let ac = '<li style="width: 150px;" id="'+agent.id+'" class="text-center" > ' + 
      '<span class="mailbox-attachment-name"><small id="on_'+agent.id+'">'+agent.orgName+'</small></span>' +
      '<span class="mailbox-attachment-icon" style="padding-bottom: 0px; padding-top:0px;"><i id="ico_'+agent.id+'"  class="fa fa-desktop '+statusColor+'"></i></span>' +
      '<span class="mailbox-attachment-name"><small id="nn_'+agent.id+'">'+agent.nodeName+'</small></span>' +
      '<div class="mailbox-attachment-info text-left" style="padding-bottom: 5px;padding-top: 5px;">' +
      		'<small id="sts_'+agent.id+'">' + agent.hostName + '</small>' +
            '<span class="mailbox-attachment-size">' +
              agent.ipAddress + ":" + agent.port + 
            '</span>' +
   			'<a id="lk_'+agent.id+'" href="/remote/'+ agent.id + '" class="btn btn-default btn-xs pull-right"><i class="fa fa-external-link"></i></a>' +
            '<button onClick="deleteAgent(\''+ agent.id + '\')" class="btn btn-default btn-xs"><i class="fa fa-trash-o text-red"></i></button>' +
      '</div>' +
    '</li>';
	return ac;
}

function getStatusColor( status ){
	switch(status) {
	    case "CONNECTING":
	        return "text-yellow"
	    case "CONNECTED":
	        return "text-green"
	    case "DISCONNECTED":
	        return "text-red"
	    case "NEW_ADDED":
	        return "text-muted"
	}
}

function addStatusColor( ele, statusColor ){
	$( ele ).removeClass("text-yellow");
	$( ele ).removeClass("text-green");
	$( ele ).removeClass("text-red");
	$( ele ).addClass( statusColor );
}