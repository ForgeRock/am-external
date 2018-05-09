/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.i18n.PreferredLocales;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

public class OneTimePasswordSmsSenderNodeTest {

    private static final String TEST_REALM = "testRealm";
    private static final String TEST_USERNAME = "testUsername";
    private static final String PHONE_NUMBER = "666999";
    private static final String PHONE_CARRIER = "@provider.com";
    private static final String PHONE_CARRIER_NO_AT = "provider.com";
    private static final String PHONE_NUMBER_ATTR = "number";
    private static final String PHONE_CARRIER_ATTR = "carrier";

    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private IdentityProvider identityProvider;
    @Mock
    private AMIdentity amIdentity;
    @Mock
    private OneTimePasswordSmsSenderNode.Config serviceConfig;
    private JsonValue sharedState;

    @BeforeMethod
    public void setUp() throws Exception {
        reset(MockSMSGateway.smsGateway);
        initMocks(this);
        initConfig();
        given(coreWrapper.convertRealmPathToRealmDn(anyString())).willReturn(TEST_REALM);
        given(identityProvider.getIdentity(eq(TEST_USERNAME), eq(TEST_REALM))).willReturn(amIdentity);
        given(amIdentity.getAttribute(PHONE_NUMBER_ATTR)).willReturn(Collections.singleton(PHONE_NUMBER));
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER));
        sharedState = json(object(field(USERNAME, TEST_USERNAME), field(REALM, TEST_REALM)));
    }


    @Test
    public void shouldSendEmailWhenConfigurationAndSharedStateEmailAddressAreValid()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER));

        //when
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));

        then(MockSMSGateway.smsGateway).should()
                .sendSMSMessage(eq(serviceConfig.fromEmailAddress()), eq(PHONE_NUMBER + PHONE_CARRIER),
                        any(), any(), any(), any());
    }

    @Test
    public void shouldSendEmailWhenAllIsValidWithNoCarrier()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(null);

        //when
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));

        then(MockSMSGateway.smsGateway).should().sendSMSMessage(eq(serviceConfig.fromEmailAddress()), eq(PHONE_NUMBER),
                any(), any(), any(), any());
    }

    @Test
    public void shouldSendEmailWhenAllIsValidWithCarrierAddingAtSymbol()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(PHONE_CARRIER_ATTR)).willReturn(Collections.singleton(PHONE_CARRIER_NO_AT));

        //when
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));

        then(MockSMSGateway.smsGateway).should()
                .sendSMSMessage(eq(serviceConfig.fromEmailAddress()),
                        eq(PHONE_NUMBER + "@" + PHONE_CARRIER_NO_AT),
                        any(), any(), any(), any());
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenEmailSendFails() throws AuthLoginException, NodeProcessException {
        willThrow(AuthLoginException.class).given(MockSMSGateway.smsGateway)
                .sendSMSMessage(any(), any(), any(), any(), any(), any());
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenFailToRetrieveIdentity()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        given(identityProvider.getIdentity(anyString(), anyString()))
                .willThrow(IdRepoException.class);
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState, preferredLocales));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenFailToRetrievePhoneNumberFromIdentity()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        given(amIdentity.getAttribute(serviceConfig.mobilePhoneAttributeName())).willThrow(IdRepoException.class);
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState, preferredLocales));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenFailToRetrieveCarrierFromIdentity()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        given(amIdentity.getAttribute(serviceConfig.mobileCarrierAttributeName().get()))
                .willThrow(IdRepoException.class);
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState, preferredLocales));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenFailToSendMessage()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        PreferredLocales preferredLocales = new PreferredLocales(ImmutableList.of(Locale.ENGLISH));
        doThrow(AuthLoginException.class).when(MockSMSGateway.smsGateway)
                .sendSMSMessage(any(), any(), any(), any(), any(), any());
        new OneTimePasswordSmsSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState, preferredLocales));
    }

    private void initConfig() {
        given(serviceConfig.fromEmailAddress()).willReturn("my.email@forgerock.com");
        given(serviceConfig.hostName()).willReturn("localhost");
        given(serviceConfig.hostPort()).willReturn(8080);
        given(serviceConfig.username()).willReturn("mcarter");
        given(serviceConfig.password()).willReturn("password".toCharArray());
        given(serviceConfig.mobilePhoneAttributeName()).willReturn(PHONE_NUMBER_ATTR);
        given(serviceConfig.mobileCarrierAttributeName()).willReturn(Optional.of(PHONE_CARRIER_ATTR));
        given(serviceConfig.sslOption()).willReturn(SmtpBaseConfig.SslOption.SSL);
        given(serviceConfig.smsGatewayImplementationClass()).willReturn(MockSMSGateway.class.getName());
    }

    static class MockSMSGateway implements SMSGateway {

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
}