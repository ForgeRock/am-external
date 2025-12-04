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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.TEST_UNIVERSAL_ID;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.reset;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.ImmutableList;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
import com.sun.identity.authentication.modules.hotp.SMSGatewayLookup;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OneTimePasswordSmsSenderNodeTest {

    private static final String TEST_REALM = "testRealm";
    private static final String TEST_USERNAME = "testUsername";
    private static final String TEST_ONE_TIME_PASSWORD = "978413";
    private static final String PHONE_NUMBER = "666999";
    private static final String PHONE_CARRIER = "@provider.com";
    private static final String PHONE_CARRIER_NO_AT = "provider.com";
    private static final String PHONE_NUMBER_ATTR = "number";
    private static final String PHONE_CARRIER_ATTR = "carrier";

    @Mock
    private NodeUserIdentityProvider identityProvider;
    @Mock
    private LocaleSelector localeSelector;
    @Mock
    private AMIdentity amIdentity;
    @Mock
    private OneTimePasswordSmsSenderNode.Config serviceConfig;
    @Mock
    private Realm realm;
    private JsonValue sharedState;
    private JsonValue secureState;
    private Map<String, SMSGateway> smsGatewayMap;
    private SMSGatewayLookup smsGatewayLookup;

    @Mock
    private OtpNodeConnectionConfigMapper connectionConfigMapper;

    @BeforeEach
    void setUp() throws Exception {
        reset(MockSMSGateway.smsGateway);
        initConfig();
        given(identityProvider.getAMIdentity(eq(Optional.of(TEST_UNIVERSAL_ID)), any()))
                .willReturn(Optional.of(amIdentity));
        given(amIdentity.getAttribute(PHONE_NUMBER_ATTR)).willReturn(Collections.singleton(PHONE_NUMBER));
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER));
        sharedState = json(object(field(USERNAME, TEST_USERNAME), field(REALM, TEST_REALM)));
        secureState = json(object(field(ONE_TIME_PASSWORD, TEST_ONE_TIME_PASSWORD)));
        smsGatewayMap = new HashMap<>();
        smsGatewayLookup = new SMSGatewayLookup(smsGatewayMap);
    }

    @Test
    void shouldSendEmailWhenConfigurationAndSharedStateEmailAddressAreValid()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER));

        //when
        new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider, localeSelector,
                smsGatewayLookup, connectionConfigMapper).process(newTreeContext(sharedState, secureState));

        then(MockSMSGateway.smsGateway).should()
                .sendSMSMessage(eq(serviceConfig.fromEmailAddress()), eq(PHONE_NUMBER + PHONE_CARRIER),
                        any(), any(), any(), any());
    }

    @Test
    void shouldSendEmailWhenAllIsValidWithNoCarrier()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(null);

        //when
        new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider, localeSelector,
                smsGatewayLookup, connectionConfigMapper).process(newTreeContext(sharedState, secureState));

        then(MockSMSGateway.smsGateway).should().sendSMSMessage(eq(serviceConfig.fromEmailAddress()), eq(PHONE_NUMBER),
                any(), any(), any(), any());
    }

    @Test
    void shouldSendEmailWhenAllIsValidWithCarrierAddingAtSymbol()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER_NO_AT));

        //when
        new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider, localeSelector,
                smsGatewayLookup, connectionConfigMapper).process(newTreeContext(sharedState, secureState));

        then(MockSMSGateway.smsGateway).should()
                .sendSMSMessage(eq(serviceConfig.fromEmailAddress()),
                        eq(PHONE_NUMBER + "@" + PHONE_CARRIER_NO_AT),
                        any(), any(), any(), any());
    }

    @Test
    void throwsNodeProcessExceptionWhenEmailSendFails() throws AuthLoginException {
        willThrow(AuthLoginException.class).given(MockSMSGateway.smsGateway)
                .sendSMSMessage(any(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider,
                localeSelector, smsGatewayLookup, connectionConfigMapper)
                .process(newTreeContext(sharedState, secureState)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void throwsNodeProcessExceptionWhenFailToRetrieveIdentity() {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        assertThatThrownBy(() -> new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider,
                localeSelector, smsGatewayLookup, connectionConfigMapper)
                .process(newTreeContext(sharedState, secureState, preferredLocales)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void throwsNodeProcessExceptionWhenFailToRetrievePhoneNumberFromIdentity() {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        assertThatThrownBy(() -> new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider,
                localeSelector, smsGatewayLookup, connectionConfigMapper)
                .process(newTreeContext(sharedState, secureState, preferredLocales)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void throwsNodeProcessExceptionWhenFailToRetrieveCarrierFromIdentity()
            throws IdRepoException, SSOException {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        assertThatThrownBy(() -> new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider,
                localeSelector, smsGatewayLookup, connectionConfigMapper)
                .process(newTreeContext(sharedState, secureState, preferredLocales)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void throwsNodeProcessExceptionWhenFailToSendMessage()
            throws AuthLoginException {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        doThrow(AuthLoginException.class).when(MockSMSGateway.smsGateway)
                .sendSMSMessage(any(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider,
                localeSelector, smsGatewayLookup, connectionConfigMapper)
                .process(newTreeContext(sharedState, secureState, preferredLocales)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldFindSMSGatewayInMap() throws IdRepoException, SSOException, NodeProcessException,
                                                               AuthLoginException {
        given(serviceConfig.smsGatewayImplementationClass()).willReturn(StubGuiceSMSGateway.class.getName());
        StubGuiceSMSGateway stubGuiceSMSGateway = new StubGuiceSMSGateway(mock(SMSGateway.class));
        smsGatewayMap.put(StubGuiceSMSGateway.class.getName(), stubGuiceSMSGateway);
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER));
        Map<String, Set<String>> map = Map.of("key", Set.of("value"));
        given(connectionConfigMapper.asConfigMap(any(), any())).willReturn(map);

        new OneTimePasswordSmsSenderNode(serviceConfig, realm, identityProvider, localeSelector,
                smsGatewayLookup, connectionConfigMapper).process(newTreeContext(sharedState, secureState));

        then(stubGuiceSMSGateway.smsGateway).should()
                .sendSMSMessage(eq(serviceConfig.fromEmailAddress()), eq(PHONE_NUMBER + PHONE_CARRIER),
                        any(), any(), any(), same(map));
    }

    private void initConfig() {
        given(serviceConfig.fromEmailAddress()).willReturn("my.email@forgerock.com");
        given(serviceConfig.mobilePhoneAttributeName()).willReturn(PHONE_NUMBER_ATTR);
        given(serviceConfig.mobileCarrierAttributeName()).willReturn(Optional.of(PHONE_CARRIER_ATTR));
        given(serviceConfig.smsGatewayImplementationClass()).willReturn(MockSMSGateway.class.getName());
    }

    public static class MockSMSGateway implements SMSGateway {

        static SMSGateway smsGateway = mock(SMSGateway.class);

        @Override
        public void sendSMSMessage(String from, String to, String subject, String message, String code,
                                   Map options) throws AuthLoginException {
            smsGateway.sendSMSMessage(from, to, subject, message, code, options);
        }

        @Override
        public void sendEmail(String from, String to, String subject, String message, String code,
                              Map options) throws AuthLoginException {
            smsGateway.sendEmail(from, to, subject, message, code, options);
        }
    }

   public static class StubGuiceSMSGateway implements SMSGateway {

        final SMSGateway smsGateway;

        StubGuiceSMSGateway(SMSGateway gateway) {
            this.smsGateway = gateway;
        }

        @Override
        public void sendSMSMessage(String from, String to, String subject, String message, String code, Map options)
                throws AuthLoginException {
            smsGateway.sendSMSMessage(from, to, subject, message, code, options);
        }

        @Override
        public void sendEmail(String from, String to, String subject, String message, String code, Map options)
                throws AuthLoginException {
            smsGateway.sendEmail(from, to, subject, message, code, options);
        }
    }
}
