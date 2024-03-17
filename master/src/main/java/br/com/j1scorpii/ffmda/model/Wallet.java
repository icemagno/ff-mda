package br.com.j1scorpii.ffmda.model;

public class Wallet {
	private String address;
	private String mnemonic;
	private String pubk;
	private String privk;
	
	public Wallet(String address, String mnemonic, String pubk, String privk) {
		super();
		this.address = address;
		this.mnemonic = mnemonic;
		this.pubk = pubk;
		this.privk = privk;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getMnemonic() {
		return mnemonic;
	}
	public void setMnemonic(String mnemonic) {
		this.mnemonic = mnemonic;
	}
	public String getPubk() {
		return pubk;
	}
	public void setPubk(String pubk) {
		this.pubk = pubk;
	}
	public String getPrivk() {
		return privk;
	}
	public void setPrivk(String privk) {
		this.privk = privk;
	}
	
}
