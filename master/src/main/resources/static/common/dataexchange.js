


$( document ).ready(function() {
	 console.log( "fdfdsf" );
	$.get("/v1/dataexchange/image/pulled", function(data, status) {
  		if ( data.exists ) { 
			$("#imageName").text( "Image ready to start a container. You can pull it again if you want to update to a new version." /*data.imageName*/ );
			$("#startCont").removeClass('disabled');
		} else $("#imageName").text("I will pull the image before start. This may take a few minutes depending on network speed and image size. ")
	});
	 
	 
});

