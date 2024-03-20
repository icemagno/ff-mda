package br.com.j1scorpii.ffmda.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
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
    
    
    //private void genAc( String certAlias, String keyStoreFile, String certificateFile, String storePassword, String privateKeyPassword, X500Name subjectName, X500Name issuerName ) {
    public void genAc( ) {	
        X500Name acIssuerName = new X500Name("CN=TESTE Autoridade Certificadora Raiz Brasileira, O=ICP-Brasil, OU=Instituto Nacional de Tecnologia da Informacao - ITI, OU=CERTIFICADO DE TESTE,ST=TESTE CASNAV, C=BR");
        try {
        	char[] pkPassword = this.password.toCharArray();
        	KeyPair keyPair = generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            // Self sign key. My PK will sign my own certificate
            PrivateKey certSignerPrivateKey = keyPair.getPrivate();
            X509Certificate cert = createCert( acIssuerName, acIssuerName, this.issuerCertificateFile, publicKey, certSignerPrivateKey );
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
    
    
    
    private X509Certificate createCert(X500Name issuerName, X500Name subjectName, String certFilePath, PublicKey publicKey, PrivateKey certSignerPrivateKey ) throws Exception {
        BigInteger serial = BigInteger.valueOf(new SecureRandom().nextInt());
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        Date BEFORE = today;
        calendar.add(Calendar.YEAR, 1);
        Date nextYear = calendar.getTime();
        Date AFTER = nextYear;
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, serial, BEFORE, AFTER, subjectName, publicKey);
        builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(publicKey));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
        builder.addExtension(Extension.keyUsage, false, usage);
        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth);
        purposes.add(KeyPurposeId.id_kp_clientAuth);
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);
        builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));
        X509Certificate cert = signCertificate(builder, certSignerPrivateKey);
		File fil = new File(certFilePath);
		FileOutputStream fos = new FileOutputStream( fil );
		fos.write( cert.getEncoded() );
		fos.flush();
		fos.close();
        return cert;
    }	    
    
    
    private SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws Exception {
        ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()));
        ASN1Sequence seq = (ASN1Sequence) is.readObject();
        is.close();
        @SuppressWarnings("deprecation")
        SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(seq);
        return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
    }    
    
    
    private X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder, PrivateKey signedWithPrivateKey) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER_NAME).build(signedWithPrivateKey);
        return new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(certificateBuilder.build(signer));
    }    
    
	

}
