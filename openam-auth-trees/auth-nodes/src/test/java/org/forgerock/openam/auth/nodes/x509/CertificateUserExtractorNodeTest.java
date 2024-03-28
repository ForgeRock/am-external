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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.x509;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeProfileMappingExtension.RFC822_NAME;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeToProfileMapping.NONE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.IdType;

public class CertificateUserExtractorNodeTest {

    @Mock
    CertificateUserExtractorNode.Config config;
    @Mock
    LegacyIdentityService identityService;

    CertificateUserExtractorNode node;

    @BeforeMethod
    public void setup() {
        node = null;
        initMocks(this);

        given(config.certificateAttributeProfileMappingExtension()).willReturn(RFC822_NAME);
        given(config.certificateAttributeToProfileMapping()).willReturn(NONE);
        node = new CertificateUserExtractorNode(config, identityService);
    }

    @Test
    public void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        given(cert.getSubjectAlternativeNames()).willReturn(List.of(List.of(1, "bob")));
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object(field("X509Certificate", List.of(cert))));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("bob");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    public void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        // Given
        X509Certificate cert = mock(X509Certificate.class);
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "root")));
        JsonValue transientState = json(object(field("X509Certificate", List.of(cert))));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.identifiedIdentity).isEmpty();
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(
                sharedState,
                transientState,
                new ExternalRequestContext.Builder().build(),
                callbacks,
                Optional.empty()
        );
    }
}
