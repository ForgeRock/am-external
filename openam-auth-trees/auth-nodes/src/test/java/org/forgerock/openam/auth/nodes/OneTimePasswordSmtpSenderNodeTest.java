/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.*;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.CoreWrapper;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

public class OneTimePasswordSmtpSenderNodeTest {

    private static final String TEST_REALM = "testRealm";
    private static final String TEST_USERNAME = "testUsername";
    private static final String TO_EMAIL_ADDRESS = "joe.bloggs@forgerock.com";
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private IdentityProvider identityProvider;
    @Mock
    private AMIdentity amIdentity;
    @Mock
    private OneTimePasswordSmtpSenderNode.Config serviceConfig;
    private JsonValue sharedState;

    @BeforeMethod
    public void setUp() throws Exception {
        reset(MockSMSGateway.smsGateway);
        initMocks(this);
        initConfig();
        given(coreWrapper.convertRealmPathToRealmDn(anyString())).willReturn(TEST_REALM);
        given(identityProvider.getIdentity(eq(TEST_USERNAME), eq(TEST_REALM))).willReturn(amIdentity);
        given(amIdentity.getAttribute(serviceConfig.emailAttribute()))
                .willReturn(Collections.singleton(TO_EMAIL_ADDRESS));
        sharedState = json(object(field(USERNAME, TEST_USERNAME),
                field(REALM, TEST_REALM)));
    }


    @Test
    public void shouldSendEmailWhenConfigurationAndSharedStateEmailAddressAreValid()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {
        //when
        new OneTimePasswordSmtpSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));

        then(MockSMSGateway.smsGateway).should().sendEmail(eq(serviceConfig.fromEmailAddress()), eq(TO_EMAIL_ADDRESS),
                any(), any(), any(), any());
    }

    @Test
    public void shouldNotGetAMIdentityIfEmailIsInSharedState()
            throws NodeProcessException, AuthLoginException, IdRepoException, SSOException {

        JsonValue state = sharedState.copy().add(EMAIL_ADDRESS, TO_EMAIL_ADDRESS);

        //when
        new OneTimePasswordSmtpSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(state));

        then(identityProvider).shouldHaveZeroInteractions();
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenEmailSendFails() throws AuthLoginException, NodeProcessException {
        willThrow(AuthLoginException.class).given(MockSMSGateway.smsGateway)
                .sendEmail(any(), any(), any(), any(), any(), any());
        new OneTimePasswordSmtpSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenIdentityProviderFails()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        given(identityProvider.getIdentity(anyString(), anyString()))
                .willThrow(IdRepoException.class);
        new OneTimePasswordSmtpSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenIdentityProviderGetsEmailFails()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(serviceConfig.emailAttribute())).willThrow(IdRepoException.class);

        new OneTimePasswordSmtpSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void throwsNodeProcessExceptionWhenEmailNotFound()
            throws AuthLoginException, NodeProcessException, IdRepoException, SSOException {
        given(amIdentity.getAttribute(serviceConfig.emailAttribute())).willReturn(null);

        new OneTimePasswordSmtpSenderNode(serviceConfig, coreWrapper, identityProvider)
                .process(newTreeContext(sharedState));
    }

    private void initConfig() {
        given(serviceConfig.emailAttribute()).willReturn("testEmailAttribute");
        given(serviceConfig.fromEmailAddress()).willReturn("my.email@forgerock.com");
        given(serviceConfig.hostName()).willReturn("localhost");
        given(serviceConfig.hostPort()).willReturn(8080);
        given(serviceConfig.username()).willReturn("mcarter");
        given(serviceConfig.password()).willReturn("password".toCharArray());
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