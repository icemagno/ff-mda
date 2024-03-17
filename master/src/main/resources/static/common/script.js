


$( document ).ready(function() {
	 getThisNode();
});

function getThisNode() {
	$( '#thisNode' ).append( formatCard('Data Exchange', '') );
	$( '#thisNode' ).append( formatCard('EthConnector', '') );
	$( '#thisNode' ).append( formatCard('Besu Node', '' ) );
	$( '#thisNode' ).append( formatCard('IPFS Peer','' ) );
	$( '#thisNode' ).append( formatCard('FireFly Core','') );
	$( '#thisNode' ).append( formatCard('Token', '') );
	$( '#thisNode' ).append( formatCard('Prometheus', '') );
	$( '#thisNode' ).append( formatCard('PostgreSQL Database', '') );
	$( '#thisNode' ).append( formatCard('Eth Signer', '') );
}


function formatCard( title, subtitle ){
	var aCard = '<div class="col-md-2 col-sm-6 col-xs-12">' +
          '<div class="info-box">' +
            '<span class="info-box-icon bg-gray"><i class="ion ion-ios-gear-outline"></i></span>' +
            '<div class="info-box-content">' +
              '<span class="info-box-text">'+title+'</span>' +
              '<span class="info-box-number"><small>'+subtitle+'</small></span>' +
            '</div>' +
          '</div>' +
        '</div>';	
	return aCard;
}