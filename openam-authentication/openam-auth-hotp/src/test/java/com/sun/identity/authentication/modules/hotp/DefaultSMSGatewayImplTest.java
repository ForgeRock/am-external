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
 * Copyright 2022-2025 Ping Identity Corporation.
 */
package com.sun.identity.authentication.modules.hotp;


import static com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl.SMTPHOSTNAME;
import static com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl.SMTPHOSTPORT;
import static com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl.SMTPSSLENABLED;
import static com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl.SMTPUSERNAME;
import static com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl.SMTPUSERPASSWORD;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.forgerock.am.mail.application.AMSendMail;
import com.sun.identity.authentication.spi.AuthLoginException;

@ExtendWith(MockitoExtension.class)
public class DefaultSMSGatewayImplTest {

    private static final String FROM = "from@example.com";
    private static final String TO = "to@example.com";
    private static final String SUBJECT = "Email Subject";
    private static final String MESSAGE = "This is the OTP: ";
    private static final String HOST = "smtp.example.com";
    private static final String PORT = "5005";
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "fox";
    private static final String SSL = "Start TLS";
    private static final String ONE_TIME_PASSWORD = "one-time-password";

    @Mock
    private AMSendMail sendMail;
    private SMSGateway emailGateway;

    @BeforeEach
    void setup() {
        emailGateway = new DefaultSMSGatewayImpl(sendMail);
    }

    @Test
    void testNullToAddressNoEmailSent() throws AuthLoginException {
        // when
        emailGateway.sendEmail(FROM, null, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, Map.of());

        // then
        then(sendMail).shouldHaveNoInteractions();
    }

    @Test
    void testNullToAddressNoSMSSent() throws AuthLoginException {
        // when
        emailGateway.sendSMSMessage(FROM, null, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, Map.of());

        // then
        then(sendMail).shouldHaveNoInteractions();
    }

    @Test
    void testMissingEmailArgsUsesDefault() throws AuthLoginException, MessagingException {
        // when
        emailGateway.sendEmail(FROM, TO, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, Map.of());

        // then
        then(sendMail).should().postMail(Arrays.array(TO), SUBJECT, MESSAGE + ONE_TIME_PASSWORD, FROM);
    }


    @Test
    void testMissingSmsArgsUsesDefault() throws AuthLoginException, MessagingException {
        // when
        emailGateway.sendSMSMessage(FROM, TO, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, Map.of());

        // then
        then(sendMail).should().postMail(Arrays.array(TO), SUBJECT, MESSAGE + ONE_TIME_PASSWORD, FROM);
    }

    @Test
    void testAllArgsSendsEmailToCorrectPlace() throws AuthLoginException, MessagingException {
        //given
        Map<String, Set<String>>  options = Map.of(SMTPHOSTNAME, Set.of(HOST),
                SMTPHOSTPORT, Set.of(PORT),
                SMTPUSERNAME, Set.of(USERNAME),
                SMTPUSERPASSWORD, Set.of(PASSWORD),
                SMTPSSLENABLED, Set.of(SSL));

        // when
        emailGateway.sendEmail(FROM, TO, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, options);

        // then
        then(sendMail).should().postMail(Arrays.array(TO), SUBJECT, MESSAGE + ONE_TIME_PASSWORD, FROM,
                "UTF-8", HOST, PORT, USERNAME, PASSWORD, false, true);
    }


    @Test
    void testAllArgsSendsSmsToCorrectPlace() throws AuthLoginException, MessagingException {
        //given
        Map<String, Set<String>>  options = Map.of(SMTPHOSTNAME, Set.of(HOST),
                SMTPHOSTPORT, Set.of(PORT),
                SMTPUSERNAME, Set.of(USERNAME),
                SMTPUSERPASSWORD, Set.of(PASSWORD),
                SMTPSSLENABLED, Set.of(SSL));

        // when
        emailGateway.sendSMSMessage(FROM, TO, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, options);

        // then
        then(sendMail).should().postMail(Arrays.array(TO), SUBJECT, MESSAGE + ONE_TIME_PASSWORD, FROM,
                "UTF-8", HOST, PORT, USERNAME, PASSWORD, false, true);
    }

    @Test
    void testThrowsExceptionWhenEmailMessageFails() throws MessagingException {
        //given
        doThrow(MessagingException.class).when(sendMail).postMail(Arrays.array(TO), SUBJECT,
                MESSAGE + ONE_TIME_PASSWORD, FROM);

        // when/then
        assertThatThrownBy(() -> emailGateway.sendEmail(FROM, TO, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, Map.of()))
                .isInstanceOf(AuthLoginException.class);
    }

    @Test
    void testThrowsExceptionWhenSmsMessageFails() throws MessagingException {
        //given
        doThrow(MessagingException.class).when(sendMail).postMail(Arrays.array(TO), SUBJECT,
                MESSAGE + ONE_TIME_PASSWORD, FROM);

        // when/then
        assertThatThrownBy(() -> emailGateway.sendSMSMessage(FROM, TO, SUBJECT, MESSAGE, ONE_TIME_PASSWORD, Map.of()))
                .isInstanceOf(AuthLoginException.class);
    }

}
