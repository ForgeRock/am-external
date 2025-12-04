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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.forgerock.secrets.Purpose.purpose;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.AttestationPreference;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidKeyVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidSafetyNetVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmManufacturer;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmVerifier;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataService;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataServiceFactory;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.CertificateVerificationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link AttestationDecoder} instances.
 */
public class AttestationVerifierFactory {
    private static final Logger logger = LoggerFactory.getLogger(AttestationVerifierFactory.class);
    private static final NoneVerifier NONE_VERIFIER = new NoneVerifier();

    private final TrustAnchorUtilities trustAnchorUtilities;
    private final TrustAnchorValidator.Factory trustAnchorValidatorFactory;
    private final SecretReferenceCache secrets;
    private final FlowUtilities flowUtilities;
    private final Set<TpmManufacturer> tpmManufacturers;
    private final MetadataServiceFactory metadataServiceFactory;

    /**
     * Constructs a new {@link AttestationVerifierFactory}.
     *
     * @param trustAnchorUtilities        utilities for the manipulation of secrets into trust anchors
     * @param trustAnchorValidatorFactory generates trust anchor validators
     * @param flowUtilities               Utilities for webauthn
     * @param tpmManufacturers            the set of verified TPM manufacturers that are supported
     * @param secrets                     used to read secrets
     * @param metadataServiceFactory      the metadata service factory
     */
    @Inject
    public AttestationVerifierFactory(TrustAnchorUtilities trustAnchorUtilities,
            TrustAnchorValidator.Factory trustAnchorValidatorFactory,
            FlowUtilities flowUtilities,
            Set<TpmManufacturer> tpmManufacturers,
            SecretReferenceCache secrets,
            MetadataServiceFactory metadataServiceFactory) {
        this.trustAnchorUtilities = trustAnchorUtilities;
        this.trustAnchorValidatorFactory = trustAnchorValidatorFactory;
        this.metadataServiceFactory = metadataServiceFactory;
        this.flowUtilities = flowUtilities;
        this.tpmManufacturers = tpmManufacturers;
        this.secrets = secrets;
    }

    /**
     * Construct a new attestation decoder.
     *
     * @param realm the realm
     * @param config the configuration of the node which is creating this flow
     * @param format the attestation format
     * @return the newly created attestation decoder
     */
    public AttestationVerifier create(Realm realm, WebAuthnRegistrationNode.Config config, String format)
            throws DecodingException, MetadataException, NoSuchSecretException {
        switch (format) {
        case "none":
            if (config.attestationPreference() == AttestationPreference.DIRECT) {
                logger.debug("direct attestation cannot be performed on the none attestation format");
                throw new DecodingException("Unacceptable attestation format provided - "
                        + "direct attestation required.");
            }
            logger.debug("none verifier selected");
            return NONE_VERIFIER;
        case "fido-u2f":
            logger.debug("fido-u2f verifier selected");
            return new FidoU2fVerifier(createTrustAnchorValidator(realm, config), config.validateFidoU2fAaguid());
        case "android-safetynet":
            logger.warn("android-safetynet verifier selected");
            return new AndroidSafetyNetVerifier();
        case "android-key":
            logger.debug("android-key verifier selected");
            return new AndroidKeyVerifier(createTrustAnchorValidator(realm, config), flowUtilities);
        case "tpm":
            logger.debug("tpm verifier selected");
            return new TpmVerifier(createTrustAnchorValidator(realm, config), tpmManufacturers);
        case "packed":
            logger.debug("packed verifier selected");
            MetadataService metadataService = metadataServiceFactory.getInstance(realm,
                    config.fidoCertificationLevel());
            return new PackedVerifier(flowUtilities, createTrustAnchorValidator(realm, config), metadataService);
        default:
            throw new DecodingException("Unacceptable attestation format provided.");
        }
    }

    private TrustAnchorValidator createTrustAnchorValidator(Realm realm, WebAuthnRegistrationNode.Config config)
            throws NoSuchSecretException {
        if (config.trustStoreAlias().isEmpty()) {
            return trustAnchorValidatorFactory.create(Collections.emptySet(), config.enforceRevocationCheck());
        }

        Purpose<CertificateVerificationKey> verificationKeyPurpose = purpose(String.format(Labels.WEBAUTHN_TRUST_STORE,
                config.trustStoreAlias().get()), CertificateVerificationKey.class);
        SecretCache spf = secrets.realm(realm);
        List<CertificateVerificationKey> secretSource = spf.valid(verificationKeyPurpose).get();
        return trustAnchorValidatorFactory.create(trustAnchorUtilities.trustAnchorsFromSecrets(secretSource),
                config.enforceRevocationCheck());
    }
}
