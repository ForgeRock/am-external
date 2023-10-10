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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.Set;

import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.secrets.SecretStoreWithMappings;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.forgerock.util.query.QueryFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;

public class LdapDecisionNodeSecretStoreConfigChangeListenerTest {

    private static final String REALM_DN = "ou=am-config";
    private static final String VALID_MTLS_SECRET_ID = "am.authentication.nodes.ldap.decision.mtls.testLabel.cert";
    private static final String VALID_MTLS_SECRET_ID_2 = "am.authentication.nodes.ldap.decision.mtls.testLabel2.cert";
    private static final String NOT_A_VALID_SECRET_ID = "not.a.valid.label";

    private LdapDecisionNodeSecretStoreConfigChangeListener listener;

    @Before
    public void setUp() {
        listener = new LdapDecisionNodeSecretStoreConfigChangeListener();
    }

    @Test
    public void shouldRefreshTheConnectionPoolsWhenSecretStoreMappingHasChanged() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            final PurposeMapping mapping = mockPurposeMapping(VALID_MTLS_SECRET_ID);

            // when
            listener.secretStoreMappingHasChanged(mapping, REALM_DN, ServiceListener.MODIFIED);

            // then
            utils.verify(() -> LDAPAuthUtils.closeConnectionPoolsBySecretLabel(
                            eq(VALID_MTLS_SECRET_ID),
                            eq(REALM_DN)),
                    times(1));
        }
    }

    @Test
    public void shouldNotRefreshTheConnectionPoolsWhenSecretStoreMappingHasChangedButMappingIsNullAndNotARemove() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            // when
            listener.secretStoreMappingHasChanged(null, REALM_DN, ServiceListener.MODIFIED);

            // then
            utils.verifyNoInteractions();
        }
    }

    @Test
    public void shouldNotRefreshTheConnectionPoolsWhenSecretStoreMappingHasChangedButRealmIsNull() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            final PurposeMapping mapping = mockPurposeMapping(VALID_MTLS_SECRET_ID);

            // when
            listener.secretStoreMappingHasChanged(mapping, null, ServiceListener.MODIFIED);

            // then
            utils.verifyNoInteractions();
        }
    }

    @Test
    public void shouldNotRefreshTheConnectionPoolsWhenSecretStoreMappingHasChangedButInvalidSecretId() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            final PurposeMapping mapping = mockPurposeMapping(NOT_A_VALID_SECRET_ID);

            // when
            listener.secretStoreMappingHasChanged(mapping, REALM_DN, ServiceListener.MODIFIED);

            // then
            utils.verifyNoInteractions();
        }
    }

    @Test
    public void shouldRefreshTheConnectionPoolsWhenSecretStoreMappingHasChangedAndSecretStoreMappingIsRemoved() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            // when
            listener.secretStoreMappingHasChanged(null, REALM_DN, ServiceListener.REMOVED);

            // then
            utils.verify(() -> LDAPAuthUtils.closeConnectionPoolsBySecretLabel(
                            isNull(),
                            eq(REALM_DN)),
                    times(1));
        }
    }

    @Test
    public void shouldRefreshTheConnectionPoolsWhenSecretStoreHasChanged() throws SMSException, SSOException {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            final Set<PurposeMapping> purposeMappings = Set.of(
                    mockPurposeMapping(VALID_MTLS_SECRET_ID),
                    mockPurposeMapping(VALID_MTLS_SECRET_ID_2),
                    mockPurposeMapping(NOT_A_VALID_SECRET_ID));

            final Multiple<PurposeMapping> multiple = mock(Multiple.class);
            given(multiple.get(QueryFilter.alwaysTrue())).willReturn(purposeMappings);

            final SecretStoreWithMappings secretStore = mock(SecretStoreWithMappings.class);
            given(secretStore.mappings()).willReturn(multiple);

            // when
            listener.secretStoreHasChanged(secretStore, REALM_DN, ServiceListener.MODIFIED);

            // then
            utils.verify(() -> LDAPAuthUtils.closeConnectionPoolsBySecretLabel(
                            eq(VALID_MTLS_SECRET_ID),
                            eq(REALM_DN)),
                    times(1));
            utils.verify(() -> LDAPAuthUtils.closeConnectionPoolsBySecretLabel(
                            eq(VALID_MTLS_SECRET_ID_2),
                            eq(REALM_DN)),
                    times(1));
            utils.verifyNoMoreInteractions();
        }
    }

    @Test
    public void shouldNotRefreshTheConnectionPoolsWhenASecretStoreIsAdded() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            final SecretStoreWithMappings secretStore = mock(SecretStoreWithMappings.class);

            // when
            listener.secretStoreHasChanged(secretStore, REALM_DN, ServiceListener.ADDED);

            // then
            utils.verifyNoInteractions();
        }
    }

    @Test
    public void shouldRefreshTheConnectionPoolsWhenSecretStoreIsDeleted() {
        try (MockedStatic<LDAPAuthUtils> utils = mockStatic(LDAPAuthUtils.class)) {
            // when
            listener.secretStoreHasChanged(null, REALM_DN, ServiceListener.REMOVED);

            // then
            utils.verify(() -> LDAPAuthUtils.closeConnectionPoolsBySecretLabel(
                            isNull(),
                            eq(REALM_DN)),
                    times(1));
            utils.verifyNoMoreInteractions();
        }
    }

    private static PurposeMapping mockPurposeMapping(final String secretId) {
        final PurposeMapping mapping = mock(PurposeMapping.class);
        given(mapping.secretId()).willReturn(secretId);
        return mapping;
    }


}