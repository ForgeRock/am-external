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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.authentication.spi.AuthLoginException;

public class SMSGatewayLookupTest {

    private Map<String, SMSGateway> smsGatewayMap;
    private SMSGatewayLookup smsGatewayLookup;

    @BeforeEach
    void setup() {
        smsGatewayMap = new HashMap<>();
        smsGatewayLookup = new SMSGatewayLookup(smsGatewayMap);
    }

    @Test
    void testGatewayLookupWithNoGuiceBindings() throws ClassNotFoundException, InstantiationException,
                                                                          IllegalAccessException {
        SMSGateway smsGateway = smsGatewayLookup.getSmsGateway(SMSGatewayTestImpl.class.getName());
        assertThat(smsGateway).isInstanceOf(SMSGatewayTestImpl.class);
    }

    @Test
    void testGatewayLookupWithGuiceBindings() throws ClassNotFoundException, InstantiationException,
                                                                      IllegalAccessException {
        GuiceSMSGatewayTestImpl expected = new GuiceSMSGatewayTestImpl("unused");
        smsGatewayMap.put(GuiceSMSGatewayTestImpl.class.getName(), expected);
        SMSGateway smsGateway = smsGatewayLookup.getSmsGateway(GuiceSMSGatewayTestImpl.class.getName());
        assertThat(smsGateway).isEqualTo(expected);
    }

    @Test
    void testGatewayLookupWithFakeClassThrowsException() {
        assertThatThrownBy(() -> smsGatewayLookup.getSmsGateway("NotAClass"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    public static class SMSGatewayTestImpl implements SMSGateway {

        @Override
        public void sendSMSMessage(String from, String to, String subject, String message, String code, Map options)
                throws AuthLoginException {
            // do nothing
        }

        @Override
        public void sendEmail(String from, String to, String subject, String message, String code, Map options)
                throws AuthLoginException {
            // do nothing
        }
    }

    public static class GuiceSMSGatewayTestImpl extends SMSGatewayTestImpl {

        public GuiceSMSGatewayTestImpl(String variable) {
            // do nothing
        }

    }

}
