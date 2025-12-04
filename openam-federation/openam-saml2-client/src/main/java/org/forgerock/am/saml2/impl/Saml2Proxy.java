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
 * Copyright 2015-2025 Ping Identity Corporation.
 */
package org.forgerock.am.saml2.impl;

import static com.sun.identity.saml2.common.SAML2Constants.RELAY_STATE;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_CODE_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.RESPONSE_KEY;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * Called on the way back into the SAML2 Authentication Module
 * by the saml2AuthAssertionConsumer jsp.
 */
public final class Saml2Proxy {

    /**
     * Default message to display when we can't even forward an error back to the Authentication Module.
     */
    static final String DEFAULT_ERROR_MESSAGE = "Request not valid!";
    /**
     * Constant to indicate the Proxy received a bad request.
     */
    private static final String BAD_REQUEST = "badRequest";
    /**
     * Constant to indicate that the proxy was unable to locate the meta manager.
     */
    private static final String MISSING_META_MANAGER = "missingMeta";
    /**
     * Constant to indicate that the proxy was unable to extract data from the meta manager.
     */
    private static final String META_DATA_ERROR = "metaError";
    /**
     * Constant to indicate that the proxy was unable to extract the SAML response.
     */
    private static final String SAML_GET_RESPONSE_ERROR = "samlGet";
    /**
     * Constant to indicate that the proxy was unable to verify the response.
     */
    private static final String SAML_VERIFY_RESPONSE_ERROR = "samlVerify";
    /**
     * Constant to indicate that SAML failover was not enabled as required.
     */
    private static final String SAML_FAILOVER_DISABLED_ERROR = "samlFailover";

    private static final Logger DEBUG = LoggerFactory.getLogger(Saml2Proxy.class);

    private static final String EMPTY_STRING = "";
    /**
     * Private, utilities-class constructor.
     */
    private Saml2Proxy() {
    }

    private static String generateKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Processes the SAML response for the SAML2 authentication module and then directs the user back to the
     * authentication process differently for XUI and non-XUI cases.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @throws IOException If there was an IO error while retrieving the SAML response.
     * @throws SAML2Exception If the RelayState is invalid.
     */
    public static void processSamlResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAML2Exception {
        String url = getUrl(request, response);
        response.sendRedirect(url);
    }

    private static String getUrl(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAML2Exception {
        if (request == null || response == null) {
            DEBUG.error("SAML2Proxy: Null request or response");
            return getUrlWithError(request, BAD_REQUEST);
        }

        try {
            SAMLUtils.checkHTTPContentLength(request);
        } catch (ServletException se) {
            DEBUG.error("SAML2Proxy: content length too large");
            return getUrlWithError(request, BAD_REQUEST);
        }

        FSUtils.setLbCookieIfNecessary(request, response);

        // get entity id and orgName
        String requestURL = request.getRequestURL().toString();
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(requestURL);
        SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
        String hostEntityId;

        if (metaManager == null) {
            DEBUG.error("SAML2Proxy: Unable to obtain metaManager");
            return getUrlWithError(request, MISSING_META_MANAGER);
        }

        try {
            hostEntityId = metaManager.getEntityByMetaAlias(metaAlias);
            if (hostEntityId == null) {
                throw new SAML2MetaException("Caught Instantly");
            }
        } catch (SAML2MetaException sme) {
            DEBUG.warn("SAML2Proxy: unable to find hosted entity with metaAlias: {} Exception: {}", metaAlias,
                    sme.toString());
            return getUrlWithError(request, META_DATA_ERROR);
        }

        String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);

        if (StringUtils.isEmpty(realm)) {
            realm = "/";
        }

        ResponseInfo respInfo;
        try {
            respInfo = SPACSUtils.getResponse(request, response, realm, hostEntityId, metaManager);
        } catch (SAML2Exception se) {
            DEBUG.error("SAML2Proxy: Unable to obtain SAML response", se);
            return getUrlWithError(request, SAML_GET_RESPONSE_ERROR, se.getL10NMessage(request.getLocale()));
        }

        Map smap;
        try {
            // check Response/Assertion and get back a Map of relevant data
            smap = SAML2Utils.verifyResponse(request, response, respInfo.getResponse(), realm, hostEntityId,
                    respInfo.getProfileBinding());
        } catch (SAML2Exception se) {
            DEBUG.error("SAML2Proxy: An error occurred while verifying the SAML response", se);
            return getUrlWithError(request, SAML_VERIFY_RESPONSE_ERROR, se.getL10NMessage(request.getLocale()));
        }
        String key = generateKey();

        //survival time is one hour
        Assertion assertion = (Assertion) smap.get(SAML2Constants.POST_ASSERTION);
        Saml2ResponseData data = new Saml2ResponseData((String) smap.get(SAML2Constants.SESSION_INDEX),
                (Subject) smap.get(SAML2Constants.SUBJECT),
                assertion,
                respInfo,
                (AuthnRequest) smap.get(SAML2Constants.AUTHN_REQUEST));

        if (respInfo.getProfileBinding().equals(SAML2Constants.HTTP_POST)) {
            String assertionId = assertion.getID();
            SPCache.assertionByIDCache.put(assertionId, SAML2Constants.ONETIME);
            try {
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(
                        assertionId,
                        SAML2Constants.ONETIME,
                        ((Long) smap.get(SAML2Constants.NOTONORAFTER)).longValue() / 1000);
            } catch (SAML2TokenRepositoryException se) {
                DEBUG.error("There was a problem saving the assertionID to the SAML2 Token Repository for assertionID:"
                        + assertionId, se);
            }
        }

        try {
            SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, data);
        } catch (SAML2TokenRepositoryException e) {
            DEBUG.error("An error occurred while persisting the SAML token", e);
            return getUrlWithError(request, SAML_FAILOVER_DISABLED_ERROR);
        }

        String relayState = getRelayState(request, requestURL, metaManager, hostEntityId, realm, respInfo);
        Optional<String> localAuthUrl = getLocalAuthURL(realm, hostEntityId, metaManager);
        return getUrlWithKey(request, key, relayState, localAuthUrl);
    }

    private static String getRelayState(HttpServletRequest request, String requestURL, SAML2MetaManager metaManager,
            String hostEntityId, String realm, ResponseInfo respInfo) throws SAML2Exception {
        String relayStateParameter;
        if (isNotBlank(respInfo.getRelayState())) {
            relayStateParameter = respInfo.getRelayState();
        } else {
            relayStateParameter = request.getParameter(RELAY_STATE);
        }
        String relayStateUrl = SPACSUtils.getRelayState(relayStateParameter, realm, hostEntityId, metaManager);
        SAML2Utils.validateRelayStateURL(realm, hostEntityId, relayStateUrl, SAML2Constants.SP_ROLE, requestURL);
        return relayStateUrl;
    }

    /**
     * Auto-submits the passed-in parameters to the authentication module, as taken from
     * a known cookie location in the request.
     *
     * @param req The request.
     * @param key The key to reference.
     * @param localAuthUrl The configured local authentication url, may be {@link Optional#empty()}.
     * @param relayState The configured relay state.
     * @return An HTML form to render to the user's user-agent.
     */
    static String getUrlWithKey(final HttpServletRequest req, final String key, final String relayState,
            Optional<String> localAuthUrl) {
        final StringBuilder value = localAuthUrl
                .map(StringBuilder::new)
                .map(sb -> appendQueryParams(sb, getLocationRequestParams(req)))
                .orElseGet(() -> getLocationValue(req));
        if (value == null) {
            throw new IllegalStateException(DEFAULT_ERROR_MESSAGE);
        }
        return encodeMessage(value, key, relayState);
    }

    /**
     * Creates a post form for forwarding error response information to the SAML2 authentication module.
     *
     * @param req The request.
     * @param errorType The Error type that has occurred.
     * @return An HTML form to render to the user's user-agent.
     */
    static String getUrlWithError(HttpServletRequest req, String errorType) {
        return getUrlWithError(req, errorType, DEFAULT_ERROR_MESSAGE);
    }

    /**
     * Creates a post form for forwarding error response information to the SAML2 authentication module.
     *
     * @param req The request.
     * @param errorType The Error type that has occurred.
     * @param messageDetail a text description of the message.
     * @return An HTML form to render to the user's user-agent.
     */
    static String getUrlWithError(HttpServletRequest req, String errorType, String messageDetail) {
        StringBuilder value = getLocationValue(req);

        if (value == null) {
            throw new IllegalStateException(DEFAULT_ERROR_MESSAGE);
        }

        value.append("&").append(ERROR_PARAM_KEY).append("=").append(true)
            .append("&").append(ERROR_CODE_PARAM_KEY).append("=")
            .append(urlEncodeQueryParameterNameOrValue(errorType))
            .append("&").append(ERROR_MESSAGE_PARAM_KEY).append("=")
            .append(urlEncodeQueryParameterNameOrValue(messageDetail));

        return value.toString();
    }

    private static String encodeMessage(StringBuilder value, String key, String relayState) {
        if (value.toString().contains("?")) {
            value.append("&");
        } else {
            value.append("?");
        }

        value.append(RESPONSE_KEY).append("=").append(urlEncodeQueryParameterNameOrValue(key))
                .append("&").append(ERROR_PARAM_KEY).append("=").append(false);

        if (isNotBlank(relayState)) {
            value.append("&").append(RELAY_STATE).append("=").append(relayState);
        }
        return value.toString();
    }

    private static StringBuilder appendQueryParams(StringBuilder sb, String paramString) {
        if (sb.indexOf("?") >= 0) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append(paramString);
        return sb;
    }

    private static String getLocationRequestParams(HttpServletRequest req) {
        StringBuilder location = getLocationValue(req);
        if (location != null) {
            int paramsStart = location.indexOf("?");
            if (paramsStart >= 0) {
                return location.substring(paramsStart + 1);
            }
        }
        return EMPTY_STRING;
    }

    private static StringBuilder getLocationValue(HttpServletRequest req) {
        String value = CookieUtils.getCookieValueFromReq(req, AM_LOCATION_COOKIE);

        if (isBlank(value)) {
            return null;
        }

        return new StringBuilder(Base64url.decodeToString(value));
    }

    private static Optional<String> getLocalAuthURL(String realm, String hostEntityId, SAML2MetaManager metaManager) {
        final String localAuthUrl = SPACSUtils.getAttributeValueFromSPSSOConfig(realm, hostEntityId, metaManager,
                SAML2Constants.LOCAL_AUTH_URL);
        return isBlank(localAuthUrl) ? Optional.empty() : Optional.of(localAuthUrl);
    }
}
