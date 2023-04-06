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
 * Copyright 2022 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.oauth2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import javax.mail.MessagingException;

import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.forgerock.am.mail.application.AMSendMail;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEmailGatewayImplTest {

    private static final String FROM = "from@example.com";
    private static final String TO = "to@example.com";
    private static final String SUBJECT = "Email Subject";
    private static final String MESSAGE = "This is the message";
    private static final String HOST = "smtp.example.com";
    private static final String PORT = "5005";
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "fox";
    private static final boolean SSL = true;

    @Mock
    private AMSendMail sendMail;
    private EmailGateway emailGateway;

    @Before
    public void setup() {
        emailGateway = new DefaultEmailGatewayImpl(sendMail);
    }

    @Test
    public void testNullToAddressNoEmailSent() throws NoEmailSentException {
        // when
        emailGateway.sendEmail(FROM, null, SUBJECT, MESSAGE, Map.of());

        // then
        then(sendMail).shouldHaveNoInteractions();
    }

    @Test
    public void testMissingMailArgsUsesDefault() throws NoEmailSentException, MessagingException {
        // when
        emailGateway.sendEmail(FROM, TO, SUBJECT, MESSAGE, Map.of());

        // then
        then(sendMail).should().postMail(Arrays.array(TO), SUBJECT, MESSAGE, FROM);
    }

    @Test
    public void testAllArgsSendsEmailToCorrectPlace() throws NoEmailSentException, MessagingException {
        //given
        Map<String, String>  options = Map.of(OAuthParam.KEY_SMTP_HOSTNAME, HOST,
                OAuthParam.KEY_SMTP_PORT, PORT,
                OAuthParam.KEY_SMTP_USERNAME, USERNAME,
                OAuthParam.KEY_SMTP_PASSWORD, PASSWORD,
                OAuthParam.KEY_SMTP_SSL_ENABLED, String.valueOf(SSL));

        // when
        emailGateway.sendEmail(FROM, TO, SUBJECT, MESSAGE, options);

        // then
        then(sendMail).should().postMail(Arrays.array(TO), SUBJECT, MESSAGE, FROM, "UTF-8", HOST, PORT,
                USERNAME, PASSWORD, SSL);
    }

    @Test
    public void testThrowsExceptionWhenMessageFails() throws MessagingException {
        //given
        doThrow(MessagingException.class).when(sendMail).postMail(Arrays.array(TO), SUBJECT, MESSAGE, FROM);

        // when/then
        assertThatThrownBy(() -> emailGateway.sendEmail(FROM, TO, SUBJECT, MESSAGE, Map.of()))
                .isInstanceOf(NoEmailSentException.class);
    }
}