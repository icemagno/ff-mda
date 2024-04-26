
let dataExchangeImageName = null;
let lastPullMessage = "";
let mainConfig = null;
let peerId = null;
let peerStatus = {"messageQueueSize": 0, "inFlightCount": 0, "peers": [] };
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
		$.get("/v1/container/get?container=dataexchange", function( container, status) {
			if( container && container.State ) processContainer( container );
		});
	}, 4000 ); 
	 
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/shell/dataexchange', (message) => {
			let payload = JSON.parse( message.body );
		});

		stompClient.subscribe('/data/dataexchange', (message) => {
			let payload = JSON.parse( message.body );
		});
		
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

	
	$("#restartCont").click( ()=>{
		if( isDisabled( "#restartCont" ) ) return;
		log("Wait...")
		$.get("/v1/container/restart?container=dataexchange", function(data, status) {
			console.log( data );
		});		
	});
		
	$("#stopCont").click( ()=>{
		if( isDisabled( "#stopCont" ) ) return;
		log("Wait...")
		$.get("/v1/container/stop?container=dataexchange", function(data, status) {
			console.log( data );
		});
	});
	
	$("#dlPeerCert").click( ()=>{
		window.open("/v1/dataexchange/peer/certificate");
	});
	
	$("#startCont").click( ()=>{
		if( isDisabled( "#startCont" ) ) return;
		log("Wait...")
		$.get("/v1/dataexchange/container/start", function(data, status) {
			console.log( data );
		});
	});
	
	$("#pullImage").click( ()=>{
		if( isDisabled( "#pullImage" ) ) return;
		$("#containerLog").empty();
		updateFixedLog("");
		log('Wait ...');
		$.get("/v1/dataexchange/image/pull", function(data, status) {
			console.log( data );
		});
	});	
	 
});

function processConfig( config, prefix ){
	for (var key in config) {
		let namespace = prefix + key;
	    if ( config.hasOwnProperty( key ) ) {
			let value = config[key];
	        if( typeof value == 'object' ) {
				$("#configTable").append("<tr><th style='background:#d2d6de'>"+namespace+"</th><th style='background:#d2d6de'>&nbsp;</th></tr>");
				processConfig( value, namespace + ".");
			} else {
				$("#configTable").append("<tr><td>"+namespace+"</td><td>"+value+"</td></tr>");
			}
	    }
	}	
}

function updateData(){
	if( working ) return;
	working = true;
	$("#configTable").empty();
	$.get("/v1/dataexchange/config/get", function(data, status) {
		mainConfig = data;
		processConfig( data.componentConfig, "" );
		if( data.certAndKeysExists ) $("#peerCertDlCont").show();
		if( data.image.exists ){
			$("#componentTips").text( "The image is ready to launch a container. You can pull it again if you want to update to a new version.");
			setButtons('play');
			dataExchangeImageName = data.image.imageName;
			$("#imageName").text( dataExchangeImageName );
			if( data.container && data.container.State ) processContainer( data.container )
		} else $("#componentTips").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
		working = false;
	});
}

function processContainer( container ){
	let dataExchangeLocalIP = container.NetworkSettings.Networks.ffmda.IPAddress
	let ports = container.Ports;
	let pmCell = "";
	let pm = [];
	ports.forEach( ( port ) => { if( port.PublicPort ) pm[ port.PrivatePort.toString() ] = port.PublicPort }); 
	
	for (var key in pm ) {
		pmCell = pmCell + pm[key] + ':' + key + '<br/>';
	};
	
	let nodeId = "Wait...";
	if( peerId ) nodeId = peerId.id;
	
	console.log( peerStatus )
	
	$("#componentTips").html(
		'<table style="width:100%">' + 
		'<tr><td>Tag</td><td>'+container.Labels.tag+'</td></tr>' +
		'<tr><td>State</td><td>'+container.State+'</td></tr>' +
		'<tr><td>Status</td><td>'+container.Status+'</td></tr>' +
		'<tr><td>Local IP</td><td>'+dataExchangeLocalIP+'</td></tr>' +
		'<tr><td>Ports</td><td>'+pmCell+'</td></tr>' +
		'<tr><td>Peer ID</td><td>'+nodeId+'</td></tr>' +
		'<tr><td>MSG Query Size</td><td>'+peerStatus.messageQueueSize+'</td></tr>' +
		'<tr><td>In Flight Count</td><td>'+peerStatus.inFlightCount+'</td></tr>' +
		'<tr><td>Peers</td><td>'+peerStatus.peers.length+'</td></tr>' +
		'</table>'
	);
	
	$.get("/v1/container/log?container=dataexchange", function(data, status) {
		if( data.result ){
			$("#containerLog").empty();
			var split = data.result.split(/\r\n/);
			split.forEach( (line)=>{
				log( line );
			});
		}
	});

	if( container.State == 'running' ){
		getPeerId()
		getPeerStatus();
		setButtons('stop-restart');
	} else {
		setButtons('play');
	}
}


function getPeerId(){
	if ( peerId ) return;
	$.get( "/v1/dataexchange/peer/id", function(data, status) {
		console.log( data );
		peerId = data;
	});
}

function getPeerStatus(){
	$.get( "/v1/dataexchange/peer/status", function(data, status) {
		peerStatus = data;
	});
}
