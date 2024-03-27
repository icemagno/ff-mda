package br.com.j1scorpii.ffmda.util;

import org.bouncycastle.asn1.x500.X500Name;

public class PKIManager {

	public PKIManager(String localDataFolder, String myPassword, String string) {
		// TODO Auto-generated constructor stub
	}

	public boolean caWasCreated() {
		// TODO Auto-generated method stub
		return false;
	}

	public void createAndSignKeysAndCert(String hostName, String componentDataFolder) {
		// TODO Auto-generated method stub
		
	}

	public void genAc( String organization, String nodeName ) {
        X500Name acIssuerName = new X500Name(
        		"CN=" + nodeName + 
        		", O=" + organization + " CA " +
        		", OU=FireFly" +
        		", OU=Multiparty Deployer Agent");
		
	}

}


//	"/CN=Common/O=Organization 01/OU=FireFly/OU=Multiparty Deployer Agent"

