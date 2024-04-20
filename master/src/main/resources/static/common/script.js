
var theCy = null;

$( document ).ready(function() {
	
	// https://blog.js.cytoscape.org/2020/05/11/layouts/#classes-of-layouts 
	theCy = cytoscape({
	  container: $('#cy')[0],
	  zoom: 1,
	  style: cytoscape.stylesheet()
	    .selector('node')
	      .css({
	    	'border-color': '#0266C8',
	    	'border-width' : '1px',
	        'shape': 'data(faveShape)',
	        'width': '120px',
	        'font-family' : 'Consolas',
	        'font-size' : '10px',
	        'content': 'data(id)',
	        'text-valign': 'center',
	        'background-color': 'data(faveColor)',
	        'color': 'data(textColor)'
	      })
	    .selector(':selected')
	      .css({
	        'border-width': '1px',
	        'border-color': 'black',
	        'background-color' : '#4D7A93',
	        'color': 'white'
	      })
	    .selector('.table')
	      .css({
	     	'border-width' : '1px',
	     	'color': 'black',
	     	'width': '75px',
	     	'font-size' : '7px',
	     	'background-color': 'white',
	     	'border-color': 'data(faveColor)',
	     	'content': 'data(description)',
	     	'background-image': 'img/gray_strips.png',
	     	'background-fit' : 'contain',
	     	'background-clip' : 'node',
	     	'background-repeat' : 'repeat',
	     	'background-image-opacity' : '0.5'
	      })
	    .selector('edge')
	      .css({
	        'opacity': 0.666,
	        'width': 1,
	        'target-arrow-shape': 'triangle',
	        'source-arrow-shape': 'circle',
	        'line-color': 'data(faveColor)',
	        'source-arrow-color': '#00933B',
	        'target-arrow-color': '#F90101'
	      })
	    .selector('edge.table')
	      .css({
	        'line-style': 'dashed',
	        'target-arrow-shape': 'triangle',
	        'source-arrow-shape': 'none',
	        'target-arrow-color': 'data(faveColor)'
	     }),

	     ready: function(){
		   //
		 }
	  
	});	 
	
	theCy.on('tap', function(){
		console.log("OnTap !");
	});
	
	theCy.on('tap', 'edge', function(){
		var sourceTag = this.data('source');
		var targetTag = this.data('target');
		var sourceNode = theCy.elements("node[id='"+sourceTag+"']");
		var targetNode = theCy.elements("node[id='"+targetTag+"']");
		console.log("OnTap + OnEdge: ");
		console.log( " > " + sourceNode.data('id') );
		console.log( " > " + targetNode.data('id') );
	});	
	
	theCy.on('tap', 'node', function(){
		console.log("OnTap + OnNode: ");
		console.log( this.data.id );
		$.each( theCy.filter('node'), function(){
			console.log( "  > " + this.data('id') );
		});
	});	
	
	if ( theCy.elements('*').size() == 0 ) {
		console.log("Nenhum elemento ainda");
	}
		
	theCy.panningEnabled( true );	
	theCy.boxSelectionEnabled(false);
	theCy.zoomingEnabled( true );
	theCy.userZoomingEnabled( false );

	populate();

});

function populate(){
	insere( "Core", "SPLIT_MAP" )
	insere( "SandBox", "SPLIT_MAP" )
	insere( "EVM Conn", "SPLIT_MAP" )
	insere( "DataExchange", "SELECT" )
	insere( "PostgreSQL", "REDUCE" )
	insere( "IPFS", "SPLIT_MAP" )
	insere( "Tokens", "SPLIT_MAP" )
	insere( "Signer", "SPLIT_MAP" )
	insere( "Besu Node", "SPLIT_MAP" )

	amarra( "SandBox", "Core" )
	amarra( "EVM Conn", "Core" )
	amarra( "Core", "EVM Conn" )
	amarra( "Core", "DataExchange" )
	amarra( "Core", "IPFS" )
	amarra( "Core", "Tokens" )
	amarra( "Core", "PostgreSQL" )
	amarra( "EVM Conn", "Signer" )
	amarra( "Signer", "Besu Node" )
	amarra( "Tokens", "EVM Conn" )
	
	// theCy.layout( {name:'breadthfirst', animate: true, fit: true, directed: true, padding: 20 } ).run();
	theCy.layout({name: 'circle'}).run();
	theCy.center();
}

function amarra( from, to ){
	var fromNode = theCy.filter('node[id = "'+from+'"]');
	var toNode = theCy.filter('node[id = "'+to+'"]');
	if( (fromNode.length > 0) && ( toNode.length > 0) ){
		console.log("Vou ligar " + from + " com " + to);
		theCy.add([{ 
			group: "edges", 
			data: { 
				source: from, 
				target: to, 
				faveColor: '#666666', 
				strength: 1 
			} 
		}]);
	}
}

function insere( tag, type ) {
		
	var textColorBlock 		= '#4D7A93';
	var nodeColor 			= "#F6F6F6";
	
	if( type == 'SELECT') {
		textColorBlock = '#F90101';
	}
	if( type == 'REDUCE') {
		textColorBlock = '#00933B';
	}
	if( type == 'SPLIT_MAP') {
		textColorBlock = '#F2B50F';
	}	
	
	theCy.add([ { 
		group: "nodes", 
		data: { 
			description: "This is a description of " + tag, 
			id: tag, 
			name: type, 
			weight: 450, 
			textColor : textColorBlock, 
			faveColor: nodeColor, 
			faveShape: 'rectangle'
		}, 
		position: { 
			x: 10, 
			y: 10 
		} 
	}]);
	
}

