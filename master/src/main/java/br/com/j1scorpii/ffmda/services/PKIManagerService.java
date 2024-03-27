package br.com.j1scorpii.ffmda.services;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class PKIManagerService {
	private Logger logger = LoggerFactory.getLogger( PKIManagerService.class );
	private String pkiFolder;
	private String certsFolder;
    private String issuerPemCertFile;    
	
	@Autowired private ShellRunnerService shellRunnerService;
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;		

	@PostConstruct
	private void init() {
		this.pkiFolder = this.localDataFolder + "/pki";
		this.certsFolder = this.pkiFolder + "/certs";
		new File( this.certsFolder ).mkdirs();	
		// this.issuerPemPrivKeyFile 	= pkiFolder + "/ca-key.pem";
	    this.issuerPemCertFile 		= pkiFolder + "/ca-cert.pem";
	    
		logger.info("init " + this.pkiFolder );

	}

	public boolean caWasCreated() {
		return new File(this.issuerPemCertFile).exists();
	}

	// ./create-sign.sh '/CN=Node-01' /home/suporte/ff-mda/master/node-test /home/suporte/ff-mda/master/pki-test
	public void createAndSignKeysAndCert(String componentName, String commonName, String toFolder) {
		String callbackChannel = "/shell/"+componentName;
	    String hostCn = "/CN="+commonName+"/OU=FireFly/OU=Multiparty Deployer Agent";
	    String[] command = { "./create-sign.sh", hostCn , toFolder, this.pkiFolder };
	    this.shellRunnerService.runShell(command, callbackChannel);
	}

	//	./create-ca.sh "/CN=Common/O=Organization 01/OU=FireFly/OU=Multiparty Deployer Agent" /home/suporte/ff-mda/master/abc
	public void genAc( String componentName, String organization, String nodeName ) {
		String callbackChannel = "/shell/"+componentName;
        String acCn = "/CN=" + nodeName + 
        		"/O=" + organization + " CA" +
        		"/OU=FireFly" +
        		"/OU=Multiparty Deployer Agent";

        String[] command = { "./create-ca.sh", acCn , this.pkiFolder };
        this.shellRunnerService.runShell(command, callbackChannel );
	}

}


