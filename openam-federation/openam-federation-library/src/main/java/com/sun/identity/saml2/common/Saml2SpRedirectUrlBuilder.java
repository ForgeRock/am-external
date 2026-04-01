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
package com.sun.identity.saml2.common;

import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.forgerock.http.util.Uris;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for building redirect URLs for SAML2 Service Provider authentication flows.
 * Handles logic for hosted SP tree configuration and local login URL construction.
 */
public class Saml2SpRedirectUrlBuilder {

    private static final Logger logger = LoggerFactory.getLogger(Saml2SpRedirectUrlBuilder.class);

    /**
     * Builds the redirect URL for SAML2 Service Provider authentication.
     * Depending on configuration, may redirect to a hosted SP tree or local login URL.
     *
     * @param realm        The realm name.
     * @param hostEntityId The entity ID of the hosted Service Provider.
     * @param metaManager  The SAML2 metadata manager.
     * @param respInfo     The response information for the SAML2 request.
     * @param request      The HTTP servlet request.
     * @param relayState   The relay state to be included in the redirect.
     * @return The constructed redirect URL for authentication.
     */
    public String getRedirectUrl(String realm,
            String hostEntityId,
            SAML2MetaManager metaManager,
            ResponseInfo respInfo,
            HttpServletRequest request,
            String relayState) {

        String treeName =
                SPACSUtils.getConfiguredServiceForHostedSP(realm, hostEntityId, metaManager,
                        SAML2Constants.REDIRECT_TREE_NAME);
        String localLoginUrl =
                SPACSUtils.getAttributeValueFromSPSSOConfig(realm, hostEntityId, metaManager,
                        SAML2Constants.LOCAL_AUTH_URL);

        if (!StringUtils.isEmptyOrEmptySelection(treeName) && StringUtils.isEmpty(localLoginUrl)) {
            return buildHostedSpTreeUrl(realm, treeName, request, relayState, respInfo);
        }
        return getLocalLoginUrl(realm, hostEntityId, metaManager, respInfo, request, relayState);
    }

    /**
     * Builds the local login URL for SAML2 authentication, including response and relay state parameters.
     *
     * @param realm        The realm/organization name.
     * @param hostEntityId The entity ID of the hosted Service Provider.
     * @param metaManager  The SAML2 metadata manager.
     * @param respInfo     The response information for the SAML2 request.
     * @param request      The HTTP servlet request.
     * @param relayState   The relay state to be included in the redirect.
     * @return The constructed local login URL.
     */
    private static String getLocalLoginUrl(
            String realm,
            String hostEntityId,
            SAML2MetaManager metaManager,
            ResponseInfo respInfo,
            HttpServletRequest request,
            String relayState
    ) {
        String localLoginUrl = SPACSUtils.prepareForLocalLogin(
                realm, hostEntityId, metaManager, respInfo, request.getRequestURL().toString());
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(localLoginUrl);
        if (localLoginUrl.contains("?")) {
            urlBuilder.append("&goto=");
        } else {
            urlBuilder.append("?goto=");
        }
        urlBuilder.append(getGotoUrl(request, relayState, respInfo));
        if (logger.isDebugEnabled()) {
            logger.debug("Local login url: " + urlBuilder);
        }
        return urlBuilder.toString();
    }

    /**
     * Builds the URL for redirecting the user to the authentication process based on the hosted SP tree name.
     *
     * @param realm      The realm/organization name.
     * @param treeName   The name of the authentication tree.
     * @param request    The HTTP servlet request.
     * @param relayState The relay state to be included in the redirect.
     * @param respInfo   The response information for the SAML2 request.
     * @return The constructed URL for the hosted SP tree.
     */
    private String buildHostedSpTreeUrl(
            String realm,
            String treeName,
            HttpServletRequest request,
            String relayState,
            ResponseInfo respInfo) {
        try {
            URIBuilder uriBuilder = new URIBuilder(request.getContextPath());
            if (isNotBlank(realm)) {
                uriBuilder.addParameter("realm", realm);
            }
            var baseUrl = uriBuilder.addParameter("authIndexType", "service")
                    .addParameter("authIndexValue", treeName)
                    .build().toString();

            return baseUrl + "&goto=" + getGotoUrl(request, relayState, respInfo);

        } catch (URISyntaxException e) {
            logger.error("Error building hosted SP tree URL", e);
            throw new IllegalStateException("Unable to build hosted SP tree URL", e);
        }
    }

    private static String getGotoUrl(HttpServletRequest request, String relayState, ResponseInfo respInfo) {
        String gotoURL = request.getRequestURL().toString() + "?resID="
                + Uris.urlEncodeQueryParameterNameOrValue(respInfo.getResponse().getID());
        if (isNotBlank(relayState)) {
            gotoURL += "&RelayState=" + Uris.urlEncodeQueryParameterNameOrValue(relayState);
        }
        return Uris.urlEncodeQueryParameterNameOrValue(gotoURL);
    }
}
