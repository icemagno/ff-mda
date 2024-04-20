
var cy = null;

$( document ).ready(function() {
	 
	cy = cytoscape({
	  container: $('#cy')[0],
	  zoom: 1,
	  pan: { x: 0, y: 0 },
	  
	  layout: {
		    name: 'breadthfirst',
		    fit: true,
		    directed: true,
		    padding: 20  
	  },
	  
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
		   window.cy = this;
		 }
	  
	});	 
	
	cy.on('tap', function(){
		console.log("OnTap !");
	});
	
	cy.on('tap', 'edge', function(){
		var sourceTag = this.data('source');
		var targetTag = this.data('target');
		var sourceNode = cy.elements("node[id='"+sourceTag+"']");
		var targetNode = cy.elements("node[id='"+targetTag+"']");
		console.log("OnTap + OnEdge: ");
		console.log( " > " + sourceNode.data('id') );
		console.log( " > " + targetNode.data('id') );
	});	
	
	cy.on('tap', 'node', function(){
		console.log("OnTap + OnNode: ");
		console.log( this.data );
		$.each( cy.filter('node'), function(){
			console.log( "  > " + this.data('id') );
		});
	});	
	
	if ( cy.elements('*').size() == 0 ) {
		console.log("Nenhum elemento ainda");
	}
		
	cy.panningEnabled( true );	
	cy.boxSelectionEnabled(false);
	cy.zoomingEnabled( true );
	cy.userZoomingEnabled( false );

	insere( "DataExchange", "SELECT" )
	insere( "PostgreSQL", "REDUCE" )
	insere( "IPFS", "SPLIT_MAP", "PostgreSQL" )
	
	
	console.log( cy );
	
	cy.load( cy.elements('*').jsons() );
	cy.pan({ x: 0, y: 0 });
	
		
	
});


function insere( tag, type, linkTo = "" ) {
		
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
	
	var sourceNode = cy.filter('node[id = "'+linkTo+'"]');
	
	cy.add([ { 
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
	
	
	if( sourceNode.length > 0 ){
		var sourceId = sourceNode.data('id');
		console.log("Vou ligar " + sourceId + " com " + tag);
		cy.add([{ 
			group: "edges", 
			data: { 
				source: sourceId, 
				target: tag, 
				faveColor: '#666666', 
				strength: 1 
			} 
		}]);
	}
	
}

