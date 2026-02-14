package com.neuwton.utils;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import static com.neuwton.utils.TestConstants.*;

public class CertChainGeneratorUtil {

    public static final String CA_REPOSITORY_URL = "http://localhost:8080";

    public static KeyStore generateFullChainKeyStoreJKS(String path) throws Exception {
        return generateKeyStoreFullChain("RSA", path);
    }

    public static KeyStore generateRootSignedCertKeyStoreJKS(String path) throws Exception {
        return generateKeyStoreRootSignedFullChain("RSA", path);
    }

    public static KeyStore generateSelfSignedCertKeyStoreJKS(String path) throws Exception {
        return generateKeyStoreSelfSigned("RSA", path);
    }

    public static KeyStore generateKeyStoreFullChain(String algorithm, String path) throws Exception {
        KeyPair leafCertKeyPair = generateKeyPair("RSA", 4096);
        String prefix = algorithm.toLowerCase();

        // Step 1: Generate Root CA
        KeyPair rootKeyPair = generateKeyPair(algorithm, algorithm.equals("RSA") ? 4096 : "secp521r1");
        X509Certificate rootCACert = generateRootCACertificate(rootKeyPair, algorithm);

        String rootCertFilename = prefix + "-root-ca.crt";

        // Step 2: Generate Intermediate CA with reference to Root CA
        KeyPair intermediateKeyPair = generateKeyPair(algorithm, algorithm.equals("RSA") ? 3072 : "secp384r1");
        X509Certificate intermediateCACert = generateIntermediateCACertificate(
                "Intermediate CA",
                intermediateKeyPair.getPublic(),
                rootCACert,
                rootKeyPair.getPrivate(),
                algorithm,
                CA_REPOSITORY_URL + rootCertFilename,  // AIA pointing to root CA
                3650  // 10 years
        );

        String intermediateCertFilename = prefix + "-intermediate-ca.crt";

        // Step 3: Generate Server Certificate with reference to Intermediate CA
        X509Certificate serverCert = generateEndEntityCertificate(
                "localhost",
                leafCertKeyPair.getPublic(),
                intermediateCACert,
                intermediateKeyPair.getPrivate(),
                algorithm,
                CA_REPOSITORY_URL + intermediateCertFilename,  // AIA pointing to intermediate CA
                false
        );

        return generateKeystoreFile("localhost", CHANGE_IT.toCharArray(), leafCertKeyPair.getPrivate(), path, serverCert, intermediateCACert, rootCACert);
    }

    public static KeyStore generateKeyStoreRootSignedFullChain(String algorithm, String path) throws Exception {
        KeyPair leafCertKeyPair = generateKeyPair("RSA", 4096);
        String prefix = algorithm.toLowerCase();

        // Step 1: Generate Root CA
        KeyPair rootKeyPair = generateKeyPair(algorithm, algorithm.equals("RSA") ? 4096 : "secp521r1");
        X509Certificate rootCACert = generateRootCACertificate(rootKeyPair, algorithm);

        String rootCertFilename = prefix + "-root-ca.crt";

        // Step 2: Generate Server Certificate with reference to Root CA
        X509Certificate serverCert = generateEndEntityCertificate(
                "localhost",
                leafCertKeyPair.getPublic(),
                rootCACert,
                rootKeyPair.getPrivate(),
                algorithm,
                CA_REPOSITORY_URL + rootCertFilename,  // AIA pointing to intermediate CA
                false
        );

        return generateKeystoreFile("localhost", CHANGE_IT.toCharArray(), leafCertKeyPair.getPrivate(), path, serverCert, rootCACert);
    }

    public static KeyStore generateKeyStoreSelfSigned(String algorithm, String path) throws Exception {
        String prefix = algorithm.toLowerCase();

        // Step 1: Generate Root CA
        KeyPair rootKeyPair = generateKeyPair(algorithm, algorithm.equals("RSA") ? 4096 : "secp521r1");
        X509Certificate rootCACert = generateSelfSignedCertificate(rootKeyPair, algorithm);

        return generateKeystoreFile("localhost", CHANGE_IT.toCharArray(), rootKeyPair.getPrivate(), path, rootCACert);
    }

    public static String generateCertificateChainText(String algorithm, KeyPair keyPair) throws Exception {
        String prefix = algorithm.toLowerCase();

        // Step 1: Generate Root CA
        KeyPair rootKeyPair = generateKeyPair(algorithm, algorithm.equals("RSA") ? 4096 : "secp521r1");
        X509Certificate rootCACert = generateRootCACertificate(rootKeyPair, algorithm);

        String rootCertFilename = prefix + "-root-ca.crt";

        // Step 2: Generate Intermediate CA with reference to Root CA
        KeyPair intermediateKeyPair = generateKeyPair(algorithm, algorithm.equals("RSA") ? 3072 : "secp384r1");
        X509Certificate intermediateCACert = generateIntermediateCACertificate(
                "Intermediate CA",
                intermediateKeyPair.getPublic(),
                rootCACert,
                rootKeyPair.getPrivate(),
                algorithm,
                CA_REPOSITORY_URL + rootCertFilename,  // AIA pointing to root CA
                3650  // 10 years
        );

        String intermediateCertFilename = prefix + "-intermediate-ca.crt";

        // Step 3: Generate Server Certificate with reference to Intermediate CA
        X509Certificate serverCert = generateEndEntityCertificate(
                "localhost",
                keyPair.getPublic(),
                intermediateCACert,
                intermediateKeyPair.getPrivate(),
                algorithm,
                CA_REPOSITORY_URL + intermediateCertFilename,  // AIA pointing to intermediate CA
                false
        );

        // Create a combined PEM file with server cert + intermediate CA + root CA
        String combinedChainPEM = certificateToPEM(serverCert) +
                certificateToPEM(intermediateCACert) +
                certificateToPEM(rootCACert);

        System.out.println("combinedChainPEM:-\n"+combinedChainPEM);

        // Convert the PEM chain to Base64
        byte[] chainBytes = combinedChainPEM.getBytes(StandardCharsets.UTF_8);
        String base64EncodedChain = Base64.getEncoder().encodeToString(chainBytes);

        System.out.println("base64EncodedChain:-\n"+base64EncodedChain);

        System.out.println("Generated " + algorithm + " certificate chain as Base64 encoded string");
        return base64EncodedChain;
    }

    /**
     * Generate a key pair with the specified algorithm and strength
     */
    public static KeyPair generateKeyPair(String algorithm, Object strength) throws Exception {
        if (algorithm.equals("RSA")) {
            // Generate RSA key pair
            int keySize = (Integer) strength;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
            keyPairGenerator.initialize(keySize, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } else if (algorithm.equals("EC")) {
            // Generate EC key pair
            String curveName = (String) strength;
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curveName);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String algorithm) throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Certificate validity
        Date startDate = new Date(System.currentTimeMillis() - 86400000L);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1); // 1 year validity
        Date endDate = calendar.getTime();

        // Serial number
        BigInteger serialNumber = new BigInteger(160, new SecureRandom());

        // Create subject/issuer name
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, "localhost");
        nameBuilder.addRDN(BCStyle.O, "Self Sufficient Organization");
        nameBuilder.addRDN(BCStyle.OU, "Certificate Authority");
        nameBuilder.addRDN(BCStyle.C, "US");
        X500Name name = nameBuilder.build();

        // Create certificate builder
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                name,                  // Issuer
                serialNumber,          // Serial number
                startDate,             // Not before
                endDate,               // Not after
                name,                  // Subject (same as issuer for self-signed)
                publicKey              // Subject public key
        );

        // Add extensions
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // Basic Constraints (critical) - CA: false
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false));

        // Key Usage (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Extended Key Usage
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(new KeyPurposeId[] {
                        KeyPurposeId.id_kp_serverAuth,
                        KeyPurposeId.id_kp_clientAuth
                }));

        // Subject Key Identifier
        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(publicKey));

        // Authority Key Identifier
        certBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(publicKey));

        // Subject Alternative Name (SAN)
        GeneralNames sans = new GeneralNames(
                new GeneralName(GeneralName.dNSName, new DERIA5String("localhost")));
        certBuilder.addExtension(
                Extension.subjectAlternativeName,
                false,
                sans);

        // Create the certificate signer
        String sigAlg = algorithm.equals("RSA") ? "SHA256withRSA" : "SHA256withECDSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC")
                .build(privateKey);

        // Generate the certificate
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);

        // Convert to X509Certificate
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // Verify the certificate
        cert.verify(publicKey);

        return cert;
    }

    private static X509Certificate generateRootCACertificate(KeyPair keyPair, String algorithm) throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // Certificate validity
        Date startDate = new Date(System.currentTimeMillis() - 86400000L);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 20); // 20 years validity
        Date endDate = calendar.getTime();

        // Serial number
        BigInteger serialNumber = new BigInteger(160, new SecureRandom());

        // Create subject/issuer name
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, algorithm + " Root CA");
        nameBuilder.addRDN(BCStyle.O, "Neuw Root PKI Organization");
        nameBuilder.addRDN(BCStyle.OU, "Certificate Authority");
        nameBuilder.addRDN(BCStyle.C, "US");
        X500Name name = nameBuilder.build();

        // Create certificate builder
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                name,                  // Issuer
                serialNumber,          // Serial number
                startDate,             // Not before
                endDate,               // Not after
                name,                  // Subject (same as issuer for self-signed)
                publicKey              // Subject public key
        );

        // Add extensions
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // Basic Constraints (critical) - CA: true
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(true));

        // Key Usage (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        // Subject Key Identifier
        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(publicKey));

        // Authority Key Identifier (points to itself for root CA)
        certBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(publicKey));

        // CRL Distribution Point
        DistributionPointName distPointName = new DistributionPointName(
                new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier,
                        new DERIA5String(CA_REPOSITORY_URL + algorithm.toLowerCase() + "-root-ca.crl"))));
        DistributionPoint[] distPoints = new DistributionPoint[1];
        distPoints[0] = new DistributionPoint(distPointName, null, null);
        certBuilder.addExtension(
                Extension.cRLDistributionPoints,
                false,
                new CRLDistPoint(distPoints));

        // Create the certificate signer
        String sigAlg = algorithm.equals("RSA") ? "SHA512withRSA" : "SHA512withECDSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC")
                .build(privateKey);

        // Generate the certificate
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);

        // Convert to X509Certificate
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // Verify the certificate
        cert.verify(publicKey);

        return cert;
    }

    private static X509Certificate generateIntermediateCACertificate(
            String commonName,
            PublicKey publicKey,
            X509Certificate issuerCert,
            PrivateKey issuerPrivateKey,
            String algorithm,
            String issuerCertUrl,  // URL where the issuer cert can be downloaded
            int validityDays) throws Exception {

        // Certificate validity
        Date startDate = new Date(System.currentTimeMillis() - 86400000L);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date endDate = calendar.getTime();

        // Serial number
        BigInteger serialNumber = new BigInteger(160, new SecureRandom());

        // Create subject name
        X500NameBuilder subjectNameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        subjectNameBuilder.addRDN(BCStyle.CN, algorithm + " " + commonName);
        subjectNameBuilder.addRDN(BCStyle.O, "Test Organization");
        subjectNameBuilder.addRDN(BCStyle.OU, "Test PKI");
        subjectNameBuilder.addRDN(BCStyle.C, "US");
        X500Name subjectName = subjectNameBuilder.build();

        // Get issuer name
        // ✅ Preserves original BC encoding
        X500Name issuerName = new JcaX509CertificateHolder(issuerCert).getSubject();

        // Create certificate builder
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerName,            // Issuer
                serialNumber,          // Serial number
                startDate,             // Not before
                endDate,               // Not after
                subjectName,           // Subject
                publicKey              // Subject public key
        );

        // Add extensions
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // Basic Constraints (critical) - CA: true, pathlen: 0
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(0));

        // Key Usage (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        // Subject Key Identifier
        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(publicKey));

        // Authority Key Identifier
        certBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(issuerCert.getPublicKey()));

        // CRL Distribution Point
        DistributionPointName distPointName = new DistributionPointName(
                new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier,
                        new DERIA5String(CA_REPOSITORY_URL + algorithm.toLowerCase() + "-intermediate-ca.crl"))));
        DistributionPoint[] distPoints = new DistributionPoint[1];
        distPoints[0] = new DistributionPoint(distPointName, null, null);
        certBuilder.addExtension(
                Extension.cRLDistributionPoints,
                false,
                new CRLDistPoint(distPoints));

        // Authority Information Access - THIS IS THE KEY PART FOR CHAIN REFERENCE
        // It tells applications where to find the certificate that issued this one
        AccessDescription[] accessDescriptions = new AccessDescription[1];
        accessDescriptions[0] = new AccessDescription(
                AccessDescription.id_ad_caIssuers,
                new GeneralName(GeneralName.uniformResourceIdentifier,
                        new DERIA5String(issuerCertUrl)));

        certBuilder.addExtension(
                Extension.authorityInfoAccess,
                false,
                new AuthorityInformationAccess(accessDescriptions));

        // Create the certificate signer
        String sigAlg = algorithm.equals("RSA") ? "SHA384withRSA" : "SHA384withECDSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC")
                .build(issuerPrivateKey);

        // Generate the certificate
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);

        // Convert to X509Certificate
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // Verify the certificate
        cert.verify(issuerCert.getPublicKey());

        return cert;
    }

    private static X509Certificate generateEndEntityCertificate(
            String commonName,
            PublicKey publicKey,
            X509Certificate issuerCert,
            PrivateKey issuerPrivateKey,
            String algorithm,
            String issuerCertUrl,  // URL where the issuer cert can be downloaded
            boolean expired) throws Exception {

        // Certificate validity
        Date startDate = new Date(System.currentTimeMillis() - 86400000L);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        if (expired) {
            calendar.add(Calendar.DATE, -1);
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 365);
        }
        Date endDate = calendar.getTime();

        // Serial number
        BigInteger serialNumber = new BigInteger(160, new SecureRandom());

        // Create subject name
        X500NameBuilder subjectNameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        subjectNameBuilder.addRDN(BCStyle.CN, commonName);
        subjectNameBuilder.addRDN(BCStyle.O, "Test Organization");
        subjectNameBuilder.addRDN(BCStyle.OU, algorithm + " Servers");
        subjectNameBuilder.addRDN(BCStyle.C, "US");
        X500Name subjectName = subjectNameBuilder.build();

        // Get issuer name
        // ✅ Preserves original BC encoding
        X500Name issuerName = new JcaX509CertificateHolder(issuerCert).getSubject();

        // Create certificate builder
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerName,            // Issuer
                serialNumber,          // Serial number
                startDate,             // Not before
                endDate,               // Not after
                subjectName,           // Subject
                publicKey              // Subject public key
        );

        // Add extensions
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // Basic Constraints (critical) - CA: false
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false));

        // Key Usage (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Extended Key Usage
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(new KeyPurposeId[] {
                        KeyPurposeId.id_kp_serverAuth,
                        KeyPurposeId.id_kp_clientAuth
                }));

        // Subject Key Identifier
        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(publicKey));

        // Authority Key Identifier
        certBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(issuerCert.getPublicKey()));

        // Subject Alternative Name (SAN)
        GeneralNames sans = new GeneralNames(
                new GeneralName(GeneralName.dNSName, new DERIA5String(commonName)));
        certBuilder.addExtension(
                Extension.subjectAlternativeName,
                false,
                sans);

        // Authority Information Access - THIS IS THE KEY PART FOR CHAIN REFERENCE
        // It tells applications where to find the certificate that issued this one
        AccessDescription[] accessDescriptions = new AccessDescription[1];
        accessDescriptions[0] = new AccessDescription(
                AccessDescription.id_ad_caIssuers,
                new GeneralName(GeneralName.uniformResourceIdentifier,
                        new DERIA5String(issuerCertUrl)));

        certBuilder.addExtension(
                Extension.authorityInfoAccess,
                false,
                new AuthorityInformationAccess(accessDescriptions));

        // Create the certificate signer
        String sigAlg = algorithm.equals("RSA") ? "SHA256withRSA" : "SHA256withECDSA";
        ContentSigner contentSigner = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC")
                .build(issuerPrivateKey);

        // Generate the certificate
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);

        // Convert to X509Certificate
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // Verify the certificate
        cert.verify(issuerCert.getPublicKey());

        return cert;
    }

    public static KeyStore generateKeystoreFile(
            String alias,
            char[] password,
            PrivateKey privateKey,
            String outputPath,
            // expect the chain: leaf → intermediate → root (required order for TLS)
            X509Certificate... certs
    ) throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {

        // PKCS12 for modern standard approach
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, password); // Initialize empty keystore

        keyStore.setKeyEntry(alias, privateKey, password, (Certificate[]) certs);
        keyStore.setCertificateEntry(ROOT_CA, getLastElement(certs));
        // assume the order to be - leaf → intermediate → root always
        if (certs.length > 1) {
            keyStore.setCertificateEntry(INTERMEDIATE_CA, certs[1]);
        }

        // Persist to disk
        if (outputPath != null) {
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                keyStore.store(fos, password);
            }
        }

        return keyStore;
    }

    private static String certificateToPEM(X509Certificate certificate) throws Exception {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(certificate);
        pemWriter.close();
        return stringWriter.toString();
    }

    private static <T> T getLastElement(T... elements) {
        if (elements == null || elements.length == 0) {
            return null;
        }
        return elements[elements.length - 1];
    }

}
