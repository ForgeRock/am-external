/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

/**
 * A class to assist with the validation of device certificate chains against a provided trust anchor.
 */
public class TrustAnchorValidator {

    private final Logger logger = LoggerFactory.getLogger(TrustAnchorValidator.class);

    private final CertificateFactory certFactory;
    private final CertPathValidator certPathValidator;
    private final Set<TrustAnchor> trustAnchors;
    private final boolean enforcedRevocation;

    /**
     * Constructor.
     *
     * @param certFactory        injected X.509 cert factory
     * @param certPathValidator  injected PKIX cert path validator
     * @param trustAnchors       Set of trusted trust anchors. Nullable
     * @param enforcedRevocation whether to enforce the checking of CRL/OCSP revocation checks
     */
    @Inject
    public TrustAnchorValidator(@Named("X.509") CertificateFactory certFactory,
                                @Named("PKIX") CertPathValidator certPathValidator,
                                @Assisted @Nullable Set<TrustAnchor> trustAnchors,
                                @Assisted boolean enforcedRevocation) {
        this.certFactory = certFactory;
        this.certPathValidator = certPathValidator;
        this.trustAnchors = trustAnchors;
        this.enforcedRevocation = enforcedRevocation;
    }

    /**
     * Determines if the attestation type for this verifier can be chained up to a CA and returns the type accordingly.
     * Will return {@link AttestationType#CA} if the chain can be validated, {@link AttestationType#BASIC} otherwise.
     *
     * @param certs The certs to check
     * @return the attestation type achieved, {@link AttestationType#CA} if the chain can be validated,
     * {@link AttestationType#BASIC} otherwise.
     */
    public AttestationType getAttestationType(List<X509Certificate> certs) {
        try {
            CertPath certPath = certFactory.generateCertPath(certs);
            if (isTrusted(certPath)) {
                logger.debug("Trust anchor chain defined and successfully validated. Attestation type: CA");
                return AttestationType.CA;
            }
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Platform does not support necessary algorithm to perform cert validation.", e);
        } catch (CertPathValidatorException | CertificateException e) {
            logger.error("Error reading or using selected certificate.", e);
        }

        logger.debug("No trust anchor chain achieved. Attestation type: BASIC");
        return AttestationType.BASIC;
    }

    /**
     * Validates the certificate against the host's trust anchors as defined in the config.
     *
     * @param certPath The cert path that must chain up to a trust anchor.
     * @return True if the cert chain is trusted, false otherwise.
     */
    public boolean isTrusted(CertPath certPath) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        return isTrusted(certPath, enforcedRevocation);
    }

    /**
     * Validates the certificate against the host's trust anchors as defined in the config.
     *
     * @param certPath           The cert path that must chain up to a trust anchor.
     * @param enforcedRevocation whether to enforce the checking of CRL/OCSP revocation checks
     * @return true if the cert chain is trusted, false otherwise.
     */
    private boolean isTrusted(CertPath certPath, boolean enforcedRevocation) throws CertPathValidatorException,
            InvalidAlgorithmParameterException {

        if (trustAnchors == null) {
            logger.debug("No trust anchor supplied, no trust chain achievable.");
            return false;
        }

        if (trustAnchors.isEmpty()) {
            logger.debug("No keys in keystore, no trust chain achievable.");
            return false;
        }

        PKIXParameters params = new PKIXParameters(trustAnchors);

        // We potentially expect a critical policy indicating the approach for verification of
        // attestation information (1.3.6.1.4.1.311.21.31). This ensures we can still successfully validate.
        params.setPolicyQualifiersRejected(false);
        params.setRevocationEnabled(enforcedRevocation);

        if (enforcedRevocation) {
            PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) certPathValidator.getRevocationChecker();
            params.addCertPathChecker(revocationChecker);
        }

        certPathValidator.validate(certPath, params);
        return true;
    }

    /**
     * Test whether the cert path provided is a root certificate.
     *
     * @param certPath the certificate path containing the certificate(s) to check
     * @return true, if the provided certPath contains only a root CA certificate, false otherwise
     */
    public boolean isRootCertificate(List<X509Certificate> certPath) {
        if (certPath.size() > 1 || trustAnchors == null) {
            return false;
        }
        return containsTrustAnchor(certPath);
    }

    /**
     * Test whether the certificate path contains a trust anchor certificate.
     *
     * @param certPath the certificate path
     * @return true, if there is a cert that contains a trusted trust anchor
     */
    public boolean containsTrustAnchor(List<X509Certificate> certPath) {
        return trustAnchors != null && trustAnchors.stream()
                .anyMatch(trustAnchor -> certPath.contains(trustAnchor.getTrustedCert()));
    }

    /**
     * Returns the underlying certificates used to validate the trust chain.
     * @return the certificates used to validate the trust chain.
     */
    public Set<X509Certificate> getCertificates() {
        if (trustAnchors == null) {
            return Collections.emptySet();
        }
        return trustAnchors.stream().map(TrustAnchor::getTrustedCert).collect(Collectors.toSet());
    }

    /**
     * Returns whether this validator enforces revocation checks.
     *
     * @return true if revocation checks are enforced, false otherwise.
     */
    public boolean enforcesRevocation() {
        return enforcedRevocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrustAnchorValidator that = (TrustAnchorValidator) o;
        return enforcedRevocation == that.enforcedRevocation
                && Objects.equals(fromTrustAnchor(trustAnchors), fromTrustAnchor(that.trustAnchors));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTrustAnchor(trustAnchors), enforcedRevocation);
    }

    private static Set<X509Certificate> fromTrustAnchor(Set<TrustAnchor> trustAnchors) {
        if (trustAnchors == null) {
            return Collections.emptySet();
        }
        return trustAnchors.stream().map(TrustAnchor::getTrustedCert).collect(Collectors.toSet());
    }

    /**
     * Factory for assisted injection of trust anchors and the enforced revocation config param.
     */
    public interface Factory {
        /**
         * Guice-assisted inject factory method.
         *
         * @param trustAnchors       the trust anchors this validator will use.
         * @param enforcedRevocation whether to enforce revocation checks performed by this validator.
         * @return a validator ready for use.
         */
        TrustAnchorValidator create(Set<TrustAnchor> trustAnchors, boolean enforcedRevocation);
    }
}
