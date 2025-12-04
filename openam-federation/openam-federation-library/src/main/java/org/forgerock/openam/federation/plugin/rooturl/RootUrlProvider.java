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
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.federation.plugin.rooturl;

import jakarta.servlet.http.HttpServletRequest;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * Interface used for getting a context's root url.
 */
public interface RootUrlProvider {

    /**
     * Gets the AM instance root url, with the context path included.
     * @param realm   the realm.
     * @param request the HttpServletRequest request.
     * @return the root url.
     * @throws SAML2Exception on failing to lookup the realm.
     */
    String getRootURL(String realm, HttpServletRequest request) throws SAML2Exception;

}
