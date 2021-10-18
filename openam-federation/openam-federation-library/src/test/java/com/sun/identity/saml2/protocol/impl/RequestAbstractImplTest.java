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
 * Copyright 2021 ForgeRock AS.
 */

package com.sun.identity.saml2.protocol.impl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.sun.identity.saml2.common.SAML2Exception;

public class RequestAbstractImplTest {

    @Test(expectedExceptions = SAML2Exception.class)
    public void shouldRejectInvalidIDValues() throws Exception {
        // Given
        RequestAbstractImpl request = mock(RequestAbstractImpl.class);
        willCallRealMethod().given(request).validateID(anyString());

        // When
        request.validateID("x\" oops=\"bad\"");
    }
}