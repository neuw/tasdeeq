# Tasdeeq

> **WORK IN PROGRESS**

## What does "Tasdeeq" mean?

**Tasdeeq** (تصديق) is an Arabic and Urdu word that means **"verification"**, **"validation"**, or **"certification"**. It is derived from the root word *sidq* (صدق), meaning truth or sincerity. In everyday usage, tasdeeq refers to the act of confirming the authenticity or truthfulness of something — to certify that something is genuine and trustworthy.

## What is this library?

Tasdeeq is a Java library for TLS/SSL certificate validation, certificate chain inspection, DNS resolution, and JVM compatibility checking. It provides a simple set of utilities to verify the trust and authenticity of digital certificates — staying true to its name.

### Core capabilities

- **Certificate Chain Validation** — Verify that a server's certificate chains back to a trusted root CA in the JVM's default truststore.
- **Downstream Certificate Retrieval** — Connect to any remote HTTPS server and retrieve its full certificate chain, with support for both strict and lenient validation modes.
- **Custom Truststore Support** — Add your own CAs (self-signed, internal PKI) for validation in development, staging, or internal environments.
- **Certificate Analysis** — Inspect certificate details including issuer, subject, validity period, key usage, and certificate type classification (root, intermediate, leaf).
- **DNS Resolution** — Perform DNS lookups (A, AAAA, MX, TXT, NS, CNAME) for any domain.
- **JVM Compatibility Checking** — Determine the JDK version a class was compiled against by inspecting its bytecode.

### Modules

| Module | Description |
|---|---|
| `tasdeeq-core` | Core library (compiled for Java 8 for maximum compatibility) |
| `tasdeeq-starter` | Spring Boot starter (Java 17+) |
| `tests` | Test suite with WireMock-based HTTPS tests |

### Key dependencies

- [BouncyCastle](https://www.bouncycastle.org/) — Cryptographic operations and certificate handling
- [ASM](https://asm.ow2.io/) — Bytecode inspection for JDK version detection
- [SLF4J](https://www.slf4j.org/) — Logging

## Important Functions & Usage

> **WORK IN PROGRESS** — APIs may change.

### DownstreamCertTasdeeq

Retrieve and inspect TLS certificates from any remote server.

```java
// Fetch certificate chain from a server (strict validation, port 443)
List<X509Certificate> chain = DownstreamCertTasdeeq.getDownstreamCert("google.com");

// Fetch from a custom port
List<X509Certificate> chain = DownstreamCertTasdeeq.getDownstreamCert("myserver.com", 8443);

// Lenient mode — tries with validation first, falls back to without
List<X509Certificate> chain = DownstreamCertTasdeeq.getDownstreamCert("self-signed.example.com", 443, false);

// Use a custom truststore (e.g. for internal/self-signed CAs)
List<X509Certificate> chain = DownstreamCertTasdeeq.getDownstreamCertWithCustomTruststore(
        "internal.mycompany.com", 443, myRootCA, myIntermediateCA);
```

### CertificateAuthorityTasdeeq

Validate certificate chains against the JVM's trusted root CAs.

```java
// Check if a certificate chain traces back to a trusted root CA
boolean trusted = CertificateAuthorityTasdeeq.rootCAisTrusted(chain);

// Get all trusted root CAs from the JVM truststore
Map<String, X509Certificate> roots = CertificateAuthorityTasdeeq.getRootCAsBySubjectDN();
Map<String, X509Certificate> rootsBySerial = CertificateAuthorityTasdeeq.getRootCAsBySerialNumber();
```

### DNSTasdeeq

Perform DNS lookups for any domain.

```java
// Query all common record types (A, AAAA, MX, TXT, NS, CNAME)
DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.tasdeeq("example.com");

// Query specific record types
DNSTasdeeq.DNSQueryResult result = DNSTasdeeq.tasdeeq("example.com", "A", "MX");

// Read results
List<String> aRecords = result.getRecords("A");
List<String> mxRecords = result.getRecords("MX");
String error = result.getConnectionError();
```

### JVMTasdeeq

Inspect JVM and bytecode version information.

```java
// Get JVM version details for the current runtime
JVMTasdeeq.JVMTasdeeqResult result = JVMTasdeeq.tasdeeq();
result.getJdkVersion();      // e.g. "8"
result.getJreVersion();      // e.g. "17.0.2"
result.getClassVersion();    // e.g. 52 (bytecode major version)
result.getVendor();          // e.g. "Eclipse Adoptium"

// Check the bytecode version of any class
int version = JVMTasdeeq.getClassVersion(MyApp.class);
String jdk = JVMTasdeeq.getJdkVersion(version); // e.g. "17"
```

## Use cases

- Automated SSL certificate monitoring and validation
- Verifying certificate chains in CI/CD pipelines before deployment
- Checking downstream service certificates at runtime in microservice architectures
- Diagnosing certificate chain and TLS handshake issues
- Infrastructure DNS verification

## Building

```bash
mvn clean install
```

Requires Maven 3. The core module targets Java 8 for use in non-Spring Boot projects. The starter module requires JDK 17+.
