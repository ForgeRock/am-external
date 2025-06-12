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
package org.forgerock.openam.auth.nodes.jwt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.FileNotFoundException;

import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.openam.auth.nodes.AuthKeyFactory;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

public class PersistentJwtProviderTest extends GuiceTestCase {

    private PersistentJwtProvider persistentJwtProvider;

    @Mock
    AuthKeyFactory mockAuthKeyFactory;

    @BeforeMethod
    public void before() {
        initMocks(this);
        persistentJwtProvider = new PersistentJwtProvider(mockAuthKeyFactory, new JwtReconstruction());
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtBadKeySSOException() throws Exception {
        when(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).thenThrow(new SSOException("test exception"));
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtBadKeySMSException() throws Exception {
        when(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).thenThrow(new SMSException("test exception"));
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtBadKeyFileNotFoundException() throws Exception {
        when(mockAuthKeyFactory.getPrivateAuthKey(any(), any())).thenThrow(new FileNotFoundException("test exception"));
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }

    @Test(expectedExceptions = InvalidPersistentJwtException.class)
    public void testGetValidDecryptedJwtNulls() throws Exception {
        persistentJwtProvider.getValidDecryptedJwt(null, null, null);
    }
}