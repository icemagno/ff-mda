
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
	
	
	console.log( cy );
	 
});

