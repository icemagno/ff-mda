package br.com.j1scorpii.ffmda.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;

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

	private Wallet myWallet;
	private String myPassword;
	private boolean imReady = false;
	private String myBalance = "0";
	
	
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
		
		// If we have a wallet then we are ready to go ahead.
		this.imReady = ( this.myWallet != null );
	}
	
	public String getMyWalletBalance() {
		return this.myBalance + " ETH";
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
	
	
}
