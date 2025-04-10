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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import static org.forgerock.openam.auth.nodes.webauthn.FidoCertificationLevel.OFF;
import static org.forgerock.openam.shared.secrets.Labels.FIDO_METADATA_SERVICE_ROOT_CERTIFICATE;
import static org.forgerock.secrets.Purpose.purpose;

import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.auth.nodes.webauthn.FidoCertificationLevel;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnMetadataServiceConfig;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.resolver.CertificateResolutionException;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.CertificateVerificationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.name.Named;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Factory for creating instances of the metadata service.
 */
@Singleton
public class DefaultMetadataServiceFactory implements MetadataServiceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetadataServiceFactory.class);
    private final Set<String> errorLoggedUris = new HashSet<>();
    private final ScheduledExecutorService errorResetScheduler = Executors.newScheduledThreadPool(1);
    private final Cache<Realm, MetadataService> serviceCache;
    private final CertificateFactory certFactory;
    private final TrustAnchorValidator.Factory trustAnchorValidatorFactory;
    private final AnnotatedServiceRegistry annotatedServiceRegistry;
    private final FidoMetadataV3ProcessorFactory fidoMetadataV3ProcessorFactory;
    private final TrustAnchorUtilities trustAnchorUtilities;
    private final SecretReferenceCache secrets;

    /**
     * Constructor.
     *
     * @param certFactory                    the X509 certificate factory
     * @param trustAnchorValidatorFactory    the trust anchor validator factory
     * @param annotatedServiceRegistry       the annotated service registry
     * @param fidoMetadataV3ProcessorFactory the {@link FidoMetadataV3ProcessorFactory}
     * @param trustAnchorUtilities           utilities for the manipulation of secrets into trust anchors
     * @param secrets                        used to read secrets
     */
    @Inject
    DefaultMetadataServiceFactory(@Named("X.509") CertificateFactory certFactory,
            TrustAnchorValidator.Factory trustAnchorValidatorFactory,
            AnnotatedServiceRegistry annotatedServiceRegistry,
            FidoMetadataV3ProcessorFactory fidoMetadataV3ProcessorFactory,
            TrustAnchorUtilities trustAnchorUtilities,
            SecretReferenceCache secrets) {
        this.certFactory = certFactory;
        this.trustAnchorValidatorFactory = trustAnchorValidatorFactory;
        this.annotatedServiceRegistry = annotatedServiceRegistry;
        this.fidoMetadataV3ProcessorFactory = fidoMetadataV3ProcessorFactory;
        this.trustAnchorUtilities = trustAnchorUtilities;
        this.secrets = secrets;
        this.serviceCache =  CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build();
    }

    /**
     * Get the instance of the default metadata service.
     *
     * @param realm                  the realm
     * @param fidoCertificationLevel the level of FIDO certification to filter by, or OFF to disable filtering
     * @return the associated metadata service
     * @throws MetadataException if an error occurs while trying to process the metadata
     */
    public MetadataService getInstance(Realm realm, FidoCertificationLevel fidoCertificationLevel)
            throws MetadataException {
        if (fidoCertificationLevel == OFF) {
            return new NoOpMetadataService();
        }

        MetadataService service = serviceCache.getIfPresent(realm);
        if (service == null) {
            try {
                service = createService(realm);
            } catch (CertificateResolutionException | NoSuchSecretException e) {
                throw new MetadataException(e.getMessage(), e);
            }
            serviceCache.put(realm, service);
        }
        return new FilteringMetadataService(service, fidoCertificationLevel);
    }

    private MetadataService createService(Realm realm)
            throws MetadataException, CertificateResolutionException, NoSuchSecretException {
        Optional<WebAuthnMetadataServiceConfig.Realm> metadataServiceConfigOptional = getMetadataServiceConfig(realm);

        List<MetadataEntry> metadataEntries = new ArrayList<>();
        if (metadataServiceConfigOptional.isPresent()) {
            WebAuthnMetadataServiceConfig.Realm config = metadataServiceConfigOptional.get();
            final Set<String> metadataServiceUris = config.fidoMetadataServiceUris();
            if (metadataServiceUris.isEmpty()) {
                return new NoOpMetadataService();
            }
            for (String metadataServiceUri : metadataServiceUris) {
                try {
                    metadataEntries.addAll(fidoMetadataV3ProcessorFactory.create(metadataServiceUri,
                            createMetadataTrustAnchorValidator(realm, config)).process());
                } catch (MetadataException e) {
                    // Failed to process the metadata service, log and continue to the next one
                    // An attempt to retry this will be made when the service cache expires
                    if (!errorLoggedUris.contains(metadataServiceUri)) {
                        LOGGER.error("Failed to process metadata service at uri: {}", metadataServiceUri, e);
                        errorLoggedUris.add(metadataServiceUri);
                        errorResetScheduler.schedule(
                                () -> errorLoggedUris.remove(metadataServiceUri), 1, TimeUnit.MINUTES);
                    } else {
                        LOGGER.debug("Failed to process metadata service at uri: {}", metadataServiceUri, e);
                    }
                }
            }
            if (metadataEntries.isEmpty()) {
                throw new MetadataException("Metadata processing failed for all provided URI(s)");
            }
        }

        return new FidoMetadataV3Service(certFactory, trustAnchorValidatorFactory, metadataEntries);
    }

    private TrustAnchorValidator createMetadataTrustAnchorValidator(Realm realm,
            WebAuthnMetadataServiceConfig.Realm config) throws NoSuchSecretException {
        Purpose<CertificateVerificationKey> verificationKeyPurpose = purpose(FIDO_METADATA_SERVICE_ROOT_CERTIFICATE,
                CertificateVerificationKey.class);
        SecretCache spf = secrets.realm(realm);
        List<CertificateVerificationKey> secretSource = spf.valid(verificationKeyPurpose).get();
        return trustAnchorValidatorFactory.create(trustAnchorUtilities.trustAnchorsFromSecrets(secretSource),
                config.enforceRevocationCheck());
    }

    private Optional<WebAuthnMetadataServiceConfig.Realm> getMetadataServiceConfig(Realm realm)
            throws MetadataException {
        try {
            return annotatedServiceRegistry
                    .getRealmSingleton(WebAuthnMetadataServiceConfig.Realm.class, realm);
        } catch (SSOException | SMSException e) {
            throw new MetadataException("Failed to read config", e);
        }
    }
}
