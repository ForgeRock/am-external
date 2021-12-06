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
 * Copyright 2020-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

/**
 * Abstract class for attestation verifiers that can use certificate chains to
 * validate a trust chain.
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
     * @param certFactory injected X.509 cert factory
     * @param certPathValidator injected PKIX cert path validator
     * @param trustAnchors Set of trusted trust anchors. Nullable.
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
     * Determines if the attestation type for this verifier can be chained up to a CA, and if so returns
     * type 'CA' rather than 'BASIC'.
     *
     * @param certs The certs that must chain up to a trust anchor.
     * @return the attestation type achieved.
     */
    public AttestationType getAttestationType(List<X509Certificate> certs) {
        if (isTrusted(certs, enforcedRevocation)) {
            logger.debug("Trust anchor chain defined and successfully validated. Attestation type: CA");
            return AttestationType.CA;
        } else {
            logger.debug("No trust anchor chain achieved. Attestation type: BASIC");
            return AttestationType.BASIC;
        }
    }

    /**
     * Validates the certificate against the host's trust anchors as defined in the config.
     *
     * @param certs The certs that must chain up to a trust anchor.
     * @param enforcedRevocation whether to enforce the checking of CRL/OCSP revocation checks
     * @return True if the cert chain is trusted, false otherwise.
     */
    private boolean isTrusted(List<X509Certificate> certs, boolean enforcedRevocation) {

        if (trustAnchors == null) {
            logger.debug("No trust anchor supplied, no trust chain achievable.");
            return false;
        }

        if (trustAnchors.size() < 1) {
            logger.debug("No keys in keystore, no trust chain achievable.");
            return false;
        }

        try {

            CertPath certPath = certFactory.generateCertPath(certs);
            PKIXParameters params = new PKIXParameters(trustAnchors);

            // We potentially expect a critical policy indicating the approach for verification of
            // attestation information (1.3.6.1.4.1.311.21.31). This ensures we can still successfully validate.
            params.setPolicyQualifiersRejected(false);
            params.setRevocationEnabled(enforcedRevocation);

            certPathValidator.validate(certPath, params);
            return true;
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("Platform does not support necessary algorithm to perform cert validation.", e);
            return false;
        } catch (CertPathValidatorException | CertificateException e) {
            logger.error("Error reading or using selected certificate.", e);
            return false;
        }
    }

    /**
     * Factory for assisted injection of trust anchors and the enforced revocation config param.
     */
    public interface Factory {
        /**
         * Guice-assisted inject factory method.
         *
         * @param trustAnchors the trust anchors this validator will use.
         * @param enforcedRevocation whether to enforce revocation checks performed by this validator.
         * @return a validator ready for use.
         */
        TrustAnchorValidator create(Set<TrustAnchor> trustAnchors, boolean enforcedRevocation);
    }
}
