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
package org.forgerock.openam.auth.nodes.webauthn.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.auth.nodes.webauthn.FidoCertificationLevel.FIDO_CERTIFIED_L1;
import static org.forgerock.openam.auth.nodes.webauthn.FidoCertificationLevel.OFF;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.forgerock.am.config.Listener;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnMetadataServiceConfig;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobReader;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.BlobVerifier;
import org.forgerock.openam.auth.nodes.webauthn.metadata.utils.ResourceResolver;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

public class DefaultMetadataServiceFactoryTest {

    private DefaultMetadataServiceFactory factory;

    @Mock
    Listener listener;
    @Mock
    AnnotatedServiceRegistry serviceRegistry;
    @Mock
    TrustAnchorUtilities trustAnchorUtilities;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    SecretReferenceCache secrets;

    @BeforeEach
    void setup() {
        openMocks(this);
        CertificateFactory certFactory = mock(CertificateFactory.class);
        TrustAnchorValidator.Factory trustAnchorValidatorFactory = mock(TrustAnchorValidator.Factory.class);
        BlobVerifier blobVerifier = mock(BlobVerifier.class);
        given(listener.onRealmChange(ArgumentMatchers.<Consumer<Realm>>any())).willReturn(listener);
        given(serviceRegistry.createListener(WebAuthnMetadataServiceConfig.Realm.class)).willReturn(listener);
        FidoMetadataV3ProcessorFactory fidoMetadataV3ProcessorFactory = new FidoMetadataV3ProcessorFactory(
                new MetadataBlobPayloadDownloader(blobVerifier,
                        new ResourceResolver(), new BlobReader(), new JwtReconstruction()));
        factory = new DefaultMetadataServiceFactory(certFactory, trustAnchorValidatorFactory, serviceRegistry,
                fidoMetadataV3ProcessorFactory, trustAnchorUtilities, secrets);
    }

    @Test
    void testGetInstanceFilteringMetadataService() throws MetadataException {
        Realm realm = mock(Realm.class);
        MetadataService service = factory.getInstance(realm, FIDO_CERTIFIED_L1);
        assertInstanceOf(FilteringMetadataService.class, service);
    }

    @Test
    void testGetInstanceNoOp() throws SMSException, SSOException, MetadataException {
        WebAuthnMetadataServiceConfig.Realm config = new WebAuthnMetadataServiceConfig.Realm() {
            @Override
            public Set<String> fidoMetadataServiceUris() {
                return Collections.emptySet();
            }
        };
        Realm realm = mock(Realm.class);
        given(serviceRegistry.getRealmSingleton(eq(WebAuthnMetadataServiceConfig.Realm.class), eq(realm)))
                .willReturn(Optional.of(config));

        MetadataService service = factory.getInstance(realm, FIDO_CERTIFIED_L1);

        assertInstanceOf(FilteringMetadataService.class, service);
        assertThat(service).extracting("delegate").isInstanceOf(NoOpMetadataService.class);
    }

    @Test
    void testGetInstanceLevelOffNoOp() throws MetadataException, SMSException, SSOException {
        WebAuthnMetadataServiceConfig.Realm config = new WebAuthnMetadataServiceConfig.Realm() {
            @Override
            public Set<String> fidoMetadataServiceUris() {
                return Set.of("FidoMetadataDownloader/fidoconformance/fido-mds3-blob.jwt");
            }
        };
        Realm realm = mock(Realm.class);
        given(serviceRegistry.getRealmSingleton(eq(WebAuthnMetadataServiceConfig.Realm.class), eq(realm)))
                .willReturn(Optional.of(config));

        MetadataService service = factory.getInstance(realm, OFF);

        assertInstanceOf(NoOpMetadataService.class, service);
    }

    @Test
    void testGetInstanceIncorrectDataFormat() throws SMSException, SSOException, MetadataException {
        final String metadataServiceUri = "FidoMetadataDownloader/fidoconformance/fido-mds3-root.crt";
        WebAuthnMetadataServiceConfig.Realm config = new WebAuthnMetadataServiceConfig.Realm() {
            @Override
            public Set<String> fidoMetadataServiceUris() {
                return Set.of(metadataServiceUri);
            }
        };
        Realm realm = mock(Realm.class);
        given(serviceRegistry.getRealmSingleton(eq(WebAuthnMetadataServiceConfig.Realm.class), eq(realm)))
                .willReturn(Optional.of(config));

        assertThatThrownBy(() ->
                factory.getInstance(realm, FIDO_CERTIFIED_L1))
                .isInstanceOf(MetadataException.class)
                .hasMessageContaining("Metadata processing failed for all provided URI(s)");
    }

    @Test
    void testCaching() throws MetadataException {
        Realm realm1 = mock(Realm.class);
        Realm realm2 = mock(Realm.class);

        MetadataService service1 = factory.getInstance(realm1, FIDO_CERTIFIED_L1);
        MetadataService service2 = factory.getInstance(realm2, FIDO_CERTIFIED_L1);
        assertThat(service1).isInstanceOf(FilteringMetadataService.class);
        assertThat(service2).isInstanceOf(FilteringMetadataService.class);
        assertNotSame(service1, service2);

        // Get a new Filtering service using the same realm - the underlying delegate will be the same
        assertThat(service1).usingRecursiveComparison().isEqualTo(factory.getInstance(realm1, FIDO_CERTIFIED_L1));
        assertThat(service2).usingRecursiveComparison().isEqualTo(factory.getInstance(realm2, FIDO_CERTIFIED_L1));
    }
}
