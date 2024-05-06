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
		$.get("/v1/container/get?container=besu", function( container, status) {
			if( container && container.State ) {
				processContainer( container );
				updateBlockchainData();
			}
		});
	}, 4000 ); 
	 
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/docker/besu/pull', (message) => {
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
	
	bindFileButtons();

	$("#restartCont").click( ()=>{
		if( isDisabled( "#restartCont" ) ) return;
		log("Wait...")
		$.get("/v1/container/restart?container=besu", function(data, status) {
			// console.log( data );
		});		
	});
		
	$("#stopCont").click( ()=>{
		if( isDisabled( "#stopCont" ) ) return;
		log("Wait...")
		$.get("/v1/container/stop?container=besu", function(data, status) {
			//console.log( data );
		});
	});
	
	$("#startCont").click( ()=>{
		if( isDisabled( "#startCont" ) ) return;
		log("Wait...")
		$.get("/v1/besu/container/start", function(data, status) {
			//console.log( data );
		});
	});
	
	$("#pullImage").click( ()=>{
		if( isDisabled( "#pullImage" ) ) return;
		$("#containerLog").empty();
		updateFixedLog("");
		log('Wait ...');
		$.get("/v1/besu/image/pull", function(data, status) {
			//console.log( data );
		});
	});	
	 
});

function updateData(){
	if( working ) return;
	working = true;
	$.get("/v1/besu/config/get", function(data, status) {
		mainConfig = data;
		if( data.image.exists ){
			$("#componentTips").text( "The image is ready to launch a container. You can pull it again if you want to update to a new version.");
			setButtons('play');
			$("#imageName").text( data.image.imageName );
			if( data.container && data.container.State ) processContainer( data.container )
			
		} else $("#componentTips").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
		working = false;
	});
	
}

function updateBlockchainData(){
	$.get("/v1/besu/blockchain", function(data, status) {
		console.log( data )
		if( data.blockNumber ) {
			$("#blockchainData").html(
				'<table style="width:100%">' + 
				'<tr><td>Block Number</td><td>'+data.blockNumber+'</td></tr>' +
				'<tr><td>Connected Peers</td><td>'+data.peers.length+'</td></tr>' +
				'</table>'
			);	
		}	
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
	
	$.get("/v1/container/log?container=besu", function(data, status) {
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

function downloadFile( what ){
	window.open("/v1/besu/config/file?name=" + what);
}

function bindFileButtons() {
	
	$("#uploadFile").change(function( input ){
		
		log( "Sending " + input.target.files[0].name + " to BESU configuration folder...");
		
		var data = new FormData( $('#uploadForm')[0] );
		data.append('file', input.target.files[0] );
		$.ajax( {
			url: '/v1/besu/config/file',
			type: 'POST',
			data: data,
			processData: false,
			contentType: false
		}).then(function( data, textStatus, jqXHR ) {
			log( input.target.files[0].name + " sent.");
			log( "Backend response: " + data );
			$('#uploadFile').val(null);
		});
		
	});	
	
	$("#dlGenesis").click( ()=>{
		downloadFile('genesis');
	});	
	$(".fa-upload").click( ()=>{
		$("#uploadFile").click();
	});	
	
	$("#dlConfig").click( ()=>{
		downloadFile('config');
	});	
	
	$("#dlKey").click( ()=>{
		downloadFile('key');
	});	
	
	$("#dlPubKey").click( ()=>{
		downloadFile('keypub');
	});	

	$("#dlStaticNodes").click( ()=>{
		downloadFile('staticnodes');
	});	

	$("#dlPermissions").click( ()=>{
		downloadFile('permissions');
	});	
	$("#dlValidators").click( ()=>{
		downloadFile('validatorpool');
	});	
		
}



