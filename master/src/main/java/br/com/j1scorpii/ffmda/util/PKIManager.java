package br.com.j1scorpii.ffmda.util;

import java.io.File;

import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.j1scorpii.ffmda.services.ShellRunnerService;

public class PKIManager {
	private String localDataFolder;
	private String pkiFolder;
	private String certsFolder;
	private String issuerPemPrivKeyFile;
    private String issuerPemCertFile;    
	
	@Autowired private ShellRunnerService shellRunnerService;
	
	public PKIManager(String localDataFolder ) {
		this.localDataFolder = localDataFolder;
		this.pkiFolder = this.localDataFolder + "/pki";
		this.certsFolder = this.pkiFolder + "/certs";
		new File( this.certsFolder ).mkdirs();	
		this.issuerPemPrivKeyFile 	= pkiFolder + "/ca-key.pem";
	    this.issuerPemCertFile 		= pkiFolder + "/ca-cert.pem";			
	}

	public boolean caWasCreated() {
		return new File(this.issuerPemCertFile).exists();
	}

	// ./create-sign.sh '/CN=Node-01' /home/suporte/ff-mda/master/node-test /home/suporte/ff-mda/master/pki-test
	public void createAndSignKeysAndCert(String commonName, String toFolder) {
	    String hostCn = "CN="+commonName+", OU=Multiparty Deployer Agent, OU=FireFly";
	    String[] command = { "./create-sign.sh", hostCn , toFolder, this.pkiFolder };
	    this.shellRunnerService.runShell(command);
	}

	//	./create-ca.sh "/CN=Common/O=Organization 01/OU=FireFly/OU=Multiparty Deployer Agent" /home/suporte/ff-mda/master/abc
	public void genAc( String organization, String nodeName ) {
        String acCn = "'CN=" + nodeName + 
        		", O=" + organization + " CA " +
        		", OU=FireFly" +
        		", OU=Multiparty Deployer Agent'";

        String[] command = { "./create-ca.sh", acCn , this.pkiFolder };
        this.shellRunnerService.runShell(command);
	}

}


