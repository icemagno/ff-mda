package br.com.j1scorpii.ffmda.services;

import java.io.File;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class LocalService {
	
	private String localDataFolder = "/ffmda";
	private String localWalletFolder = localDataFolder + "/wallets";
	
	@PostConstruct
	private void init() {
		new File( localWalletFolder ).mkdirs();
	}
	
	
}
