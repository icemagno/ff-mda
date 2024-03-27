package br.com.j1scorpii.ffmda.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemWriter;


public class PKIManager {
	private String localDataFolder;
	private String pkiFolder;
	private String certsFolder;
	private String password;
    private final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;
    private final String SIGNATURE_ALGORITHM = "SHA1WithRSA";
    private final String KEY_GENERATION_ALGORITHM = "RSA";
    private String pkiACAlias;
    private String issuerKeyStore;
    private String issuerCertificateFile;
    private String issuerPemPrivKeyFile;
    private String issuerPemCertFile;    
    
    
    public PKIManager( String localDataFolder, String password, String pkiACAlias ) {
		this.localDataFolder = localDataFolder;
		this.password = password;
		this.pkiFolder = this.localDataFolder + "/pki";
		this.certsFolder = this.pkiFolder + "/certs";
		new File( this.certsFolder ).mkdirs();
		Security.addProvider( new BouncyCastleProvider() );
		this.pkiACAlias = pkiACAlias;
		
	    this.issuerKeyStore			= pkiFolder + "/pki-keystore.jks";
	    this.issuerCertificateFile 	= pkiFolder + "/pki-certificate.cer";
	    this.issuerPemPrivKeyFile 	= pkiFolder + "/pki-pem-private.key.pem";
	    this.issuerPemCertFile 		= pkiFolder + "/pki-pem-cert.cer.pem";		
	}
    
    /*
		CN: CommonName
		OU: OrganizationalUnit
		O: Organization
		L: Locality
		S: StateOrProvinceName
		C: CountryName   
    */
    public void genAc( String organization, String nodeName ) {	
        X500Name acIssuerName = new X500Name(
        		"CN=" + nodeName + 
        		", O=" + organization + " CA " +
        		", OU=FireFly" +
        		", OU=Multiparty Deployer Agent");
        try {
        	char[] pkPassword = this.password.toCharArray();
        	KeyPair keyPair = generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            // Self sign key. My PK will sign my own certificate
            PrivateKey certSignerPrivateKey = keyPair.getPrivate();
            X509Certificate cert = createCert( null, acIssuerName, acIssuerName, this.issuerCertificateFile, publicKey, certSignerPrivateKey );
            X509Certificate[] outChain = { cert };
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load( null , pkPassword );
            ks.setKeyEntry( this.pkiACAlias, certSignerPrivateKey, pkPassword, outChain);
            OutputStream writeStream = new FileOutputStream( this.issuerKeyStore );
            ks.store( writeStream, pkPassword );
            writeStream.close();
            
            savePrivKey( this.issuerPemPrivKeyFile, certSignerPrivateKey );
            saveCertificate( this.issuerPemCertFile, cert);	            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean caWasCreated() {
    	return new File(this.issuerCertificateFile).exists();
    }
    
    private void saveCertificate( String file, X509Certificate cert) throws IOException {
    	StringWriter sw = new StringWriter();
    	try (PemWriter pw = new PemWriter(sw)) {
    	  PemObjectGenerator genCer = new JcaMiscPEMGenerator(cert);
    	  pw.writeObject(genCer);
    	}
    	PrintWriter writer = new PrintWriter( file );
    	writer.println( sw.toString() );
    	writer.close();    	
    }    
    
    private void savePrivKey(String file, PrivateKey key) throws Exception {
    	StringWriter sw = new StringWriter();
    	try (PemWriter pw = new PemWriter(sw)) {
    	  PemObjectGenerator genKey = new JcaPKCS8Generator(key, null );
    	  pw.writeObject(genKey);
    	}
    	PrintWriter writer = new PrintWriter( file );
    	writer.println( sw.toString() );
    	writer.close();    	
    }    
    
    
    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( KEY_GENERATION_ALGORITHM, PROVIDER_NAME );
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }
    
    private X509Certificate doCreateCert(
    		X509Certificate authorityCert,
    		X500Name issuerName, 
    		X500Name subjectName, 
    		String certFilePath, 
    		PublicKey publicKey, 
    		PrivateKey certSignerPrivateKey, 
    		boolean saveCert ) throws Exception {
    	
        BigInteger serial = BigInteger.valueOf(new SecureRandom().nextInt());
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        Date BEFORE = today;
        calendar.add(Calendar.YEAR, 100);
        Date nextYear = calendar.getTime();
        Date AFTER = nextYear;
        
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, BEFORE, AFTER, subjectName, publicKey);
        KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
        
        builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(publicKey));
        if( authorityCert != null ) builder.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyIdentifier( authorityCert.getPublicKey() ));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, false, usage);

        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth);
        purposes.add(KeyPurposeId.id_kp_clientAuth);
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);
        builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));

        X509Certificate cert = signCertificate(builder, certSignerPrivateKey);
        
        if( saveCert ) {
			File fil = new File(certFilePath);
			FileOutputStream fos = new FileOutputStream( fil );
			fos.write( cert.getEncoded() );
			fos.flush();
			fos.close();
        }
        return cert;
    }    

    
    private X509Certificate createCert(
    		X509Certificate authorityCert, 
    		X500Name issuerName, 
    		X500Name subjectName, 
    		String certFilePath, 
    		PublicKey publicKey, 
    		PrivateKey certSignerPrivateKey ) throws Exception {
    	// This is for reorder the RDN order. When I create the X500 name by myself using string the certificate 
    	// change the order and can't find the certificate signer later at createUserCertAndSignWithAC() result
    	// even when the issuer is the actually the same. For some reason getIssuerX500Principal give the names in another order.
    	// I've decided to make the BoucyCastle always tell the order instead I change it in the string on "new X500Name( ... )"  
    	// so I create a temp cert and take the names in correct order with theTempCert.getIssuerX500Principal()
    	X509Certificate theTempCert = doCreateCert( authorityCert, issuerName, subjectName, certFilePath, publicKey, certSignerPrivateKey, false);
    	X500Name correctIssuerName = new X500Name(  theTempCert.getIssuerX500Principal().toString() );
    	X500Name correctSubjectName = new X500Name(  theTempCert.getSubjectX500Principal().toString() );
    	X509Certificate realCert = doCreateCert(authorityCert, correctIssuerName, correctSubjectName, certFilePath, publicKey, certSignerPrivateKey, true);
    	return realCert;
    }     
    
    
    private SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws Exception {
    	byte[] encoded = key.getEncoded();
    	SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance( ASN1Sequence.getInstance(encoded) );
        return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
    	/*
        ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()));
        ASN1Sequence seq = (ASN1Sequence) is.readObject();
        is.close();
        @SuppressWarnings("deprecation")
        SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(seq);
        return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
        */
    }
    
    private AuthorityKeyIdentifier createAuthorityKeyIdentifier( Key key ) throws Exception {
    	byte[] encoded = key.getEncoded();
    	SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance( ASN1Sequence.getInstance(encoded) );
        return new BcX509ExtensionUtils().createAuthorityKeyIdentifier(info);
    }    
    
    private X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder, PrivateKey signedWithPrivateKey) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER_NAME).build(signedWithPrivateKey);
        return new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificateBuilder.build(signer));
    }    
    
    
	public void createAndSignKeysAndCert( String commonName, String toFolder) {
		new File(toFolder).mkdirs();
        X500Name requerente = new X500Name("CN="+commonName+", OU=Multiparty Deployer Agent, OU=FireFly");
		createUserCertAndSignWithAC( commonName, toFolder, requerente );
	}
	
    private void createUserCertAndSignWithAC( String commonName, String toFolder,  X500Name subjectName ) {
    	
        String certificateFile = toFolder + "/" + commonName + ".cer";
	    String pemPrivKeyFile = toFolder + "/key.pem";
	    String pemCertFile = toFolder + "/cert.pem"; 
        
    	try {
        	char[] pkPassword = this.password.toCharArray();
        	
        	KeyPair keyPair = generateKeyPair();
            PublicKey userPublicKey = keyPair.getPublic();
       	
        	
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load( getKeyStore( this.issuerKeyStore ) , pkPassword );
            PrivateKey certSignerPrivateKey = (PrivateKey)ks.getKey( this.pkiACAlias, pkPassword );        	

            X509Certificate signerCert = (X509Certificate) ks.getCertificate(this.pkiACAlias);
            X500Name acIssuerName = new X500Name( signerCert.getIssuerX500Principal().getName() );
          
            
            X509Certificate cert = createCert( signerCert, acIssuerName, subjectName, certificateFile, userPublicKey, certSignerPrivateKey );
            X509Certificate[] outChain = { cert };

            PublicKey certSignerPublicKey = signerCert.getPublicKey();
            cert.checkValidity( new Date() );
            cert.verify( certSignerPublicKey );            
            
            
            ks.setKeyEntry( commonName, certSignerPrivateKey, pkPassword, outChain);
            OutputStream writeStream = new FileOutputStream( this.issuerKeyStore );
            ks.store( writeStream, pkPassword );
            writeStream.close();
            
            savePrivKey( pemPrivKeyFile, certSignerPrivateKey );
            saveCertificate( pemCertFile, cert);	 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    	
    	
    }
    
    private InputStream getKeyStore( String fileName  ) throws Exception {
    	File fil = new File(fileName );
    	if ( fil.exists() ) {
    		return new FileInputStream( fileName );
    	}
    	return null;
    }    
    

}
