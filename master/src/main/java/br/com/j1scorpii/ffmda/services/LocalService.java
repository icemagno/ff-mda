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
	
	private String localDataFolder 		= "/ffmda";
	private String localWalletFolder 	= localDataFolder + "/wallets";
	private String myPasswordFile 		= localDataFolder + "/password.txt";
	private String myConfigFile			= localDataFolder + "/agent-config.json";

	private Wallet myWallet;
	private String myPassword;
	private boolean imReady = false;
	private String myBalance = "0";
	private JSONObject agentConfig;
	
	@PostConstruct
	private void init() {
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
				this.agentConfig = new JSONObject();
				JSONObject stackStatus = new JSONObject();
				
				stackStatus
				.put("locked", false)
				.put("dataExchangeOk", false)
				.put("postgresOk", false)
				.put("ipfsOk", false)
				.put("besuOk", false)
				.put("tokensOk", false)
				.put("signerOk", false)
				.put("evmConnOk", false)
				.put("sandboxOk", false)
				.put("coreOk", false);
				
				this.agentConfig.put("orgName", "");
				this.agentConfig.put("nodeName", "");
				this.agentConfig.put("stackStatus", stackStatus);
				
				saveConfig();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		// If we have a wallet and a valid config data then we are ready to go ahead.
		this.imReady = ( this.myWallet != null && this.agentConfig != null && this.agentConfig.has("orgName") );
	}
	
	private void saveConfig() throws Exception {
		BufferedWriter writer = new BufferedWriter( new FileWriter( myConfigFile) );
		writer.write( this.agentConfig.toString() );
		writer.close();			
	}
	
	public String getMyWalletBalance() {
		return this.myBalance + " ETH";
	}
	
	public JSONObject getAgentConfig() {
		return this.agentConfig;
	}
	
	public boolean amIReady() {
		return this.imReady;
	}
	
	public Wallet getWallet() {
		return myWallet;
	}
	
	private void readPassword() {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// Get my wallet data from file. Since we was not stored the Wallet object at creation stage, we don't have the mnemonic anymore
	// If you think this could be useful then store it in "createWallet()"
	private void loadWallet( File f ) throws Exception {
		Credentials credentials = WalletUtils.loadCredentials( this.myPassword, f );
		ECKeyPair privateKey = credentials.getEcKeyPair();
		this.myWallet = new Wallet(credentials.getAddress(), null, privateKey.getPublicKey().toString(16), privateKey.getPrivateKey().toString(16) );
	}
	
	// Create my wallet ( private and public blockchain keys also the mnemonic )
	// But I think we don't need to store the mnemonic
	private void createWallet( ) throws Exception {
		Bip39Wallet wallet = WalletUtils.generateBip39Wallet( this.myPassword, new File( localWalletFolder ) );
		Credentials credentials = WalletUtils.loadBip39Credentials( this.myPassword, wallet.getMnemonic() );	
		ECKeyPair privateKey = credentials.getEcKeyPair();
		this.myWallet = new Wallet(credentials.getAddress(), wallet.getMnemonic(), privateKey.getPublicKey().toString(16), privateKey.getPrivateKey().toString(16) );
	}
	
	
	private String readFile(String path, Charset encoding)  throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	private void loadConfig() throws Exception {
		String content = readFile( myConfigFile , StandardCharsets.UTF_8);
		this.agentConfig = new JSONObject(content);		
	}

	// Save Organization name and NOde name to configuration
	public JSONObject saveOrgAndNodeNames(String data) throws Exception {
		JSONObject obj = new JSONObject( data );
		if( obj.has("data") ) {
			obj = obj.getJSONObject("data");
			if( obj.has("orgName") ) this.agentConfig.put("orgName", obj.getString("orgName") );
			if( obj.has("nodeName") ) this.agentConfig.put("nodeName", obj.getString("nodeName") );
			this.saveConfig();
		}
		return this.agentConfig;
	}

	// Reload configuration from disk. Useful when someone edits the JSON file directly
	public void reloadConfig() {
		try {
			loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}		
	
	
	
	
}
