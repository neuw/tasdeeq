package com.neuwton.tasdeeq.config.actuators.contributors.models;

public class CACertDetails {

    private String certificateName;
    private String serialNumber;
    private String issuerDN;
    private String validFrom;
    private String validUntil;
    private Long validFromEpochMs;
    private Long validUntilEpochMs;
    private String signatureAlgorithm;
    private String basicConstraints;
    private String certificateType;
    private String status;

    public String getCertificateName() {
        return certificateName;
    }

    public CACertDetails setCertificateName(String certificateName) {
        this.certificateName = certificateName;
        return this;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public CACertDetails setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public CACertDetails setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
        return this;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public CACertDetails setValidFrom(String validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public CACertDetails setValidUntil(String validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public CACertDetails setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
        return this;
    }

    public String getBasicConstraints() {
        return basicConstraints;
    }

    public CACertDetails setBasicConstraints(String basicConstraints) {
        this.basicConstraints = basicConstraints;
        return this;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public CACertDetails setCertificateType(String certificateType) {
        this.certificateType = certificateType;
        return this;
    }

    public Long getValidFromEpochMs() {
        return validFromEpochMs;
    }

    public CACertDetails setValidFromEpochMs(Long validFromEpochMs) {
        this.validFromEpochMs = validFromEpochMs;
        return this;
    }

    public Long getValidUntilEpochMs() {
        return validUntilEpochMs;
    }

    public CACertDetails setValidUntilEpochMs(Long validUntilEpochMs) {
        this.validUntilEpochMs = validUntilEpochMs;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public CACertDetails setStatus(String status) {
        this.status = status;
        return this;
    }
}
