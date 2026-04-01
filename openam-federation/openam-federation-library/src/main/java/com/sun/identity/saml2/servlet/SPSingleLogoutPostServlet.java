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
package com.sun.identity.saml2.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.sae.api.SecureAttrs;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.CacheObject;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.SPSingleLogout;
import com.sun.identity.saml2.protocol.LogoutRequest;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Single log out (SLO) servlet.
 */
@Singleton
public class SPSingleLogoutPostServlet extends AbstractSAMLHttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SPSingleLogoutPostServlet.class);

    void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        try {
            // Retrieves the LogoutRequest or LogoutResponse
            //Retrieves :
            //- RelayState - the target URL on successful Single Logout
            //- SAMLRequest - the LogoutRequest
            //OR
            //- SAMLResponse - the LogoutResponse

            String relayState = request.getParameter(SAML2Constants.RELAY_STATE);
            if (relayState != null) {
                CacheObject tmpRs =
                        (CacheObject) SPCache.relayStateHash.remove(relayState);
                if ((tmpRs != null)) {
                    relayState = (String) tmpRs.getObject();
                }
            }
            if (!ESAPI.validator().isValidInput("HTTP Query String: " + relayState, relayState, "HTTPQueryString", 2000,
                    true)) {
                relayState = null;
            }
            String samlResponse = request.getParameter(SAML2Constants.SAML_RESPONSE);
            if (samlResponse != null) {
                try {
                    /**
                     * Gets and processes the Single <code>LogoutResponse</code> from IDP,
                     * destroys the local session, checks response's issuer
                     * and inResponseTo.
                     *
                     * @param request the HttpServletRequest.
                     * @param response the HttpServletResponse.
                     * @param samlResponse <code>LogoutResponse</code> in the
                     *          XML string format.
                     * @param relayState the target URL on successful
                     * <code>LogoutResponse</code>.
                     * @throws SAML2Exception if error processing
                     *          <code>LogoutResponse</code>.
                     */
                    Map<String, String> infoMap =
                            SPSingleLogout.processLogoutResponse(request, response, samlResponse, relayState);
                    String inRes = infoMap.get("inResponseTo");
                    LogoutRequest origLogoutRequest = (LogoutRequest)
                            IDPCache.proxySPLogoutReqCache.get(inRes);
                    if (origLogoutRequest != null && !origLogoutRequest.equals("")) {
                        IDPCache.proxySPLogoutReqCache.remove(inRes);
                        IDPProxyUtil.sendProxyLogoutResponse(response, request,
                                origLogoutRequest.getID(), infoMap,
                                origLogoutRequest.getIssuer().getValue(),
                                SAML2Constants.HTTP_POST);
                        return;
                    }
                } catch (SAML2Exception sse) {
                    logger.error("Error processing LogoutResponse :", sse);
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "LogoutResponseProcessingError",
                            SAML2Utils.bundle.getString("LogoutResponseProcessingError") +
                                    " " + sse.getMessage());
                    return;
                } catch (Exception e) {
                    logger.error("Error processing LogoutResponse ", e);
                    SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                            "LogoutResponseProcessingError",
                            SAML2Utils.bundle.getString("LogoutResponseProcessingError") +
                                    " " + e.getMessage());
                    return;
                }

                boolean isRelayStateURLValid = false;
                if (!SPCache.isFedlet) {
                    isRelayStateURLValid = relayState != null && !relayState.isEmpty() &&
                            SAML2Utils.isRelayStateURLValid(request, relayState, SAML2Constants.SP_ROLE, true);
                } else {
                    SAML2MetaManager manager = new SAML2MetaManager();
                    String metaAlias = null;
                    List<String> spMetaAliases = manager.getAllHostedServiceProviderMetaAliases("/");
                    if (spMetaAliases != null && !spMetaAliases.isEmpty()) {
                        // get first one
                        metaAlias = spMetaAliases.get(0);
                    }
                    String requestUrl = request.getRequestURL().toString();
                    isRelayStateURLValid = relayState != null && !relayState.isEmpty() &&
                            SAML2Utils.isRelayStateURLValid(metaAlias, relayState, SAML2Constants.SP_ROLE, true,
                                    requestUrl);
                }
                if (isRelayStateURLValid) {
                    try {
                        response.sendRedirect(relayState);
                    } catch (java.io.IOException ioe) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(
                                    "Exception when redirecting to " +
                                            relayState, ioe);
                        }
                    }
                } else {
                    request.getRequestDispatcher(Constants.SP_SLO_SUCCESS).forward(request, response);
                }
            } else {
                String samlRequest = request.getParameter(SAML2Constants.SAML_REQUEST);
                if (samlRequest != null) {
                    // Logout SP app via SAE first. App is obligated to redirect back
                    // to complete this SLO request.
                    if (!SPCache.isFedlet) {
                        if (processSAELogout(request, response)) {
                            return;
                        }
                    }

                    try {
                        /**
                         * Gets and processes the Single <code>LogoutRequest</code> from
                         * IDP.
                         *
                         * @param request the HttpServletRequest.
                         * @param response the HttpServletResponse.
                         * @param samlRequest <code>LogoutRequest</code> in the
                         *          XML string format.
                         * @param relayState the target URL on successful
                         * <code>LogoutRequest</code>.
                         * @throws SAML2Exception if error processing
                         *          <code>LogoutRequest</code>.
                         */
                        SPSingleLogout.processLogoutRequest(request, response, new PrintWriter(response.getWriter(), true),
                                samlRequest, relayState);
                    } catch (SAML2Exception sse) {
                        logger.error("Error processing LogoutRequest :", sse);
                        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                                "LogoutRequestProcessingError",
                                SAML2Utils.bundle.getString("LogoutRequestProcessingError")
                                        + " " + sse.getMessage());
                        return;
                    } catch (Exception e) {
                        logger.error("Error processing LogoutRequest ", e);
                        SAMLUtils.sendError(request, response, response.SC_BAD_REQUEST,
                                "LogoutRequestProcessingError",
                                SAML2Utils.bundle.getString("LogoutRequestProcessingError")
                                        + " " + e.getMessage());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    boolean processSAELogout(HttpServletRequest request, HttpServletResponse response) {
        String saeData = request.getParameter(SecureAttrs.SAE_PARAM_APPRETURN);
        if (saeData != null) { // App returned back.
            return false;
        }

        try {
            String metaAlias =
                    SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());
            String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            SAML2MetaManager mm = SAML2Utils.getSAML2MetaManager();
            String entityId = mm.getEntityByMetaAlias(metaAlias);
            SPSSOConfigElement spConfig = mm.getSPSSOConfig(realm, entityId);
            String appSLOUrlStr = null;
            if (spConfig != null) {
                appSLOUrlStr = SAML2Utils.getAttributeValueFromSPSSOConfig(
                        spConfig, SAML2Constants.SAE_SP_LOGOUT_URL);
            }
            if (appSLOUrlStr == null) {
                logger.debug(
                        "spSLOPOST:SAE:appSLOUrl not configured.");
                return false;
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                        "spSLOPOST:SAE:processing App SLO" + appSLOUrlStr);
            }
            StringBuffer appSLOUrl = new StringBuffer(appSLOUrlStr);
            Map hp = SAML2Utils.getSAEAttrs(
                    realm, entityId, SAML2Constants.SP_ROLE, appSLOUrlStr);
            if (hp == null) {
                logger.error(
                        "spSLOPOST:SAE:processing App SLO: getSAEAttrs returned null");
                return false;
            }
            String cryptoType = (String) hp.get(SecureAttrs.SAE_CRYPTO_TYPE);
            String secret = null;
            String encSecret = null;
            String encAlg = (String) hp.get(
                    SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG);
            String encStrength = (String) hp.get(
                    SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH);
            if (SecureAttrs.SAE_CRYPTO_TYPE_SYM.equals(cryptoType)) {
                // Shared secret between FM-IDP and IDPApp
                secret = (String) hp.get(SecureAttrs.SAE_CONFIG_SHARED_SECRET);
                encSecret = secret;
            } else {
                // IDPApp's public key
                secret = (String) hp.get(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS);
                encSecret =
                        (String) hp.get(SecureAttrs.SAE_CONFIG_PUBLIC_KEY_ALIAS);
            }
            if (secret == null || secret.length() == 0) {
                logger.error(
                        "spSLOPOST:SAE:processing App SLO:getSAEAttrs no secret/key");
                return false;
            }
            if (encAlg == null) {
                encSecret = null;
            }

            String returnURL = request.getRequestURL() +
                    "?" + request.getQueryString() + "&" +
                    SecureAttrs.SAE_PARAM_APPRETURN + "=true";
            HashMap map = new HashMap();
            map.put(SecureAttrs.SAE_PARAM_CMD, SecureAttrs.SAE_CMD_LOGOUT);
            map.put(SecureAttrs.SAE_PARAM_APPSLORETURNURL, returnURL);
            String saInstanceName = cryptoType + "_" + encAlg + "_" + encStrength;
            SecureAttrs sa = SecureAttrs.getInstance(saInstanceName);
            if (sa == null) {
                Properties prop = new Properties();
                prop.setProperty(SecureAttrs.SAE_CONFIG_CERT_CLASS,
                        "com.sun.identity.sae.api.FMCerts");
                if (encAlg != null) {
                    prop.setProperty(
                            SecureAttrs.SAE_CONFIG_DATA_ENCRYPTION_ALG, encAlg);
                }

                if (encStrength != null) {
                    prop.setProperty(
                            SecureAttrs.SAE_CONFIG_ENCRYPTION_KEY_STRENGTH, encStrength);
                }
                SecureAttrs.init(saInstanceName, cryptoType, prop);
                sa = SecureAttrs.getInstance(saInstanceName);
            }

            if (sa == null) {
                logger.error(
                        "spSLOPOST:SAE:processing App SLO:null SecureAttrs instance");
                return false;
            }
            String encodedString = sa.getEncodedString(map, secret, encSecret);
            if (encodedString != null) {
                if (appSLOUrl.indexOf("?") > 0) {
                    appSLOUrl.append("&").append(SecureAttrs.SAE_PARAM_DATA)
                            .append("=").append(encodedString);
                } else {
                    appSLOUrl.append("?").append(SecureAttrs.SAE_PARAM_DATA)
                            .append("=").append(encodedString);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("spSLOPOST:SAE:about to redirect" +
                            appSLOUrl);
                }
                response.sendRedirect(appSLOUrl.toString());
                return true;
            } else {
                logger.error(
                        "spSLOPOST:SAE:SecureAttrs.getEncodedStr failed");
            }
        } catch (Exception ex) {
            logger.error("spSLOPOST:SAE:SecureAttrs.Fatal:", ex);
        }
        return false;
    }
}

