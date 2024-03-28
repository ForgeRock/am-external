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
 * Copyright 2017-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.TEST_UNIVERSAL_ID;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
import com.sun.identity.authentication.modules.hotp.SMSGatewayLookup;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

@RunWith(MockitoJUnitRunner.class)
public class OneTimePasswordSmtpSenderNodeTest {

    private static final String TEST_REALM = "testRealm";
    private static final String TEST_USERNAME = "testUsername";
    private static final String TO_EMAIL_ADDRESS = "joe.bloggs@forgerock.com";
    private static final String TEST_ONE_TIME_PASSWORD = "978413";

    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private LocaleSelector localeSelector;
    @Mock
    private IdmIntegrationService idmIntegrationService;
    @Mock
    private AMIdentity amIdentity;
    @Mock
    private OneTimePasswordSmtpSenderNode.Config serviceConfig;
    @Mock
    private Realm realm;
    private JsonValue sharedState;
    private JsonValue secureState;
    private Map<String, SMSGateway> smsGatewayMap;
    private SMSGatewayLookup smsGatewayLookup;

    @Mock
    private OtpNodeConnectionConfigMapper connectionConfigMapper;

    @Before
    public void setUp() throws Exception {
        reset(MockSMSGateway.smsGateway);
        initConfig();
        given(coreWrapper.getIdentity(eq(TEST_UNIVERSAL_ID))).willReturn(amIdentity);
        given(amIdentity.getAttribute(serviceConfig.emailAttribute()))
                .willReturn(Collections.singleton(TO_EMAIL_ADDRESS));
        sharedState = json(object(field(USERNAME, TEST_USERNAME),
                field(REALM, TEST_REALM)));
        secureState = json(object(field(ONE_TIME_PASSWORD, TEST_ONE_TIME_PASSWORD)));
        smsGatewayMap = new HashMap<>();
        smsGatewayLookup = new SMSGatewayLookup(smsGatewayMap);
    }


    @Test
    public void shouldSendEmailWhenConfigurationAndSharedStateEmailAddressAreValid()
            throws NodeProcessException, AuthLoginException {

        Map<String, Set<String>> map = Map.of("key", Set.of("value"));
        given(connectionConfigMapper.asConfigMap(any(), any())).willReturn(map);

        //when
        new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm, coreWrapper, identityService,
                idmIntegrationService, localeSelector, smsGatewayLookup)
                .process(newTreeContext(sharedState, secureState));

        then(MockSMSGateway.smsGateway).should().sendEmail(eq(serviceConfig.fromEmailAddress()), eq(TO_EMAIL_ADDRESS),
                any(), any(), any(), same(map));
    }

    @Test
    public void shouldNotGetAMIdentityIfEmailIsInSharedState()
            throws NodeProcessException {

        JsonValue state = sharedState.copy().add(EMAIL_ADDRESS, TO_EMAIL_ADDRESS);

        //when
        new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm, coreWrapper, identityService,
                idmIntegrationService, localeSelector, smsGatewayLookup).process(newTreeContext(state, secureState));

        then(coreWrapper).shouldHaveNoInteractions();
    }

    @Test
    public void throwsNodeProcessExceptionWhenEmailSendFails() throws AuthLoginException {
        willThrow(AuthLoginException.class).given(MockSMSGateway.smsGateway)
                .sendEmail(any(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm,
                coreWrapper, identityService, idmIntegrationService, localeSelector, smsGatewayLookup)
                .process(newTreeContext(sharedState, secureState)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    public void throwsNodeProcessExceptionWhenIdentityProviderFails() {
        given(coreWrapper.getIdentity(anyString())).willReturn(null);
        assertThatThrownBy(() -> new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm,
                coreWrapper, identityService, idmIntegrationService, localeSelector, smsGatewayLookup)
                .process(newTreeContext(sharedState, secureState)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    public void throwsNodeProcessExceptionWhenIdentityProviderGetsEmailFails() throws IdRepoException, SSOException {
        given(amIdentity.getAttribute(serviceConfig.emailAttribute())).willThrow(IdRepoException.class);

        assertThatThrownBy(() -> new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm,
                coreWrapper, identityService, idmIntegrationService, localeSelector, smsGatewayLookup)
                .process(newTreeContext(sharedState, secureState)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    public void throwsNodeProcessExceptionWhenEmailNotFound() throws IdRepoException, SSOException {
        given(amIdentity.getAttribute(serviceConfig.emailAttribute())).willReturn(null);

        assertThatThrownBy(() -> new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm,
                coreWrapper, identityService, idmIntegrationService, localeSelector, smsGatewayLookup)
                .process(newTreeContext(sharedState, secureState)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    public void shouldFindSMSGatewayInMap()
            throws NodeProcessException, AuthLoginException {
        given(serviceConfig.smsGatewayImplementationClass()).willReturn(StubGuiceSMSGateway.class.getName());
        StubGuiceSMSGateway stubGuiceSMSGateway = new StubGuiceSMSGateway(mock(SMSGateway.class));
        smsGatewayMap.put(StubGuiceSMSGateway.class.getName(), stubGuiceSMSGateway);
        //when
        new OneTimePasswordSmtpSenderNode(connectionConfigMapper, serviceConfig, realm, coreWrapper, identityService,
                idmIntegrationService, localeSelector, smsGatewayLookup)
                .process(newTreeContext(sharedState, secureState));

        then(stubGuiceSMSGateway.smsGateway).should().sendEmail(eq(serviceConfig.fromEmailAddress()),
                eq(TO_EMAIL_ADDRESS), any(), any(), any(), any());
    }


    private void initConfig() {
        given(serviceConfig.emailAttribute()).willReturn("testEmailAttribute");
        given(serviceConfig.fromEmailAddress()).willReturn("my.email@forgerock.com");
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
