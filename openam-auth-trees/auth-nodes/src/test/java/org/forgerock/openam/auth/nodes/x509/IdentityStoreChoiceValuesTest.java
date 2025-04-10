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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static com.sun.identity.idm.IdConstants.REPO_SERVICE;
import static com.sun.identity.shared.Constants.ORGANIZATION_NAME;
import static com.sun.identity.shared.Constants.SSO_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * Unit tests for {@link IdentityStoreChoiceValues}.
 */
@ExtendWith(MockitoExtension.class)
class IdentityStoreChoiceValuesTest {

    @Mock
    private ServiceConfigManagerFactory serviceConfigManagerFactory;

    @InjectMocks
    private IdentityStoreChoiceValues identityStoreChoiceValues;

    @Test
    void shouldGetChoiceValuesGivenConfiguredIdentityStoresAndAdminToken() throws SMSException, SSOException {
        // Given
        SSOToken adminToken = mock(SSOToken.class);
        Map<String, Object> envParams = Map.of(SSO_TOKEN, adminToken, ORGANIZATION_NAME, "testRealm");
        ServiceConfigManager serviceConfigManager = mock(ServiceConfigManager.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        given(serviceConfig.getSubConfigNames()).willReturn(Set.of("store1", "store2"));
        given(serviceConfigManager.getOrganizationConfig("testRealm", null)).willReturn(serviceConfig);
        given(serviceConfigManagerFactory.create(REPO_SERVICE, adminToken))
                .willReturn(serviceConfigManager);

        // When
        Map<String, String> choices = identityStoreChoiceValues.getChoiceValues(envParams);

        // Then
        assertThat(choices).containsExactlyInAnyOrderEntriesOf(Map.of(
                ISAuthConstants.BLANK, ISAuthConstants.BLANK,
                "store1", "store1",
                "store2", "store2"));
    }

    @Test
    void shouldGetChoiceValuesGivenConfiguredIdentityStoresAndNoAdminToken() throws SMSException, SSOException {
        // Given
        Map<String, Object> envParams = Map.of(ORGANIZATION_NAME, "testRealm");
        ServiceConfigManager serviceConfigManager = mock(ServiceConfigManager.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        given(serviceConfig.getSubConfigNames()).willReturn(Set.of("store1", "store2"));
        given(serviceConfigManager.getOrganizationConfig("testRealm", null)).willReturn(serviceConfig);
        given(serviceConfigManagerFactory.create(REPO_SERVICE)).willReturn(serviceConfigManager);

        // When
        Map<String, String> choices = identityStoreChoiceValues.getChoiceValues(envParams);

        // Then
        assertThat(choices).containsExactlyInAnyOrderEntriesOf(Map.of(
                ISAuthConstants.BLANK, ISAuthConstants.BLANK,
                "store1", "store1",
                "store2", "store2"));
    }

}