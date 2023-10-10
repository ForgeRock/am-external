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
package org.forgerock.openam.auth.nodes.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.forgerock.openam.auth.nodes.LdapDecisionNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.test.rules.LoggerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

import ch.qos.logback.classic.spi.ILoggingEvent;

@RunWith(MockitoJUnitRunner.class)
public class TestLdapDecisionNodeSecretIdProvider {

    @Mock
    AnnotatedServiceRegistry serviceRegistry;

    @Mock
    LdapDecisionNode.Config config;

    @Mock
    Realm realm;

    @Rule
    public LoggerRule loggerRule = new LoggerRule(LdapDecisionNodeSecretIdProvider.class);

    private LdapDecisionNodeSecretIdProvider provider;

    @Before
    public void setUp() {
        provider = new LdapDecisionNodeSecretIdProvider(serviceRegistry);
    }

    @Test
    public void testGetRealmMultiInstanceSecretIds() throws SMSException, SSOException {
        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.of("banana"));
        ImmutableSet<LdapDecisionNode.Config> ldapNodeConfigs = ImmutableSet.of(config);
        given(serviceRegistry.getRealmInstances(LdapDecisionNode.Config.class, realm))
                .willReturn(ldapNodeConfigs);

        // When
        Multimap<String, String> realmMultiInstanceSecretIds = provider.getRealmMultiInstanceSecretIds(null, realm);
        Collection<String> actual = realmMultiInstanceSecretIds.get("LdapNode");

        // Then
        assertThat(actual).containsExactly("am.authentication.nodes.ldap.decision.mtls.banana.cert");
    }

    @Test
    public void testLogsErrorWhenExceptionThrown() throws SMSException, SSOException {
        given(serviceRegistry.getRealmInstances(LdapDecisionNode.Config.class, realm))
                .willThrow(new SMSException());

        // When
        Multimap<String, String> actual = provider.getRealmMultiInstanceSecretIds(null, realm);

        // Then
        assertThat(actual.isEmpty()).isTrue();
        List<String> errors = loggerRule.getErrors(ILoggingEvent::getFormattedMessage);
        assertThat(errors).containsExactly("Failed to get ldap node secret labels");
    }

    @Test
    public void testThrowsNullPointerWhenRealmIsNull() {
        assertThatThrownBy(() -> provider.getRealmMultiInstanceSecretIds(null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
