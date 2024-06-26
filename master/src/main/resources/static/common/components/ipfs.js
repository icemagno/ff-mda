let lastPullMessage = "";
let mainConfig = null;
let peerId = null;
let working = false;

$( document ).ready(function() {
	setButtons('play');
	updateData();
	const ws = new SockJS( "/ws" );
	var stompClient = Stomp.over(ws);
	stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};
	
	setInterval( ()=>{
		$.get("/v1/container/get?container=ipfs", function( container, status) {
			if( container && container.State ) processContainer( container );
		});
	}, 4000 ); 
	 
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/docker/ipfs/pull', (message) => {
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


	$("#restartCont").click( ()=>{
		if( isDisabled( "#restartCont" ) ) return;
		log("Wait...")
		$.get("/v1/container/restart?container=ipfs", function(data, status) {
			console.log( data );
		});		
	});
		
	$("#stopCont").click( ()=>{
		if( isDisabled( "#stopCont" ) ) return;
		log("Wait...")
		$.get("/v1/container/stop?container=ipfs", function(data, status) {
			//console.log( data );
		});
	});
	
	$("#startCont").click( ()=>{
		if( isDisabled( "#startCont" ) ) return;
		log("Wait...")
		$.get("/v1/ipfs/container/start", function(data, status) {
			//console.log( data );
		});
	});
	
	$("#pullImage").click( ()=>{
		if( isDisabled( "#pullImage" ) ) return;
		$("#containerLog").empty();
		updateFixedLog("");
		log('Wait ...');
		$.get("/v1/ipfs/image/pull", function(data, status) {
			//console.log( data );
		});
	});	
	 
});

function updateData(){
	if( working ) return;
	working = true;
	$.get("/v1/ipfs/config/get", function(data, status) {
		mainConfig = data;
		if( data.image.exists ){
			$("#componentTips").text( "The image is ready to launch a container. You can pull it again if you want to update to a new version.");
			setButtons('play');
			$("#imageName").text( data.image.imageName );
			if( data.container && data.container.State ) processContainer( data.container )
			if( data.nodeConfig ) $("#peerId").text( data.nodeConfig.Identity.PeerID );
		} else $("#componentTips").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
		working = false;
	});
	
}

function processContainer( container ){
	let localIP = container.NetworkSettings.Networks.ffmda.IPAddress
	let ports = container.Ports;
	let pmCell = "";
	let pm = [];
	ports.forEach( ( port ) => { if( port.PublicPort ) pm[ port.PrivatePort.toString() ] = port.PublicPort }); 
	
	for (var key in pm ) {
		pmCell = pmCell + pm[key] + ':' + key + '<br/>';
	};
	
	$("#componentTips").html(
		'<table style="width:100%">' + 
		'<tr><td>State</td><td>'+container.State+'</td></tr>' +
		'<tr><td>Status</td><td>'+container.Status+'</td></tr>' +
		'<tr><td>Local IP</td><td>'+localIP+'</td></tr>' +
		'<tr><td>Ports</td><td>'+pmCell+'</td></tr>' +
		'</table>'
	);
	
	$.get("/v1/container/log?container=ipfs", function(data, status) {
		if( data.result ){
			$("#containerLog").empty();
			var split = data.result.split(/\r\n/);
			split.forEach( (line)=>{
				log( line );
			});
		}
	});

	if( container.State == 'running' ){
		setButtons('stop-restart');
	} else {
		setButtons('play');
	}
}




