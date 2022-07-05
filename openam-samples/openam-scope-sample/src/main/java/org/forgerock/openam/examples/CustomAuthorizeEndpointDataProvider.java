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
 * Copyright 2021-2022 ForgeRock AS.
 */

package org.forgerock.openam.examples;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.plugins.AuthorizeEndpointDataProvider;

/**
 * Custom implementation of the Authorize Endpoint Data Provider
 * plugin interface {@link org.forgerock.oauth2.core.plugins.AuthorizeEndpointDataProvider}
 *
 * <li>
 * The {@code provide} method returns hard coded additional value.
 * </li>
 *
 */
public class CustomAuthorizeEndpointDataProvider implements AuthorizeEndpointDataProvider {

    @Override
    public Map<String, String> provide(Map<String, Token> tokens, OAuth2Request request) {
        Map<String, String> customMapping = new HashMap<String, String>();
        customMapping.put("additional", "field");
        return customMapping;
    }
}
