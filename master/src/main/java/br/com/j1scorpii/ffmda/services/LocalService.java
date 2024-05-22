package br.com.j1scorpii.ffmda.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import br.com.j1scorpii.ffmda.model.Wallet;
import jakarta.annotation.PostConstruct;

@Service
public class LocalService {
	private Logger logger = LoggerFactory.getLogger( LocalService.class );
	
	@Value("${ffmda.local.data.folder}")
	private String localDataFolder;	

	private String localWalletFolder;
	private String myPasswordFile;
	private String myConfigFile;

	private Wallet myWallet;
	private String myPassword;
	private boolean imReady = false;
	private String myBalance = "0";
	private JSONObject mainConfig;
	
	@Autowired private PKIManagerService pkiManager;
	@Autowired private ContainerManager containerManager;
	
	@PostConstruct
	private void init() {
		
		localWalletFolder 	= localDataFolder + "/wallets";
		myPasswordFile 		= localDataFolder + "/.password.txt";
		myConfigFile		= localDataFolder + "/config.json";		
		
		logger.info("init");
		
		File f = new File( localWalletFolder );
		
		// Just to be sure the folder exists
		f.mkdirs();
		
		// We need a password to manage the wallet
		this.readPassword();
		
		// If we don't have any password at this point then everything was broken
		if( this.myPassword == null ) {
			logger.error("can't create the wallet password. ABORTING!");
			return;
		}
		
		// ... in any case, check if we have a wallet ( key address ) already
		// Must be just one file here that is my wallet. If we have more than one
		// for some strange reason, use the first one.
		File[] listOfFiles = f.listFiles();
		try {
			if( listOfFiles.length == 0 ){
				logger.info("No wallet found. Will create one.");
				createWallet();
				logger.info("Wallet created. My address is " + myWallet.getAddress() );
			} else {
				loadWallet( listOfFiles[0] );
				logger.info("Wallet found. My address is " + myWallet.getAddress() );				
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Load this local agent configuration
		try {
			if( new File( myConfigFile ).exists() ) {
				logger.info("Configuration file found.");
				this.loadConfig();
			} else {
				logger.info("Configuration file not found. Will create one.");
				this.mainConfig = new JSONObject();
				JSONObject stackStatus = new JSONObject();
				
				stackStatus
				.put("locked", false)
				.put("dataExchange", false)
				.put("postgresql", false)
				.put("ipfs", false)
				.put("besu", false)
				.put("tokens", false)
				.put("signer", false)
				.put("evmConn", false)
				.put("sandbox", false)
				.put("core", false);
				
				this.mainConfig.put("orgName", "");
				this.mainConfig.put("nodeName", "");
				// TODO: Try to get IP and name from Docker NAT
				this.mainConfig.put("hostName", "");
				this.mainConfig.put("ipAddress", "");
				this.mainConfig.put("stackStatus", stackStatus);
				
				saveConfig();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		// If we have a wallet and a valid config data then we are ready to go ahead.
		this.imReady = ( this.myWallet != null && this.mainConfig != null && this.mainConfig.has("orgName") );
		
		// Start the PKI Manager
		if( this.imReady ) {
			// Just to avoid someone unlock the config by changing the JSON config file
			// we will check if we have the CA files (certificate) already created. 
			// If so, will lock config again
			if( this.pkiManager.caWasCreated() ) {
				this.mainConfig.getJSONObject("stackStatus").put("locked", true);	
				try { this.saveConfig(); } catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
	
	public PKIManagerService getPkiManager() {
		return this.pkiManager;
	}
	
	// Read password from file or create a new one.
	private boolean readPassword() {
		try {
			File pFile = new File( myPasswordFile );
			if( pFile.exists() ) {
				BufferedReader reader = new BufferedReader( new FileReader( myPasswordFile ) );
				this.myPassword = reader.readLine();
				reader.close();
				
			} else {
				this.myPassword = UUID.randomUUID().toString();				
				BufferedWriter writer = new BufferedWriter( new FileWriter( myPasswordFile , true) );
				writer.write( this.myPassword );
				writer.close();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	// Get my wallet data from file. Since we was not stored the Wallet object at creation stage, we don't have the mnemonic anymore
	// If you think this could be useful then store it in "createWallet()"
	private void loadWallet( File f ) throws Exception {
		Credentials credentials = WalletUtils.loadCredentials( this.myPassword, f );
		ECKeyPair privateKey = credentials.getEcKeyPair();
		this.myWallet = new Wallet(credentials.getAddress(), null, privateKey.getPublicKey().toString(16), privateKey.getPrivateKey().toString(16) );
		/*		
			{
			     "address": "0x51956efe0a1e998f901458a449876afaef80fdb3",
			     "privk": "61cd0dc3d2de6d2c8415de3a81f2385c96290598695798dc765b60386cfa31f6",
			     "pubk": "6acd8cbfe5f6803a14ce8d60fead8e8fca8d96cc744079d0c671a1e084ab6f1813c009d0686c671561820f6f99f2abef6645dacbe5755ce89a37ef948210392"
			}	
		*/		
	}
	
	// Create my wallet ( private and public blockchain keys also the mnemonic )
	// But I think we don't need to store the mnemonic
	private void createWallet( ) throws Exception {
		Bip39Wallet wallet = WalletUtils.generateBip39Wallet( this.myPassword, new File( localWalletFolder ) );
		Credentials credentials = WalletUtils.loadBip39Credentials( this.myPassword, wallet.getMnemonic() );	
		ECKeyPair privateKey = credentials.getEcKeyPair();
		this.myWallet = new Wallet(credentials.getAddress(), wallet.getMnemonic(), privateKey.getPublicKey().toString(16), privateKey.getPrivateKey().toString(16) );
	}
	
	
	// Save Organization name and Node name to configuration
	public JSONObject saveOrgData(String data) throws Exception {
		// Will only save Org config if it is open
		if ( this.mainConfig.getJSONObject("stackStatus").getBoolean("locked") == false ) {
			JSONObject obj = new JSONObject( data );
			if( obj.has("data") ) {
				obj = obj.getJSONObject("data");
				if( obj.has("orgName") ) this.mainConfig.put("orgName", obj.getString("orgName") );
				if( obj.has("nodeName") ) this.mainConfig.put("nodeName", obj.getString("nodeName") );
				if( obj.has("ipAddress") ) this.mainConfig.put("ipAddress", obj.getString("ipAddress") );
				if( obj.has("hostName") ) this.mainConfig.put("hostName", obj.getString("hostName") );	
				// Lock this part of configuration
				this.mainConfig.getJSONObject("stackStatus").put("locked", true);	
				this.saveConfig();
				
				// Create the Certificate Authority for all Conglomerate
				// Because it is running on the Master Agent, this server will 
				// represent the Certificate Authority that will sign the certificates of the Data Exchange nodes.
				this.pkiManager.genAc( "ff-mda", obj.getString("orgName"), obj.getString("nodeName") );
			}
		}
		// Just return current config ( changed or not )
		return this.mainConfig;
	}

	// Reload configuration from disk. Useful when someone edits the JSON file directly
	public void reloadConfig() {
		try {
			loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}		
	
	private String readFile(String path, Charset encoding)  throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	private void loadConfig() throws Exception {
		// REad config file from disk
		String content = readFile( myConfigFile , StandardCharsets.UTF_8);
		this.mainConfig = new JSONObject(content);
		
		// Get the stack situation
		this.mainConfig.getJSONObject("stackStatus").put("dataExchange", containerManager.getContainer( "dataexchange" ) );
		this.mainConfig.getJSONObject("stackStatus").put("postgresql", containerManager.getContainer( "postgresql" ) );
		this.mainConfig.getJSONObject("stackStatus").put("ipfs", containerManager.getContainer( "ipfs" ) );
		this.mainConfig.getJSONObject("stackStatus").put("besu", containerManager.getContainer( "besu" ) );
		this.mainConfig.getJSONObject("stackStatus").put("core", containerManager.getContainer( "core" ) );
		this.mainConfig.getJSONObject("stackStatus").put("sandbox", containerManager.getContainer( "sandbox" ) );
		this.mainConfig.getJSONObject("stackStatus").put("signer", containerManager.getContainer( "signer" ) );
		this.mainConfig.getJSONObject("stackStatus").put("tokens", containerManager.getContainer( "tokens" ) );
		this.mainConfig.getJSONObject("stackStatus").put("evmConn", containerManager.getContainer( "evmconnect" ) );
		
	}
	
	public String getMyWalletBalance() {
		return this.myBalance + " ETH";
	}
	
	public JSONObject getMainConfig() {
		reloadConfig();
		return this.mainConfig;
	}
	
	public boolean amIReady() {
		return this.imReady;
	}
	
	public Wallet getWallet() {
		return this.myWallet;
	}
	
	private void saveConfig() throws Exception {
		BufferedWriter writer = new BufferedWriter( new FileWriter( myConfigFile) );
		writer.write( this.mainConfig.toString() );
		writer.close();			
	}

	public Resource getOrgCertificateFile() throws Exception {
	    return this.getPkiManager().getOrgCertificateFile();	
	}	
	
}
