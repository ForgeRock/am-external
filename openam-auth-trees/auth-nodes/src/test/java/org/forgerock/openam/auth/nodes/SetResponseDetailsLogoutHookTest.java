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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.SetResponseDetailsLogoutHook.LOGOUT_DETAILS_ATTRIBUTE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonValue;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SetResponseDetailsLogoutHookTest {

    @Mock
    private HttpServletRequest request;

    private static final Map<String, String> EXISTING_LOGOUT_DETAILS = Map.of("key0", "value0");
    private static final Map<String, String> NEW_LOGOUT_DETAILS = Map.of("key1", "value1");
    private static final Map<String, String> MODIFIED_LOGOUT_DETAILS = Map.of("key0", "value1");

    @Test
    void shouldSetLogoutDetailsAttribute() {
        JsonValue data = new JsonValue(EXISTING_LOGOUT_DETAILS);
        SetResponseDetailsLogoutHook setResponseDetailsLogoutHook =
                new SetResponseDetailsLogoutHook(Optional.of(request), data);
        setResponseDetailsLogoutHook.onLogout();
        verify(request).setAttribute(LOGOUT_DETAILS_ATTRIBUTE, EXISTING_LOGOUT_DETAILS);
    }

    @Test
    void shouldAddDataToExistingLogoutDetailsAttribute() {
        given(request.getAttribute(LOGOUT_DETAILS_ATTRIBUTE)).willReturn(EXISTING_LOGOUT_DETAILS);
        JsonValue data = new JsonValue(NEW_LOGOUT_DETAILS);
        SetResponseDetailsLogoutHook setResponseDetailsLogoutHook =
                new SetResponseDetailsLogoutHook(Optional.of(request), data);
        setResponseDetailsLogoutHook.onLogout();
        Map<String, String> combinedLogoutDetails = new HashMap<>();
        combinedLogoutDetails.putAll(EXISTING_LOGOUT_DETAILS);
        combinedLogoutDetails.putAll(NEW_LOGOUT_DETAILS);
        verify(request).setAttribute(LOGOUT_DETAILS_ATTRIBUTE, combinedLogoutDetails);
    }

    @Test
    void shouldOverrideExistingLogoutDetailsAttribute() {
        given(request.getAttribute(LOGOUT_DETAILS_ATTRIBUTE)).willReturn(EXISTING_LOGOUT_DETAILS);
        JsonValue data = new JsonValue(MODIFIED_LOGOUT_DETAILS);
        SetResponseDetailsLogoutHook setResponseDetailsLogoutHook =
                new SetResponseDetailsLogoutHook(Optional.of(request), data);
        setResponseDetailsLogoutHook.onLogout();
        verify(request).setAttribute(LOGOUT_DETAILS_ATTRIBUTE, MODIFIED_LOGOUT_DETAILS);
    }
}
