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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.ad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.forgerock.openam.auth.nodes.ADDecisionNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

import ch.qos.logback.classic.spi.ILoggingEvent;

@ExtendWith(MockitoExtension.class)
public class ADDecisionNodeSecretIdProviderTest {

    @Mock
    AnnotatedServiceRegistry serviceRegistry;

    @Mock
    ADDecisionNode.Config config;

    @Mock
    Realm realm;

    @RegisterExtension
    public LoggerExtension loggerExtension = new LoggerExtension(ADDecisionNodeSecretIdProvider.class);

    private ADDecisionNodeSecretIdProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ADDecisionNodeSecretIdProvider(serviceRegistry);
    }

    @Test
    void testGetRealmMultiInstanceSecretIds() throws SMSException, SSOException {
        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.of("banana"));
        ImmutableSet<ADDecisionNode.Config> adNodeConfigs = ImmutableSet.of(config);
        given(serviceRegistry.getRealmInstances(ADDecisionNode.Config.class, realm))
                .willReturn(adNodeConfigs);

        // When
        Multimap<String, String> realmMultiInstanceSecretIds = provider.getRealmMultiInstanceSecretIds(null, realm);
        Collection<String> actual = realmMultiInstanceSecretIds.get("ADNode");

        // Then
        assertThat(actual).containsExactly("am.authentication.nodes.ad.decision.mtls.banana.cert");
    }

    @Test
    void testLogsErrorWhenExceptionThrown() throws SMSException, SSOException {
        given(serviceRegistry.getRealmInstances(ADDecisionNode.Config.class, realm))
                .willThrow(new SMSException());

        // When
        Multimap<String, String> actual = provider.getRealmMultiInstanceSecretIds(null, realm);

        // Then
        assertThat(actual.isEmpty()).isTrue();
        List<String> errors = loggerExtension.getErrors(ILoggingEvent::getFormattedMessage);
        assertThat(errors).containsExactly("Failed to get AD node secret labels");
    }

    @Test
    void testThrowsNullPointerWhenRealmIsNull() {
        assertThatThrownBy(() -> provider.getRealmMultiInstanceSecretIds(null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
