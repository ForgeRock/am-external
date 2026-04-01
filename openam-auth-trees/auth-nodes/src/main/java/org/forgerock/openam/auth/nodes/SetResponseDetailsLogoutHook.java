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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.logouthook.api.LogoutHook;

import com.google.inject.assistedinject.Assisted;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This LogoutHook implementation class copies logout detail set by SetLogoutDetailsNode into the request.
 * Later on this information is put by LogoutActionHandler into the response body.
 */
public class SetResponseDetailsLogoutHook implements LogoutHook {

    private final Optional<HttpServletRequest> request;
    private final JsonValue data;

    /**
     * Construct RedirectLogoutHook object.
     *
     * @param request The Http request.
     * @param data The JsonValue object containing logout details.
     */
    @Inject
    public SetResponseDetailsLogoutHook(@Assisted Optional<HttpServletRequest> request, @Assisted JsonValue data) {
        this.request = request;
        this.data = data;
    }

    @Override
    public void onLogout() {
        request.ifPresent(request -> {
            Map<String, Object> newLogoutDetails = data.asMap();
            if (newLogoutDetails != null && !newLogoutDetails.isEmpty()) {
                Map<String, Object> logoutDetails = new HashMap<>();
                Map<String, Object> existingLogoutDetails =
                        (Map<String, Object>) request.getAttribute(LOGOUT_DETAILS_ATTRIBUTE);
                if (existingLogoutDetails != null) {
                    logoutDetails.putAll(existingLogoutDetails);
                    logoutDetails.putAll(newLogoutDetails);
                } else {
                    logoutDetails = newLogoutDetails;
                }
                request.setAttribute(LOGOUT_DETAILS_ATTRIBUTE, logoutDetails);
            }
        });
    }
}
