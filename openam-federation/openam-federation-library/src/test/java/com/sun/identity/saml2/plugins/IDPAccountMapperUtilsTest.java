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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.sun.identity.saml2.common.SOAPCommunicator;
import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.http.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.profile.IDPSSOUtil;

@ExtendWith(MockitoExtension.class)
public class IDPAccountMapperUtilsTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(SOAPCommunicator.class, mock(SOAPCommunicator.class)).build();

    @Mock
    private Session session;

    private static final String ENTITY = "foobar";

    private IDPAccountMapperUtils idpAccountMapperUtils; // class under test

    @BeforeEach
    void setup() {
        idpAccountMapperUtils = new IDPAccountMapperUtils();
    }

    @Test
    void getNameIdFromSessionReturnsNullIfParamsNull() {
        // when
        String nameId = idpAccountMapperUtils.getNameIdFromSession(null, null);

        // then
        assertThat(nameId).isNull();
    }

    @Test
    void getNameIdFromSessionReturnsNullIfNoSessionIndexFound() {
        try (MockedStatic<IDPSSOUtil> idpssoUtilMockedStatic = Mockito.mockStatic(IDPSSOUtil.class)) {
            // given
            idpssoUtilMockedStatic.when(() -> IDPSSOUtil.getSessionIndex(session)).thenReturn(null);

            // when
            String nameId = idpAccountMapperUtils.getNameIdFromSession(session, ENTITY);

            // then
            assertThat(nameId).isNull();
        }
    }
}
