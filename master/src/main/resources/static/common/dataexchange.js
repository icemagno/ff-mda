
let dataExchangeImageName = null;
let lastPullMessage = "";
let mainConfig = null;
let wsDE = null;

$( document ).ready(function() {

	updateData();
	const ws = new SockJS( "/ws" );
	var stompClient = Stomp.over(ws);
	stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};
	
	setInterval( ()=>{
		
		$.get("/v1/dataexchange/container/get", function( container, status) {
			if( container && container.State ) processContainer( container );
		});
		
	}, 4000 ); 
	 
	stompClient.connect( thisheaders , (frame) => {
		console.log('WebSocket Conected.');  

		stompClient.subscribe('/shell/dataexchange', (message) => {
			let payload = JSON.parse( message.body );
			console.log( payload );
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

	$("#removeCont").click( ()=>{
		if( isDisabled( "#removeCont" ) ) return;
		log('Not Implemented');
	});
		
	$("#restartCont").click( ()=>{
		if( isDisabled( "#restartCont" ) ) return;
		log("Wait...")
		$.get("/v1/dataexchange/container/restart", function(data, status) {
			console.log( data );
		});		
	});
		
	$("#stopCont").click( ()=>{
		if( isDisabled( "#stopCont" ) ) return;
		log("Wait...")
		$.get("/v1/dataexchange/container/stop", function(data, status) {
			console.log( data );
		});
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

function updateFixedLog( what ){
	$("#containerLogFixed").html( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}

function log( what ){
	if( lastPullMessage == what ) return;
	lastPullMessage = what;
	if( $("#containerLog p").length > 20 ) $("#containerLog p").first().remove();
	$("#containerLog").append( '<p style="margin:0px;padding:0px">' + what + '</p>' );
}

function isDisabled( who ){
	return $(who).hasClass( "disabled" )	
}

function enableBtn( btn ){
	$( btn ).removeClass('disabled');
	$( btn ).removeClass('btn-default');
	$( btn ).addClass('btn-primary');
}

function disableBtn( btn ){
	$( btn ).addClass('disabled');
	$( btn ).addClass('btn-default');
	$( btn ).removeClass('btn-primary');
}

function setButtons( what ){
	
	if( what == 'play' ){
		enableBtn("#startCont");
		disableBtn("#stopCont");
		disableBtn("#restartCont");
	}
	
	if( what == 'stop-restart' ){
		disableBtn("#startCont");
		enableBtn("#stopCont");
		enableBtn("#restartCont");
	}
}



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
		        // console.log( "   > " + namespace + " = " + value + "  " + typeof value );
			}
	    }
	}	
}

function updateData(){

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
		
	});
	
}

function processContainer( container ){
	let dataExchangeLocalIP = container.NetworkSettings.Networks.ffmda.IPAddress
	
	// TODO: Change this with a table. Put the local agent external IP address and host name
	// We will find this information on mainConfig
	$("#componentTips").text( container.Labels.tag + " " + container.Status + " " + dataExchangeLocalIP );
	
	$.get("/v1/dataexchange/container/log", function(data, status) {
		if( data.result ){
			$("#containerLog").empty();
			var split = data.result.split(/\r\n/);
			split.forEach( (line)=>{
				log( line );
			});
		}
	});

	if( container.State == 'running' ){
		listenToDEWebSocket();
		setButtons('stop-restart');
	} else {
		setButtons('play');
	}
}

function listenToDEWebSocket(){
	if( wsDE ) return;
	
	wsDE = new SockJS( mainConfig.localAgentConfig.ipAddress + ":" + 10205 );
	var stompDEClient = Stomp.over(wsDE);
	//stompClient.debug = null;

	var thisheaders = {
        "Origin": "*",
        "withCredentials": 'false',
	};
	 
	stompDEClient.connect( thisheaders , (frame) => {
		console.log( frame );	
	});
	
}
