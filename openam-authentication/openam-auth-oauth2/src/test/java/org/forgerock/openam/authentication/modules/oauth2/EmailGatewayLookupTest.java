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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EmailGatewayLookupTest {

    private Map<String, EmailGateway> emailGatewayMap;
    private EmailGatewayLookup emailGatewayLookup;

    @BeforeEach
    void setup() {
        emailGatewayMap = new HashMap<>();
        emailGatewayLookup = new EmailGatewayLookup(emailGatewayMap);
    }

    @Test
    void testGatewayLookupWithNoGuiceBindings() throws ClassNotFoundException, InstantiationException,
                                                                      IllegalAccessException {
        EmailGateway emailGateway = emailGatewayLookup.getEmailGateway(EmailGatewayTestImpl.class.getName());
        assertThat(emailGateway).isInstanceOf(EmailGatewayTestImpl.class);
    }

    @Test
    void testGatewayLookupWithGuiceBindings() throws ClassNotFoundException, InstantiationException,
                                                                    IllegalAccessException {
        GuiceEmailGatewayTestImpl expected = new GuiceEmailGatewayTestImpl("unused");
        emailGatewayMap.put(GuiceEmailGatewayTestImpl.class.getName(), expected);
        EmailGateway emailGateway = emailGatewayLookup.getEmailGateway(GuiceEmailGatewayTestImpl.class.getName());
        assertThat(emailGateway).isEqualTo(expected);
    }

    @Test
    void testGatewayLookupWithFakeClassThrowsException() {
        assertThatThrownBy(() -> emailGatewayLookup.getEmailGateway("NotAClass"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    public static class EmailGatewayTestImpl implements EmailGateway {
        @Override
        public void sendEmail(String from, String to, String subject, String message, Map<String, String> options)
                throws NoEmailSentException {
            // do nothing
        }
    }

    public static class GuiceEmailGatewayTestImpl extends EmailGatewayTestImpl {

        public GuiceEmailGatewayTestImpl(String variable) {
            // do nothing
        }

    }
}
